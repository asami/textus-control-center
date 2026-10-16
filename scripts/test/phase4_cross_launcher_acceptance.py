# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC4-S7A: real canonical Launcher JVMs sharing production safe evidence."""

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import zipfile


PROJECT = Path(__file__).resolve().parents[2]
OUTPUT = PROJECT / "target" / "phase-4-cross-launcher"
STARTUP_SECONDS = 90
OBSERVATION_SECONDS = 30
READER_SECONDS = 15
NORMAL_STOP_SECONDS = 15
TERM_SECONDS = 5
KILL_SECONDS = 5
PORT_SECONDS = 10
POLL_SECONDS = 0.5
TOKEN_ENV = "PHASE4_CROSS_LAUNCHER_REGISTRATION_TOKEN"
LIST_FIELDS = {
    "launcherKind", "instanceId", "target", "artifactId", "executionMode",
    "subsystemName", "subsystemVersion", "runtimeVersion", "startedAt",
    "lastSeenAt", "stoppedAt",
}


class AcceptanceFailure(Exception):
    pass


class PendingObservation(AcceptanceFailure):
    pass


class Arguments(argparse.ArgumentParser):
    def error(self, message):
        raise AcceptanceFailure(message)


def require(condition, message):
    if not condition:
        raise AcceptanceFailure(message)


def stage(clause, message):
    print(f"{clause} {message}", flush=True)


def timestamp():
    return datetime.now(timezone.utc).isoformat()


def atomic_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent,
                                     prefix=path.name + ".", suffix=".tmp",
                                     delete=False) as output:
        temporary = Path(output.name)
        json.dump(value, output, indent=2)
        output.write("\n")
    os.replace(temporary, path)


def absolute(value, label):
    path = Path(value)
    require(path.is_absolute(), f"Given prerequisite: {label} must be absolute: {value}")
    return path.resolve()


def regular(path):
    require(path.is_file() and path.stat().st_size > 0,
            f"Given prerequisite: missing or empty native input {path}")
    return path


def classpath(path):
    entries = [Path(part.strip()) for line in regular(path).read_text().splitlines()
               for part in line.split(os.pathsep) if part.strip()]
    require(entries and all(entry.is_absolute() and entry.exists() for entry in entries),
            f"Given prerequisite: classpath requires existing absolute native outputs: {path}")
    return list(dict.fromkeys(entry.resolve() for entry in entries))


def contains_class(entries, marker, owned_root=None):
    for entry in entries:
        if owned_root is not None and not entry.is_relative_to(owned_root):
            continue
        if entry.is_dir() and (entry / marker).is_file():
            return True
        if entry.is_file() and zipfile.is_zipfile(entry):
            with zipfile.ZipFile(entry) as archive:
                if marker in archive.namelist():
                    return True
    return False


def coordinate(value, label):
    require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._+-]*", value) is not None,
            f"Given prerequisite: {label} must be a concrete coordinate, not a path: {value}")
    require(value not in {"recommended", "latest", "latest-stable", "latest.release",
                          "latest-snapshot", "newest"},
            f"Given prerequisite: {label} cannot be a dynamic selector: {value}")
    return value


def yaml_string(value):
    return "'" + str(value).replace("'", "''") + "'"


def parse_arguments(argv):
    parser = Arguments(description=__doc__)
    for name in ("java", "cncf-launcher-classpath-file", "textus-launcher-classpath-file",
                 "runtime-root", "component-root", "car-file", "artifact-id",
                 "artifact-version", "runtime-version", "result-receipt"):
        parser.add_argument("--" + name, required=True)
    return parser.parse_args(argv)


def receipt_selector(argv):
    parser = Arguments(add_help=False)
    parser.add_argument("--result-receipt", required=True)
    selected, _ = parser.parse_known_args(argv)
    path = absolute(selected.result_receipt, "--result-receipt")
    require(path.is_relative_to(OUTPUT.resolve()) and path != OUTPUT.resolve(),
            f"Given prerequisite: --result-receipt must be beneath {OUTPUT}")
    require(path.suffix == ".json" and not path.is_dir(),
            "Given prerequisite: --result-receipt must select a JSON file")
    return path


