(function () {
  "use strict";

  const endpoint = "/rest/v1/textus-admin/subsystem-inventory";
  const elements = {
    refresh: document.getElementById("refresh"),
    search: document.getElementById("search"),
    loading: document.getElementById("loading"),
    empty: document.getElementById("empty"),
    error: document.getElementById("error"),
    inventory: document.getElementById("inventory"),
    instances: document.getElementById("instances"),
    dialog: document.getElementById("detail-dialog"),
    closeDetail: document.getElementById("close-detail"),
    detailFields: document.getElementById("detail-fields")
  };

  let records = [];

  function text(value) {
    return value === undefined || value === null || value === "" ? "—" : String(value);
  }

  function clearState() {
    elements.loading.hidden = true;
    elements.empty.hidden = true;
    elements.error.hidden = true;
    elements.inventory.hidden = true;
  }

  function showError(message) {
    clearState();
    elements.error.textContent = message;
    elements.error.hidden = false;
  }

  function link(label, url) {
    if (!url) return "";
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.target = "_blank";
    anchor.rel = "noreferrer";
    anchor.textContent = label;
    return anchor;
  }

  function render() {
    clearState();
    const query = elements.search.value.trim().toLowerCase();
    const visible = records.filter((record) => [record.instanceId, record.launcherKind, record.target, record.hostLabel]
      .filter(Boolean).join(" ").toLowerCase().includes(query));
    elements.instances.replaceChildren();
    if (visible.length === 0) {
      elements.empty.textContent = query ? "No registered Subsystems match this filter." : "No launcher-started Subsystems are registered.";
      elements.empty.hidden = false;
      return;
    }
    visible.forEach((record) => {
      const row = document.createElement("tr");
      const status = document.createElement("span");
      status.className = `status ${text(record.status).toLowerCase()}`;
      status.textContent = text(record.status);
      const statusCell = document.createElement("td");
      statusCell.append(status);
      const instance = document.createElement("button");
      instance.className = "button secondary";
      instance.type = "button";
      instance.textContent = text(record.instanceId);
      instance.addEventListener("click", () => loadDetail(record.instanceId));
      const instanceCell = document.createElement("td");
      instanceCell.append(instance);
      const launcherCell = document.createElement("td");
      launcherCell.innerHTML = `${escapeHtml(text(record.launcherKind))}<br><span class="subtle">${escapeHtml(text(record.target))}</span>`;
      const subsystemCell = document.createElement("td");
      subsystemCell.innerHTML = `${escapeHtml(text(record.subsystemName))}<br><span class="subtle">${escapeHtml(text(record.subsystemVersion))} / ${escapeHtml(text(record.runtimeVersion))}</span>`;
      const baseUrlCell = document.createElement("td");
      const baseUrl = link(text(record.baseUrl), record.baseUrl);
      if (baseUrl) baseUrlCell.append(baseUrl); else baseUrlCell.textContent = "—";
      const lastSeenCell = document.createElement("td");
      lastSeenCell.textContent = formatInstant(record.lastSeenAt);
      const linksCell = document.createElement("td");
      linksCell.className = "links";
      [link("Dashboard", record.dashboardUrl), link("System Admin", record.systemAdminUrl)].filter(Boolean).forEach((item) => linksCell.append(item));
      if (!linksCell.hasChildNodes()) linksCell.textContent = "—";
      [statusCell, instanceCell, launcherCell, subsystemCell, baseUrlCell, lastSeenCell, linksCell].forEach((cell) => row.append(cell));
      elements.instances.append(row);
    });
    elements.inventory.hidden = false;
  }

  function escapeHtml(value) {
    const node = document.createElement("span");
    node.textContent = value;
    return node.innerHTML;
  }

  function formatInstant(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.valueOf()) ? text(value) : date.toLocaleString();
  }

  function responseMessage(body, status) {
    return body?.message || body?.error?.message || body?.conclusion?.message || `The inventory request failed (HTTP ${status}).`;
  }

  async function request(path) {
    const response = await fetch(`${endpoint}/${path}`, { credentials: "same-origin" });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(responseMessage(body, response.status));
    return body;
  }

  async function loadInventory() {
    clearState();
    elements.loading.hidden = false;
    try {
      const response = await request("list-subsystems?offset=0&limit=100");
      records = Array.isArray(response.data) ? response.data : [];
      render();
    } catch (error) {
      showError(error.message || "The subsystem inventory could not be loaded.");
    }
  }

  async function loadDetail(instanceId) {
    try {
      const response = await request(`get-subsystem?instanceId=${encodeURIComponent(instanceId)}`);
      elements.detailFields.replaceChildren();
      ["instanceId", "status", "launcherKind", "target", "subsystemName", "subsystemVersion", "runtimeVersion", "baseUrl", "hostLabel", "startedAt", "lastSeenAt"].forEach((name) => {
        const term = document.createElement("dt");
        term.textContent = name;
        const definition = document.createElement("dd");
        definition.textContent = name.endsWith("At") ? formatInstant(response[name]) : text(response[name]);
        elements.detailFields.append(term, definition);
      });
      elements.dialog.showModal();
    } catch (error) {
      showError(error.message || "The subsystem detail could not be loaded.");
    }
  }

  elements.refresh.addEventListener("click", loadInventory);
  elements.search.addEventListener("input", render);
  elements.closeDetail.addEventListener("click", () => elements.dialog.close());
  loadInventory();
}());
