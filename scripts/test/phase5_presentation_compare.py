# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC5-S2D diagnostic collection only; never native UI or product acceptance."""

import argparse
from datetime import datetime
import importlib.util
import json
import os
from pathlib import Path
import plistlib
import re
import shlex
import signal
import sys
import time

_spec = importlib.util.spec_from_file_location(
    "phase5_native_route", Path(__file__).with_name("phase5_native_route.py"))
base = importlib.util.module_from_spec(_spec)
# Keep generated artifacts in the private workflow directory, including imports.
sys.dont_write_bytecode = True
_spec.loader.exec_module(base)


def arguments(argv):
    parser = argparse.ArgumentParser(description=__doc__, allow_abbrev=False, add_help=False)
    for name in ("execution-directory", "request-id", "cncf-classpath", "textus-classpath",
                 "runtime-source", "java", "control-center-car", "supervisor-car", "diagnostics-root"):
        base.require(argv.count("--" + name) == 1, f"Exactly one --{name} is required")
        parser.add_argument("--" + name, required=True)
    args = parser.parse_args(argv)
    args.http_only = True
    return args


def canonical(value, directory=False):
    path = base.canonical(value, [base.D], directory=directory)
    base.require(not any(part.is_symlink() for part in (path, *path.parents)),
                 f"Symlink component forbidden: {path}")
    return path


