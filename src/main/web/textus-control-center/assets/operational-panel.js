(function () {
  "use strict";

  const managementEndpoint = "/rest/v1/textus-control-center/operational-management";
  const lifecycleEndpoint = "/rest/v1/textus-control-center/lifecycle-control";
  const inventoryEndpoint = "/rest/v1/textus-control-center/subsystem-inventory";
  const catalogEndpoint = "/rest/v1/textus-control-center/car-catalog";
  const elements = {
    refresh: document.getElementById("refresh"), loading: document.getElementById("operational-loading"), empty: document.getElementById("operational-empty"),
    error: document.getElementById("operational-error"), inventory: document.getElementById("operational-inventory"), rows: document.getElementById("operational-component-rows"),
    dialog: document.getElementById("operational-detail-dialog"), closeDetail: document.getElementById("close-operational-detail"), detailFields: document.getElementById("operational-detail-fields"),
    lifecycleHistory: document.getElementById("lifecycle-request-history"), sources: document.getElementById("operational-component-sources")
  };
  let components = [];
  let invocations = [];

  function text(value) { return value === undefined || value === null || value === "" ? "—" : String(value); }
  function formatInstant(value) { const date = new Date(value); return !value || Number.isNaN(date.valueOf()) ? text(value) : date.toLocaleString(); }
  function message(body, status) { return body?.message || body?.error?.message || body?.conclusion?.message || `The operation failed (HTTP ${status}).`; }
  function clearState() { elements.loading.hidden = true; elements.empty.hidden = true; elements.error.hidden = true; elements.inventory.hidden = true; }
  function showError(value) { clearState(); elements.error.textContent = value; elements.error.hidden = false; }
  function componentRecord(value) { return { artifactId: value.artifact_id, managementState: value.management_state, firstManagedAt: value.first_managed_at, lastObservedAt: value.last_observed_at }; }
  function invocationRecord(value) { return { artifactId: value.artifact_id, status: value.status, instanceId: value.instance_id, baseUrl: value.base_url }; }
  function catalogRecord(value) { return { componentName: value.component_name, sources: Array.isArray(value.sources) ? value.sources : [] }; }
  async function request(endpoint, path) {
    const response = await fetch(`${endpoint}/${path}`, { credentials: "same-origin" });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(message(body, response.status));
    return body;
  }
  function runtime(component) {
    const values = invocations.filter((value) => value.artifactId === component.artifactId).map((value) => String(value.status || "").toLowerCase());
    if (values.includes("running")) return "running";
    if (values.includes("starting")) return "starting";
    if (values.includes("stale")) return "stale";
    if (values.includes("stopped")) return "stopped";
    return "not-running";
  }
  function activeUrls(component) { return invocations.filter((value) => value.artifactId === component.artifactId && value.baseUrl && ["running", "starting"].includes(String(value.status || "").toLowerCase())).map((value) => value.baseUrl); }
  function button(label, action, component) {
    const value = document.createElement("button");
    value.type = "button"; value.className = "button secondary operational-action"; value.textContent = label;
    value.addEventListener("click", (event) => { event.stopPropagation(); requestLifecycle(action, component, value); });
    return value;
  }
  function render() {
    clearState(); elements.rows.replaceChildren();
    if (!components.length) { elements.empty.hidden = false; return; }
    components.forEach((component) => {
      const row = document.createElement("tr"); row.className = "inventory-row"; row.tabIndex = 0; row.setAttribute("role", "button");
      row.addEventListener("click", () => loadDetail(component)); row.addEventListener("keydown", (event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); loadDetail(component); } });
      const state = runtime(component); const runtimeCell = document.createElement("td"); const mark = document.createElement("span"); mark.className = `status ${state}`; mark.textContent = state; runtimeCell.append(mark);
      const componentCell = document.createElement("td"); const name = document.createElement("strong"); name.textContent = text(component.artifactId); componentCell.append(name);
      const managementCell = document.createElement("td"); const management = document.createElement("span"); management.className = "execution-mark execution-development"; management.textContent = text(component.managementState); managementCell.append(management);
      const observedCell = document.createElement("td"); observedCell.textContent = formatInstant(component.lastObservedAt);
      const actionsCell = document.createElement("td"); actionsCell.className = "operational-actions";
      [button("Start", "start", component), button("Stop", "stop", component), button("Restart", "restart", component)].forEach((value) => actionsCell.append(value));
      const remove = document.createElement("button"); remove.type = "button"; remove.className = "button secondary operational-action"; remove.textContent = "Remove";
      remove.addEventListener("click", (event) => { event.stopPropagation(); removeComponent(component, remove); }); actionsCell.append(remove);
      [runtimeCell, componentCell, managementCell, observedCell, actionsCell].forEach((cell) => row.append(cell)); elements.rows.append(row);
    });
    elements.inventory.hidden = false;
  }
  async function load() {
    clearState(); elements.loading.hidden = false;
    try {
      const [managed, registered] = await Promise.all([
        request(managementEndpoint, "list-operational-components?offset=0&limit=100"),
        request(inventoryEndpoint, "list-subsystems?offset=0&limit=100")
      ]);
      components = Array.isArray(managed.data) ? managed.data.map(componentRecord) : [];
      invocations = Array.isArray(registered.data) ? registered.data.map(invocationRecord) : [];
      render();
    } catch (error) { showError(error.message || "The operational component panel could not be loaded."); }
  }
  async function requestLifecycle(action, component, control) {
    control.disabled = true;
    try {
      const key = `${action}-${component.artifactId}-${Date.now()}`;
      const result = await request(lifecycleEndpoint, `${action}-operational-component?artifactId=${encodeURIComponent(component.artifactId)}&idempotencyKey=${encodeURIComponent(key)}`);
      await loadDetail(component, result);
    } catch (error) { showError(error.message || "The lifecycle request could not be recorded."); }
    finally { control.disabled = false; }
  }
  async function removeComponent(component, control) {
    control.disabled = true;
    try { await request(managementEndpoint, `remove-operational-component?artifactId=${encodeURIComponent(component.artifactId)}`); await load(); }
    catch (error) { showError(error.message || "The component could not be removed from management."); }
    finally { control.disabled = false; }
  }
  async function loadDetail(component, latest) {
    try {
      const [detail, history, catalog] = await Promise.all([
        request(managementEndpoint, `get-operational-component?artifactId=${encodeURIComponent(component.artifactId)}`),
        request(lifecycleEndpoint, `list-lifecycle-requests?artifactId=${encodeURIComponent(component.artifactId)}&offset=0&limit=20`),
        request(catalogEndpoint, `get-managed-car?artifactId=${encodeURIComponent(component.artifactId)}`).catch(() => null)
      ]);
      const record = componentRecord(detail); const source = catalog ? catalogRecord(catalog) : { componentName: null, sources: [] }; const urls = activeUrls(record); elements.detailFields.replaceChildren(); elements.sources.replaceChildren(); elements.lifecycleHistory.replaceChildren();
      [["Artifact ID", record.artifactId], ["Component", source.componentName], ["Management", record.managementState], ["Runtime", runtime(record)], ["Active URL", urls.join(", ")], ["First managed", formatInstant(record.firstManagedAt)], ["Last observed", formatInstant(record.lastObservedAt)]].forEach(([label, value]) => { const term = document.createElement("dt"); term.textContent = label; const definition = document.createElement("dd"); definition.textContent = text(value); elements.detailFields.append(term, definition); });
      if (!source.sources.length) { elements.sources.textContent = "No source facts are available."; }
      source.sources.forEach((value) => { const item = document.createElement("p"); item.className = "subtle"; item.textContent = [value.source_kind, value.source_id, value.refresh_state, value.recommended_version || value.latest_version, value.diagnostic, value.private_locator].filter(Boolean).join(" · "); elements.sources.append(item); });
      const requests = Array.isArray(history.data) ? history.data : [];
      if (!requests.length) { elements.lifecycleHistory.textContent = latest ? `${text(latest.lifecycle_action)}: ${text(latest.request_state)}` : "No lifecycle requests have been recorded."; }
      requests.forEach((value) => {
        const item = document.createElement("section"); item.className = "catalog-source";
        const title = document.createElement("h4"); title.textContent = [text(value.lifecycle_action), text(value.request_state)].join(" · "); item.append(title);
        [["Request", value.request_id], ["Supervisor", value.supervisor_id], ["Instance", value.instance_id], ["Requested", formatInstant(value.requested_at)], ["Accepted", formatInstant(value.accepted_at)], ["Completed", formatInstant(value.completed_at)], ["Diagnostic", value.diagnostic_code || value.diagnostic]].forEach(([label, fact]) => {
          if (fact && fact !== "—") { const detail = document.createElement("p"); detail.textContent = `${label}: ${fact}`; item.append(detail); }
        });
        elements.lifecycleHistory.append(item);
      });
      elements.dialog.showModal();
    } catch (error) { showError(error.message || "The operational component detail could not be loaded."); }
  }
  elements.refresh.addEventListener("click", load); elements.closeDetail.addEventListener("click", () => elements.dialog.close()); load();
}());
