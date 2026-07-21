#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
page="$project_root/src/main/web/textus-control-center/index.html"
script="$project_root/src/main/web/textus-control-center/assets/subsystem-inventory.js"
style="$project_root/src/main/web/textus-control-center/assets/subsystem-inventory.css"
form="$project_root/src/main/web-inf/form.yaml"
web="$project_root/src/main/web-inf/web.yaml"

rg -F -- 'Subsystem control panel' "$page" >/dev/null
rg -F -- 'Invocation inventory' "$page" >/dev/null
rg -F -- '/web/assets/bootstrap.min.css' "$page" >/dev/null
rg -F -- '/web/assets/textus-bootstrap-material.css' "$page" >/dev/null
rg -F -- 'class="control-center-topbar navbar navbar-expand-lg' "$page" >/dev/null
rg -F -- 'aria-label="Control Center navigation"' "$page" >/dev/null
rg -F -- 'aria-label="Control Center mobile navigation"' "$page" >/dev/null
rg -F -- 'href="/web/system/dashboard"' "$page" >/dev/null
rg -F -- 'href="/web/system/admin"' "$page" >/dev/null
rg -F -- 'href="/man/textus-control-center"' "$page" >/dev/null
rg -F -- '/web/textus-control-center/assets/subsystem-inventory.css' "$page" >/dev/null
rg -F -- '/web/textus-control-center/assets/subsystem-inventory.js' "$page" >/dev/null
rg -F -- 'class="table align-middle mb-0"' "$page" >/dev/null
rg -F -- 'id="loading"' "$page" >/dev/null
rg -F -- 'id="empty"' "$page" >/dev/null
rg -F -- 'id="error"' "$page" >/dev/null
rg -F -- 'id="instances"' "$page" >/dev/null
rg -F -- 'id="running-count"' "$page" >/dev/null
rg -F -- 'id="stopped-count"' "$page" >/dev/null
rg -F -- 'id="attention-count"' "$page" >/dev/null
rg -F -- 'id="registered-count"' "$page" >/dev/null
rg -F -- 'const endpoint = "/rest/v1/textus-control-center/subsystem-inventory"' "$script" >/dev/null
rg -F -- 'request("list-subsystems?offset=0&limit=100")' "$script" >/dev/null
rg -F -- 'request(`get-subsystem?instanceId=${encodeURIComponent(instanceId)}`)' "$script" >/dev/null
rg -F -- 'instanceId: record.instance_id' "$script" >/dev/null
rg -F -- 'dashboardUrl: record.dashboard_url' "$script" >/dev/null
rg -F -- 'dashboardUrl' "$script" >/dev/null
rg -F -- 'systemAdminUrl' "$script" >/dev/null
rg -F -- 'credentials: "same-origin"' "$script" >/dev/null
rg -F -- 'function renderOverview()' "$script" >/dev/null
rg -F -- 'function resetOverview()' "$script" >/dev/null
rg -F -- '@media' "$style" >/dev/null
rg -F -- 'textus-control-center.subsystem-inventory.list-subsystems: protected' "$form" >/dev/null
rg -F -- 'textus-control-center.subsystem-inventory.get-subsystem: protected' "$form" >/dev/null
rg -F -- 'route: /web/{component}/textus-control-center' "$web" >/dev/null
rg -F -- 'path: /web/textus-control-center' "$web" >/dev/null
rg -F -- 'kind: alias' "$web" >/dev/null
rg -F -- 'component: TextusControlCenter' "$web" >/dev/null
rg -F -- 'app: textus-control-center' "$web" >/dev/null
! rg -F -- 'subsystem-management' "$form"
! rg -F -- 'http://' "$page" "$script" "$style"
! rg -F -- 'https://' "$page" "$script" "$style"

printf 'Textus Control Center Web inventory assets are internally packaged and operation-backed.\n'
