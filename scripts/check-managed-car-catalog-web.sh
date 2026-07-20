#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
page="$project_root/src/main/web/textus-control-center/catalog.html"
script="$project_root/src/main/web/textus-control-center/assets/managed-car-catalog.js"
style="$project_root/src/main/web/textus-control-center/assets/subsystem-inventory.css"
form="$project_root/src/main/web-inf/form.yaml"

rg -F -- 'Managed CAR catalog' "$page" >/dev/null
rg -F -- 'href="/web/textus-control-center/catalog.html"' "$page" >/dev/null
rg -F -- 'Subsystem inventory' "$page" >/dev/null
rg -F -- 'id="cars"' "$page" >/dev/null
rg -F -- 'id="detail-sources"' "$page" >/dev/null
rg -F -- '/web/textus-control-center/assets/managed-car-catalog.js' "$page" >/dev/null
rg -F -- 'const endpoint = "/rest/v1/textus-control-center/car-catalog"' "$script" >/dev/null
rg -F -- 'request("list-managed-cars?offset=0&limit=100")' "$script" >/dev/null
rg -F -- 'request("refresh-car-catalog")' "$script" >/dev/null
rg -F -- 'request(`get-managed-car?artifactId=${encodeURIComponent(artifactId)}`)' "$script" >/dev/null
rg -F -- 'privateLocator: source.private_locator' "$script" >/dev/null
rg -F -- 'credentials: "same-origin"' "$script" >/dev/null
rg -F -- '.source-marks' "$style" >/dev/null
rg -F -- '.catalog-source' "$style" >/dev/null
rg -F -- 'textus-control-center.car-catalog.refresh-car-catalog: protected' "$form" >/dev/null
rg -F -- 'textus-control-center.car-catalog.list-managed-cars: protected' "$form" >/dev/null
rg -F -- 'textus-control-center.car-catalog.get-managed-car: protected' "$form" >/dev/null
! rg -F -- 'http://' "$page" "$script" "$style"
! rg -F -- 'https://' "$page" "$script" "$style"

printf 'Textus Control Center managed CAR catalog assets are internally packaged and operation-backed.\n'
