const policy = require("../src/main/web/textus-control-center/assets/operational-link-policy.js");
const panel = require("node:fs").readFileSync("src/main/web/textus-control-center/assets/operational-panel.js", "utf8");

function equal(actual, expected, label) {
  if (actual !== expected) throw new Error(`${label}: expected ${expected}, got ${actual}`);
}

// Given representative invocation, catalog, evidence, and Control Center facts.
const component = { artifactId: "example" };
const controlCenter = { artifactId: "textus-control-center" };
const none = [{ artifactId: "example", status: "running", baseUrl: "http://127.0.0.1:38000" }];
const registered = [{ artifactId: "example", status: "running", baseUrl: "http://127.0.0.1:38000", applicationUrl: "http://127.0.0.1:38000/web" }];
const snakeRegistered = [{ artifact_id: "example", status: "running", base_url: "http://127.0.0.1:38000", application_url: "http://127.0.0.1:38000/web/portal" }];
const starting = [{ artifactId: "example", status: "starting" }];
const stale = [{ artifactId: "example", status: "stale" }];
const stopped = [{ artifactId: "example", status: "stopped" }];
const catalogNotRunning = [{ artifactId: "example", runtimeState: "not-running" }];
const catalogRunning = [{ artifactId: "example", runtimeState: "running" }];
const currentRegistered = [{ artifactId: "example", decision: "current-registered" }];
const currentEvidenceOnly = [{ artifactId: "example", decision: "current-evidence-only" }];
const historicalStopped = [{ artifactId: "example", decision: "current-registered", stoppedAt: "2026-08-27T00:00:00Z" }];
function fakeDocument() {
  return { createElement(tagName) { return { tagName, listeners: {}, addEventListener(name, listener) { this.listeners[name] = listener; } }; } };
}
function control(label, source) { return policy[label](fakeDocument(), component, source); }
// When the shared operational policy projects links and runtime state.
const registeredControl = control("renderOpenApp", registered);
const refreshedAbsentControl = control("renderOpenApp", none);

// Then link rendering, state precedence, and Control Center treatment remain stable.
equal(policy.openApp(component, none).disabled, true, "Open App is disabled without applicationUrl");
equal(policy.openApp(component, registered).url, "http://127.0.0.1:38000/web", "Open App uses registered /web URL");
equal(policy.dashboard(component, registered).url, "http://127.0.0.1:38000/web/system/dashboard", "Dashboard remains separate");
equal(policy.openApp(component, []).disabled, true, "links refresh when the invocation disappears");
equal(policy.openApp(component, snakeRegistered).url, "http://127.0.0.1:38000/web/portal", "snake-case projection is normalized");
equal(registeredControl.tagName, "a", "row Open App renders as a link");
equal(refreshedAbsentControl.tagName, "button", "refresh replacement turns Open App from a link into a disabled button");
equal(refreshedAbsentControl.disabled, true, "row Open App is disabled when absent");
equal(refreshedAbsentControl.title, "Open App is unavailable because no active application URL is registered.", "disabled Open App explains the missing URL");
equal(Object.keys(refreshedAbsentControl.listeners).sort().join(","), "click,keydown", "link controls isolate row click and keyboard handlers");
equal(control("renderOpenApp", snakeRegistered).href, "http://127.0.0.1:38000/web/portal", "snake-case projection renders an Open App link");
equal(control("renderDashboard", registered).href, "http://127.0.0.1:38000/web/system/dashboard", "Dashboard renderer stays separate");
equal(control("renderDashboard", none).tagName, "a", "Dashboard remains enabled from base URL when Open App is absent");
equal(control("renderOpenApp", registered).target, "_blank", "Open App renderer has a safe target");
equal(control("renderOpenApp", registered).rel, "noopener", "Open App renderer has a safe rel");
equal(control("renderOpenApp", registered).textContent, "Open App", "Open App renderer has accessible text");
equal(policy.runtimeState(component, stale, [], catalogNotRunning), "not-running", "stale-only invocation uses the catalog not-running state");
equal(policy.runtimeState(component, registered, [], catalogNotRunning), "running", "active running invocation takes precedence over catalog state");
equal(policy.runtimeState(component, starting, [], catalogNotRunning), "starting", "active starting invocation takes precedence over catalog state");
equal(policy.runtimeState(component, [], currentRegistered, catalogNotRunning), "running", "current registered evidence takes precedence over catalog state");
equal(policy.runtimeState(component, [], currentEvidenceOnly, catalogRunning), "evidence-current", "current evidence-only state takes precedence over catalog state");
equal(policy.runtimeState(component, stale, [], catalogRunning), "running", "catalog state takes precedence over stale invocation history");
equal(policy.runtimeState(component, stopped, [], catalogRunning), "running", "catalog state takes precedence over stopped invocation history");
equal(policy.runtimeState(component, stale, [], []), "stale", "stale invocation remains the fallback when catalog state is absent");
equal(policy.runtimeState(component, stopped, [], []), "stopped", "stopped invocation remains the fallback when catalog state is absent");
equal(policy.runtimeState(component, [], historicalStopped, []), "not-running", "stopped evidence does not become current runtime state");
equal(policy.runtimeState(component, [], [], []), "not-running", "missing runtime facts fall back to not-running");
equal(policy.isControlCenter(controlCenter), true, "Control Center identity is recognized narrowly");
equal(policy.isControlCenter(component), false, "ordinary artifacts are not Control Center");
equal(policy.runtimeState(controlCenter, [], [], []), "running", "Control Center is intrinsically running without self-registration");
if (!panel.includes("linkPolicy.runtimeState(component, invocations, evidence, catalog)")) throw new Error("operational panel must delegate runtime state to the shared policy");
if (!panel.includes("linkPolicy.renderOpenApp(document, component, invocations)") || !panel.includes("linkPolicy.renderDashboard(document, component, invocations)")) throw new Error("operational panel must delegate row and detail link rendering to the shared policy");
if (!panel.includes("if (!linkPolicy.isControlCenter(component))")) throw new Error("operational panel must omit Control Center lifecycle controls");
if (!panel.includes("registration observation remains pending")) throw new Error("operational panel must retain observation timeout as pending");
if (!/^\(function \(\) \{\n  "use strict";/m.test(panel)) throw new Error("operational panel must keep strict mode as the first IIFE statement");