class Case(base.Case):
    def __init__(self, directory, argv, request_id):
        self.apps, self.request_id = {}, request_id
        super().__init__(directory, argv)

    def save(self):
        self.result.update(schema="textus-control-center.phase5-presentation-comparison.v1",
                           request_id=self.request_id, validationScope="presentation-comparison",
                           productAcceptance=False)
        self.result["observed"].update(validationScope="presentation-comparison",
            productAcceptance=False, comparisonCompleted=self.result["clients"],
            disclaimer="Diagnostic collection only. Zero or many AX windows are observations; "
                       "AX hierarchy alone proves no visual presence/absence or root cause. "
                       "Production Summary/button/CAR/login acceptance remains outstanding.",
            applications=self.apps)
        super().save()

    def persist_app(self, app):
        base.atomic(self.processdir / f"native-{app['role']}.json", app)
        self.save()

    def candidates_for(self, app):
        previous, self.executable = self.executable, Path(app["executable"])
        try:
            candidates = super().candidates()
            base.require(len(candidates) <= 1, f"{app['role']}: ambiguous exact UID/path candidates")
            return candidates
        finally:
            self.executable = previous

    def acquire_app(self, app, final=False):
        end = time.monotonic() + self.budget(10)
        previous, self.commandlimit = self.commandlimit, end
        lastabsent = False
        try:
            while time.monotonic() < end:
                with base.masked():
                    candidates = self.candidates_for(app)
                    fact = self.identity(candidates[0]) if candidates else None
                    lastabsent = not candidates or fact is None
                    if fact is not None:
                        # ps renders argv[0] paths with spaces without shell quoting.
                        # Parse only the exact known executable boundary, then its arguments.
                        command = fact["command"]
                        executable = app["executable"]
                        base.require(command == executable or command.startswith(executable + " "),
                                     f"{app['role']}: command argv[0] differs from exact executable")
                        words = [executable] + shlex.split(command[len(executable):])
                        started = time.mktime(time.strptime(fact["startedAt"], "%a %b %d %H:%M:%S %Y"))
                        expected = [app["executable"]]
                        if app["role"] == "production":
                            expected += ["--server-url", self.endpoint]
                        base.require(fact["comm"] == app["executable"]
                            and started >= int(app["launchEpoch"]) and words == expected,
                            f"{app['role']}: unverifiable launch identity; refusing ownership/signals")
                        app["identity"] = fact
                        app["acquiredMonotonic"] = time.monotonic()
                        app["acquiredAt"] = base.now()
                        self.persist_app(app)
                        return
                time.sleep(min(0.1, max(0, end - time.monotonic())))
            if final and lastabsent:
                with base.masked():
                    app["absentAt"], app["shutdownEpoch"] = base.now(), time.time()
                    app["finalDiscovery"] = ("no exact UID/path candidate remained at the last bounded "
                                             "final discovery observation; later LaunchServices startup is possible")
                    self.persist_app(app)
                    return
            raise base.RouteFailure(f"{app['role']}: detached identity not acquired within ten seconds")
        except BaseException as error:
            self.diagnostics.append(f"{app['role']} acquisition: {type(error).__name__}: {error}")
            raise
        finally:
            self.commandlimit = previous

    def same_app(self, app):
        fact = self.identity(app["identity"]["pid"])
        if fact is not None:
            base.require(all(fact[key] == app["identity"][key] for key in ("pid", "startedAt", "comm")),
                         f"{app['role']}: PID identity changed; refusing signal")
        return fact

    def mark_absent(self, app):
        app["absentAt"], app["shutdownEpoch"] = base.now(), time.time()
        self.persist_app(app)

    def stop_app(self, app):
        try:
            self.terminate_app(app)
        except BaseException as error:
            self.diagnostics.append(f"{app['role']} termination: {type(error).__name__}: {error}")
            raise

    def terminate_app(self, app):
        if "absentAt" in app:
            return
        for number, seconds in ((signal.SIGTERM, 5), (signal.SIGKILL, 3)):
            end = time.monotonic() + self.budget(seconds)
            previous, self.commandlimit = self.commandlimit, end
            try:
                with base.masked():
                    if self.same_app(app) is None:
                        self.mark_absent(app)
                        return
                    try:
                        os.kill(app["identity"]["pid"], number)
                    except ProcessLookupError:
                        self.mark_absent(app)
                        return
                while time.monotonic() < end:
                    if self.same_app(app) is None:
                        self.mark_absent(app)
                        return
                    time.sleep(min(0.1, max(0, end - time.monotonic())))
            finally:
                self.commandlimit = previous
        raise base.RouteFailure(f"{app['role']}: exact app did not disappear")

    def traces(self, app):
        # One retained query per acquired PID, also when collection failed earlier.
        if app.get("traceQueryAttempted") or "identity" not in app or "absentAt" not in app:
            return
        app["traceQueryAttempted"] = True
        start = datetime.fromtimestamp(app["launchEpoch"] - 1).strftime("%Y-%m-%d %H:%M:%S")
        end = datetime.fromtimestamp(app["shutdownEpoch"] + 1).strftime("%Y-%m-%d %H:%M:%S")
        predicate = (f'(processIdentifier == {app["identity"]["pid"]}) AND '
                     'subsystem == "org.simplemodeling.textus.controlcenter.menubar" AND '
                     'category == "cc5-presentation"')
        app["traceInterval"] = dict(start=start, end=end, timezone="local")
        self.persist_app(app)
        code, output, error = self.command(f"{app['role']}-oslog", ["/usr/bin/log", "show",
            "--style", "json", "--start", start, "--end", end, "--predicate", predicate])
        empty = not output or re.fullmatch(r"\[\s*\]", output) is not None
        app["trace"] = dict(exit=code, stdout=output, stderr=error,
                            available=code == 0 and not empty,
                            reason="query exited nonzero" if code else "empty log result" if empty else None,
                            caveat="Missing events and asynchronous ordering cannot prove root cause.")
        self.persist_app(app)

    def before_observation(self, app):
        pass

    def observation_arguments(self, app):
        return [str(app["identity"]["pid"])]

    def observe_app(self, app):
        base.stage("When", f"{app['role']} exact diagnostic app is launched for observation")
        with base.masked():
            app.update(launchIntent=True, launchEpoch=time.time(), launchAt=base.now())
            self.persist_app(app)
        launch = ["/usr/bin/open", "-n", app["bundle"], "--args"]
        if app["role"] == "production":
            launch += ["--server-url", self.endpoint]
        code, output, error = self.command(f"{app['role']}-open", launch)
        app["open"] = dict(exit=code, stdout=output, stderr=error)
        self.persist_app(app)
        base.require(code == 0, f"{app['role']}: open failed: {error}")
        self.acquire_app(app)
        while time.monotonic() - app["acquiredMonotonic"] < 16:
            base.require(self.same_app(app) == app["identity"], f"{app['role']}: liveness identity changed/disappeared")
            time.sleep(min(0.25, self.budget(0.25)))
        self.before_observation(app)
        app["observationStartAt"], app["observationStartEpoch"] = base.now(), time.time()
        self.persist_app(app)
        base.stage("When", f"{app['role']} PID-scoped bounded observations begin")
        try:
            code, output, error = self.command(f"{app['role']}-observation", ["/usr/bin/osascript",
                Path(__file__).with_name("phase5_presentation_compare.applescript")]
                + self.observation_arguments(app))
            app["observation"] = dict(exit=code, stdout=output, stderr=error)
            base.require(code == 0 and output.startswith("CC5-PRESENTATION-OBSERVED\n"),
                         f"{app['role']}: diagnostic observation failed: {error}")
        finally:
            app["observationEndAt"], app["observationEndEpoch"] = base.now(), time.time()
            self.persist_app(app)
            base.stage("Then", f"{app['role']} observation ended; this is diagnostic data only")
        self.stop_app(app)
        self.traces(app)

    def exercise(self, args):
        base.require(re.fullmatch(r"cc5-summary-http-r[1-9][0-9]*", args.request_id), "Unexpected request identity")
        root = canonical(args.diagnostics_root, directory=True)
        base.require(root == base.D / "presentation-diagnostics-build-r1", "Frozen diagnostics root required")
        for role, relative, executable, domain in (
            ("minimal", "minimal/CC5 Minimal.app", "CC5MinimalMenuBar", "org.simplemodeling.textus.controlcenter.cc5.minimal"),
            ("production", "production/Textus Control Center.app", "TextusControlCenterMenuBar", base.DOMAIN)):
            bundle = canonical(str(root / relative), directory=True)
            binary = canonical(str(bundle / "Contents/MacOS" / executable))
            base.require(os.access(binary, os.X_OK), f"{role}: regular executable required")
            info = plistlib.loads(canonical(str(bundle / "Contents/Info.plist")).read_bytes())
            base.require(info.get("CFBundleIdentifier") == domain and info.get("CFBundleExecutable") == executable
                         and info.get("LSUIElement") is True, f"{role}: diagnostic plist mismatch")
            app = dict(role=role, bundle=str(bundle), executable=str(binary))
            self.apps[role] = app
            code, _, error = self.command(f"{role}-codesign", ["/usr/bin/codesign", "--verify", "--strict", bundle])
            base.require(code == 0, f"{role}: diagnostic signature verification failed: {error}")
            base.require(not self.candidates_for(app), f"{role}: exact app already running")
        base.require_unused_preferences(Path.home() / f"Library/Preferences/{base.DOMAIN}.plist")
        code, output, error = self.command("preferences-preflight", ["/usr/bin/defaults", "read", base.DOMAIN])
        base.require(code != 0 or re.fullmatch(r"\{\s*\}", output),
                     f"Own cached preference settings exist or are malformed: {error}")
        try:
            super().exercise(args)
        finally:
            self.result["clients"] = False
            self.result["observed"]["validationScope"] = "presentation-comparison"
            self.save()
        production = self.apps["production"]
        code, output, error = self.command("production-summary-client", [production["executable"],
            "--summary-once", "--server-url", self.endpoint])
        base.require(code == 0, f"Production Summary diagnostic client failed: {error}")
        client = base.summary(json.loads(output))
        base.require(all(client[key] == self.result["observed"]["api"][key]
                         for key in ("componentId", "instanceId", "protocolVersion", "runtimes")),
                     "Diagnostic Summary client identity/runtime projection differs from real API")
        self.result["observed"]["summaryClient"] = client
        self.observe_app(self.apps["minimal"])
        self.observe_app(production)
        self.result["clients"] = True
        self.save()
        base.stage("Then", "presentation-comparison collection completed; productAcceptance=false; no UI/root-cause judgment")

    def cleanup(self):
        self.cleaning = True
        def attempt(label, action):
            try:
                action()
            except BaseException as error:
                self.diagnostics.append(f"{label}: {type(error).__name__}: {error}")
        try:
            for child in list(reversed(self.children)):
                if child["record"]["role"] != "server":
                    attempt("outstanding command child", lambda child=child: self.stop_group(child))
            for app in self.apps.values():
                if app.get("launchIntent") and "identity" not in app and "absentAt" not in app:
                    attempt(f"{app['role']} final discovery", lambda app=app: self.acquire_app(app, final=True))
                if "identity" in app:
                    attempt(f"{app['role']} stop exact app", lambda app=app: self.stop_app(app))
                    attempt(f"{app['role']} trace collection", lambda app=app: self.traces(app))
            if self.apps.get("production", {}).get("launchIntent"):
                attempt("remove only new own serverURL", self.clear_preference)
        finally:
            self.installing, self.discovery, self.native = False, False, None
            super().cleanup()


def main(argv, case_type=Case):
    case = None
    try:
        args = arguments(argv)
        directory = canonical(args.execution_directory, directory=True)
        base.require(directory != base.D and not any((directory / name).exists() or (directory / name).is_symlink()
                     for name in ("result.json", "process")), "Fresh outer-owned execution directory required")
        with base.masked():
            case = case_type(directory, argv, args.request_id)
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
                try:
                    observed = tuple(case.signals)
                    case.save()
                    if observed != tuple(case.signals):
                        case.save()
                finally:
                    for number, handler in case.previous.items():
                        signal.signal(number, handler)
            base.stage("Then", f"{'PASS' if case.result['success'] else 'FAIL'} diagnostic collection only; "
                       f"productAcceptance=false; result {case.directory / 'result.json'}")
    return 0 if case and case.result["success"] else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
