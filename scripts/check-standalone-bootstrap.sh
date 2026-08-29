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
rg -F -- 'endpoint: http://127.0.0.1:18777/rest/v1/org-simplemodeling-textus-control-center/subsystem-inventory' "$locator" >/dev/null
rg -F -- 'credentialRef: credentials/launcher-registration.token' "$locator" >/dev/null
rg -F -- 'timeout: 5s' "$locator" >/dev/null
rg -F -- 'hostLabel: bootstrap-check' "$locator" >/dev/null
rg -F -- 'textus.local-data.org.simplemodeling.textus.control-center.application.path' "$server_config" >/dev/null
rg -F -- "textus-control-center.home\": \"$root\"" "$server_config" >/dev/null

first_token="$(<"$credential")"
[[ -n "$first_token" ]]
! rg -F -- "$first_token" "$locator" "$first_output" >/dev/null

first_scope="$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")"
first_installation="$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")"
first_timeout="$(awk -F ': ' '$1 == "timeout" { print $2 }' "$locator")"
first_heartbeat_interval="$(awk -F ': ' '$1 == "heartbeatInterval" { print $2 }' "$locator")"
first_host_label="$(awk -F ': ' '$1 == "hostLabel" { print $2 }' "$locator")"
first_locator_mode="$(stat -f '%Lp' "$locator")"
first_credential_mode="$(stat -f '%Lp' "$credential")"
first_server_config_mode="$(stat -f '%Lp' "$server_config")"
first_server_config="$(<"$server_config")"

awk -F ': ' '$1 == "endpoint" { print "endpoint: http://127.0.0.1:18777/rest/v1/textus-control-center/subsystem-inventory"; next } { print }' "$locator" > "$work_dir/stale-locator.yaml"
chmod "$first_locator_mode" "$work_dir/stale-locator.yaml"
mv -f "$work_dir/stale-locator.yaml" "$locator"
bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check > "$work_dir/repair-output.txt"
rg -F -- 'endpoint: http://127.0.0.1:18777/rest/v1/org-simplemodeling-textus-control-center/subsystem-inventory' "$locator" >/dev/null
! rg -F -- "$first_token" "$locator" "$work_dir/repair-output.txt" >/dev/null
[[ "$(<"$credential")" == "$first_token" ]]
[[ "$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")" == "$first_scope" ]]
[[ "$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")" == "$first_installation" ]]
[[ "$(awk -F ': ' '$1 == "timeout" { print $2 }' "$locator")" == "$first_timeout" ]]
[[ "$(awk -F ': ' '$1 == "heartbeatInterval" { print $2 }' "$locator")" == "$first_heartbeat_interval" ]]
[[ "$(awk -F ': ' '$1 == "hostLabel" { print $2 }' "$locator")" == "$first_host_label" ]]
[[ "$(<"$server_config")" == "$first_server_config" ]]
[[ "$(stat -f '%Lp' "$locator")" == "$first_locator_mode" ]]
[[ "$(stat -f '%Lp' "$credential")" == "$first_credential_mode" ]]
[[ "$(stat -f '%Lp' "$server_config")" == "$first_server_config_mode" ]]

awk '
  /^"textus\.component\.org\.simplemodeling\.textus\.control-center\.datastores\.application\.policy": / {
    sub("^\\\"textus\\.component\\.org\\.simplemodeling\\.textus\\.control-center\\.datastores\\.application\\.policy\\\"", "\"textus.component.textus-control-center.datastores.application.policy\"")
  }
  /^"textus\.local-data\.org\.simplemodeling\.textus\.control-center\.application\.path": / {
    sub("^\\\"textus\\.local-data\\.org\\.simplemodeling\\.textus\\.control-center\\.application\\.path\\\"", "\"textus.local-data.textus-control-center.application.path\"")
  }
  { print }
' "$server_config" > "$work_dir/legacy-server-config.yaml"
chmod "$first_server_config_mode" "$work_dir/legacy-server-config.yaml"
mv -f "$work_dir/legacy-server-config.yaml" "$server_config"

bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check > "$work_dir/legacy-reuse-output.txt"
rg -F -- 'textus.component.org.simplemodeling.textus.control-center.datastores.application.policy' "$server_config" >/dev/null
rg -F -- 'textus.local-data.org.simplemodeling.textus.control-center.application.path' "$server_config" >/dev/null
! rg -F -- 'textus.component.textus-control-center.datastores.application.policy' "$server_config"
! rg -F -- 'textus.local-data.textus-control-center.application.path' "$server_config"
[[ "$(<"$credential")" == "$first_token" ]]
[[ "$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")" == "$first_scope" ]]
[[ "$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")" == "$first_installation" ]]
[[ "$(<"$server_config")" == "$first_server_config" ]]
[[ "$(stat -f '%Lp' "$server_config")" == "$first_server_config_mode" ]]
! rg -F -- "$first_token" "$locator" "$work_dir/legacy-reuse-output.txt" >/dev/null

bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check > "$work_dir/reuse-output.txt"
[[ "$(<"$credential")" == "$first_token" ]]

bash "$bootstrap" --cncf-home "$cncf_home" --port 18777 --host-label bootstrap-check --rotate > "$work_dir/rotate-output.txt"
second_token="$(<"$credential")"
[[ "$second_token" != "$first_token" ]]
[[ "$(awk -F ': ' '$1 == "scopeId" { print $2 }' "$locator")" == "$first_scope" ]]
[[ "$(awk -F ': ' '$1 == "installationId" { print $2 }' "$locator")" == "$first_installation" ]]
! rg -F -- "$second_token" "$locator" "$work_dir/rotate-output.txt" >/dev/null

printf 'Standalone bootstrap creates, repairs, reuses, and rotates local credentials without disclosing tokens.\n'
