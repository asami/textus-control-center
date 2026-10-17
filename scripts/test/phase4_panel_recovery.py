# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC4-S9A: actual packaged panel actions and same-state JVM reconstruction."""

import json
import os
from pathlib import Path
import shlex
import socket
import sys
import tempfile
import time
import uuid

sys.dont_write_bytecode = True
import phase4_packaged_operations as native


NODE = Path("/Users/asami/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node")
PLAYWRIGHT = Path("/Users/asami/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright")
CHROME = Path("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome")
CLIENT = Path(__file__).resolve().with_name("phase4_panel_client.cjs")
CASE_SECONDS = 900
SESSION_SECONDS = 2100
CLEANUP_ALLOWANCE = 400
IDENTITY = ("request_id", "supervisor_id", "instance_id")


def parse_arguments(argv):
    parser = native.Arguments(description=__doc__)
    for name in ("java", "cncf-launcher-classpath-file", "runtime-root",
                 "control-center-car", "supervisor-car", "development-fixtures-root",
                 "component-version", "runtime-version", "result-receipt", "node",
                 "playwright-package-root", "browser-executable"):
        parser.add_argument("--" + name, required=True)
    return parser.parse_args(argv)


class PanelInputs(native.NativeInputs):
    def __init__(self, args):
        super().__init__(args)
        self.node = native.regular(native.absolute(args.node))
        self.chrome = native.regular(native.absolute(args.browser_executable))
        self.playwright = native.absolute(args.playwright_package_root)
        native.require(self.node == NODE.resolve() and self.chrome == CHROME.resolve() and
                       self.playwright == PLAYWRIGHT.resolve(),
                       "Given the explicitly selected bundled Node/Playwright and installed Chrome are required")
        native.require(os.access(self.node, os.X_OK) and os.access(self.chrome, os.X_OK),
                       "Given Node and Chrome must be actual executable regular files")
        package = json.loads(native.regular(self.playwright / "package.json").read_text())
        native.require(package.get("name") == "playwright" and self.playwright.is_dir(),
                       "Given the existing actual Playwright package is required")
        native.regular(self.playwright / "index.js")
        native.regular(CLIENT)


class PanelSignals(native.RunSignals):
    def __init__(self, result):
        super().__init__(result)
        # At most 1700s of active work leaves 400s for both inherited cleanups.
        self.work_deadline = time.monotonic() + SESSION_SECONDS - CLEANUP_ALLOWANCE

    def check(self):
        super().check()
        if not self.cleaning:
            deadline = min(self.work_deadline, self.case.deadline) if self.case else self.work_deadline
            native.require(time.monotonic() < deadline,
                           "Then panel/reconstruction work must finish within its owned session/case bound")


def audit_map(rows):
    result = {native.nonempty(row, "request_id"): row for row in rows}
    native.require(len(result) == len(rows), "Then audit request identities must be distinct")
    return result


def same_identity(actual, expected):
    native.require(all(native.nonempty(actual, name) == expected[name] for name in IDENTITY),
                   "Then retained request, opaque Supervisor and instance identities must agree")


