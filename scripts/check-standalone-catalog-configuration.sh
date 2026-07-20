#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
configure="$project_root/scripts/configure-standalone-catalog.sh"
bootstrap="$project_root/scripts/bootstrap-standalone.sh"
work_dir="$(mktemp -d "$project_root/target/standalone-catalog.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

cncf_home="$work_dir/cncf-home"
development_root="$work_dir/dev2026"
local_catalog="$cncf_home/local/repository/catalog/car"
mkdir -p "$development_root" "$local_catalog"
bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label catalog-check > "$work_dir/bootstrap-output.txt"

bash "$configure" \
  --cncf-home "$cncf_home" \
  --development-root "$development_root" \
  --public-subscription textus-user-notification \
  --public-subscription textus-user-account > "$work_dir/output.txt"

catalog="$cncf_home/textus-control-center/catalog.yaml"
[[ -f "$catalog" ]]
[[ "$(stat -f '%Lp' "$catalog")" == "600" ]]
rg -F -- 'schema: textus-control-center.catalog.v1' "$catalog" >/dev/null
rg -F -- "path: $development_root" "$catalog" >/dev/null
rg -F -- "catalog-root: $local_catalog" "$catalog" >/dev/null
rg -F -- 'catalog-base-url: https://www.simplemodeling.org/repository/catalog/car' "$catalog" >/dev/null
rg -F -- '      - textus-user-notification' "$catalog" >/dev/null
rg -F -- '      - textus-user-account' "$catalog" >/dev/null
rg -F -- "textus-control-center.home\": \"$cncf_home/textus-control-center\"" "$cncf_home/textus-control-center/server-config.yaml" >/dev/null

if bash "$configure" --cncf-home "$cncf_home" --development-root "$development_root" --public-subscription textus-user-notification --public-subscription textus-user-notification > /dev/null 2>&1; then
  printf '%s\n' 'duplicate public subscriptions must be rejected' >&2
  exit 1
fi

printf 'Standalone catalog configuration records explicit local and public CAR sources.\n'
