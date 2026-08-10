(function () {
  "use strict";

  const managementEndpoint = "/rest/v1/org-simplemodeling-textus-control-center/operational-management";
  const lifecycleEndpoint = "/rest/v1/org-simplemodeling-textus-control-center/lifecycle-control";
  const lifecyclePollIntervalMs = 250;
  const lifecyclePollTimeoutMs = 20000;
  const runtimeObservationPollIntervalMs = 750;
  const runtimeObservationTimeoutMs = 60000;
  const inventoryEndpoint = "/rest/v1/org-simplemodeling-textus-control-center/subsystem-inventory";
  const catalogEndpoint = "/rest/v1/org-simplemodeling-textus-control-center/car-catalog";
  const evidenceEndpoint = "/rest/v1/org-simplemodeling-textus-control-center/launcher-evidence";
  const elements = {
    refresh: document.getElementById("refresh"), loading: document.getElementById("operational-loading"), empty: document.getElementById("operational-empty"),
    error: document.getElementById("operational-error"), inventory: document.getElementById("operational-inventory"), rows: document.getElementById("operational-component-rows"), catalogStatus: document.getElementById("operational-catalog-status"), evidenceStatus: document.getElementById("operational-evidence-status"),
    dialog: document.getElementById("operational-detail-dialog"), closeDetail: document.getElementById("close-operational-detail"), detailFields: document.getElementById("operational-detail-fields"),
    lifecycleHistory: document.getElementById("lifecycle-request-history"), sources: document.getElementById("operational-component-sources")
  };
  let components = [];
  let invocations = [];
  let evidence = [];
  let catalog = [];
  const rowStates = new Map();

  function text(value) { return value === undefined || value === null || value === "" ? "—" : String(value); }
  function formatInstant(value) { const date = new Date(value); return !value || Number.isNaN(date.valueOf()) ? text(value) : date.toLocaleString(); }
  function message(body, status) { return body?.message || body?.error?.message || body?.conclusion?.message || `The operation failed (HTTP ${status}).`; }
  function clearState() { elements.loading.hidden = true; elements.empty.hidden = true; elements.error.hidden = true; elements.inventory.hidden = true; }
  function showError(value) { clearState(); elements.error.textContent = value; elements.error.hidden = false; }
  function showCatalogStatus(value, unavailable) { elements.catalogStatus.textContent = value; elements.catalogStatus.classList.toggle("unavailable", Boolean(unavailable)); }
  function showEvidenceStatus(value, unavailable) { elements.evidenceStatus.textContent = value; elements.evidenceStatus.classList.toggle("unavailable", Boolean(unavailable)); }
  function componentRecord(value) { return { artifactId: value.artifact_id, managementState: value.management_state, firstManagedAt: value.first_managed_at, lastObservedAt: value.last_observed_at }; }
  function invocationRecord(value) {
    return {
      artifactId: value.artifact_id || value.artifactId || value.target,
      status: value.status,
      instanceId: value.instance_id || value.instanceId,
      baseUrl: value.base_url || value.baseUrl,
      target: value.target
    };
  }
  function evidenceRecord(value) { return { artifactId: value.artifact_id, instanceId: value.instance_id, decision: value.evidence_decision, stoppedAt: value.stopped_at, launcherKind: value.launcher_kind, executionMode: value.execution_mode, lastSeenAt: value.last_seen_at }; }
  function catalogRecord(value) {
    return {
      artifactId: value.artifact_id || value.artifactId,
      componentName: value.component_name || value.componentName,
      runtimeState: value.runtime_state || value.runtimeState,
      activeInstanceIds: Array.isArray(value.active_instance_ids) ? value.active_instance_ids : (Array.isArray(value.activeInstanceIds) ? value.activeInstanceIds : []),
      observedBaseUrls: Array.isArray(value.observed_base_urls) ? value.observed_base_urls : (Array.isArray(value.observedBaseUrls) ? value.observedBaseUrls : []),
      sources: Array.isArray(value.sources) ? value.sources : []
    };
  }
  function rowState(component) { return rowStates.get(component.artifactId); }
  function runtimeClass(state) { return String(state || "unknown").toLowerCase().replace(/[^a-z0-9-]/g, "-"); }
  function runtimeBadge(state) {
    const value = String(state || "unknown").toLowerCase();
    return value === "not-running" || value === "stopped" || value === "running" || value === "starting" || value === "launching" || value === "stopping" || value === "stale" || value === "evidence-current" ? value : "unknown";
  }
  function expectedRuntime(action) { return action === "stop" ? ["not-running", "stopped"] : ["running"]; }
  function authoritativeRuntime(component) { return rowState(component)?.authoritativeState || runtimeBadge(catalog.find((value) => value.artifactId === component.artifactId)?.runtimeState || runtime(component)); }
  function actionAvailability(state, action, candidateAvailable) {
    const value = runtimeBadge(state);
    if (["launching", "starting", "stopping"].includes(value)) return false;
    if (value === "not-running" || value === "stopped") return action === "start" && candidateAvailable;
    if (value === "running") return action === "stop" || (action === "restart" && candidateAvailable);
    return false;
  }
  function actionTitle(state, action, candidateAvailable) {
    if ((action === "start" || action === "restart") && !candidateAvailable && ["not-running", "stopped", "running"].includes(runtimeBadge(state))) return "Select an available source before starting or restarting.";
    return `Lifecycle action unavailable while runtime state is ${state}.`;
  }
  function updateActionControls(component, state) {
    const current = rowState(component);
    if (!current || current.busy) return;
    const candidateAvailable = candidates(component).length > 0;
    current.controls.forEach((control, action) => {
      const enabled = actionAvailability(state, action, candidateAvailable);
      control.disabled = !enabled;
      control.title = enabled ? "" : actionTitle(state, action, candidateAvailable);
    });
  }
  function updateRuntimeRow(component, state, authoritative) {
    const current = rowState(component);
    if (!current) return;
    const normalized = runtimeBadge(state);
    if (authoritative) current.authoritativeState = normalized;
    current.runtimeBadge.className = `status ${runtimeClass(normalized)}`;
    current.runtimeBadge.textContent = normalized;
    current.row.dataset.runtimeState = normalized;
    updateActionControls(component, normalized);
  }
  function setRowFeedback(component, value, tone) {
    const current = rowState(component);
    if (!current) return;
    current.feedback.className = `lifecycle-feedback${tone ? ` ${tone}` : ""}`;
    current.feedback.textContent = value;
    current.feedback.hidden = !value;
  }
  function setLifecycleBusy(component, action, busy) {
    const current = rowState(component);
    if (!current) return;
    current.busy = busy;
    current.controls.forEach((control, controlAction) => {
      control.disabled = busy || !actionAvailability(authoritativeRuntime(component), controlAction, candidates(component).length > 0);
      control.title = busy ? "A lifecycle request is already in progress for this component." : (control.disabled ? actionTitle(authoritativeRuntime(component), controlAction, candidates(component).length > 0) : "");
      control.textContent = busy && controlAction === action ? ({ start: "Starting…", stop: "Stopping…", restart: "Restarting…" }[action]) : ({ start: "Start", stop: "Stop", restart: "Restart" }[controlAction]);
    });
  }
  async function request(endpoint, path, signal) {
    const response = await fetch(`${endpoint}/${path}`, { credentials: "same-origin", ...(signal ? { signal } : {}) });
    const body = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(message(body, response.status));
    return body;
  }
  function timedOutLifecycleResult(result, requestId) { return { ...result, request_id: requestId, request_state: "timed-out", diagnostic: `Request ${requestId}: terminal status was not observed before timeout.` }; }
  function runtime(component) {
    const values = invocations.filter((value) => value.artifactId === component.artifactId).map((value) => String(value.status || "").toLowerCase());
    if (values.includes("running")) return "running";
    if (values.includes("starting")) return "starting";
    if (values.includes("stale")) return "stale";
    if (values.includes("stopped")) return "stopped";
    const observed = evidence.filter((value) => value.artifactId === component.artifactId && !value.stoppedAt).map((value) => String(value.decision || "").toLowerCase());
    if (observed.includes("current-registered")) return "running";
    if (observed.includes("current-evidence-only")) return "evidence-current";
    return catalog.find((value) => value.artifactId === component.artifactId)?.runtimeState || "not-running";
  }
  function activeUrls(component) {
    const registered = invocations.filter((value) => value.artifactId === component.artifactId && value.baseUrl && ["running", "starting"].includes(String(value.status || "").toLowerCase())).map((value) => value.baseUrl);
    return registered.length ? registered : (catalog.find((value) => value.artifactId === component.artifactId)?.observedBaseUrls || []);
  }
  function appLink(component) {
    const urls = activeUrls(component);
    const value = document.createElement(urls.length ? "a" : "button");
    value.className = "button secondary operational-action"; value.textContent = "Open app";
    if (urls.length) { value.href = urls[0]; value.target = "_blank"; value.rel = "noopener"; }
    else { value.type = "button"; value.disabled = true; value.title = "The component has no active application URL."; }
    return value;
  }
  function updateOpenApp(component) {
    const current = rowState(component);
    if (!current) return;
    const next = appLink(component);
    current.openApp.replaceWith(next);
    current.openApp = next;
  }
  function button(label, action, component, resolveCandidate) {
    const value = document.createElement("button");
    value.type = "button"; value.className = "button secondary operational-action"; value.textContent = label; value.dataset.lifecycleAction = action;
    value.addEventListener("click", (event) => { event.stopPropagation(); requestLifecycle(action, component, action === "stop" ? null : resolveCandidate(), value); });
    return value;
  }
  function candidates(component) { return catalog.find((value) => value.artifactId === component.artifactId)?.sources.filter((source) => String(source.refresh_state || source.refreshState).toLowerCase() === "available") || []; }
  function sourceKind(candidate) { return candidate.source_kind || candidate.sourceKind; }
  function sourceId(candidate) { return candidate.source_id || candidate.sourceId; }
  function sourceVersion(candidate) { return candidate.recommended_version || candidate.latest_version || candidate.recommendedVersion || candidate.latestVersion || "Version unavailable"; }
  function sourceKindLabel(candidate) {
    const kind = String(sourceKind(candidate) || "").toUpperCase();
    return { DEV: "Development", PUBLIC: "Release", LOCAL: "Local release" }[kind] || text(sourceKind(candidate));
  }
  function sourceLabel(candidate) { return [sourceKindLabel(candidate), sourceVersion(candidate), `Source: ${text(sourceId(candidate))}`].filter(Boolean).join(" · "); }
  function appendLaunchVersionDetails(parent, candidate) {
    const details = document.createElement("span"); details.className = "launch-version-option-details";
    const kindVersion = document.createElement("span"); kindVersion.className = "launch-version-kind-version";
    const kind = document.createElement("strong"); kind.className = "launch-version-kind"; kind.textContent = sourceKindLabel(candidate);
    const version = document.createElement("span"); version.className = "launch-version-version"; version.textContent = sourceVersion(candidate);
    kindVersion.append(kind, version);
    const source = document.createElement("span"); source.className = "launch-version-source"; source.textContent = `Source: ${text(sourceId(candidate))}`;
    details.append(kindVersion, source); parent.append(details);
  }
  function render() {
    clearState(); elements.rows.replaceChildren(); rowStates.clear();
    if (!components.length) { elements.empty.hidden = false; return; }
    components.forEach((component, componentIndex) => {
      const values = candidates(component);
      const row = document.createElement("tr"); row.className = "inventory-row"; row.tabIndex = 0; row.setAttribute("role", "button");
      row.addEventListener("click", () => loadDetail(component)); row.addEventListener("keydown", (event) => { if (event.key === "Enter" || event.key === " ") { event.preventDefault(); loadDetail(component); } });
      const state = runtime(component); const runtimeCell = document.createElement("td"); const mark = document.createElement("span"); mark.className = `status ${runtimeClass(state)}`; mark.textContent = state; runtimeCell.append(mark);
      const componentCell = document.createElement("td"); const name = document.createElement("strong"); name.textContent = text(component.artifactId); componentCell.append(name);
      let resolveCandidate = () => values[0] || null;
      if (values.length > 1) {
        const launchChoices = document.createElement("fieldset"); launchChoices.className = "launch-version-choice";
        const legend = document.createElement("legend"); legend.textContent = "Launch version"; launchChoices.append(legend);
        const groupName = `launch-version-${componentIndex}-${String(component.artifactId).replace(/[^a-z0-9_-]/gi, "-")}`;
        values.forEach((candidate, index) => {
          const option = document.createElement("label"); option.className = "launch-version-option";
          const radio = document.createElement("input"); radio.type = "radio"; radio.name = groupName; radio.value = String(index); radio.checked = index === 0;
          option.append(radio); appendLaunchVersionDetails(option, candidate); launchChoices.append(option);
        });
        componentCell.append(launchChoices);
        resolveCandidate = () => {
          const checked = launchChoices.querySelector("input[type=\"radio\"]:checked");
          return values[Number(checked?.value)] || null;
        };
        launchChoices.addEventListener("click", (event) => event.stopPropagation()); launchChoices.addEventListener("keydown", (event) => event.stopPropagation());
      } else {
        const source = document.createElement("div"); source.className = values.length ? "launch-version-summary" : "launch-version-unavailable";
        if (values.length) {
          const label = document.createElement("span"); label.className = "launch-version-summary-label"; label.textContent = "Launch version";
          const details = document.createElement("span"); details.className = "launch-version-summary-details"; details.textContent = sourceLabel(values[0]); source.append(label, details);
        } else {
          source.textContent = "Launch version unavailable — No available source";
        }
        componentCell.append(source);
      }
      const managementCell = document.createElement("td"); const management = document.createElement("span"); management.className = "execution-mark execution-development"; management.textContent = text(component.managementState); managementCell.append(management);
      const observedCell = document.createElement("td"); observedCell.textContent = formatInstant(component.lastObservedAt);
      const actionsCell = document.createElement("td"); actionsCell.className = "operational-actions";
      const lifecycleControls = new Map();
      [button("Start", "start", component, resolveCandidate), button("Stop", "stop", component, () => null), button("Restart", "restart", component, resolveCandidate)].forEach((value) => { lifecycleControls.set(value.dataset.lifecycleAction, value); actionsCell.append(value); });
      const openApp = appLink(component); actionsCell.append(openApp);
      const remove = document.createElement("button"); remove.type = "button"; remove.className = "button secondary operational-action"; remove.textContent = "Remove";
      remove.addEventListener("click", (event) => { event.stopPropagation(); removeComponent(component, remove); }); actionsCell.append(remove);
      const feedback = document.createElement("div"); feedback.className = "lifecycle-feedback"; feedback.hidden = true; feedback.setAttribute("role", "status"); feedback.setAttribute("aria-live", "polite");
      runtimeCell.append(feedback);
      row.dataset.artifactId = component.artifactId; row.dataset.runtimeState = runtimeBadge(state);
      [runtimeCell, componentCell, managementCell, observedCell, actionsCell].forEach((cell) => row.append(cell)); elements.rows.append(row);
      rowStates.set(component.artifactId, { row, runtimeBadge: mark, feedback, controls: lifecycleControls, openApp, authoritativeState: runtimeBadge(state), busy: false });
      updateActionControls(component, state);
    });
    elements.inventory.hidden = false;
  }
  async function load() {
    clearState(); elements.loading.hidden = false;
    try {
      try {
        await request(catalogEndpoint, "refresh-car-catalog");
        showCatalogStatus("Registered development sources were refreshed before operational components loaded.", false);
      } catch (error) {
        showCatalogStatus("Registered development source refresh is temporarily unavailable. Retained operational components remain usable.", true);
      }
      try {
        const refresh = await request(evidenceEndpoint, "refresh-launcher-evidence?refresh=true");
        showEvidenceStatus(`Launcher evidence reconciled at ${formatInstant(refresh.observed_at || refresh.observedAt)}. Current evidence is observation only; lifecycle actions remain Launcher-authorized.`, false);
      } catch (error) {
        showEvidenceStatus("Launcher evidence is temporarily unavailable. Retained inventory and management rows remain usable; no lifecycle authority is inferred from unavailable evidence.", true);
      }
      const [managed, registered, observed, discovered] = await Promise.all([
        request(managementEndpoint, "list-operational-components?offset=0&limit=100"),
        request(inventoryEndpoint, "list-subsystems?offset=0&limit=100"),
        request(evidenceEndpoint, "list-launcher-evidence?offset=0&limit=100"),
        request(catalogEndpoint, "list-managed-cars?offset=0&limit=100")
      ]);
      components = Array.isArray(managed.data) ? managed.data.map(componentRecord) : [];
      invocations = Array.isArray(registered.data) ? registered.data.map(invocationRecord) : [];
      evidence = Array.isArray(observed.data) ? observed.data.map(evidenceRecord) : [];
      catalog = Array.isArray(discovered.data) ? discovered.data.map(catalogRecord) : [];
      render();
    } catch (error) { showError(error.message || "The operational component panel could not be loaded."); }
  }
  async function observeRuntime(action, component, result) {
    const acceptedState = String(result.request_state || "accepted").toLowerCase();
    const actionLabel = action.charAt(0).toUpperCase() + action.slice(1);
    setRowFeedback(component, `Supervisor ${acceptedState} ${actionLabel.toLowerCase()}; observing runtime state…`, "busy");
    const deadline = Date.now() + runtimeObservationTimeoutMs;
    while (Date.now() < deadline) {
      const remaining = deadline - Date.now();
      const controller = new AbortController();
      const abortTimer = setTimeout(() => controller.abort(), remaining);
      try {
        const observed = catalogRecord(await request(catalogEndpoint, `get-managed-car?artifactId=${encodeURIComponent(component.artifactId)}`, controller.signal));
        const index = catalog.findIndex((value) => value.artifactId === component.artifactId);
        if (index >= 0) catalog[index] = observed; else catalog.push(observed);
        const observedState = runtimeBadge(observed.runtimeState);
        updateRuntimeRow(component, observedState, true);
        let urlDiagnostic = "";
        if (observedState === "running") {
          try {
            const activeInstanceIds = observed.activeInstanceIds;
            const records = await Promise.all(activeInstanceIds.map((instanceId) => request(inventoryEndpoint, `get-subsystem?instanceId=${encodeURIComponent(instanceId)}`, controller.signal)));
            const mapped = records.map(invocationRecord);
            invocations = invocations.filter((value) => value.artifactId !== component.artifactId);
            invocations.push(...mapped);
            updateOpenApp(component);
            if (!activeInstanceIds.length || !mapped.some((value) => value.baseUrl)) urlDiagnostic = " Open app URL is not yet available.";
          } catch (error) {
            urlDiagnostic = ` Open app URL synchronization failed: ${error.message || "network error"}.`;
          }
        } else if (observedState === "stopped" || observedState === "not-running") {
          invocations = invocations.filter((value) => value.artifactId !== component.artifactId || !["running", "starting"].includes(String(value.status || "").toLowerCase()));
          updateOpenApp(component);
        }
        if (expectedRuntime(action).includes(observedState)) {
          setRowFeedback(component, `${actionLabel} succeeded; runtime is ${observedState}.${urlDiagnostic}`, urlDiagnostic ? "error" : "success");
          return true;
        }
      } catch (error) {
        if (controller.signal.aborted && error?.name === "AbortError") break;
        updateRuntimeRow(component, "unknown", true);
        setRowFeedback(component, `Supervisor ${acceptedState} ${actionLabel.toLowerCase()}, but runtime observation failed: ${error.message || "network error"}.`, "error");
        return false;
      } finally { clearTimeout(abortTimer); }
      await new Promise((resolve) => setTimeout(resolve, Math.min(runtimeObservationPollIntervalMs, Math.max(0, deadline - Date.now()))));
    }
    updateRuntimeRow(component, "unknown", true);
    setRowFeedback(component, `Supervisor ${acceptedState} ${actionLabel.toLowerCase()}, but runtime observation timed out after ${Math.round(runtimeObservationTimeoutMs / 1000)} seconds.`, "error");
    return false;
  }
  async function requestLifecycle(action, component, candidate, control) {
    if (action !== "stop" && !candidate) {
      setRowFeedback(component, "No available launch source is selected.", "error");
      return;
    }
    setLifecycleBusy(component, action, true);
    updateRuntimeRow(component, action === "stop" ? "stopping" : "launching");
    setRowFeedback(component, `${action.charAt(0).toUpperCase() + action.slice(1)} requested; waiting for supervisor…`, "busy");
    try {
      const key = `${action}-${component.artifactId}-${Date.now()}`;
      const source = action === "stop" ? "" : `&sourceKind=${encodeURIComponent(candidate.source_kind || candidate.sourceKind)}&sourceId=${encodeURIComponent(candidate.source_id || candidate.sourceId)}`;
      let result = await request(lifecycleEndpoint, `${action}-operational-component?artifactId=${encodeURIComponent(component.artifactId)}&idempotencyKey=${encodeURIComponent(key)}${source}`);
      if (result.request_state === "queued" && result.request_id) {
        const requestId = result.request_id;
        const deadline = Date.now() + lifecyclePollTimeoutMs;
        while (result.request_state === "queued") {
          const remaining = deadline - Date.now();
          if (remaining <= 0) break;
          const controller = new AbortController();
          const abortTimer = setTimeout(() => controller.abort(), remaining);
          try {
            result = await request(lifecycleEndpoint, `get-lifecycle-request?requestId=${encodeURIComponent(requestId)}`, controller.signal);
          } catch (error) {
            if (controller.signal.aborted && error?.name === "AbortError") { result = timedOutLifecycleResult(result, requestId); break; }
            throw error;
          } finally { clearTimeout(abortTimer); }
          if (result.request_state === "queued") {
            const remaining = deadline - Date.now();
            if (remaining > 0) await new Promise((resolve) => setTimeout(resolve, Math.min(lifecyclePollIntervalMs, remaining)));
          }
        }
        if (result.request_state === "queued") {
          result = timedOutLifecycleResult(result, requestId);
        }
      }
      const resultState = String(result.request_state || "").toLowerCase();
      const diagnostic = result.diagnostic_code || result.diagnostic;
      if (["failed", "rejected", "timed-out"].includes(resultState)) {
        updateRuntimeRow(component, resultState === "timed-out" ? "unknown" : authoritativeRuntime(component), resultState === "timed-out");
        setRowFeedback(component, `${action.charAt(0).toUpperCase() + action.slice(1)} ${resultState}: ${diagnostic || "No diagnostic was provided."}`, "error");
      } else if (["accepted", "running", "stopped"].includes(resultState)) {
        await observeRuntime(action, component, result);
      } else {
        updateRuntimeRow(component, "unknown", true);
        setRowFeedback(component, `${action.charAt(0).toUpperCase() + action.slice(1)} returned an unexpected lifecycle state: ${resultState || "unknown"}.`, "error");
      }
    } catch (error) {
      updateRuntimeRow(component, "unknown", true);
      setRowFeedback(component, `${action.charAt(0).toUpperCase() + action.slice(1)} request failed: ${error.message || "network error"}.`, "error");
    } finally {
      setLifecycleBusy(component, action, false);
      updateActionControls(component, authoritativeRuntime(component));
    }
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
      if (urls.length) { const open = document.createElement("a"); open.className = "button secondary operational-action"; open.textContent = "Open app"; open.href = urls[0]; open.target = "_blank"; open.rel = "noopener"; elements.sources.append(open); }
      if (!source.sources.length) { const empty = document.createElement("p"); empty.textContent = "No source facts are available."; elements.sources.append(empty); }
      source.sources.forEach((value) => { const item = document.createElement("p"); item.className = "subtle"; item.textContent = [value.source_kind, value.source_id, value.refresh_state, value.recommended_version || value.latest_version, value.diagnostic, value.private_locator].filter(Boolean).join(" · "); elements.sources.append(item); });
      const componentEvidence = evidence.filter((value) => value.artifactId === record.artifactId);
      const detailedEvidence = await Promise.all(componentEvidence.map((value) => request(evidenceEndpoint, `get-launcher-evidence?instanceId=${encodeURIComponent(value.instanceId)}`).catch(() => value)));
      detailedEvidence.forEach((value) => { const item = document.createElement("p"); item.className = "subtle"; item.textContent = ["Launcher evidence", value.launcher_kind || value.launcherKind, value.execution_mode || value.executionMode, value.evidence_decision || value.decision, value.development_directory, formatInstant(value.last_seen_at || value.lastSeenAt)].filter(Boolean).join(" · "); elements.sources.append(item); });
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
      const latestState = String(latest?.request_state || "").toLowerCase();
      const latestDiagnostic = latest?.diagnostic_code || latest?.diagnostic;
      const matchingLatest = latest && requests.find((value) => value.request_id === latest.request_id);
      const matchingDiagnostic = matchingLatest?.diagnostic_code || matchingLatest?.diagnostic;
      const latestIsStale = latest && (!matchingLatest || matchingLatest.request_state !== latest.request_state || (["failed", "rejected"].includes(latestState) && latestDiagnostic && matchingDiagnostic !== latestDiagnostic) || latestState === "timed-out");
      if (latestIsStale) {
        const item = document.createElement("section"); item.className = "catalog-source";
        const title = document.createElement("h4"); title.textContent = ["Latest result", text(latest.lifecycle_action), text(latest.request_state)].join(" · "); item.append(title);
        [["Request", latest.request_id], ["Supervisor", latest.supervisor_id], ["Instance", latest.instance_id], ["Accepted", formatInstant(latest.accepted_at)], ["Completed", formatInstant(latest.completed_at)], ["Diagnostic", latestDiagnostic]].forEach(([label, fact]) => {
          if (fact && fact !== "—") { const detail = document.createElement("p"); detail.textContent = `${label}: ${fact}`; item.append(detail); }
        });
        elements.lifecycleHistory.append(item);
      }
      elements.dialog.showModal();
    } catch (error) { showError(error.message || "The operational component detail could not be loaded."); }
  }
  elements.refresh.addEventListener("click", load); elements.closeDetail.addEventListener("click", () => elements.dialog.close()); load();
}());
