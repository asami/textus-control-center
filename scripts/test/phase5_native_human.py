# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC5-S2: human Summary comparison and actual Open Dashboard smoke check."""

import importlib.util
from pathlib import Path
import stat
import sys

sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location(
    "phase5_native_route", Path(__file__).with_name("phase5_native_route.py"))
base = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(base)


class HumanCase(base.Case):
    def exercise(self, args):
        base.require(not args.http_only, "Human UI smoke requires the installed native route")
        super().exercise(args)

    def native_ui(self, actual):
        observed = self.result["observed"]
        observed["validationMethod"] = "human-ui-smoke"
        observation = self.directory / "manual-observation.json"
        base.require(not observation.exists() and not observation.is_symlink(),
                     "Fresh absent manual-observation.json required")
        expected = [f"Version: {actual['controlCenter']['version']['value']}",
                    f"Health (control-center): {actual['overallHealth']['status']}",
                    f"Registered runtimes: {actual['runtimes']['total']} total; {actual['runtimes']['returned']} returned"]
        observed["humanExpected"] = expected
        observed["humanExpectedDashboard"] = self.endpoint + actual["dashboardUrl"]
        self.save()
        base.stage("When", "After the sound, click the server-rack menu icon and compare these three values:\n"
                   + "\n".join(expected) + "\nManually click Open Dashboard and inspect the browser at "
                   + observed["humanExpectedDashboard"]
                   + ". Then return the Summary comparison and Dashboard result in chat. "
                   "You may leave the menu and return to chat; no hold-open is required. "
                   "Only the parent records your actual feedback in manual-observation.json.")
        code, output, error = self.command("audible-cue",
            ["/usr/bin/afplay", "/System/Library/Sounds/Pop.aiff"])
        observed["audibleCue"] = dict(exit=code, output=output, error=error)
        self.save()
        base.require(code == 0, f"Audible cue command failed: {error}")
        end = base.time.monotonic() + self.budget(300)
        previous = self.commandlimit
        self.commandlimit = min(previous, end) if previous is not None else end
        observed["manualObservationStartedAt"] = base.now()
        try:
            self.save()
            while True:
                base.require(not self.signals and not self.cleaning,
                             "Signal or cleanup prevents human UI observation")
                self.budget(0.5)
                base.require(base.time.monotonic() < end, "Human UI observation timed out")
                base.require(self.identity(self.native["pid"]) == self.native,
                             "Owned native process identity changed during human observation")
                base.require(base.time.monotonic() < end, "Human UI observation timed out")
                try:
                    info = observation.lstat()
                except FileNotFoundError:
                    base.time.sleep(min(0.5, self.budget(0.5), max(0, end - base.time.monotonic())))
                    continue
                base.require(stat.S_ISREG(info.st_mode) and info.st_size <= 8192,
                             "Human observation must be a regular non-symlink file of at most 8192 bytes")
                with observation.open("rb") as stream:
                    data = stream.read(8193)
                base.require(len(data) <= 8192, "Human observation exceeds 8192 bytes")
                record = base.json.loads(data.decode("utf-8"))
                observed["humanObservation"] = record
                self.save()
                base.require(isinstance(record, dict)
                             and record.get("schema") == "textus-control-center.phase5-human-ui.v1"
                             and record.get("request_id") == self.result["request_id"],
                             "Human observation schema/request identity mismatch")
                base.require(type(record.get("summary_matches_api")) is bool
                             and type(record.get("dashboard_opened")) is bool,
                             "Human observation requires exact boolean outcomes")
                source = record.get("source_user_message")
                base.require(isinstance(source, dict) and source.get("task_id") == "/root"
                             and type(source.get("occurrence")) is int and source["occurrence"] > 0
                             and isinstance(source.get("text"), str) and bool(source["text"].strip()),
                             "Human observation requires attributable user feedback")
                base.require(record["summary_matches_api"] and record["dashboard_opened"],
                             "Human did not confirm both Summary comparison and Dashboard opening")
                observed["humanCheckSucceeded"] = True
                self.save()
                base.stage("Then", "human feedback confirms Summary/API values and the actual Dashboard action")
                return
        finally:
            self.commandlimit = previous
            observed["manualObservationEndedAt"] = base.now()
            self.save()


def main(argv):
    return base.main(argv, case_type=HumanCase)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
