# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC4-S8A: packaged generated Operations through both production Supervisors."""

import argparse
from datetime import datetime, timezone
import http.client
import io
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
from urllib.parse import urlencode
import uuid
import zipfile


PROJECT = Path(__file__).resolve().parents[2]
OUTPUT = PROJECT / "target/phase-4-packaged-operations"
VERSION = "0.1.1-SNAPSHOT"
RUNTIME_VERSION = "0.5.3-SNAPSHOT"
STARTUP_SECONDS = 90
CASE_SECONDS = 420
REQUEST_SECONDS = 25
OBSERVATION_SECONDS = 35
TERM_SECONDS = 8
KILL_SECONDS = 5
PORT_SECONDS = 10
MAXIMUM_BYTES = 1024 * 1024
TOKEN_ENV = "PHASE4_PACKAGED_REGISTRATION_TOKEN"
QUERY = {"textus.format": "json", "textus.output.shape": "data",
         "textus.command.execution-mode": "sync"}


class AcceptanceFailure(Exception):
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


def interrupt_request(signum, frame):
    raise AcceptanceFailure("Then HTTP request must finish before its total deadline")


def atomic_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent,
                                     prefix=path.name + ".", delete=False) as stream:
        temporary = Path(stream.name)
        json.dump(value, stream, indent=2)
        stream.write("\n")
    os.replace(temporary, path)


class RunSignals:
    def __init__(self, result):
        self.result, self.case, self.first = result, None, None
        self.cleaning = False
        self.previous = {}

    def install(self):
        for signum in (signal.SIGINT, signal.SIGTERM):
            self.previous[signum] = signal.signal(signum, self.interrupt)

    def interrupt(self, signum, frame):
        # Never throw asynchronously across acquisition, finally entry or cleanup.
        if self.first is None:
            self.first = signum
        self.capture(AcceptanceFailure(f"Primary run interrupted by signal {self.first}"))

    def capture(self, error):
        if self.result["error"] is None:
            self.result["error"] = f"{type(error).__name__}: {error}"
        self.result["success"] = False
        if self.case is not None:
            if self.case.result["error"] is None:
                self.case.result["error"] = self.result["error"]
            self.case.result["success"] = False

    def check(self):
        if self.first is not None:
            raise AcceptanceFailure(f"Primary run interrupted by signal {self.first}")

    def save(self, path, value):
        observed = self.first
        atomic_json(path, value)
        if observed != self.first:
            # A first interruption during serialization must reach the receipt too.
            atomic_json(path, value)

    def restore(self):
        for signum, handler in self.previous.items():
            signal.signal(signum, handler)


def absolute(value):
    path = Path(value)
    require(path.is_absolute(), f"Given an absolute input path is required: {value}")
    return path.resolve()


def regular(path):
    require(path.is_file() and path.stat().st_size > 0,
            f"Given a nonempty native regular file is required: {path}")
    return path


def classpath(path):
    entries = [Path(part.strip()) for line in regular(path).read_text().splitlines()
               for part in line.split(os.pathsep) if part.strip()]
    require(entries and all(entry.is_absolute() and entry.exists() for entry in entries),
            f"Given existing absolute native classpath entries are required: {path}")
    return list(dict.fromkeys(entry.resolve() for entry in entries))


def contains_class(entries, marker):
    for entry in entries:
        if entry.is_dir() and (entry / marker).is_file():
            return True
        if entry.is_file() and zipfile.is_zipfile(entry):
            with zipfile.ZipFile(entry) as archive:
                if marker in archive.namelist():
                    return True
    return False


def yaml_value(value):
    return json.dumps(str(value))


def data_record(value):
    # CNCF's explicit data shape is the record itself; its default names are snake_case.
    require(isinstance(value, dict) and value and not {"error", "conclusion"}.intersection(value),
            "Then the Operation must return a nonempty data record, not an error/envelope")
    return value


def nonempty(record, name):
    value = record.get(name)
    require(isinstance(value, str) and bool(value.strip()), f"Then {name} must be nonempty: {record}")
    return value


def safe_audit(value):
    if isinstance(value, dict):
        require(not {"operatorSubjectId", "operator_subject_id", "idempotencyKey",
                     "idempotency_key"}.intersection(value),
                "Then safe lifecycle audit must omit operator identity and idempotency key")
        for item in value.values():
            safe_audit(item)
    elif isinstance(value, list):
        for item in value:
            safe_audit(item)


