# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC5-S2D: human icon opening, actual Summary and production Dashboard AXPress."""

import importlib.util
from pathlib import Path
import sys

sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location(
    "phase5_native_route", Path(__file__).with_name("phase5_native_route.py"))
base = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(base)


class ManualCase(base.Case):
    def native_ui(self, actual):
        code, output, error = self.command("status-item", ["/usr/bin/osascript",
            Path(__file__).with_name("phase5_status_item.applescript"), str(self.native["pid"])])
        self.result["observed"]["statusItem"] = dict(exit=code, output=output, error=error)
        self.save()
        base.require(code == 0 and output.startswith("CC5-STATUS-ITEM\n"),
                     f"Owned status-item observation failed: {error}")
        base.stage("Then", f"Participant target cue:\n{output}\n"
                   "Geometry does not establish physical visibility or reachability.")
        code, output, error = self.command("audible-cue",
            ["/usr/bin/afplay", "/System/Library/Sounds/Pop.aiff"])
        self.result["observed"]["audibleCue"] = dict(exit=code, output=output, error=error)
        self.save()
        base.require(code == 0, f"Audible cue command failed: {error}")
        self.result["observed"].update(
            interactionStrategy="manual-icon-then-dashboard-axpress", actualHumanClickConfirmed=False)
        self.save()
        base.require(self.budget(75) == 75, "Full seventy-five-second UI observation budget required")
        base.stage("When", f"PID {self.native['pid']}: click the announced own menu icon ONE time now "
                   "and keep its content open. The icon-opening opportunity lasts at most 60 seconds. "
                   "Do not click Dashboard/Quit or reply in chat until completion. "
                   "The verifier will collect the Summary and press the actual Dashboard button. "
                   "Accessibility, text and sound do not confirm a physical click, hearing or human browser visibility.")
        helper = Path(__file__).with_name("phase5_menu_bar.applescript")
        self.result["observed"]["manualObservationStartedAt"] = base.now()
        try:
            self.save()
            code, output, error = self.command("ui-observe", ["/usr/bin/osascript", helper,
                str(self.native["pid"]), "observe-open"], timeout=75)
            self.record_ui(actual, code, output, error)
        finally:
            self.result["observed"]["manualObservationEndedAt"] = base.now()
            self.save()
        base.stage("Then", "actual visible Summary values match the API")
        code, output, error = self.command("ui-dashboard", ["/usr/bin/osascript", helper,
            str(self.native["pid"]), "dashboard-open"])
        self.record_dashboard(code, output, error)
        base.stage("Then", "the actual production Open Dashboard button was pressed")


def main(argv):
    return base.main(argv, case_type=ManualCase)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
