# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC5-S2C: actual generated Summary, CAR installer, login and owned native UI."""

import argparse
from contextlib import contextmanager
from datetime import datetime, timezone
import http.client
import json
import os, platform, re, shlex, signal, socket, subprocess, sys, threading, time
from pathlib import Path
import plistlib
P = Path(__file__).resolve().parents[2]
D = P / ".codex-workflow/phases/PHASE-5"
K = P / ".codex-workflow/checkouts/goldenport-cncf-source"
JAVA = Path("/Users/asami/.sdkman/candidates/java/17.0.20-amzn/bin/java")
DOMAIN = "org.simplemodeling.textus.controlcenter.menubar"
SUMMARY = "/rest/v1/org-simplemodeling-textus-control-center/operational-summary/get-summary"
DASHBOARD = "/web/textus-control-center"
LIMIT = 65536
SIGNALS = {signal.SIGINT, signal.SIGTERM}
class RouteFailure(Exception): pass
def require(condition, message):
    if not condition:
        raise RouteFailure(message)
def require_unused_preferences(path):
    require(not path.exists() or plistlib.loads(path.read_bytes()) == {},
            "Own preference domain already contains settings")
def stage(clause, text): print(f"{clause} {text}", flush=True)
def now(): return datetime.now(timezone.utc).isoformat()
def atomic(path, value):
    temporary = path.with_name("." + path.name + ".tmp")
    temporary.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")
    os.replace(temporary, path)
def canonical(value, roots, directory=False):
    path = Path(value)
    require(path.is_absolute() and path == path.resolve(strict=True), f"Canonical input required: {value}")
    require(any(path.is_relative_to(root) for root in roots), f"Input outside declared roots: {path}")
    require(path.is_dir() if directory else path.is_file(), f"Wrong input kind: {path}")
    return path
def arguments(argv):
    parser = argparse.ArgumentParser(description=__doc__, allow_abbrev=False, add_help=False)
    for name in ("execution-directory", "request-id", "cncf-classpath", "textus-classpath",
                 "runtime-source", "java", "control-center-car", "supervisor-car"):
        require(argv.count("--" + name) == 1, f"Exactly one --{name} is required")
        parser.add_argument("--" + name, required=True)
    require(argv.count("--http-only") <= 1, "At most one --http-only is allowed")
    parser.add_argument("--http-only", action="store_true")
    return parser.parse_args(argv)
@contextmanager
def masked():
    previous = signal.pthread_sigmask(signal.SIG_BLOCK, SIGNALS)
    try:
        yield previous
    finally:
        signal.pthread_sigmask(signal.SIG_SETMASK, previous)
def summary(value):
    # Generated GetSummary is an OperationResult record; the canonical adapter's data shape has no wrapper.
    require(isinstance(value, dict), "Summary must be the actual JSON object")
    for key in ("componentId", "instanceId", "observedAt"):
        require(isinstance(value.get(key), str) and value[key].strip(), f"Summary requires nonempty {key}")
    datetime.fromisoformat(value["observedAt"].replace("Z", "+00:00"))
    require(value.get("protocolVersion") == 1 and value.get("dashboardUrl") == DASHBOARD
            and value.get("refreshAfterSeconds") == 15, "Summary protocol, Dashboard or refresh contract mismatch")
    control = value["controlCenter"]
    require(control["status"] == "available", "Control Center must be available")
    for key, expected in (("version", "0.1.1-SNAPSHOT"), ("runtimeVersion", "0.5.3-SNAPSHOT")):
        require(control[key] == {"state": "available", "value": expected},
                f"Selected real runtime must expose {key}={expected}; actual={control[key]}")
    uptime = control["uptime"]
    require(uptime.get("state") == "available" and type(uptime.get("seconds")) is int
            and uptime["seconds"] >= 0, f"Selected runtime uptime unavailable or invalid: {uptime}")
    health, runtimes = value["overallHealth"], value["runtimes"]
    require(health["scope"] == "control-center" and isinstance(health["status"], str)
            and bool(health["status"]) and isinstance(health["checks"], list), "Scoped health required")
    require(all(set(check) == {"name", "status"} and all(isinstance(v, str) and v for v in check.values())
                for check in health["checks"]), "Safe actual health checks required")
    counts = [runtimes[key] for key in ("total", "returned", "starting", "running", "stopped", "stale")]
    require(all(type(count) is int and count >= 0 for count in counts)
            and sum(counts[2:]) == counts[0] and 0 <= counts[1] <= min(50, counts[0])
            and type(runtimes["truncated"]) is bool and runtimes["truncated"] == (counts[1] < counts[0])
            and isinstance(runtimes["data"], list) and len(runtimes["data"]) == counts[1]
            and runtimes["health"] == "unavailable", "Actual registry counts/list must be consistent")
    allowed = {"instanceId", "target", "artifactId", "subsystemName", "subsystemVersion", "runtimeVersion",
               "launcherState", "status"}
    require(all(set(row) <= allowed and all(isinstance(row.get(k), str) and row[k]
                for k in ("instanceId", "target", "launcherState", "status")) for row in runtimes["data"]),
            "Runtime entries must contain only the documented safe projection")
    return value
