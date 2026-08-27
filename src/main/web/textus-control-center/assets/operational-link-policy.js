(function (root, factory) {
  const policy = factory();
  if (typeof module === "object" && module.exports) module.exports = policy;
  root.TextusOperationalLinkPolicy = policy;
}(typeof globalThis === "undefined" ? this : globalThis, function () {
  function artifactId(value) { return value.artifactId || value.artifact_id || value.target || ""; }
  function isControlCenter(component) { return artifactId(component) === "textus-control-center"; }
  function status(value) { return value.status || value.launcherState || value.launcher_state || ""; }
  function valueOf(value, camel, snake) { return value[camel] || value[snake] || ""; }
  function currentEvidence(value) { return !value.stoppedAt && !value.stopped_at; }
  function evidenceDecision(value) { return value.decision || value.evidenceDecision || value.evidence_decision || ""; }
  function activeInvocations(component, invocations) {
    return invocations.filter((value) => artifactId(value) === artifactId(component) && ["running", "starting"].includes(String(status(value)).toLowerCase()));
  }
  function catalogRecord(component, catalog) {
    const records = Array.isArray(catalog) ? catalog : (catalog ? [catalog] : []);
    return records.find((value) => artifactId(value) === artifactId(component));
  }
  function runtimeState(component, invocations, evidence, catalog) {
    if (isControlCenter(component)) return "running";
    const values = invocations.filter((value) => artifactId(value) === artifactId(component)).map((value) => String(status(value)).toLowerCase());
    if (values.includes("running")) return "running";
    if (values.includes("starting")) return "starting";
    const observed = evidence.filter((value) => artifactId(value) === artifactId(component) && currentEvidence(value)).map((value) => String(evidenceDecision(value)).toLowerCase());
    if (observed.includes("current-registered")) return "running";
    if (observed.includes("current-evidence-only")) return "evidence-current";
    const record = catalogRecord(component, catalog);
    const state = record && valueOf(record, "runtimeState", "runtime_state");
    if (state) return state;
    if (values.includes("stale")) return "stale";
    if (values.includes("stopped")) return "stopped";
    return "not-running";
  }
  function applicationUrl(component, invocations) {
    return activeInvocations(component, invocations).map((value) => valueOf(value, "applicationUrl", "application_url")).find(Boolean) || "";
  }
  function dashboardUrl(component, invocations) {
    const active = activeInvocations(component, invocations);
    const projected = active.map((value) => valueOf(value, "dashboardUrl", "dashboard_url")).find(Boolean);
    if (projected) return projected;
    const base = active.map((value) => valueOf(value, "baseUrl", "base_url")).find(Boolean);
    return base ? `${base.replace(/\/$/, "")}/web/system/dashboard` : "";
  }
  function openApp(component, invocations) {
    const url = applicationUrl(component, invocations);
    return { url, disabled: !url, title: "Open App is unavailable because no active application URL is registered." };
  }
  function dashboard(component, invocations) {
    const url = dashboardUrl(component, invocations);
    return { url, disabled: !url, title: "Dashboard is unavailable because no active registered URL is available." };
  }
  function renderControl(document, label, state) {
    const control = document.createElement(state.url ? "a" : "button");
    control.className = "button secondary operational-action";
    control.textContent = label;
    control.addEventListener("click", (event) => { event.stopPropagation(); });
    control.addEventListener("keydown", (event) => { event.stopPropagation(); });
    if (state.url) {
      control.href = state.url;
      control.target = "_blank";
      control.rel = "noopener";
    } else {
      control.type = "button";
      control.disabled = state.disabled;
      control.title = state.title;
    }
    return control;
  }
  function renderOpenApp(document, component, invocations) { return renderControl(document, "Open App", openApp(component, invocations)); }
  function renderDashboard(document, component, invocations) { return renderControl(document, "Dashboard", dashboard(component, invocations)); }
  return { applicationUrl, dashboardUrl, openApp, dashboard, renderOpenApp, renderDashboard, isControlCenter, runtimeState };
}));
