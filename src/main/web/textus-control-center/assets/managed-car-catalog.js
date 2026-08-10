(function () {
  "use strict";

  const endpoint = "/rest/v1/org-simplemodeling-textus-control-center/car-catalog";
  const elements = {
    refresh: document.getElementById("refresh"), search: document.getElementById("search"), loading: document.getElementById("loading"),
    empty: document.getElementById("empty"), error: document.getElementById("error"), catalog: document.getElementById("catalog"),
    cars: document.getElementById("cars"), dialog: document.getElementById("detail-dialog"), closeDetail: document.getElementById("close-detail"),
    detailFields: document.getElementById("detail-fields"), detailSources: document.getElementById("detail-sources")
  };
  let records = [];

  function text(value) { return value === undefined || value === null || value === "" ? "—" : String(value); }
  function clearState() { elements.loading.hidden = true; elements.empty.hidden = true; elements.error.hidden = true; elements.catalog.hidden = true; }
  function showError(message) { clearState(); elements.error.textContent = message; elements.error.hidden = false; }
  function responseMessage(body, status) { return body?.message || body?.error?.message || body?.conclusion?.message || `The CAR catalog request failed (HTTP ${status}).`; }
  function formatInstant(value) { const date = new Date(value); return !value || Number.isNaN(date.valueOf()) ? text(value) : date.toLocaleString(); }
  function sourceRecord(source) { return { sourceId: source.source_id, sourceKind: source.source_kind, refreshState: source.refresh_state, componentName: source.component_name, recommendedVersion: source.recommended_version, latestVersion: source.latest_version, snapshotAt: source.snapshot_at, diagnostic: source.diagnostic, privateLocator: source.private_locator }; }
  function catalogRecord(record) { return { artifactId: record.artifact_id, componentName: record.component_name, runtimeState: record.runtime_state, activeInstanceIds: record.active_instance_ids || [], staleInstanceIds: record.stale_instance_ids || [], createdAt: record.created_at, updatedAt: record.updated_at, sources: Array.isArray(record.sources) ? record.sources.map(sourceRecord) : [] }; }
  function sourceMarks(record) { return record.sources.map((source) => source.sourceKind).filter(Boolean).join(" / ") || "—"; }
  function selectedVersion(record) { const versions = record.sources.flatMap((source) => [source.recommendedVersion, source.latestVersion]).filter(Boolean); return versions.length ? [...new Set(versions)].join(" / ") : "—"; }
  function runtimeClass(state) { return `status ${String(state || "not-running").toLowerCase()}`; }

  async function request(path) {
    const response = await fetch(`${endpoint}/${path}`, { credentials: "same-origin" });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(responseMessage(body, response.status));
    return body;
  }

  function render() {
    clearState();
    const query = elements.search.value.trim().toLowerCase();
    const visible = records
      .filter((record) => [record.artifactId, record.componentName, ...record.sources.flatMap((source) => [source.sourceKind, source.sourceId, source.refreshState, source.componentName, source.recommendedVersion, source.latestVersion, source.diagnostic])].filter(Boolean).join(" ").toLowerCase().includes(query));
    elements.cars.replaceChildren();
    if (!visible.length) { elements.empty.textContent = query ? "No managed CARs match this filter." : "No managed CARs are available from the configured sources."; elements.empty.hidden = false; return; }
    visible.forEach((record) => {
      const row = document.createElement("tr"); row.className = "inventory-row"; row.tabIndex = 0; row.setAttribute("role", "button"); row.setAttribute("aria-label", `Show details for ${record.artifactId}`);
      row.addEventListener("click", () => loadDetail(record.artifactId)); row.addEventListener("keydown", (event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); loadDetail(record.artifactId); } });
      const runtime = document.createElement("span"); runtime.className = runtimeClass(record.runtimeState); runtime.textContent = text(record.runtimeState);
      const runtimeCell = document.createElement("td"); runtimeCell.append(runtime);
      const carCell = document.createElement("td"); const artifact = document.createElement("strong"); artifact.textContent = text(record.artifactId); const component = document.createElement("div"); component.className = "subtle"; component.textContent = text(record.componentName); carCell.append(artifact, component);
      const sourcesCell = document.createElement("td"); const marks = document.createElement("span"); marks.className = "source-marks"; marks.textContent = sourceMarks(record); sourcesCell.append(marks);
      const versionCell = document.createElement("td"); versionCell.textContent = selectedVersion(record);
      const instancesCell = document.createElement("td"); instancesCell.textContent = record.activeInstanceIds.length ? record.activeInstanceIds.join(", ") : (record.staleInstanceIds.length ? `Stale: ${record.staleInstanceIds.join(", ")}` : "—");
      [runtimeCell, carCell, sourcesCell, versionCell, instancesCell].forEach((cell) => row.append(cell)); elements.cars.append(row);
    });
    elements.catalog.hidden = false;
  }

  async function loadCatalog() {
    clearState(); elements.loading.hidden = false;
    try { const response = await request("list-managed-cars?offset=0&limit=100"); records = Array.isArray(response.data) ? response.data.map(catalogRecord) : []; render(); }
    catch (error) { showError(error.message || "The managed CAR catalog could not be loaded."); }
  }

  async function refreshCatalog() {
    elements.refresh.disabled = true;
    try { await request("refresh-car-catalog"); await loadCatalog(); }
    catch (error) { showError(error.message || "The managed CAR catalog could not be refreshed."); }
    finally { elements.refresh.disabled = false; }
  }

  async function loadDetail(artifactId) {
    try {
      const response = catalogRecord(await request(`get-managed-car?artifactId=${encodeURIComponent(artifactId)}`));
      elements.detailFields.replaceChildren(); elements.detailSources.replaceChildren();
      [["Artifact ID", response.artifactId], ["Component", response.componentName], ["Runtime", response.runtimeState], ["Active instances", response.activeInstanceIds.join(", ")], ["Stale instances", response.staleInstanceIds.join(", ")], ["First observed", formatInstant(response.createdAt)], ["Last observed", formatInstant(response.updatedAt)]].forEach(([name, value]) => { const term = document.createElement("dt"); term.textContent = name; const definition = document.createElement("dd"); definition.textContent = text(value); elements.detailFields.append(term, definition); });
      response.sources.forEach((source) => { const item = document.createElement("article"); item.className = "catalog-source"; const heading = document.createElement("h4"); heading.textContent = `${text(source.sourceKind)} · ${text(source.sourceId)}`; const facts = document.createElement("p"); facts.textContent = [source.refreshState, source.recommendedVersion, source.latestVersion, source.diagnostic].filter(Boolean).join(" · ") || "No source facts"; item.append(heading, facts); if (source.privateLocator) { const locator = document.createElement("p"); locator.className = "subtle"; locator.textContent = source.privateLocator; item.append(locator); } elements.detailSources.append(item); });
      elements.dialog.showModal();
    } catch (error) { showError(error.message || "The managed CAR detail could not be loaded."); }
  }

  elements.refresh.addEventListener("click", refreshCatalog); elements.search.addEventListener("input", render); elements.closeDetail.addEventListener("click", () => elements.dialog.close()); loadCatalog();
}());