class Case:
    def __init__(self, directory, argv):
        self.directory, self.processdir = directory, directory / "process"
        self.processdir.mkdir(mode=0o700)
        self.total = time.monotonic() + 600
        self.children, self.native, self.reservation = [], None, None
        self.port, self.endpoint, self.executable = None, None, None
        self.cleaning, self.installing, self.discovery, self.login, self.commandlimit = False, False, False, None, None
        self.diagnostics, self.signals = [], []
        self.result = dict(schema="textus-control-center.phase5-native-route.v1", request_id="cc5-native-route-r1",
                           argv=list(sys.argv), cwd=os.getcwd(), executionDirectory=str(directory),
                           processDirectory=str(self.processdir), readiness=False, clients=False, cleanup=False,
                           terminal="incomplete", success=False, observed={}, error=None)
        self.environment = {key: os.environ[key] for key in ("HOME", "USER", "LOGNAME", "TMPDIR",
                            "__CF_USER_TEXT_ENCODING") if key in os.environ}
        self.environment.update(PATH="/usr/bin:/bin:/usr/sbin:/sbin", LC_ALL="C")
        self.previous = {number: signal.signal(number, self.interrupt) for number in SIGNALS}
        self.save()
    def interrupt(self, number, frame):
        self.signals.append(number)
        self.result["success"] = False
        if self.result["error"] is None:
            self.result["error"] = f"Interrupted by signal {number}"
        if not self.cleaning:
            raise RouteFailure(self.result["error"])
    def budget(self, seconds):
        remaining = self.total - (0 if self.cleaning else 80) - time.monotonic()
        require(remaining > 0, "Bounded route deadline expired")
        return min(seconds, remaining)
    def save(self):
        self.result["observed"]["cleanupDiagnostics"] = self.diagnostics
        self.result["observed"]["signals"] = self.signals
        atomic(self.directory / "result.json", self.result)
    def spawn(self, role, argv, private=False, retain=True):
        environment = dict(self.environment)
        if private:
            environment.update(HOME=str(self.directory / "user-home"), TMPDIR=str(self.directory / "tmp"))
        with masked() as previous:
            process = subprocess.Popen(list(map(str, argv)), cwd=self.directory, env=environment,
                                       stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                       start_new_session=True,
                                       preexec_fn=lambda: signal.pthread_sigmask(signal.SIG_SETMASK, previous - SIGNALS))
            record = dict(role=role, pid=process.pid, processGroup=process.pid, startedAt=now(),
                          argv=list(map(str, argv)), cwd=str(self.directory), returncode=None)
            child = dict(process=process, record=record, output=[bytearray(), bytearray()],
                         size=0, overflow=False, readerror=None, terminal=False, threads=[], lock=threading.Lock(), retain=retain)
            self.children.append(child)
            for index, pipe in enumerate((process.stdout, process.stderr)):
                thread = threading.Thread(target=self.drain, args=(child, index, pipe), daemon=True)
                child["threads"].append(thread)
                thread.start()
        if retain:
            atomic(self.processdir / f"{role}.json", record)
        return child
    def drain(self, child, index, pipe):
        try:
            while data := pipe.read(4096):
                with child["lock"]:
                    available = max(0, LIMIT - child["size"])
                    child["output"][index].extend(data[:available])
                    child["size"] += len(data)
                    child["overflow"] |= child["size"] > LIMIT
        except BaseException as error:
            child["readerror"] = str(error)
        finally:
            pipe.close()
    def logs(self, child):
        child["record"]["returncode"] = child["process"].poll()
        child["record"]["outputExceeded64KiB"] = child["overflow"]
        texts = [bytes(data).decode("utf-8", "replace") for data in child["output"]]
        if child["retain"]:
            for suffix, text in zip(("stdout", "stderr"), texts):
                safe = re.sub(r"(?i)(bearer\s+|(?:token|password|secret)\s*[=:]\s*)\S+", r"\1[redacted]", text)
                (self.directory / f"{child['record']['role']}.{suffix}.log").write_text(safe, encoding="utf-8")
            atomic(self.processdir / f"{child['record']['role']}.json", child["record"])
        return texts
    def command(self, role, argv, retain=True, timeout=60):
        end = min(time.monotonic() + self.budget(timeout), self.commandlimit or self.total)
        child = self.spawn(role, argv, retain=retain)
        while child["process"].poll() is None:
            require(time.monotonic() < end and not child["overflow"], f"{role}: command timeout/output bound")
            time.sleep(0.05)
        for thread in child["threads"]:
            thread.join(timeout=self.budget(1))
        output, error = self.logs(child)
        require(not child["overflow"] and child["readerror"] is None and not any(t.is_alive() for t in child["threads"]), f"{role}: output bound/read failure")
        try:
            os.killpg(child["process"].pid, 0)
        except ProcessLookupError:
            child["terminal"] = True
        return child["process"].returncode, output.strip(), error.strip()
    def http(self, name, path, invalid=False):
        def expired(number, frame):
            raise TimeoutError(f"{name}: five-second HTTP deadline")
        previous = signal.signal(signal.SIGALRM, expired)
        signal.setitimer(signal.ITIMER_REAL, self.budget(5))
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=5)
        try:
            headers = {"Accept": "text/html" if path == DASHBOARD else "application/json", "Connection": "close"}
            if invalid:
                headers["Authorization"] = "Bearer cc5-deliberately-invalid"
            connection.request("GET", path, headers=headers)
            response = connection.getresponse()
            body = response.read(1048577)
            require(len(body) <= 1048576, f"{name}: HTTP body exceeds 1 MiB")
            text = body.decode("utf-8", "replace")
            atomic(self.directory / f"{name}.json", dict(status=response.status,
                   contentType=response.getheader("Content-Type"), body=text))
            return response.status, text, response.getheader("Content-Type", "")
        finally:
            connection.close()
            signal.setitimer(signal.ITIMER_REAL, 0)
            signal.signal(signal.SIGALRM, previous)
    def candidates(self):
        code, output, error = self.command("native-candidates", ["/usr/bin/pgrep", "-U", str(os.getuid()), "-f", "^" + re.escape(str(self.executable)) + "( |$)"], retain=False)
        pids = output.splitlines()
        require((code == 1 and not output) or (code == 0 and pids and all(re.fullmatch(r"[1-9][0-9]*", pid) for pid in pids) and len(set(pids)) == len(pids)),
                f"Owned process discovery failed or malformed: exit={code}; output={output!r}; {error}")
        return list(map(int, pids))
    def identity(self, pid):
        facts = {}
        for field in ("comm", "lstart", "command"):
            code, output, error = self.command("native-identity", ["/bin/ps", "-ww", "-p", str(pid), "-o", field + "="], retain=False)
            require(code in (0, 1), f"Exact native PID observation failed: {error}")
            if code == 1 and not output:
                return None
            require(code == 0 and output, "Exact native identity field unavailable")
            facts[field] = output
        return dict(pid=pid, startedAt=facts["lstart"], comm=facts["comm"], command=facts["command"])
    def discover(self):
        end = time.monotonic() + self.budget(10)
        previous, self.commandlimit = self.commandlimit, end
        try:
            while time.monotonic() < end:
                with masked():
                    candidates = self.candidates()
                    require(len(candidates) <= 1, "Ambiguous case-installed native process identity")
                    fact = self.identity(candidates[0]) if candidates else None
                    if fact:
                        words = shlex.split(fact["command"])
                        started = time.mktime(time.strptime(fact["startedAt"], "%a %b %d %H:%M:%S %Y"))
                        valid = (fact["comm"] == str(self.executable) and started >= int(self.launched)
                                 and "--server-url" in words and words[words.index("--server-url") + 1] == self.endpoint
                                 and not {"--register-login", "--unregister-login", "--summary-once"}.intersection(words))
                        if valid:
                            self.native = fact
                            self.nativeacquired = time.monotonic()
                            atomic(self.processdir / "native.json", fact)
                            return
                time.sleep(min(0.1, max(0, end - time.monotonic())))
        finally:
            self.commandlimit = previous
        raise RouteFailure("Detached app identity not acquired within ten seconds")
    def java(self, cp, main, private=False):
        options = [str(JAVA), f"-Djava.io.tmpdir={self.directory / 'tmp'}"]
        if private: options.append(f"-Duser.home={self.directory / 'user-home'}")
        return options + ["-cp", cp, main]
    def exercise(self, args):
        stage("Given", "Darwin arm64, canonical selected inputs and a private native route directory")
        require(platform.system() == "Darwin" and platform.machine() == "arm64", "Actual Darwin arm64 is required")
        require(re.fullmatch(r"cc5-summary-http-r[1-9][0-9]*" if args.http_only else r"cc5-native-route-r[1-9][0-9]*", args.request_id),
                "Unexpected request identity")
        roots = [P, Path("/Users/asami/.ivy2"), Path("/Users/asami/.cache"),
                 Path("/Users/asami/Library/Caches/Coursier"), JAVA.parent.parent]
        for value, expected, directory in ((args.runtime_source, K, True), (args.java, JAVA, False),
                (args.supervisor_car, P / ".codex-workflow/checkouts/textus-supervisor/target/textus-supervisor-0.1.1-SNAPSHOT.car", False)):
            require(canonical(value, roots, directory) == expected, f"Frozen input mismatch: {value}")
        car = canonical(args.control_center_car, [D])
        require(car.name == "textus-control-center-0.1.1-SNAPSHOT.car", "Frozen Control Center CAR basename required")
        classpaths = []
        for name, supplied in (("cncf", args.cncf_classpath), ("textus", args.textus_classpath)):
            path = canonical(supplied, [D])
            require(path == D / f"packaging-inputs-r1/{name}-launcher-runtime-classpath.txt", "Frozen classpath file required")
            entries = path.read_text(encoding="utf-8").strip().replace("\n", os.pathsep).split(os.pathsep)
            require(entries and all(entries), "Nonempty actual exported classpath required")
            classpaths.append(os.pathsep.join(str(canonical(entry, roots, Path(entry).is_dir())) for entry in entries))
        for name in ("user-home", "launcher-home", "control-center.home", "tmp", "local-data", "development"):
            (self.directory / name).mkdir(mode=0o700)
        home = self.directory / "control-center.home"
        (home / "catalog.yaml").write_text("schema: textus-control-center.catalog.v1\nrefresh:\n  timeout: 5s\n"
            f"development:\n  roots:\n    - id: cc5-native\n      path: {json.dumps(str(self.directory / 'development'))}\n"
            "      include-prefix: textus-\npublic-repositories: []\n", encoding="utf-8")
        assembly = self.directory / "assembly.yaml"
        assembly.write_text((P / "conf/cncf/assembly-standalone.yaml").read_text(encoding="utf-8"), encoding="utf-8")
        config = self.directory / "runtime.yaml"
        config.write_text("".join(f"{key}: {json.dumps(str(value))}\n" for key, value in (
            ("textus.assembly.descriptor", assembly), ("textus.subsystem.file", assembly),
            ("textus-control-center.home", home), ("textus.local-data.root", self.directory / "local-data"))), encoding="utf-8")
        catalog = self.directory / "runtime-catalog.yaml"
        catalog.write_text("schemaVersion: 1\nversions:\n  - version: 0.5.3-SNAPSHOT\n"
                           "    module: org.goldenport:goldenport-cncf_3:0.5.3-SNAPSHOT\n", encoding="utf-8")
        launcher = self.directory / "launcher.yaml"
        launcher.write_text(f"runtime.version: 0.5.3-SNAPSHOT\nruntime.catalog.url: {json.dumps(str(catalog))}\n", encoding="utf-8")
        with masked():
            self.reservation = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.reservation.bind(("127.0.0.1", 0))
            self.port = self.reservation.getsockname()[1]
        self.endpoint = f"http://127.0.0.1:{self.port}"
        stage("When", "the real CNCF Launcher starts the normal Control Center and Supervisor assembly")
        argv = self.java(classpaths[0], "cncf.launcher.CncfLauncherMain", True) + ["--launcher-home",
            str(self.directory / "launcher-home"), "--config", str(launcher), "--runtime-dev-dir", str(K),
            "--cncf-config", str(config), "server", f"--component-file={args.control_center_car},{args.supervisor_car}",
            f"--textus.assembly.descriptor={assembly}", f"--textus.subsystem.file={assembly}",
            f"--textus.server.port={self.port}", "--textus.server.host=127.0.0.1"]
        self.reservation.close()
        server = self.spawn("server", argv, private=True)
        server["record"]["endpoint"] = self.endpoint
        atomic(self.processdir / "server.json", server["record"])
        end = time.monotonic() + self.budget(90)
        while True:
            require(server["process"].poll() is None and not server["overflow"] and server["readerror"] is None, "Generated server exited/output failed")
            try:
                status, body, _ = self.http("summary", SUMMARY + "?limit=50")
            except (OSError, http.client.HTTPException):
                status = None
            if status == 200:
                actual = summary(json.loads(body))
                break
            require(status in (None, 404, 502, 503) and time.monotonic() < end, f"Summary readiness failed: HTTP {status}")
            time.sleep(0.25)
        self.result["readiness"] = True
        self.result["observed"]["api"] = actual
        stage("Then", "canonical Summary provides actual identity, metadata, scoped health and registry counts")
        stage("When", "invalid Bearer and out-of-range limit use the same generated endpoint")
        require(self.http("invalid-bearer", SUMMARY + "?limit=50", True)[0] in (401, 403), "Invalid Bearer gained Operator access")
        require(not 200 <= self.http("invalid-limit", SUMMARY + "?limit=101")[0] < 300, "limit=101 was admitted")
        status, body, contenttype = self.http("dashboard", actual["dashboardUrl"])
        require(status == 200 and "text/html" in contenttype.lower() and re.search(r"<(?:!doctype\s+html|html)\b", body, re.I), "Actual Dashboard HTML required")
        self.result["observed"]["dashboard"] = dict(path=actual["dashboardUrl"], status=status)
        stage("Then", "authorization/range failures and same-origin Dashboard HTML are actual transport results")
        if args.http_only:
            self.result["observed"]["validationScope"] = "server-http-only"
            self.result["clients"] = True
            return
        stage("Given", "the first-use own preference domain and narrowly discovered bundle preconditions")
        require_unused_preferences(Path.home() / f"Library/Preferences/{DOMAIN}.plist")
        code, bundles, error = self.command("bundle-precondition", ["/usr/bin/mdfind", f'kMDItemCFBundleIdentifier == "{DOMAIN}"'])
        require(code == 0 and all(Path(path).resolve().is_relative_to(D) for path in bundles.splitlines()), f"Existing bundle outside D or metadata failure: {error}")
        stage("When", "normal CNCF platform stage selects the signed parent-bundled Darwin arm64 app")
        code, output, error = self.command("platform-stage", self.java(classpaths[0], "cncf.launcher.CncfLauncherMain") +
            ["platform", "stage", "--car", args.control_center_car, "--id", DOMAIN, "--os", "darwin", "--arch", "arm64", "--destination", str(self.directory / "stage")])
        require(code == 0, f"Actual platform stage failed: {error}")
        self.result["observed"]["stage"] = json.loads(output)
        self.executable = self.directory / "Applications/Textus Control Center.app/Contents/MacOS/TextusControlCenterMenuBar"
        require(not self.candidates(), "Case-installed executable was already running before launch")
        stage("Then", "the production stage receipt selects actual packaged bytes")
        stage("When", "normal Textus menu-bar install requests real login registration and detached launch")
        self.installing, self.discovery, self.launched = True, True, time.time()
        code, output, error = self.command("menu-bar-install", self.java(classpaths[1], "textus.launcher.TextusMain") +
            ["menu-bar", "install", "--selection", str(self.directory / "stage/selection.json"), "--applications-dir",
             str(self.directory / "Applications"), "--server-url", self.endpoint, "--login", "--launch"])
        installed = json.loads(output)
        self.login = installed
        self.result["observed"]["login"] = installed
        require(code == 0 and installed.get("loginState") == "enabled" and installed.get("launchResult") == "requested"
                and installed.get("installedPath") == str(self.executable.parents[2]) and installed.get("serverUrl") == self.endpoint,
                f"Actual login/launch failed or pending: exit={code}; {installed}; {error}")
        self.discover()
        stage("Then", "enabled login and the exact detached native PID/start/path/endpoint are observed")
        stage("When", "the same installed production client reads Summary and remains live through a refresh interval")
        code, output, error = self.command("summary-client", [self.executable, "--summary-once", "--server-url", self.endpoint])
        require(code == 0, f"Production Summary client failed: {error}")
        client = summary(json.loads(output))
        require(all(client[key] == actual[key] for key in ("componentId", "instanceId", "protocolVersion", "runtimes")), "Native client identity/runtime projection differs")
        self.result["observed"]["summaryClient"] = client
        while time.monotonic() - self.nativeacquired < 16:
            require(self.identity(self.native["pid"]) == self.native, "Owned native process did not remain live")
            time.sleep(min(0.25, self.budget(0.25)))
        stage("Then", "the actual native client matches the API and its owned main survives at least sixteen seconds")
        self.native_ui(actual)
        self.result["clients"] = True
    def native_ui(self, actual):
        stage("When", "PID-scoped Accessibility observes the Menu Bar and presses its actual Open Dashboard button")
        helper = P / "scripts/test/phase5_menu_bar.applescript"
        code, output, error = self.command("ui-observe", ["/usr/bin/osascript", helper, str(self.native["pid"]), "observe"])
        self.record_ui(actual, code, output, error)
        code, output, error = self.command("ui-dashboard", ["/usr/bin/osascript", helper, str(self.native["pid"]), "dashboard"])
        self.record_dashboard(code, output, error)
        stage("Then", "actual UI values match Summary and its production Dashboard button was pressed")
    def record_ui(self, actual, code, output, error):
        self.result["observed"]["ui"] = dict(exit=code, output=output, error=error)
        require(code == 0 and output.startswith("CC5-UI-OBSERVED\n"), f"Actual Menu Bar UI unavailable: {error}")
        texts = [part for line in output.splitlines()[1:] for part in line.split("\t")[1:]]
        for expected in (f"Version: {actual['controlCenter']['version']['value']}",
                         f"Health (control-center): {actual['overallHealth']['status']}",
                         f"Registered runtimes: {actual['runtimes']['total']} total; {actual['runtimes']['returned']} returned"):
            require(expected in texts, f"Actual UI does not show API value: {expected}")
    def record_dashboard(self, code, output, error):
        self.result["observed"]["dashboardAction"] = dict(exit=code, output=output, error=error)
        require(code == 0 and output == "CC5-DASHBOARD-PRESSED\tOpen Dashboard", f"Actual Dashboard AXPress failed: {error}")
    def stop_group(self, child):
        if child["terminal"]:
            return
        process, group = child["process"], child["record"]["processGroup"]
        def signalgroup(number, deadline):
            process.poll()
            try:
                os.killpg(group, number)
            except PermissionError as error:
                try:
                    process.wait(timeout=max(0, deadline - time.monotonic()))
                except subprocess.TimeoutExpired:
                    raise error
                os.killpg(group, number)
        for number, seconds in ((signal.SIGTERM, 5), (signal.SIGKILL, 3)):
            end = time.monotonic() + self.budget(seconds)
            try:
                signalgroup(number, end)
            except ProcessLookupError:
                pass
            while time.monotonic() < end:
                process.poll()
                try:
                    signalgroup(0, end)
                except ProcessLookupError:
                    process.wait(timeout=0)
                    for thread in child["threads"]:
                        thread.join(timeout=self.budget(1))
                    self.logs(child)
                    require(not any(t.is_alive() for t in child["threads"]), "Owned output reader not terminal")
                    require(not child["overflow"] and child["readerror"] is None, "Owned output exceeded bound/read failed")
                    child["terminal"] = True
                    return
                time.sleep(0.05)
        raise RouteFailure(f"Owned process group not terminal: {group}")
    def cleanup(self):
        self.cleaning = True
        stage("When", "owned login, preferences, detached PID, child groups and reserved port are finalized")
        def attempt(label, action):
            try:
                action()
            except BaseException as error:
                self.diagnostics.append(f"{label}: {type(error).__name__}: {error}")
        for child in list(reversed(self.children)):
            if child["record"]["role"] != "server":
                attempt("child group", lambda child=child: self.stop_group(child))
        if self.discovery and self.native is None:
            attempt("final bounded detached discovery", self.discover)
        if self.native:
            attempt("detached native", self.stop_native)
        if self.installing:
            if self.login and ((self.login.get("loginState"), self.login.get("loginDetail")) in
                               (("enabled", "enabled"), ("pending", "requiresApproval"))):
                attempt("unregister own login", self.unregister)
            else:
                self.diagnostics.append("Registration outcome unknown/failed; no guessed login rollback")
            attempt("remove only new own serverURL", self.clear_preference)
        for child in list(reversed(self.children)):
            attempt("owned child terminal", lambda child=child: self.stop_group(child))
        if self.reservation: attempt("reservation close", self.reservation.close)
        if self.port:
            attempt("port reuse", self.port_reuse)
        self.result["cleanup"] = not self.diagnostics
        self.result["terminal"] = "terminal" if self.result["cleanup"] else "incomplete"
        self.result["success"] = (self.result["readiness"] and self.result["clients"] and
                                  self.result["cleanup"] and self.result["error"] is None)
        stage("Then", "owned resource terminal facts and any cleanup diagnostics are retained")
    def stop_native(self):
        for number, seconds in ((signal.SIGTERM, 5), (signal.SIGKILL, 3)):
            with masked():
                fact = self.identity(self.native["pid"])
                if fact is None:
                    return
                require(all(fact[key] == self.native[key] for key in ("pid", "startedAt", "comm")), "Native PID identity changed; refusing signal")
                os.kill(self.native["pid"], number)
            end = time.monotonic() + self.budget(seconds)
            while time.monotonic() < end:
                if self.identity(self.native["pid"]) is None:
                    atomic(self.processdir / "native.json", {**self.native, "absentAt": now()})
                    return
                time.sleep(0.1)
        raise RouteFailure("Exact detached native PID did not disappear")
    def unregister(self):
        code, output, error = self.command("unregister-login", [self.executable, "--unregister-login"])
        self.result["observed"]["unregisterLogin"] = dict(exit=code, output=output, error=error)
        require(code == 0 and output == "notRegistered", "Own login did not become notRegistered")
    def clear_preference(self):
        code, output, _ = self.command("read-own-server-url", ["/usr/bin/defaults", "read", DOMAIN, "serverURL"])
        if code == 0:
            require(output == self.endpoint, "Own preference differs from selected endpoint; refusing delete")
            code, _, error = self.command("delete-own-server-url", ["/usr/bin/defaults", "delete", DOMAIN, "serverURL"])
            require(code == 0, f"Own serverURL deletion failed: {error}")
    def port_reuse(self):
        end = time.monotonic() + self.budget(10)
        while time.monotonic() < end:
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as candidate:
                candidate.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                try:
                    candidate.bind(("127.0.0.1", self.port))
                    candidate.listen(1)
                    return
                except OSError:
                    pass
            time.sleep(0.1)
        raise RouteFailure("Actual server port is not reusable")