class PanelCase(native.Case):
    def __init__(self, directory, placement, inputs, signals):
        super().__init__(directory, placement, inputs, signals)
        # Omit only child HOME so native admission preserves explicit JVM user.home.
        self.environment.pop("HOME", None)
        self.deadline = min(time.monotonic() + CASE_SECONDS, signals.work_deadline)

    def java_command(self):
        argv = super().java_command()
        index = argv.index("--launcher-home")
        argv[index + 1] = str(self.home)
        return argv

    def prepare(self):
        super().prepare()
        wrapper = self.directory / "bin/cncf"
        shebang, serverbody = wrapper.read_text(encoding="utf-8").split("\n", 1)
        wrapper.write_text(shebang + '\ncase "${1-}:${2-}" in\n'
                           '  launcher:evidence)\n    exec ' + shlex.join(self.java_command()) +
                           ' "$@"\n    ;;\nesac\n' + serverbody, encoding="utf-8")
        # Append after the inherited CC/target/external indices, never insert.
        reservation = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.reservations.append(reservation)
        reservation.bind(("127.0.0.1", 0))
        self.ports.append(reservation.getsockname()[1])
        self.chromeport = self.ports[-1]

    def target_source(self):
        native.stage("Given", f"{self.placement}: the genuine native DEV target is auto-managed and stopped")
        native.stage("When", "the actual native CAR catalog is refreshed and its managed cars are listed")
        self.operation("CarCatalog", "refreshCarCatalog")
        cars = self.rows("CarCatalog", "listManagedCars")
        target = [row for row in cars if row.get("artifact_id") == "textus-supervisor"]
        native.stage("Then", "one available DEV source identifies the auto-managed stopped native target")
        native.require(len(target) == 1 and isinstance(target[0].get("sources"), list),
                       "Then exactly one native target with declared sources must exist")
        sources = [row for row in target[0]["sources"]
                   if row.get("source_kind") == "DEV" and row.get("refresh_state") == "available"]
        native.require(len(sources) == 1, "Then exactly one available DEV source is required")
        native.require(self.management("get").get("management_state") == "auto-managed" and
                       not native.port_ready(self.targetport),
                       "Then the panel scenario must begin with an auto-managed stopped target")
        return {"sourceKind": native.nonempty(sources[0], "source_kind"),
                "sourceId": native.nonempty(sources[0], "source_id")}

    def browser(self):
        native.stage("Given", f"{self.placement}: a separately acquired private headless Chrome")
        profile = self.directory / "chrome-profile"
        profile.mkdir(mode=0o700)
        self.reservations[-1].close()
        chrome = self.acquire("chrome", [str(self.native.chrome), f"--user-data-dir={profile}",
                              "--remote-debugging-address=127.0.0.1",
                              f"--remote-debugging-port={self.chromeport}", "--headless=new",
                              "--no-first-run", "--no-default-browser-check",
                              "--disable-background-networking", "--disable-component-update",
                              "--disable-sync", "--disable-extensions"])
        deadline = min(self.deadline, time.monotonic() + 30)
        while not native.port_ready(self.chromeport):
            self.signals.check()
            native.require(chrome.poll() is None, "Then the acquired Chrome must remain live")
            native.require(time.monotonic() < deadline, "Then owned Chrome must be ready within 30s")
            time.sleep(0.1)
        native.require(chrome.poll() is None, "Then the ready Chrome must be the acquired live process")
        before = audit_map(self.audit())
        receipt = self.directory / "panel-result.json"
        # Conservative before the browser can submit Start or Remove: uncertainty retains cleanup.
        self.started = True
        self.excluded = True
        native.stage("When", "the actual packaged panel performs Start, detail, Restart, Stop, Remove and Refresh")
        client = self.acquire("panel-client", [str(self.native.node), str(CLIENT),
                              "--cdp-url", f"http://127.0.0.1:{self.chromeport}",
                              "--base-url", f"http://127.0.0.1:{self.ccport}",
                              "--artifact", "textus-supervisor", "--result", str(receipt),
                              "--playwright-package-root", str(self.native.playwright)])
        deadline = min(self.deadline, time.monotonic() + 240)
        while client.poll() is None:
            self.signals.check()
            native.require(chrome.poll() is None, "Then Chrome must remain live during panel actions")
            native.require(time.monotonic() < deadline, "Then the Node panel client must finish within 240s")
            time.sleep(0.1)
        client.wait(timeout=0)
        native.require(client.returncode == 0, f"Then the real panel client must succeed; inspect {receipt}")
        browserresult = json.loads(native.regular(receipt).read_text())
        native.require(browserresult.get("success") is True,
                       "Then browser success must be explicit in its terminal atomic receipt")
        native.stage("Then", "three actual protected lifecycle audit records corroborate the panel actions")
        after = audit_map(self.audit())
        native.require(all(after.get(key) == value for key, value in before.items()) and
                       len(after) == len(before) + 3,
                       "Then panel lifecycle actions must add exactly three safe audit records")
        added = [row for key, row in after.items() if key not in before]
        actions = {row.get("lifecycle_action"): row for row in added}
        native.require(set(actions) == {"start", "restart", "stop"},
                       "Then actual audit must contain one panel Start, Restart and Stop")
        for row in added:
            for name in IDENTITY:
                native.nonempty(row, name)
        native.require(len({row["supervisor_id"] for row in added}) == 1 and
                       actions["start"]["instance_id"] != actions["restart"]["instance_id"] and
                       actions["stop"]["instance_id"] == actions["restart"]["instance_id"] and
                       actions["start"].get("request_state") == "accepted" and
                       actions["restart"].get("request_state") == "accepted" and
                       actions["stop"].get("request_state") == "stopped",
                       "Then panel audit must preserve Supervisor identity and correct instance transitions")
        native.await_port_reuse(self.targetport)
        self.started = False
        native.stage("When", "protected Restore readmits the panel-excluded DEV target")
        restored = self.management("restore")
        native.stage("Then", "protected Restore confirms auto-managed state before reconstruction")
        native.require(restored.get("management_state") == "auto-managed" and
                       self.management("get").get("management_state") == "auto-managed",
                       "Then protected Restore must confirm auto-managed state before reconstruction")
        self.excluded = False
        self.observed.append({"action": "actual-panel", "receipt": str(receipt),
                              "audit": added, "restored": restored})
        self.save()

    def current_child(self, role):
        return next((process, record) for process, record in reversed(self.children)
                    if record["role"] == role)

    def replace_parent(self, role, index, configuration, assembly, cars):
        old, oldrecord = self.current_child(role)
        native.stage("Given", f"{self.placement}: exact acquired {role} parent and unchanged private durable state")
        self.signals.check()
        native.require(old.poll() is None, "Then the exact JVM parent must be live before replacement")
        native.stage("When", f"only acquired direct JVM {old.pid} receives normal SIGTERM")
        old.terminate()
        deadline = min(self.deadline, time.monotonic() + native.TERM_SECONDS)
        while old.poll() is None:
            self.signals.check()
            native.require(time.monotonic() < deadline, "Then the exact parent must terminate within eight seconds")
            time.sleep(0.1)
        old.wait(timeout=0)
        oldrecord["parentReapedAt"] = native.timestamp()
        oldrecord["replacementReturncode"] = old.returncode
        self.save()  # Old group remains acquired, even if its target descendant survives.
        native.await_port_reuse(self.ports[index])
        native.stage("Then", "the reaped parent releases its service port while the retained target remains available")
        native.require(native.port_ready(self.targetport),
                       "Then the retained target must remain available across direct parent termination")
        replacementrole = role + "-generation-2"
        self.launch(replacementrole, index, configuration, assembly, cars)
        new, newrecord = self.current_child(replacementrole)
        native.stage("Then", "a different acquired JVM serves the same port, configuration and private state")
        native.require(new.pid != old.pid and old.returncode is not None and new.poll() is None,
                       "Then old parent reaping and distinct live replacement must both be actual")
        if index == 0:
            native.atomic_json(self.directory / "server-process.json", {
                **newrecord, "baseUrl": f"http://127.0.0.1:{self.ccport}",
                "supervisorBaseUrl": f"http://127.0.0.1:{self.ports[2] if self.placement == 'external' else self.ccport}"})
        self.observed.append({"action": "replace-direct-parent", "role": role,
                              "oldPid": old.pid, "newPid": new.pid,
                              "oldParentReapedAt": oldrecord["parentReapedAt"],
                              "oldReturncode": old.returncode})
        self.save()

    def retained_start(self, key, source, first, originalaudit, boundary):
        native.stage("When", f"{boundary}: protected request lookup and exact original Start retry are requested")
        native.stage("Then", "the same request, Supervisor and running instance survive without another audit identity")
        detail = self.detail(first["request_id"], "accepted")
        same_identity(detail, first)
        retry = self.action("start", key, "accepted", source)
        same_identity(retry, first)
        projection = self.runtime_observation(first["instance_id"])
        native.require(audit_map(self.audit()) == originalaudit,
                       "Then reconstructed lookup and exact retry must preserve the original safe audit")
        self.observed.append({"action": "retained-start", "boundary": boundary,
                              "detail": detail, "retry": retry, "runtimeProjection": projection})
        self.save()

    def recovery(self, source):
        native.stage("Given", f"{self.placement}: a restored native target and a new generated Start key")
        before = audit_map(self.audit())
        key = uuid.uuid4().hex
        self.started = True
        native.stage("When", "the actual protected Start acquires the recovery target")
        first = self.action("start", key, "accepted", source)
        native.stage("Then", "Start has safe durable correlation, a registered running projection and real TCP readiness")
        for name in IDENTITY:
            native.nonempty(first, name)
        projection = self.runtime_observation(first["instance_id"])
        native.stage("When", "the actual private native Launcher ownership evidence is read before reconstruction")
        evidencepath = self.home / ".cncf/launcher/server-evidence.json"
        evidence = json.loads(native.regular(evidencepath).read_text())
        native.stage("Then", "versioned native evidence identifies exactly the started development Supervisor instance")
        native.require(isinstance(evidence, dict) and
                       evidence.get("schema") == "cncf.launcher.server-evidence.v1" and
                       isinstance(evidence.get("entries"), list),
                       "Then actual Launcher evidence must have its versioned native schema and entries")
        entries = [entry for entry in evidence["entries"]
                   if isinstance(entry, dict) and entry.get("instanceId") == first["instance_id"]]
        native.require(len(entries) == 1,
                       "Then actual Launcher evidence must contain exactly one matching instance")
        entry = entries[0]
        native.require(type(entry.get("processId")) is int and entry["processId"] > 0 and
                       isinstance(entry.get("processStartedAt"), str) and
                       bool(entry["processStartedAt"].strip()) and
                       entry.get("artifactId") == "textus-supervisor" and
                       entry.get("executionMode") == "development",
                       "Then matching native evidence must identify a positive PID, process start and development Supervisor")
        self.observed.append({"action": "native-recovery-evidence", "path": str(evidencepath),
                              "instance_id": first["instance_id"]})
        originalaudit = audit_map(self.audit())
        native.require(set(originalaudit) == set(before) | {first["request_id"]} and
                       len(originalaudit) == len(before) + 1 and
                       all(originalaudit.get(name) == row for name, row in before.items()),
                       "Then recovery Start must add exactly one durable safe audit record")
        self.observed.append({"action": "recovery-start", "result": first,
                              "audit": list(originalaudit.values()), "runtimeProjection": projection})
        self.save()
        if self.placement == "external":
            supervisor, _ = self.current_child("external-supervisor")
        self.replace_parent("control-center", 0, self.ccconfig, self.ccassembly,
                            ["textus-control-center", "textus-supervisor"])
        if self.placement == "external":
            native.require(supervisor.poll() is None and native.port_ready(self.targetport),
                           "Then external Supervisor and target stay live during CC-only reconstruction")
        self.retained_start(key, source, first, originalaudit, "Control Center reconstruction")
        if self.placement == "external":
            cc, _ = self.current_child("control-center-generation-2")
            self.replace_parent("external-supervisor", 2, self.remoteconfig, self.remoteassembly,
                                ["textus-supervisor"])
            native.require(cc.poll() is None and native.port_ready(self.ccport),
                           "Then Control Center stays live during external Supervisor-only reconstruction")
            self.retained_start(key, source, first, originalaudit, "external Supervisor reconstruction")
        native.stage("Given", "the original Start is retained through every selected JVM reconstruction")
        restarted = self.idempotent("restart", source, "accepted")
        native.require(restarted["instance_id"] != first["instance_id"],
                       "Then reconstructed Restart must acquire a different opaque instance")
        stopped = self.idempotent("stop", None, "stopped")
        native.require(stopped["instance_id"] == restarted["instance_id"],
                       "Then reconstructed Stop must identify the restarted instance")
        native.await_port_reuse(self.targetport)
        self.started = False

    def exercise(self):
        source = self.target_source()
        self.browser()
        self.recovery(source)
        self.signals.check()
        self.result["clients"] = True
        self.save()


