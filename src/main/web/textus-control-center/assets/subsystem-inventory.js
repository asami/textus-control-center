(function () {
  "use strict";

  const endpoint = "/rest/v1/textus-control-center/subsystem-inventory";
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
    detailFields: document.getElementById("detail-fields"),
    runningCount: document.getElementById("running-count"),
    stoppedCount: document.getElementById("stopped-count"),
    attentionCount: document.getElementById("attention-count"),
    registeredCount: document.getElementById("registered-count")
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

  function renderOverview() {
    const counts = records.reduce((result, record) => {
      const status = String(record.status || "").toLowerCase();
      result.total += 1;
      if (status === "running") result.running += 1;
      else if (status === "stopped" || status === "not-running") result.stopped += 1;
      else if (status === "starting" || status === "stale") result.attention += 1;
      return result;
    }, { running: 0, stopped: 0, attention: 0, total: 0 });
    elements.runningCount.textContent = counts.running;
    elements.stoppedCount.textContent = counts.stopped;
    elements.attentionCount.textContent = counts.attention;
    elements.registeredCount.textContent = counts.total;
  }

  function resetOverview() {
    [elements.runningCount, elements.stoppedCount, elements.attentionCount, elements.registeredCount].forEach((element) => {
      element.textContent = "—";
    });
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
    const visible = records.filter((record) => [record.instanceId, record.launcherKind, record.target, record.subsystemName, record.executionMode, record.hostLabel]
      .filter(Boolean).join(" ").toLowerCase().includes(query));
    elements.instances.replaceChildren();
    if (visible.length === 0) {
      elements.empty.textContent = query ? "No registered Subsystems match this filter." : "No launcher-started Subsystems are registered.";
      elements.empty.hidden = false;
      return;
    }
    visible.forEach((record) => {
      const row = document.createElement("tr");
      row.className = "inventory-row";
      row.tabIndex = 0;
      row.setAttribute("role", "button");
      row.setAttribute("aria-label", `Show details for ${componentName(record)}`);
      row.addEventListener("click", () => loadDetail(record.instanceId));
      row.addEventListener("keydown", (event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault();
          loadDetail(record.instanceId);
        }
      });
      const status = document.createElement("span");
      status.className = `status ${text(record.status).toLowerCase()}`;
      status.textContent = text(record.status);
      const statusCell = document.createElement("td");
      statusCell.append(status);
      const subsystemCell = document.createElement("td");
      const component = document.createElement("strong");
      component.textContent = componentName(record);
      const version = document.createElement("div");
      version.className = "subtle";
      version.textContent = [record.subsystemVersion, record.runtimeVersion].filter(Boolean).join(" / ") || "—";
      subsystemCell.append(component, version);
      const executionCell = document.createElement("td");
      const execution = document.createElement("span");
      execution.className = `execution-mark ${executionClass(record.executionMode)}`;
      execution.textContent = executionLabel(record.executionMode);
      const launcher = document.createElement("div");
      launcher.className = "subtle";
      launcher.textContent = text(record.launcherKind);
      executionCell.append(execution, launcher);
      const baseUrlCell = document.createElement("td");
      const baseUrl = link(text(record.baseUrl), record.baseUrl);
      if (baseUrl) {
        baseUrl.addEventListener("click", (event) => event.stopPropagation());
        baseUrlCell.append(baseUrl);
      } else baseUrlCell.textContent = "—";
      const lastSeenCell = document.createElement("td");
      lastSeenCell.textContent = formatInstant(record.lastSeenAt);
      const linksCell = document.createElement("td");
      linksCell.className = "links";
      [link("Dashboard", record.dashboardUrl), link("System Admin", record.systemAdminUrl)].filter(Boolean).forEach((item) => {
        item.addEventListener("click", (event) => event.stopPropagation());
        linksCell.append(item);
      });
      if (!linksCell.hasChildNodes()) linksCell.textContent = "—";
      [statusCell, subsystemCell, executionCell, baseUrlCell, lastSeenCell, linksCell].forEach((cell) => row.append(cell));
      elements.instances.append(row);
    });
    elements.inventory.hidden = false;
  }

  function componentName(record) {
    return text(record.subsystemName || record.target);
  }

  function executionLabel(mode) {
    const labels = {
      development: "DEV",
      repository: "CAR",
      artifact: "CAR",
      "artifact-file": "FILE"
    };
    return labels[mode] || "—";
  }

  function executionClass(mode) {
    return mode ? `execution-${String(mode).replace(/[^a-z0-9]+/gi, "-").toLowerCase()}` : "execution-unknown";
  }

  function formatInstant(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.valueOf()) ? text(value) : date.toLocaleString();
  }

  function responseMessage(body, status) {
    return body?.message || body?.error?.message || body?.conclusion?.message || `The inventory request failed (HTTP ${status}).`;
  }

  function inventoryRecord(record) {
    return {
      ...record,
      instanceId: record.instance_id,
      launcherKind: record.launcher_kind,
      executionMode: record.execution_mode,
      developmentDirectory: record.development_directory,
      subsystemName: record.subsystem_name,
      subsystemVersion: record.subsystem_version,
      runtimeVersion: record.runtime_version,
      baseUrl: record.base_url,
      hostLabel: record.host_label,
      startedAt: record.started_at,
      lastSeenAt: record.last_seen_at,
      dashboardUrl: record.dashboard_url,
      systemAdminUrl: record.system_admin_url
    };
  }

  async function request(path) {
    const response = await fetch(`${endpoint}/${path}`, { credentials: "same-origin" });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(responseMessage(body, response.status));
    return body;
  }

  async function loadInventory() {
    clearState();
    resetOverview();
    elements.loading.hidden = false;
    try {
      const response = await request("list-subsystems?offset=0&limit=100");
      records = Array.isArray(response.data) ? response.data.map(inventoryRecord) : [];
      renderOverview();
      render();
    } catch (error) {
      showError(error.message || "The subsystem inventory could not be loaded.");
    }
  }

  async function loadDetail(instanceId) {
    try {
      const response = await request(`get-subsystem?instanceId=${encodeURIComponent(instanceId)}`);
      elements.detailFields.replaceChildren();
      const record = inventoryRecord(response);
      ["instanceId", "status", "launcherKind", "executionMode", "target", "subsystemName", "subsystemVersion", "runtimeVersion", "developmentDirectory", "baseUrl", "hostLabel", "startedAt", "lastSeenAt"].forEach((name) => {
        const term = document.createElement("dt");
        term.textContent = name;
        const definition = document.createElement("dd");
        definition.textContent = name.endsWith("At") ? formatInstant(record[name]) : text(record[name]);
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