def main(argv, case_type=Case):
    case = None
    try:
        require(argv.count("--execution-directory") == 1, "Exactly one execution directory is required")
        directory = canonical(argv[argv.index("--execution-directory") + 1], [D], directory=True)
        require(directory != D and not (directory / "result.json").exists() and not (directory / "process").exists(), "Fresh outer-owned execution directory required")
        case = case_type(directory, argv)
        args = arguments(argv)
        case.result["request_id"] = args.request_id
        case.result["observed"]["validationScope"] = "server-http-only" if args.http_only else "native-route"
        case.exercise(args)
    except BaseException as error:
        if case:
            case.result["error"] = case.result["error"] or f"{type(error).__name__}: {error}"
        else:
            print(f"FAIL: {type(error).__name__}: {error}", file=sys.stderr)
    finally:
        if case:
            try:
                case.cleanup()
            except BaseException as error:
                case.diagnostics.append(f"cleanup: {type(error).__name__}: {error}")
                case.result.update(cleanup=False, terminal="incomplete", success=False)
            finally:
                observed = tuple(case.signals)
                case.save()
                if observed != tuple(case.signals):
                    case.save()
                for number, handler in case.previous.items():
                    signal.signal(number, handler)
            stage("Then", f"{'PASS' if case.result['success'] else 'FAIL'}; result {case.directory / 'result.json'}")
    return 0 if case and case.result["success"] else 1
if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