def main(argv):
    try:
        receipt = native.receipt_selector(argv)
    except BaseException as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    native.OUTPUT.mkdir(parents=True, exist_ok=True)
    directory = Path(tempfile.mkdtemp(prefix="case-panel-run-", dir=native.OUTPUT))
    result = {"readiness": False, "clients": False, "cleanup": False,
              "processCompletion": "incomplete", "success": False, "error": None,
              "cleanupDiagnostics": [], "runDirectory": str(directory), "placements": [],
              "sessionSeconds": SESSION_SECONDS, "caseSeconds": CASE_SECONDS,
              "nativeFixtureUse": "read-only reuse; no fresh producer execution"}
    cases = []
    native.atomic_json(receipt, result)
    signals = PanelSignals(result)
    signals.install()
    try:
        args = parse_arguments(argv)
        native.stage("Given", "explicit native inputs and existing bundled Node/Playwright with installed Chrome")
        inputs = PanelInputs(args)
        for placement in ("embedded", "external"):
            signals.check()
            case = PanelCase(directory / f"case-{placement}", placement, inputs, signals)
            signals.case = case
            cases.append(case)
            result["placements"].append(case.result)
            try:
                signals.check()
                case.prepare()
                signals.check()
                case.start_servers()
                signals.check()
                case.exercise()
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
            native.require(case.result["success"], f"Then {placement} cleanup must succeed: {case.diagnostics}")
    except BaseException as error:
        signals.capture(error)
    finally:
        signals.cleaning = True
        result["readiness"] = len(cases) == 2 and all(case.result["readiness"] for case in cases)
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
    native.stage("Then", f"{'PASS' if result['success'] else 'FAIL'}; retained run directory {directory}")
    if result["error"]:
        print(result["error"], file=sys.stderr)
    for diagnostic in result["cleanupDiagnostics"]:
        print(f"Cleanup: {diagnostic}", file=sys.stderr)
    return 0 if result["success"] else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