class Case:
    def __init__(self, receipt):
        OUTPUT.mkdir(parents=True, exist_ok=True)
        self.directory = Path(tempfile.mkdtemp(prefix="case-", dir=OUTPUT))
        self.receipt_path = receipt
        self.children = []
        self.reservations = []
        self.ports = []
        self.errors = []
        self.server_ids = None
        self.receipt = {
            "readiness": False, "clients": False, "cleanup": False,
            "processCompletion": "incomplete", "runDirectory": str(self.directory),
            "success": False, "error": None, "cleanupDiagnostics": [],
        }
        self.home = self.directory / "home"
        self.temporary = self.directory / "tmp"
        self.token = os.urandom(24).hex()
        self.environment = os.environ.copy()
        for key in (
            "CNCF_LAUNCHER_DEV_DIR", "TEXTUS_LAUNCHER_DEV_DIR",
            "CNCF_LAUNCHER_DEV_DELEGATED", "TEXTUS_LAUNCHER_DEV_DELEGATED",
            "CNCF_LAUNCHER_ARGS_FILE", "TEXTUS_LAUNCHER_ARGS_FILE",
            "CNCF_RUNTIME_DEV_DIR", "TEXTUS_RUNTIME_DEV_DIR",
            "CNCF_USE_DEVELOPMENT", "TEXTUS_USE_DEVELOPMENT",
            "CNCF_RUNTIME_VERSION", "CNCF_VERSION",
        ):
            self.environment.pop(key, None)
        self.environment[TOKEN_ENV] = self.token

    def save_receipt(self):
        atomic_json(self.receipt_path, self.receipt)

    def save_processes(self):
        atomic_json(self.directory / "processes.json", [
            {**record, "returncode": process.poll()}
            for process, record in self.children
        ])

    def prepare(self, args):
        self.args = args
        self.java = regular(absolute(args.java, "--java"))
        require(os.access(self.java, os.X_OK), f"Given prerequisite: Java is not executable: {self.java}")
        self.runtime = absolute(args.runtime_root, "--runtime-root")
        self.component = absolute(args.component_root, "--component-root")
        for root, marker in (
            (self.runtime, "src/main/scala/org/goldenport/cncf/CncfMain.scala"),
            (self.component, "src/main/scala/org/simplemodeling/textus/supervisor/impl/ComponentFactory.scala"),
        ):
            regular(root / "build.sbt")
            regular(root / marker)
        self.artifact = coordinate(args.artifact_id, "--artifact-id")
        self.version = coordinate(args.artifact_version, "--artifact-version")
        self.runtime_version = coordinate(args.runtime_version, "--runtime-version")
        build_version = re.search(r'(?:ThisBuild\s*/\s*)?version\s*:=\s*"([^"\n]+)"',
                                  (self.runtime / "build.sbt").read_text())
        require(build_version is not None and build_version.group(1) == self.runtime_version,
                "Given prerequisite: --runtime-version must equal the actual CNCF build version")
        self.cncf_classpath = classpath(absolute(args.cncf_launcher_classpath_file,
                                               "--cncf-launcher-classpath-file"))
        self.textus_classpath = classpath(absolute(args.textus_launcher_classpath_file,
                                                 "--textus-launcher-classpath-file"))
        for entries, marker in (
            (self.cncf_classpath, "cncf/launcher/CncfLauncherMain.class"),
            (self.textus_classpath, "textus/launcher/TextusMain.class"),
        ):
            require(contains_class(entries, marker), f"Given prerequisite: actual compiled {marker} is missing")
        runtime_classpath = classpath(self.runtime / "target/cncf.d/runtime-classpath.txt")
        require(contains_class(runtime_classpath, "org/goldenport/cncf/CncfMain.class", self.runtime / "target"),
                "Given prerequisite: genuine CNCF own-target CncfMain output is missing")
        regular(self.component / "project.yaml")
        regular(self.component / "target/cncf.d/car-runtime-manifest.json")
        regular(self.component / "target/cncf.d/component-descriptor.json")
        component_classpath = classpath(self.component / "target/cncf.d/runtime-classpath.txt")
        require(contains_class(component_classpath,
                               "org/simplemodeling/textus/supervisor/impl/ComponentFactory.class",
                               self.component / "target"),
                "Given prerequisite: genuine Supervisor own-target ComponentFactory output is missing")
        abi = json.loads(regular(self.component / "src/main/car/abi-manifest.json").read_text())
        expected_component = {"namespace": "org.simplemodeling.textus", "id": "Supervisor",
                              "version": self.version}
        require(abi.get("component") == expected_component,
                "Given prerequisite: Supervisor source ABI coordinate disagrees with --artifact-version")
        car = regular(absolute(args.car_file, "--car-file"))
        require(car.is_relative_to(self.component / "target") and car.suffix == ".car",
                "Given prerequisite: --car-file must be a native CAR beneath the component target")
        with zipfile.ZipFile(car) as archive:
            packaged_abi = json.loads(archive.read("abi-manifest.json"))
            manifest = json.loads(archive.read("car-runtime-manifest.json"))
            require(packaged_abi.get("component") == expected_component,
                    "Given prerequisite: built CAR ABI disagrees with the explicit Supervisor coordinate")
            require(manifest.get("car", {}).get("name") == self.artifact and
                    manifest.get("car", {}).get("version") == self.version,
                    "Given prerequisite: built CAR manifest disagrees with explicit artifact coordinates")
        for path in (self.home, self.temporary, self.directory / "local-data"):
            path.mkdir()
        repository = self.directory / "repository" / "car"
        destination = repository / self.artifact / self.version / f"{self.artifact}-{self.version}.car"
        destination.parent.mkdir(parents=True)
        shutil.copyfile(car, destination)
        catalog = self.home / ".cncf/catalog/textus/runtime-catalog.yaml"
        catalog.parent.mkdir(parents=True)
        catalog.write_text(
            "# Explicit local fixture for the supplied native development runtime.\n"
            "schemaVersion: 1\nversions:\n"
            f"  - version: {self.runtime_version}\n"
            f"    module: org.goldenport:goldenport-cncf_3:{self.runtime_version}\n")
        assembly = self.directory / "assembly.yaml"
        assembly.write_text(
            "subsystem: cross-launcher-supervisor\n"
            f"version: {yaml_string(self.version)}\ncomponents:\n"
            "  - namespace: org.simplemodeling.textus\n    id: Supervisor\n"
            f"    version: {yaml_string(self.version)}\n"
            "config:\n  textus.subsystem.user-mode: standalone\n"
            "security:\n  authentication:\n    convention: enabled\n"
            "    fallback_privilege: disabled\n    local_subject:\n"
            "      id: cross-launcher-local-operator\n      roles:\n        - operator\n"
            "      capabilities:\n        - operator\n      security_level: operator\n")
        self.runtime_config = self.directory / "runtime.yaml"
        self.runtime_config.write_text(
            f"textus.assembly.descriptor: {yaml_string(assembly)}\n"
            f"textus.subsystem.file: {yaml_string(assembly)}\n"
            f"textus.local-data.root: {yaml_string(self.directory / 'local-data')}\n")
        self.assembly = assembly
        registration = (
            "textus-control-center:\n  registration:\n    enabled: true\n"
            "    endpoint: http://127.0.0.1:1\n"
            f"    token-env: {TOKEN_ENV}\n    timeout: 100ms\n"
            "    heartbeat-interval: 1s\n    host-label: cross-launcher-case\n")
        self.launcher_config = self.directory / "launcher.yaml"
        self.launcher_config.write_text(
            registration + f"runtime.version: {yaml_string(self.runtime_version)}\n"
            f"runtime.catalog.url: {yaml_string(catalog)}\n"
            f"repositories:\n  car:\n    - {yaml_string(repository)}\n")

    def reserve_ports(self):
        for _ in range(2):
            reservation = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.reservations.append(reservation)
            reservation.bind(("127.0.0.1", 0))
            self.ports.append(reservation.getsockname()[1])
        require(len(set(self.ports)) == 2 and all(port > 0 for port in self.ports),
                "Given prerequisite: two distinct positive loopback ports were not acquired")

    def java_command(self, kind):
        entries, main = ((self.cncf_classpath, "cncf.launcher.CncfLauncherMain") if kind == "cncf"
                         else (self.textus_classpath, "textus.launcher.TextusMain"))
        return [str(self.java), f"-Duser.home={self.home}", f"-Djava.io.tmpdir={self.temporary}",
                "-cp", os.pathsep.join(map(str, entries)), main,
                "--launcher-home", str(self.home), "--config", str(self.launcher_config)]

    def acquire(self, role, argv, cwd):
        outpath, errpath = self.directory / f"{role}.stdout.log", self.directory / f"{role}.stderr.log"
        with outpath.open("wb") as output, errpath.open("wb") as error:
            process = subprocess.Popen(argv, cwd=cwd, env=self.environment, stdin=subprocess.DEVNULL,
                                       stdout=output, stderr=error)
            record = {"role": role, "pid": process.pid, "startedAt": timestamp(),
                      "argv": argv, "cwd": str(cwd), "stdout": str(outpath), "stderr": str(errpath)}
            self.children.append((process, record))
        self.save_processes()
        return process, record

    def start_servers(self):
        cncf_argv = self.java_command("cncf") + [
            "--runtime-dev-dir", str(self.runtime), "--cncf-config", str(self.runtime_config),
            "server", f"--textus.assembly.descriptor={self.assembly}",
            f"--textus.server.port={self.ports[0]}", "--textus.server.host=127.0.0.1"]
        textus_argv = self.java_command("textus") + [
            "--runtime", self.runtime_version, "--runtime-dev-dir", str(self.runtime),
            f"{self.artifact}.car:{self.version}", "server",
            f"--textus.assembly.descriptor={self.assembly}", f"--textus.subsystem.file={self.assembly}",
            f"--textus.local-data.root={self.directory / 'local-data'}",
            f"--textus.server.port={self.ports[1]}", "--textus.server.host=127.0.0.1"]
        self.reservations[0].close()
        self.cncf, record = self.acquire("cncf-server", cncf_argv, self.component)
        atomic_json(self.directory / "server-process.json", {
            "pid": self.cncf.pid, "startedAt": record["startedAt"],
            "baseUrl": f"http://127.0.0.1:{self.ports[0]}",
            "textusBaseUrl": f"http://127.0.0.1:{self.ports[1]}",
        })
        self.reservations[1].close()
        self.textus, _ = self.acquire("textus-server", textus_argv, self.directory)
        require(self.cncf.pid > 0 and self.textus.pid > 0 and self.cncf.pid != self.textus.pid,
                "Then the two actual Launcher JVM acquisitions must have distinct positive PIDs")

    def alive(self, processes):
        for process in processes:
            require(process.poll() is None,
                    f"Then acquired server PID {process.pid} exited early ({process.returncode}); "
                    f"inspect retained server logs at {self.directory}")

    def await_readiness(self):
        deadline = time.monotonic() + STARTUP_SECONDS
        while time.monotonic() < deadline:
            self.alive([self.cncf, self.textus])
            ready = []
            for port in self.ports:
                try:
                    with socket.create_connection(("127.0.0.1", port), timeout=0.25):
                        ready.append(port)
                except OSError:
                    pass
            self.alive([self.cncf, self.textus])
            if len(ready) == 2:
                self.receipt["readiness"] = True
                self.save_receipt()
                return
            time.sleep(POLL_SECONDS)
        raise AcceptanceFailure(f"Then both native servers must become TCP-ready within {STARTUP_SECONDS}s; "
                                f"ports={self.ports}, logs={self.directory}")

    def projection(self, deadline):
        role = f"evidence-{len(self.children):04d}"
        process, record = self.acquire(role, self.java_command("cncf") +
                                       ["launcher", "evidence", "list", "--format", "json"], self.directory)
        try:
            process.wait(timeout=max(0.01, min(READER_SECONDS, deadline - time.monotonic())))
        except subprocess.TimeoutExpired as error:
            raise AcceptanceFailure(f"Then evidence reader must finish within its deadline; logs={record}") from error
        require(process.returncode == 0, f"Then safe-list CLI must succeed ({process.returncode}); logs={record}")
        text = Path(record["stdout"]).read_text()
        require(self.token not in text, "Then safe-list projection must not contain the case credential")
        document = json.loads(text)
        require(isinstance(document, dict) and set(document) == {"schema", "entries"} and
                document["schema"] == "cncf.launcher.evidence-projection.v1",
                f"Then safe-list must return the documented projection; output={record['stdout']}")
        rows = document["entries"]
        require(isinstance(rows, list), "Then safe-list entries must be a list")
        for row in rows:
            require(isinstance(row, dict) and set(row).issubset(LIST_FIELDS),
                    "Then safe-list must exclude development directories, process identities, credentials and arguments")
            require(all(not isinstance(value, (dict, list)) for value in row.values()),
                    "Then safe-list fields must be scalar protocol facts")
        return rows

    def expect_projection(self, rows, stopped):
        if len(rows) != 2:
            raise PendingObservation(f"expected exactly two shared rows, observed {len(rows)}")
        bykind = {row.get("launcherKind"): row for row in rows}
        require(set(bykind) == {"cncf", "textus"}, "Then safe-list must retain exactly both Launcher kinds")
        ids = {kind: row.get("instanceId") for kind, row in bykind.items()}
        require(all(isinstance(value, str) and value for value in ids.values()) and len(set(ids.values())) == 2,
                "Then safe-list must contain two distinct invocation IDs")
        if self.server_ids is not None:
            require(ids == self.server_ids, "Then both writers must preserve their original shared invocation IDs")
        for kind, row in bykind.items():
            require(row.get("executionMode") == ("development" if kind == "cncf" else "artifact"),
                    f"Then {kind} must retain its canonical execution mode")
            require(row.get("target") == ("Supervisor" if kind == "cncf" else self.artifact) and
                    row.get("artifactId") == self.artifact and row.get("runtimeVersion") == self.runtime_version,
                    f"Then {kind} must retain the correct explicit target, artifact and runtime")
            if (row.get("stoppedAt") is not None) != (kind in stopped):
                raise PendingObservation(f"{kind} has not reached expected stopped={kind in stopped}")
            if kind in stopped:
                require(isinstance(row["stoppedAt"], str) and bool(row["stoppedAt"]),
                        f"Then {kind} must retain a normal termination timestamp")
        self.server_ids = ids

    def observe(self, stopped, live, polls=1):
        deadline = time.monotonic() + OBSERVATION_SECONDS
        matched = 0
        last = "no observation"
        while time.monotonic() < deadline:
            self.alive(live)
            rows = self.projection(deadline)
            self.alive(live)
            try:
                self.expect_projection(rows, stopped)
                matched += 1
                if matched >= polls:
                    return
            except PendingObservation as error:
                last = str(error)
                matched = 0
            time.sleep(POLL_SECONDS)
        raise AcceptanceFailure(f"Then shared safe evidence must converge before its deadline: {last}; "
                                f"logs={self.directory}")

    def normal_stop(self, process):
        require(process.poll() is None, f"When stopping PID {process.pid}, the acquired server must still be live")
        process.terminate()
        try:
            process.wait(timeout=NORMAL_STOP_SECONDS)
        except subprocess.TimeoutExpired as error:
            raise AcceptanceFailure(f"Then normal TERM of PID {process.pid} must finish without KILL; "
                                    f"logs={self.directory}") from error
        require(process.returncode in (0, -signal.SIGTERM, 128 + signal.SIGTERM),
                f"Then server PID {process.pid} must terminate normally, observed {process.returncode}")

    def cleanup(self):
        stage("Then", "every acquired child is reaped and selected ports are reusable")
        for reservation in self.reservations:
            try:
                reservation.close()
            except OSError as error:
                self.errors.append(f"reservation close: {error}")
        for process, record in reversed(self.children):
            try:
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=TERM_SECONDS)
                    except subprocess.TimeoutExpired:
                        process.kill()
                process.wait(timeout=KILL_SECONDS)
            except Exception as error:
                self.errors.append(f"cleanup {record['role']} PID {process.pid}: {error}")
        unreaped = [record["role"] for process, record in self.children if process.poll() is None]
        remaining = set(self.ports)
        deadline = time.monotonic() + PORT_SECONDS
        while remaining and time.monotonic() < deadline:
            for port in tuple(remaining):
                try:
                    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
                        probe.bind(("127.0.0.1", port))
                    remaining.remove(port)
                except OSError:
                    pass
            if remaining:
                time.sleep(POLL_SECONDS)
        if unreaped:
            self.errors.append(f"unreaped acquired children: {unreaped}")
        if remaining:
            self.errors.append(f"selected ports not reusable: {sorted(remaining)}")
        self.receipt["cleanupDiagnostics"] = self.errors
        self.receipt["cleanup"] = not self.errors and not unreaped and not remaining
        if self.receipt["cleanup"]:
            self.receipt["processCompletion"] = "terminal"
        self.save_processes()

    def run(self, argv):
        primary = None

        def interrupted(signum, _frame):
            raise AcceptanceFailure(f"Interrupted by signal {signum}; exact acquired handles require cleanup")

        previous = {signum: signal.signal(signum, interrupted) for signum in (signal.SIGTERM, signal.SIGINT)}
        try:
            self.save_receipt()
            stage("Given", f"native prepared runtime, Supervisor and Launchers; retained case {self.directory}")
            self.prepare(parse_arguments(argv))
            self.reserve_ports()
            stage("When", "independent CNCF development and Textus CAR JVMs start with notification unavailable")
            self.start_servers()
            stage("Then", "both actual native server handles remain live and TCP-ready")
            self.await_readiness()
            stage("Then", "three simultaneous safe-list polls preserve both current invocation identities")
            self.observe(set(), [self.cncf, self.textus], polls=3)
            stage("When", "the acquired Textus JVM receives normal TERM")
            self.normal_stop(self.textus)
            stage("Then", "Textus is stopped while CNCF remains current and live")
            self.observe({"textus"}, [self.cncf])
            stage("When", "the acquired CNCF JVM receives normal TERM")
            self.normal_stop(self.cncf)
            stage("Then", "the same two safe-list identities retain both normal stoppedAt facts")
            self.observe({"cncf", "textus"}, [])
            self.receipt["clients"] = True
        except BaseException as error:
            primary = f"{type(error).__name__}: {error}"
            self.receipt["error"] = primary
        finally:
            # A second interrupt is recorded while every acquired handle is attempted.
            for signum in previous:
                signal.signal(signum, lambda signum, _frame: self.errors.append(f"cleanup interrupted by signal {signum}"))
            try:
                self.cleanup()
            except Exception as error:
                self.errors.append(f"cleanup finalization: {type(error).__name__}: {error}")
                self.receipt["cleanup"] = False
                self.receipt["processCompletion"] = "incomplete"
                self.receipt["cleanupDiagnostics"] = self.errors
            self.receipt["success"] = primary is None and all(
                self.receipt[key] for key in ("readiness", "clients", "cleanup"))
            try:
                self.save_receipt()
            finally:
                for signum, handler in previous.items():
                    signal.signal(signum, handler)
        if not self.receipt["success"]:
            raise AcceptanceFailure(f"{primary or 'Cleanup did not complete'}; "
                                    f"secondary cleanup diagnostics={self.errors}; retained case={self.directory}")
        stage("Then", f"cross-Launcher acceptance passed; receipt={self.receipt_path}; case={self.directory}")


def main():
    try:
        receipt = receipt_selector(sys.argv[1:])
        Case(receipt).run(sys.argv[1:])
        return 0
    except Exception as error:
        print(f"Then acceptance failed: {error}", file=sys.stderr, flush=True)
        return 1


if __name__ == "__main__":
    sys.exit(main())