def parse_arguments(argv):
    parser = Arguments(description=__doc__)
    for name in ("java", "cncf-launcher-classpath-file", "runtime-root",
                 "control-center-car", "supervisor-car", "development-fixtures-root", "component-version",
                 "runtime-version", "result-receipt"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--inject-failure-after-start", action="store_true")
    return parser.parse_args(argv)


def receipt_selector(argv):
    parser = Arguments(add_help=False)
    parser.add_argument("--result-receipt", required=True)
    selected, _ = parser.parse_known_args(argv)
    path = absolute(selected.result_receipt)
    require(path.is_relative_to(OUTPUT.resolve()), f"Given receipt must be beneath {OUTPUT}")
    relative = path.relative_to(OUTPUT.resolve())
    require(len(relative.parts) >= 2 and relative.parts[0].startswith("case-") and
            path.suffix == ".json" and not path.is_dir(),
            "Given receipt must select case-*/<name>.json beneath the output root")
    return path


class NativeInputs:
    def __init__(self, args):
        require(args.component_version == VERSION and args.runtime_version == RUNTIME_VERSION,
                f"Given concrete component {VERSION} and runtime {RUNTIME_VERSION} are required")
        self.java = regular(absolute(args.java))
        require(os.access(self.java, os.X_OK), "Given Java must be executable")
        self.runtime = absolute(args.runtime_root)
        regular(self.runtime / "src/main/scala/org/goldenport/cncf/CncfMain.scala")
        build = regular(self.runtime / "build.sbt").read_text()
        version = re.search(r'(?:ThisBuild\s*/\s*)?version\s*:=\s*"([^"\n]+)"', build)
        require(version is not None and version.group(1) == RUNTIME_VERSION,
                "Given runtime version must match the actual native CNCF build")
        self.entries = classpath(absolute(args.cncf_launcher_classpath_file))
        require(contains_class(self.entries, "cncf/launcher/CncfLauncherMain.class"),
                "Given actual compiled CNCF Launcher is required")
        runtimeentries = classpath(self.runtime / "target/cncf.d/runtime-classpath.txt")
        require(contains_class([entry for entry in runtimeentries
                                if entry.is_relative_to(self.runtime / "target")],
                               "org/goldenport/cncf/CncfMain.class"),
                "Given genuine CNCF own-target runtime classes are required")
        self.cars = {}
        for artifact, component, supplied, package in (
            ("textus-control-center", "ControlCenter", args.control_center_car, "controlcenter"),
            ("textus-supervisor", "Supervisor", args.supervisor_car, "supervisor"),
        ):
            path = regular(absolute(supplied))
            require(path.suffix == ".car" and "target" in path.parts,
                    f"Given native target CAR is required: {path}")
            with zipfile.ZipFile(path) as archive:
                abi = json.loads(archive.read("abi-manifest.json"))
                descriptor = json.loads(archive.read("component-descriptor.json"))
                manifest = json.loads(archive.read("car-runtime-manifest.json"))
                identity = {"namespace": "org.simplemodeling.textus", "id": component, "version": VERSION}
                require(abi.get("format") == "cozy.car.abi-manifest.v2" and
                        abi.get("component") == identity and descriptor.get("component") == identity,
                        f"Given {artifact} native ABI/descriptor identity must agree")
                require(manifest.get("schemaVersion") == "cncf.car-runtime-manifest.v1" and
                        manifest.get("car") == {"name": artifact, "version": VERSION,
                                                "component": "org.simplemodeling.textus." + component} and
                        RUNTIME_VERSION in manifest.get("runtime", {}).get("cncf", {}).get("tested", []),
                        f"Given {artifact} native runtime manifest must declare the selected tested runtime")
                with zipfile.ZipFile(io.BytesIO(archive.read("component/main.jar"))) as classes:
                    require(f"org/simplemodeling/textus/{package}/impl/ComponentFactory.class" in classes.namelist(),
                            f"Given {artifact} must contain genuine compiled ComponentFactory")
            self.cars[artifact] = path
        self.fixtures = {}
        fixturesroot = absolute(args.development_fixtures_root)
        require(fixturesroot.is_relative_to(OUTPUT.resolve()) and
                fixturesroot.name.startswith("case-fixtures-") and fixturesroot.is_dir(),
                f"Given a fresh prepared case-fixtures-* directory beneath {OUTPUT} is required")
        for placement in ("embedded", "external"):
            fixture = (fixturesroot / placement / "textus-supervisor").resolve()
            require(fixture.is_relative_to(fixturesroot) and fixture.is_dir(),
                    f"Given a private prepared {placement}/textus-supervisor directory is required")
            project = regular(fixture / "project.yaml").read_text(encoding="utf-8")
            declarations = re.findall(r'^[ \t]*textus\.server\.default-port[ \t]*:.*$', project, re.MULTILINE)
            require(len(declarations) == 1,
                    f"Given exactly one configured textus.server.default-port is required: {fixture}")
            configured = re.fullmatch(r'''[ \t]+textus\.server\.default-port:[ \t]*(["'])([0-9]+)\1[ \t]*''',
                                      declarations[0])
            require(configured is not None and 1024 <= int(configured.group(2)) <= 65535,
                    f"Given the fixture port must be a quoted string in 1024..65535: {fixture}")
            abi = json.loads(regular(fixture / "src/main/car/abi-manifest.json").read_text(encoding="utf-8"))
            descriptor = json.loads(regular(fixture / "target/cncf.d/component-descriptor.json").read_text(encoding="utf-8"))
            manifest = json.loads(regular(fixture / "target/cncf.d/car-runtime-manifest.json").read_text(encoding="utf-8"))
            identity = {"namespace": "org.simplemodeling.textus", "id": "Supervisor", "version": VERSION}
            require(abi.get("format") == "cozy.car.abi-manifest.v2" and
                    abi.get("component") == identity and descriptor.get("schemaVersion") == 3 and
                    descriptor.get("component") == identity,
                    f"Given the prepared fixture must have matching native Supervisor ABI/descriptor identity: {fixture}")
            require(manifest.get("schemaVersion") == "cncf.car-development-runtime-manifest.v2" and
                    manifest.get("sourceKind") == "development-directory" and
                    manifest.get("car") == {"name": "textus-supervisor", "version": VERSION,
                                            "component": "Supervisor"} and
                    RUNTIME_VERSION in manifest.get("runtime", {}).get("cncf", {}).get("tested", []),
                    f"Given the prepared fixture must declare the native development identity and tested runtime: {fixture}")
            entries = classpath(fixture / "target/cncf.d/runtime-classpath.txt")
            ownentries = [entry for entry in entries if entry.is_dir() and
                          entry.is_relative_to(fixture / "target")]
            require(contains_class(ownentries, "org/simplemodeling/textus/supervisor/impl/ComponentFactory.class"),
                    f"Given genuine Supervisor classes in the fixture's own target directory are required: {fixture}")
            # Production admission still validates native evidence integrity and compatibility.
            self.fixtures[placement] = (fixture, int(configured.group(2)))
        require(self.fixtures["embedded"][1] != self.fixtures["external"][1],
                "Given distinct parent-prepared target ports are required for both placements")


class Case:
    def __init__(self, directory, placement, native, signals):
        self.directory, self.placement, self.native = directory, placement, native
        self.signals = signals
        directory.mkdir(mode=0o700)
        self.children, self.reservations, self.ports, self.observed = [], [], [], []
        self.diagnostics = []
        self.started = False
        self.excluded = False
        self.ready = False
        self.deadline = time.monotonic() + CASE_SECONDS
        self.result = {"placement": placement, "runDirectory": str(directory),
                       "readiness": False, "clients": False, "cleanup": False,
                       "processCompletion": "incomplete", "success": False,
                       "error": None, "cleanupDiagnostics": self.diagnostics}
        self.environment = os.environ.copy()
        for key in ("CNCF_LAUNCHER_DEV_DIR", "TEXTUS_LAUNCHER_DEV_DIR",
                    "CNCF_LAUNCHER_DEV_DELEGATED", "TEXTUS_LAUNCHER_DEV_DELEGATED",
                    "CNCF_LAUNCHER_ARGS_FILE", "TEXTUS_LAUNCHER_ARGS_FILE",
                    "CNCF_RUNTIME_DEV_DIR", "TEXTUS_RUNTIME_DEV_DIR", "CNCF_USE_DEVELOPMENT",
                    "TEXTUS_USE_DEVELOPMENT", "CNCF_RUNTIME_VERSION", "CNCF_VERSION"):
            self.environment.pop(key, None)
        self.token, self.remote_token = uuid.uuid4().hex, uuid.uuid4().hex
        self.environment[TOKEN_ENV] = self.token
        self.http_sequence = 0
        self.save()

    def save(self):
        self.result["observedCases"] = self.observed
        atomic_json(self.directory / "processes.json", [
            {**record, "returncode": process.poll()} for process, record in self.children])
        self.signals.save(self.directory / "result.json", self.result)

    def prepare(self):
        stage("Given", f"{self.placement}: native CARs, a private catalog and acquired loopback ports")
        self.home = self.directory / "user-home"
        self.launcherhome = self.directory / "launcher-home"
        self.cchome = self.directory / "control-center.home"
        self.tmp = self.directory / "tmp"
        self.fixture, configuredport = self.native.fixtures[self.placement]
        self.development = self.fixture.parent
        for path in (self.home, self.launcherhome, self.cchome, self.tmp,
                     self.directory / "local-data", self.directory / "bin"):
            path.mkdir(parents=True, mode=0o700)
        for port in ((0, configuredport, 0) if self.placement == "external" else (0, configuredport)):
            reservation = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.reservations.append(reservation)
            reservation.bind(("127.0.0.1", port))
            self.ports.append(reservation.getsockname()[1])
        self.ccport, self.targetport = self.ports[:2]
        self.copies = {}
        for artifact, source in self.native.cars.items():
            destination = self.directory / "cars" / f"{artifact}-{VERSION}.car"
            destination.parent.mkdir(exist_ok=True)
            shutil.copyfile(source, destination)
            self.copies[artifact] = destination
        # Both producer catalog readers accept this canonical double-quoted form.
        (self.cchome / "catalog.yaml").write_text(
            "schema: textus-control-center.catalog.v1\nrefresh:\n  timeout: 5s\n"
            "development:\n  roots:\n    - id: packaged-case\n"
            f"      path: {yaml_value(self.development)}\n      include-prefix: textus-\n"
            "public-repositories: []\n", encoding="utf-8")
        catalog = self.home / ".cncf/catalog/textus/runtime-catalog.yaml"
        catalog.parent.mkdir(parents=True)
        catalog.write_text(f"schemaVersion: 1\nversions:\n  - version: {RUNTIME_VERSION}\n"
                           f"    module: org.goldenport:goldenport-cncf_3:{RUNTIME_VERSION}\n", encoding="utf-8")
        self.launcherconfig = self.directory / "launcher.yaml"
        self.launcherconfig.write_text(
            f"runtime.version: {RUNTIME_VERSION}\nruntime.catalog.url: {yaml_value(catalog)}\n"
            "textus-control-center:\n  registration:\n    enabled: true\n"
            f"    endpoint: http://127.0.0.1:{self.ccport}/rest/v1/org.simplemodeling.textus.ControlCenter/SubsystemInventory\n    token-env: {TOKEN_ENV}\n"
            "    timeout: 200ms\n    heartbeat-interval: 1s\n    host-label: packaged-case\n", encoding="utf-8")
        self.childassembly = self.assembly("child", False)
        self.ccassembly = self.assembly("control-center", True)
        self.remoteassembly = self.assembly("external-supervisor", False, remote=True)
        self.childconfig = self.configuration("child", self.childassembly)
        self.ccconfig = self.configuration("control-center", self.ccassembly)
        self.remoteconfig = self.configuration("external-supervisor", self.remoteassembly)
        wrapper = self.directory / "bin/cncf"
        childargs = self.java_command() + ["--runtime-dev-dir", str(self.native.runtime),
                                         "--cncf-config", str(self.childconfig)]
        additions = [f"--component-file={self.copies['textus-supervisor']}",
                     f"--textus.assembly.descriptor={self.childassembly}",
                     f"--textus.subsystem.file={self.childassembly}", "--textus.server.host=127.0.0.1"]
        wrapper.write_text("#!/bin/sh\nexec " + shlex.join(childargs) +
                           ' "$@" ' + shlex.join(additions) + "\n", encoding="utf-8")
        wrapper.chmod(0o700)
        self.environment["PATH"] = str(wrapper.parent) + os.pathsep + self.environment.get("PATH", "")

    def assembly(self, name, controlcenter, remote=False):
        path = self.directory / f"{name}-assembly.yaml"
        components = (["ControlCenter", "Supervisor"] if controlcenter else ["Supervisor"])
        document = f"subsystem: {'textus-supervisor' if name == 'child' else name}\nversion: {VERSION}\ncomponents:\n"
        for component in components:
            document += f"  - namespace: org.simplemodeling.textus\n    id: {component}\n    version: {VERSION}\n"
        if controlcenter:
            document += ("spi:\n  bindings:\n    - socket:\n"
                         "        component: org.simplemodeling.textus.ControlCenter\n"
                         "        contract: supervisor\n      provider: {}\n")
        document += f"config:\n  textus.subsystem.user-mode: {'multi-user' if remote else 'standalone'}\n"
        if controlcenter:
            document += ("  textus.component.org.simplemodeling.textus.control-center.datastores.application.policy: local-default\n"
                         f"  textus-control-center.registration.authentication.token: {yaml_value(self.token)}\n")
            if self.placement == "external":
                document += (f"  textus-supervisor.external.endpoint: http://127.0.0.1:{self.ports[2]}\n"
                             f"  textus-supervisor.external.authentication.token: {yaml_value(self.remote_token)}\n"
                             "  textus-supervisor.external.timeout: PT20S\n")
        if remote:
            document += f"  textus-supervisor.service.authentication.token: {yaml_value(self.remote_token)}\n"
        document += "security:\n  authentication:\n    convention: enabled\n    fallback_privilege: disabled\n"
        if not remote:
            document += ("    local_subject:\n      id: textus-control-center-local-operator\n"
                         "      roles:\n        - operator\n      capabilities:\n        - operator\n"
                         "      security_level: operator\n")
        if remote or controlcenter:
            provider = "textus-supervisor-service" if remote else "textus-control-center-launcher-registration"
            component = "Supervisor" if remote else "ControlCenter"
            document += (f"    providers:\n      - name: {provider}\n"
                         f"        component: org.simplemodeling.textus.{component}\n"
                         "        kind: service\n        enabled: true\n        priority: 100\n"
                         f"        default: {'false' if remote else 'true'}\n")
        path.write_text(document, encoding="utf-8")
        path.chmod(0o600)
        return path

    def configuration(self, name, assembly):
        path = self.directory / f"{name}-runtime.yaml"
        path.write_text(f"textus.assembly.descriptor: {yaml_value(assembly)}\n"
                        f"textus.subsystem.file: {yaml_value(assembly)}\n"
                        f"textus-control-center.home: {yaml_value(self.cchome if name != 'child' else self.directory / 'child.home')}\n"
                        f"textus.local-data.root: {yaml_value(self.directory / 'local-data' / name)}\n", encoding="utf-8")
        return path

    def java_command(self):
        return [str(self.native.java), f"-Duser.home={self.home}", f"-Djava.io.tmpdir={self.tmp}",
                "-cp", os.pathsep.join(map(str, self.native.entries)), "cncf.launcher.CncfLauncherMain",
                "--launcher-home", str(self.launcherhome), "--config", str(self.launcherconfig)]

    def acquire(self, role, argv):
        self.signals.check()
        outpath, errpath = self.directory / f"{role}.stdout.log", self.directory / f"{role}.stderr.log"
        with outpath.open("wb") as output, errpath.open("wb") as error:
            previous = signal.pthread_sigmask(signal.SIG_BLOCK, {signal.SIGINT, signal.SIGTERM})
            try:
                process = subprocess.Popen(argv, cwd=self.directory, env=self.environment, stdin=subprocess.DEVNULL,
                                           stdout=output, stderr=error, start_new_session=True,
                                           preexec_fn=lambda: signal.pthread_sigmask(signal.SIG_SETMASK, previous))
                # setsid establishes this exact acquired child's group, including inherited descendants.
                record = {"role": role, "pid": process.pid, "processGroup": process.pid,
                          "startedAt": timestamp(), "argv": argv, "cwd": str(self.directory),
                          "stdout": str(outpath), "stderr": str(errpath)}
                self.children.append((process, record))
            except BaseException as failure:
                self.signals.capture(failure)  # Preserve acquisition failure before pending signals run.
                raise
            finally:
                signal.pthread_sigmask(signal.SIG_SETMASK, previous)
        self.save()
        self.signals.check()
        return process

    def launch(self, role, index, configuration, assembly, cars):
        self.reservations[index].close()
        argv = self.java_command() + ["--runtime-dev-dir", str(self.native.runtime),
                                     "--cncf-config", str(configuration), "server",
                                     "--component-file=" + ",".join(str(self.copies[car]) for car in cars),
                                     f"--textus.assembly.descriptor={assembly}",
                                     f"--textus.subsystem.file={assembly}",
                                     f"--textus.server.port={self.ports[index]}", "--textus.server.host=127.0.0.1"]
        process = self.acquire(role, argv)
        deadline = time.monotonic() + STARTUP_SECONDS
        while time.monotonic() < deadline:
            self.signals.check()
            require(process.poll() is None, f"Then {role} must remain live; inspect {self.directory}")
            if port_ready(self.ports[index]):
                return
            time.sleep(0.25)
        raise AcceptanceFailure(f"Then {role} must become ready within {STARTUP_SECONDS}s")

    def start_servers(self):
        stage("When", f"{self.placement}: native generated servers start through the real Launcher")
        if self.placement == "external":
            self.launch("external-supervisor", 2, self.remoteconfig, self.remoteassembly, ["textus-supervisor"])
        self.launch("control-center", 0, self.ccconfig, self.ccassembly,
                    ["textus-control-center", "textus-supervisor"])
        record = next(record for _, record in self.children if record["role"] == "control-center")
        baseurl = f"http://127.0.0.1:{self.ccport}"
        supervisorurl = f"http://127.0.0.1:{self.ports[2]}" if self.placement == "external" else baseurl
        atomic_json(self.directory / "server-process.json",
                    {**record, "baseUrl": baseurl, "supervisorBaseUrl": supervisorurl})
        self.signals.result.setdefault("processDirectory", str(self.directory))
        self.reservations[1].close()
        self.ready = True
        self.result["readiness"] = True
        self.save()

    def operation(self, service, operation, parameters=None, denied=False, deadline=None):
        if not self.signals.cleaning:
            self.signals.check()
        remaining = min(self.deadline, deadline if deadline is not None else self.deadline) - time.monotonic()
        require(remaining > 0, "Then the bounded case/client deadline must not expire")
        end = time.monotonic() + min(REQUEST_SECONDS, remaining)
        query = urlencode({**QUERY, **(parameters or {})})
        path = f"/rest/v1/org.simplemodeling.textus.ControlCenter/{service}/{operation}?{query}"
        require(len(path.encode()) <= 8192, "Then the bounded request URL must fit")
        connection = http.client.HTTPConnection("127.0.0.1", self.ccport, timeout=end - time.monotonic())
        headers = {"Accept": "application/json", "Connection": "close"}
        if denied:
            headers["Authorization"] = "Bearer invalid-" + uuid.uuid4().hex
        self.http_sequence += 1
        prefix = self.directory / f"http-{self.http_sequence:04d}"
        previous = signal.signal(signal.SIGALRM, interrupt_request)
        signal.setitimer(signal.ITIMER_REAL, max(0.001, end - time.monotonic()))
        try:
            connection.request("GET", path, headers=headers)
            response = connection.getresponse()
            chunks, size = [], 0
            while True:
                remaining = end - time.monotonic()
                require(remaining > 0, "Then HTTP response must complete before its total deadline")
                if connection.sock is not None:
                    connection.sock.settimeout(remaining)
                chunk = response.read1(min(8192, MAXIMUM_BYTES + 1 - size))
                if not chunk:
                    break
                chunks.append(chunk)
                size += len(chunk)
                require(size <= MAXIMUM_BYTES, "Then HTTP response must fit its byte limit")
            raw = b"".join(chunks)
            prefix.with_suffix(".body").write_bytes(raw)
            atomic_json(prefix.with_suffix(".json"), {"path": path, "status": response.status,
                                                      "body": str(prefix.with_suffix('.body'))})
            require("json" in response.getheader("Content-Type", ""), "Then response must declare JSON")
            body = json.loads(raw)
            if not self.signals.cleaning:
                self.signals.check()
            if denied:
                require(response.status in {401, 403} and isinstance(body, dict) and
                        bool({"error", "conclusion", "message", "status"}.intersection(body)),
                        "Then invalid Bearer must produce structured HTTP 401/403")
                return body
            require(response.status == 200, f"Then generated Operation must succeed: HTTP {response.status}, {prefix}")
            return data_record(body)
        finally:
            signal.setitimer(signal.ITIMER_REAL, 0)
            signal.signal(signal.SIGALRM, previous)
            connection.close()

    def rows(self, service, operation, parameters=None):
        body = self.operation(service, operation, {"offset": 0, "limit": 100, **(parameters or {})})
        totalcount = body.get("total_count")
        require(type(totalcount) is int and totalcount >= 0,
                "Then a data page must have a nonnegative integer total_count")
        if "data" not in body:
            require(totalcount == 0 and type(body.get("offset")) is int and body["offset"] == 0 and
                    type(body.get("limit")) is int and body["limit"] == 100,
                    "Then omitted data must identify the exact empty full-page response")
            rows = []
        else:
            rows = body["data"]
        require(isinstance(rows, list) and all(isinstance(row, dict) for row in rows) and
                totalcount == len(rows), "Then a complete bounded data page is required")
        return rows

    def audit(self):
        rows = self.rows("LifecycleControl", "listLifecycleRequests", {"artifactId": "textus-supervisor"})
        safe_audit(rows)
        return rows

    def detail(self, requestid, state):
        deadline = min(self.deadline, time.monotonic() + OBSERVATION_SECONDS)
        while time.monotonic() < deadline:
            if not self.signals.cleaning:
                self.signals.check()
            value = self.operation("LifecycleControl", "getLifecycleRequest", {"requestId": requestid},
                                   deadline=deadline)
            safe_audit(value)
            require(value.get("request_id") == requestid, "Then detail must preserve request identity")
            observed = value.get("request_state")
            if observed == state:
                return value
            pending = {"queued"} if state == "accepted" else {"queued", "accepted"}
            require(observed in pending, f"Then expected {state}, received {value}")
            time.sleep(min(0.25, max(0, deadline - time.monotonic())))
        raise AcceptanceFailure(f"Then request {requestid} must reach {state}")

    def runtime_observation(self, instanceid):
        require(isinstance(instanceid, str) and bool(instanceid.strip()),
                "Then runtime observation must correlate a nonempty reserved instance ID")
        deadline = min(self.deadline, time.monotonic() + OBSERVATION_SECONDS)
        while time.monotonic() < deadline:
            self.signals.check()
            value = self.operation("CarCatalog", "getManagedCar", {"artifactId": "textus-supervisor"},
                                   deadline=deadline)
            require(value.get("artifact_id") == "textus-supervisor",
                    "Then protected runtime observation must identify the exact target artifact")
            state = value.get("runtime_state")
            require(isinstance(state, str) and state in {"not-ready", "not-running", "starting", "running"},
                    f"Then runtime observation must have an expected nonterminal or running state: {value}")
            if "active_instance_ids" in value:
                activeids = value["active_instance_ids"]
                require(isinstance(activeids, list) and
                        all(isinstance(item, str) and bool(item.strip()) for item in activeids),
                        f"Then active instance IDs must be a list of nonempty strings: {value}")
            else:
                # Native Record.dataAuto omits an empty sequence only during these pending states.
                require(state in {"not-running", "starting"},
                        f"Then runtime observation must supply active instance IDs: {value}")
            if state == "running":
                require(value["active_instance_ids"] == [instanceid],
                        f"Then running projection must contain exactly reserved instance {instanceid}: {value}")
                require(port_ready(self.targetport),
                        "Then the actual target port must be ready with the correlated running projection")
                return value
            time.sleep(min(0.25, max(0, deadline - time.monotonic())))
        raise AcceptanceFailure(f"Then target runtime must reach running for reserved instance {instanceid}")

    def action(self, action, key, expected, source=None):
        parameters = {"artifactId": "textus-supervisor", "idempotencyKey": key, **(source or {})}
        value = self.operation("LifecycleControl", action + "OperationalComponent", parameters)
        safe_audit(value)
        return self.detail(nonempty(value, "request_id"), expected)

    def idempotent(self, action, source, expected):
        before = self.audit()
        key = uuid.uuid4().hex
        stage("When", f"{self.placement}: generated {action} and its exact idempotent retry")
        if action == "start":
            self.started = True  # Cleanup remains obligated even when submit/read fails after acquisition.
        first = self.action(action, key, expected, source)
        for name in ("request_id", "supervisor_id", "instance_id"):
            nonempty(first, name)
        stage("Then", f"{action} reaches {expected}, and its retry preserves correlation and safe audit")
        if expected == "accepted":
            firstprojection = self.runtime_observation(first["instance_id"])
        else:
            await_port_reuse(self.targetport)
        second = self.action(action, key, expected, source)
        require(all(second.get(name) == first[name] for name in ("request_id", "instance_id", "supervisor_id")),
                "Then duplicate action must retain request, instance and supervisor")
        if expected == "accepted":
            secondprojection = self.runtime_observation(second["instance_id"])
        else:
            await_port_reuse(self.targetport)
        after = self.audit()
        require(len(after) == len(before) + 1 and
                {row.get("request_id") for row in after} ==
                {row.get("request_id") for row in before} | {first["request_id"]},
                "Then exact retry must add one audit record only")
        observation = {"action": action, "result": first, "duplicate": second}
        if expected == "accepted":
            observation.update({"runtimeProjection": firstprojection,
                                "duplicateRuntimeProjection": secondprojection})
        self.observed.append(observation)
        self.save()
        return first

    def management(self, operation):
        return self.operation("OperationalManagement", operation + "OperationalComponent",
                              {"artifactId": "textus-supervisor"})

    def remove_restore(self):
        stage("Given", f"{self.placement}: an auto-managed DEV target is running")
        stage("When", "Remove excludes the running target")
        removed = self.management("remove")
        self.excluded = True
        stage("Then", "Remove records exclusion while the target keeps running")
        require(removed.get("management_state") == "excluded" and port_ready(self.targetport),
                "Then Remove must exclude without stopping")
        stage("When", "the catalog is refreshed with the target excluded")
        self.operation("CarCatalog", "refreshCarCatalog")
        stage("Then", "refresh preserves exclusion and the running target")
        require(self.management("get").get("management_state") == "excluded" and port_ready(self.targetport),
                "Then refresh must retain exclusion and running target")
        stage("When", "the normal operational management list is requested")
        rows = self.rows("OperationalManagement", "listOperationalComponents")
        stage("Then", "the normal management list omits the excluded target")
        require(all(row.get("artifact_id") != "textus-supervisor" for row in rows),
                "Then normal management list must omit excluded target")
        # Existing policy rejects every new lifecycle action while excluded, including explicit Stop.
        stage("When", "an explicit Stop is requested for the excluded target")
        stopped = self.action("stop", uuid.uuid4().hex, "rejected")
        stage("Then", "excluded Stop is rejected as component-not-managed and the target keeps running")
        require(stopped.get("diagnostic_code") == "component-not-managed" and port_ready(self.targetport),
                "Then explicit excluded Stop must retain current rejection policy and running target")
        stage("When", "Restore readmits the original DEV target")
        restored = self.management("restore")
        self.excluded = False
        stage("Then", "Restore returns the target to auto-managed DEV management")
        require(restored.get("management_state") == "auto-managed", "Then Restore must admit original DEV management")
        self.observed.append({"action": "remove-restore", "removed": removed,
                              "excludedStop": stopped, "restored": restored})

    def exercise(self, inject):
        stage("When", f"{self.placement}: generated refresh/list discover the real case development source")
        self.operation("CarCatalog", "refreshCarCatalog")
        cars = self.rows("CarCatalog", "listManagedCars")
        target = [row for row in cars if row.get("artifact_id") == "textus-supervisor"]
        require(len(target) == 1 and isinstance(target[0].get("sources"), list),
                "Then configured native target must appear in catalog")
        sources = [row for row in target[0]["sources"]
                   if row.get("source_kind") == "DEV" and row.get("refresh_state") == "available"]
        require(len(sources) == 1, "Then exactly one available DEV source must be returned")
        source = {"sourceKind": nonempty(sources[0], "source_kind"),
                  "sourceId": nonempty(sources[0], "source_id")}
        require(self.management("get").get("management_state") == "auto-managed",
                "Then development refresh must create auto-managed operational target")
        stage("Given", "an explicit invalid Bearer suppresses standalone local-operator fallback")
        before = self.audit()
        stage("When", "Start is requested with the invalid Bearer")
        self.operation("LifecycleControl", "startOperationalComponent",
                       {"artifactId": "textus-supervisor", "idempotencyKey": uuid.uuid4().hex, **source}, denied=True)
        stage("Then", "authorization denial leaves lifecycle audit and target server unchanged")
        require(self.audit() == before and not port_ready(self.targetport),
                "Then authorization denial must create neither lifecycle audit nor target server")
        for roundnumber in range(3):
            stage("Given", f"{self.placement}: generated lifecycle-key round {roundnumber + 1}/3")
            first = self.idempotent("start", source, "accepted")
            if inject:
                raise AcceptanceFailure("Injected primary failure after successful live Start")
            if roundnumber == 0:
                self.remove_restore()
            restarted = self.idempotent("restart", source, "accepted")
            require(restarted["instance_id"] != first["instance_id"],
                    "Then Restart must acquire a new opaque instance correlation")
            stopped = self.idempotent("stop", None, "stopped")
            require(stopped["instance_id"] == restarted["instance_id"],
                    "Then Stop must identify the restarted instance")
            self.started = False  # Both Stop calls, port checks, correlation and audit are fully verified.
        self.result["clients"] = True

    def cleanup(self):
        stage("When", f"{self.placement}: bounded protected Stop precedes acquired-group cleanup")
        def attempt(label, action):
            try:
                action()
            except BaseException as error:
                self.diagnostics.append(f"{label}: {type(error).__name__}: {error}")
        if self.ready and self.started:
            self.deadline = time.monotonic() + 2 * REQUEST_SECONDS + OBSERVATION_SECONDS
            if self.excluded:
                attempt("restore before cleanup Stop", lambda: self.management("restore"))
            attempt("protected cleanup Stop", lambda: self.action("stop", uuid.uuid4().hex, "stopped"))
        for reservation in self.reservations:
            attempt("close case port reservation", reservation.close)
        for process, record in reversed(self.children):
            attempt(f"cleanup acquired group {record['processGroup']}", lambda p=process, r=record: stop_group(p, r))
        for port in self.ports:
            attempt(f"reuse case port {port}", lambda p=port: await_port_reuse(p))
        self.result["cleanup"] = not self.diagnostics
        self.result["processCompletion"] = "terminal" if not self.diagnostics else "incomplete"
        self.result["success"] = bool(self.result["clients"] and not self.result["error"] and not self.diagnostics)
        attempt("persist process completion", self.save)
        if self.diagnostics:
            self.result["success"] = self.result["cleanup"] = False
            self.result["processCompletion"] = "incomplete"


def port_ready(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=0.25):
            return True
    except OSError:
        return False


def await_port_reuse(port):
    deadline = time.monotonic() + PORT_SECONDS
    lastfailure = "port release not yet observed"
    while time.monotonic() < deadline:
        if port_ready(port):
            lastfailure = "TCP listener remains ready"
        else:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as candidate:
                try:
                    candidate.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                    candidate.bind(("127.0.0.1", port))
                    return
                except OSError as error:
                    lastfailure = f"loopback bind failed: {error}"
        time.sleep(min(0.1, max(0, deadline - time.monotonic())))
    raise AcceptanceFailure(f"Then case port {port} must have no TCP listener and be reusable: {lastfailure}")


def group_present(group):
    try:
        os.killpg(group, 0)
        return True
    except ProcessLookupError:
        return False


def stop_group(process, record):
    group = record["processGroup"]
    for signum, seconds in ((signal.SIGTERM, TERM_SECONDS), (signal.SIGKILL, KILL_SECONDS)):
        if group_present(group):
            try:
                os.killpg(group, signum)
            except ProcessLookupError:
                pass
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            process.poll()  # Reaps this exact direct child; descendants belong to its acquired group.
            if process.returncode is not None and not group_present(group):
                process.wait(timeout=0)
                record["reapedAt"] = timestamp()
                record["groupAbsent"] = True
                return
            time.sleep(0.1)
    raise AcceptanceFailure(f"acquired direct child/group not terminal: {record['role']} group={group}")


def main(argv):
    try:
        receipt = receipt_selector(argv)
    except BaseException as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    OUTPUT.mkdir(parents=True, exist_ok=True)
    directory = Path(tempfile.mkdtemp(prefix="case-run-", dir=OUTPUT))
    result = {"readiness": False, "clients": False, "cleanup": False,
              "processCompletion": "incomplete", "success": False, "error": None,
              "cleanupDiagnostics": [], "runDirectory": str(directory), "placements": []}
    cases = []
    atomic_json(receipt, result)  # Written before parsing remaining inputs or acquiring any child.
    signals = RunSignals(result)
    signals.install()
    try:
        args = parse_arguments(argv)
        stage("Given", "explicit native Java, Launcher, runtime, two genuine packaged CARs and prepared development fixtures")
        native = NativeInputs(args)
        signals.check()
        for placement in (("embedded",) if args.inject_failure_after_start else ("embedded", "external")):
            signals.check()
            case = Case(directory / f"case-{placement}", placement, native, signals)
            signals.case = case
            cases.append(case)
            result["placements"].append(case.result)
            try:
                signals.check()
                case.prepare()
                signals.check()
                case.start_servers()
                signals.check()
                case.exercise(args.inject_failure_after_start)
                signals.check()
            except BaseException as error:
                signals.capture(error)
                raise
            finally:
                signals.cleaning = True
                case.cleanup()
            signals.case = None
            signals.cleaning = False
            signals.check()
            require(case.result["success"], f"Then {placement} cleanup must succeed: {case.diagnostics}")
    except BaseException as error:
        signals.capture(error)
    finally:
        signals.cleaning = True
        result["readiness"] = bool(cases) and all(case.result["readiness"] for case in cases)
        result["clients"] = len(cases) == 2 and all(case.result["clients"] for case in cases)
        result["cleanupDiagnostics"] = [f"{case.placement}: {message}" for case in cases for message in case.diagnostics]
        result["cleanup"] = all(case.result["cleanup"] for case in cases)
        result["processCompletion"] = "terminal" if result["cleanup"] else "incomplete"
        result["success"] = result["clients"] and result["cleanup"] and result["error"] is None
        observed = signals.first
        try:
            signals.save(directory / "result.json", result)
        finally:
            try:
                signals.save(receipt, result)
                if observed != signals.first:
                    signals.save(directory / "result.json", result)
            finally:
                signals.restore()
    stage("Then", f"{'PASS' if result['success'] else 'FAIL'}; retained run directory {directory}")
    if result["error"]:
        print(result["error"], file=sys.stderr)
    for diagnostic in result["cleanupDiagnostics"]:
        print(f"Cleanup: {diagnostic}", file=sys.stderr)
    return 0 if result["success"] else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
