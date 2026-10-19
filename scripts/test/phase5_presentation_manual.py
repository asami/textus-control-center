# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///
"""CC5-S2D manual-click opportunity followed by read-only diagnostic collection."""

import importlib.util
from pathlib import Path
import sys
import time

sys.dont_write_bytecode = True
_spec = importlib.util.spec_from_file_location(
    "phase5_presentation_compare", Path(__file__).with_name("phase5_presentation_compare.py"))
comparison = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(comparison)
base = comparison.base


class ManualCase(comparison.Case):
    def before_observation(self, app):
        previous = self.commandlimit
        try:
            code, output, error = self.command(f"{app['role']}-status-item", ["/usr/bin/osascript",
                Path(__file__).with_name("phase5_status_item.applescript"), str(app["identity"]["pid"])])
            app["statusItem"] = dict(exit=code, stdout=output, stderr=error)
            self.persist_app(app)
            base.require(code == 0 and output.startswith("CC5-STATUS-ITEM\n"),
                         f"{app['role']}: owned status-item observation failed: {error}")
            base.stage("Then", f"{app['role']} participant target cue:\n{output}\n"
                       "Geometry does not establish physical visibility or reachability.")
            code, output, error = self.command(f"{app['role']}-audible-cue",
                ["/usr/bin/afplay", "/System/Library/Sounds/Pop.aiff"])
            app["audibleCue"] = dict(exit=code, stdout=output, stderr=error)
            self.persist_app(app)
            base.require(code == 0, f"{app['role']}: audible cue command failed: {error}")
            with base.masked():
                app.update(manualWaitStartedAt=base.now(), plannedManualWaitSeconds=60,
                           interaction="human-click", actualHumanClickConfirmed=False)
                self.persist_app(app)
            base.require(self.budget(60) == 60, f"{app['role']}: full sixty-second manual wait budget required")
            base.stage("When", f"{app['role']} app {app['bundle']} (PID {app['identity']['pid']}): "
                       "physically click this app's menu icon ONE time now and keep its content open "
                       "through the next 60 seconds. Do not click again or use Dashboard/Quit.")
            end = time.monotonic() + 60
            while time.monotonic() < end:
                # Bound each identity read while allowing the final read to finish.
                self.commandlimit = time.monotonic() + 0.5
                base.require(self.same_app(app) == app["identity"],
                             f"{app['role']}: manual-wait liveness identity changed/disappeared")
                time.sleep(min(0.25, max(0, end - time.monotonic()), self.budget(0.25)))
        finally:
            self.commandlimit = previous
            with base.masked():
                if "manualWaitStartedAt" in app:
                    app["manualWaitEndedAt"] = base.now()
                    self.persist_app(app)

    def observation_arguments(self, app):
        return [str(app["identity"]["pid"]), "--observe-only"]

    def save(self):
        self.result["observed"].update(
            interactionStrategy="manual-click-then-read-only", actualHumanClickConfirmed=False,
            manualInteractionCaveat="Elapsed wait does not establish an actual physical click or visual success. "
                                   "Status-item geometry does not establish physical visibility/reachability; "
                                   "a successful audio command does not establish that the human heard it. "
                                   "Only source observations plus an actual later human report may establish "
                                   "those facts; this harness does not confirm them.")
        super().save()


def main():
    return comparison.main(sys.argv[1:], case_type=ManualCase)


if __name__ == "__main__":
    sys.exit(main())
