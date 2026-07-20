#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
bootstrap="$project_root/scripts/bootstrap-standalone.sh"
work_dir="$(mktemp -d "$project_root/target/standalone-bootstrap.XXXXXX")"
trap 'rm -rf "$work_dir"' EXIT

cncf_home="$work_dir/cncf-home"
first_output="$work_dir/first-output.txt"
bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check > "$first_output"

root="$cncf_home/textus-control-center"
locator="$root/standalone-locator.yaml"
credential="$root/credentials/launcher-registration.token"
server_config="$root/server-config.yaml"

[[ -f "$locator" ]]
[[ -f "$credential" ]]
[[ -f "$server_config" ]]
[[ "$(stat -f '%Lp' "$credential")" == "600" ]]
[[ "$(stat -f '%Lp' "$locator")" == "600" ]]
[[ "$(stat -f '%Lp' "$server_config")" == "600" ]]
rg -F -- 'profile: standalone' "$locator" >/dev/null
rg -F -- 'endpoint: http://127.0.0.1:18777/rest/v1/textus-control-center/subsystem-inventory' "$locator" >/dev/null
rg -F -- 'credentialRef: credentials/launcher-registration.token' "$locator" >/dev/null
rg -F -- 'hostLabel: bootstrap-check' "$locator" >/dev/null
rg -F -- 'textus.local-data.textus-control-center.application.path' "$server_config" >/dev/null
rg -F -- "textus-control-center.home\": \"$root\"" "$server_config" >/dev/null

first_token="$(<"$credential")"
[[ -n "$first_token" ]]
! rg -F -- "$first_token" "$locator" "$first_output"

first_scope="$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")"
first_installation="$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")"
bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check > "$work_dir/reuse-output.txt"
[[ "$(<"$credential")" == "$first_token" ]]

bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check --rotate > "$work_dir/rotate-output.txt"
second_token="$(<"$credential")"
[[ "$second_token" != "$first_token" ]]
[[ "$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")" == "$first_scope" ]]
[[ "$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")" == "$first_installation" ]]
! rg -F -- "$second_token" "$locator" "$work_dir/rotate-output.txt"

printf 'Standalone bootstrap creates, reuses, and rotates local credentials without disclosing tokens.\n'
