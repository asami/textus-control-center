#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
page="$project_root/src/main/web/textus-admin/index.html"
script="$project_root/src/main/web/textus-admin/assets/subsystem-inventory.js"
style="$project_root/src/main/web/textus-admin/assets/subsystem-inventory.css"
form="$project_root/src/main/web-inf/form.yaml"
web="$project_root/src/main/web-inf/web.yaml"

rg -F -- 'Subsystem inventory' "$page" >/dev/null
rg -F -- '/web/assets/bootstrap.min.css' "$page" >/dev/null
rg -F -- 'class="table align-middle mb-0"' "$page" >/dev/null
rg -F -- 'id="loading"' "$page" >/dev/null
rg -F -- 'id="empty"' "$page" >/dev/null
rg -F -- 'id="error"' "$page" >/dev/null
rg -F -- 'id="instances"' "$page" >/dev/null
rg -F -- 'const endpoint = "/rest/v1/textus-admin/subsystem-inventory"' "$script" >/dev/null
rg -F -- 'request("list-subsystems?offset=0&limit=100")' "$script" >/dev/null
rg -F -- 'request(`get-subsystem?instanceId=${encodeURIComponent(instanceId)}`)' "$script" >/dev/null
rg -F -- 'dashboardUrl' "$script" >/dev/null
rg -F -- 'systemAdminUrl' "$script" >/dev/null
rg -F -- 'credentials: "same-origin"' "$script" >/dev/null
rg -F -- '@media' "$style" >/dev/null
rg -F -- 'textus-admin.subsystem-inventory.list-subsystems: protected' "$form" >/dev/null
rg -F -- 'textus-admin.subsystem-inventory.get-subsystem: protected' "$form" >/dev/null
rg -F -- 'route: /web/textus-admin' "$web" >/dev/null
! rg -F -- 'subsystem-management' "$form"
! rg -F -- 'http://' "$page" "$script" "$style"
! rg -F -- 'https://' "$page" "$script" "$style"

printf 'Textus Admin Web inventory assets are internally packaged and operation-backed.\n'
