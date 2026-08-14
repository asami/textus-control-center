(function (root, factory) {
  const policy = factory();
  if (typeof module === "object" && module.exports) module.exports = policy;
  root.TextusOperationalLinkPolicy = policy;
}(typeof globalThis === "undefined" ? this : globalThis, function () {
  function artifactId(value) { return value.artifactId || value.artifact_id || value.target || ""; }
  function status(value) { return value.status || value.launcherState || value.launcher_state || ""; }
  function valueOf(value, camel, snake) { return value[camel] || value[snake] || ""; }
  function activeInvocations(component, invocations) {
    return invocations.filter((value) => artifactId(value) === artifactId(component) && ["running", "starting"].includes(String(status(value)).toLowerCase()));
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
  return { applicationUrl, dashboardUrl, openApp, dashboard, renderOpenApp, renderDashboard };
}));
