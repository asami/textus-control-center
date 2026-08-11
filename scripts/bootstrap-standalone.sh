#!/usr/bin/env bash
set -euo pipefail

cncf_home="${HOME:?HOME is required}/.cncf"
port="18000"
host_label="local"
rotate="false"
canonical_inventory_path="/rest/v1/org-simplemodeling-textus-control-center/subsystem-inventory"

usage() {
  printf '%s\n' 'Usage: bootstrap-standalone.sh [--cncf-home <path>] [--port <port>] [--host-label <label>] [--rotate]'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cncf-home)
      cncf_home="$2"
      shift 2
      ;;
    --port)
      port="$2"
      shift 2
      ;;
    --host-label)
      host_label="$2"
      shift 2
      ;;
    --rotate)
      rotate="true"
      shift
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage >&2
      exit 2
      ;;
  esac
done

if ! [[ "$port" =~ ^[0-9]+$ ]] || (( port < 1 || port > 65535 )); then
  printf '%s\n' 'port must be an integer between 1 and 65535' >&2
  exit 2
fi

if ! [[ "$host_label" =~ ^[A-Za-z0-9._-]+$ ]]; then
  printf '%s\n' 'host label must contain only letters, digits, dot, underscore, or hyphen' >&2
  exit 2
fi

canonical_inventory_endpoint="http://127.0.0.1:$port$canonical_inventory_path"

root="$cncf_home/textus-control-center"
credential_dir="$root/credentials"
state_dir="$root/state"
locator="$root/standalone-locator.yaml"
credential="$credential_dir/launcher-registration.token"
server_config="$root/server-config.yaml"

umask 077
mkdir -p "$credential_dir" "$state_dir"

read_locator_value() {
  local key="$1"
  awk -F ': ' -v wanted="$key" '$1 == wanted { print substr($0, length(wanted) + 3); exit }' "$locator"
}

repair_locator_endpoint() {
  local endpoint="$1"
  local locator_mode
  local locator_tmp
  locator_mode="$(stat -f '%Lp' "$locator")"
  locator_tmp="$(mktemp "$root/.standalone-locator.yaml.XXXXXX")"
  if ! awk -v endpoint="$endpoint" '
    BEGIN { replaced = 0 }
    /^endpoint: / { print "endpoint: " endpoint; replaced = 1; next }
    { print }
    END { exit(replaced ? 0 : 1) }
  ' "$locator" > "$locator_tmp"; then
    rm -f "$locator_tmp"
    printf '%s\n' 'existing standalone locator is missing an endpoint; repair it explicitly before bootstrap' >&2
    return 1
  fi
  chmod "$locator_mode" "$locator_tmp"
  mv -f "$locator_tmp" "$locator"
}

migrate_server_config_datastore_keys() {
  local server_config_mode
  local server_config_tmp
  server_config_mode="$(stat -f '%Lp' "$server_config")"
  server_config_tmp="$(mktemp "$root/.server-config.yaml.XXXXXX")"
  if ! awk \
    -v canonical_policy='textus.component.org.simplemodeling.textus.control-center.datastores.application.policy' \
    -v canonical_path='textus.local-data.org.simplemodeling.textus.control-center.application.path' \
    -v legacy_policy='textus.component.textus-control-center.datastores.application.policy' \
    -v legacy_path='textus.local-data.textus-control-center.application.path' '
      function key_line(key, line) {
        return line ~ ("^\\\"" key "\\\": ")
      }
      {
        lines[NR] = $0
        if (key_line(canonical_policy, $0)) {
          canonical_policy_count++
          canonical_policy_line = $0
        }
        if (key_line(canonical_path, $0)) {
          canonical_path_count++
          canonical_path_line = $0
        }
        if (key_line(legacy_policy, $0)) {
          legacy_policy_count++
          legacy_policy_line = $0
        }
        if (key_line(legacy_path, $0)) {
          legacy_path_count++
          legacy_path_line = $0
        }
      }
      END {
        if (canonical_policy_count > 1 || canonical_path_count > 1 || legacy_policy_count > 1 || legacy_path_count > 1)
          exit 1
        if (canonical_policy_count && legacy_policy_count && canonical_policy_line != legacy_policy_line) {
          sub("^\\\"" canonical_policy "\\\": ", "", canonical_policy_line)
          sub("^\\\"" legacy_policy "\\\": ", "", legacy_policy_line)
          if (canonical_policy_line != legacy_policy_line)
            exit 1
        }
        if (canonical_path_count && legacy_path_count && canonical_path_line != legacy_path_line) {
          sub("^\\\"" canonical_path "\\\": ", "", canonical_path_line)
          sub("^\\\"" legacy_path "\\\": ", "", legacy_path_line)
          if (canonical_path_line != legacy_path_line)
            exit 1
        }
        for (i = 1; i <= NR; i++) {
          line = lines[i]
          if (key_line(legacy_policy, line)) {
            if (!canonical_policy_count) {
              sub("^\\\"" legacy_policy "\\\"", "\"" canonical_policy "\"", line)
              print line
            }
          } else if (key_line(legacy_path, line)) {
            if (!canonical_path_count) {
              sub("^\\\"" legacy_path "\\\"", "\"" canonical_path "\"", line)
              print line
            }
          } else {
            print line
          }
        }
      }
    ' "$server_config" > "$server_config_tmp"; then
    rm -f "$server_config_tmp"
    printf '%s\n' 'existing standalone server configuration cannot be safely migrated; repair its datastore keys explicitly before bootstrap' >&2
    return 1
  fi
  if cmp -s "$server_config" "$server_config_tmp"; then
    rm -f "$server_config_tmp"
  else
    chmod "$server_config_mode" "$server_config_tmp"
    mv -f "$server_config_tmp" "$server_config"
  fi
}

if [[ -f "$locator" ]]; then
  scope_id="$(read_locator_value scopeId)"
  installation_id="$(read_locator_value installationId)"
  if [[ -z "$scope_id" || -z "$installation_id" ]]; then
    printf '%s\n' 'existing standalone locator is missing scopeId or installationId; repair it explicitly before bootstrap' >&2
    exit 1
  fi
else
  installation_id="standalone-$(openssl rand -hex 16)"
  scope_id="scope-$(openssl rand -hex 16)"
fi

if [[ -f "$credential" && "$rotate" != "true" && -f "$locator" && -f "$server_config" ]]; then
  migrate_server_config_datastore_keys
  current_endpoint="$(read_locator_value endpoint)"
  if [[ "$current_endpoint" != "$canonical_inventory_endpoint" ]]; then
    repair_locator_endpoint "$canonical_inventory_endpoint"
  fi
  printf 'Standalone Control Center bootstrap is already initialized at %s.\n' "$root"
  printf 'Use --rotate to replace the launcher credential without changing the installation identity.\n'
  exit 0
fi

if [[ -f "$credential" && "$rotate" != "true" ]] && { [[ ! -f "$locator" ]] || [[ ! -f "$server_config" ]]; }; then
  printf '%s\n' 'incomplete standalone bootstrap state exists; repair it explicitly or use --rotate' >&2
  exit 1
fi

token="$(openssl rand -hex 32)"
credential_tmp="$(mktemp "$credential_dir/.launcher-registration.token.XXXXXX")"
locator_tmp="$(mktemp "$root/.standalone-locator.yaml.XXXXXX")"
server_config_tmp="$(mktemp "$root/.server-config.yaml.XXXXXX")"

cleanup() {
  rm -f "$credential_tmp" "$locator_tmp" "$server_config_tmp"
}
trap cleanup EXIT

printf '%s\n' "$token" > "$credential_tmp"
cat > "$locator_tmp" <<EOF
schemaVersion: 1
profile: standalone
scopeId: $scope_id
installationId: $installation_id
endpoint: $canonical_inventory_endpoint
credentialRef: credentials/launcher-registration.token
timeout: 2s
heartbeatInterval: 30s
hostLabel: $host_label
EOF
cat > "$server_config_tmp" <<EOF
"textus-control-center.registration.authentication.token": "$token"
"textus-control-center.registration.authentication.principal-id": "textus-control-center-launcher"
"textus-control-center.home": "$root"
"textus.component.org.simplemodeling.textus.control-center.datastores.application.policy": "local-default"
"textus.local-data.org.simplemodeling.textus.control-center.application.path": "$state_dir/registry.sqlite"
EOF

chmod 600 "$credential_tmp" "$locator_tmp" "$server_config_tmp"
mv -f "$credential_tmp" "$credential"
mv -f "$locator_tmp" "$locator"
mv -f "$server_config_tmp" "$server_config"
trap - EXIT

printf 'Initialized standalone Textus Control Center at %s.\n' "$root"
printf 'Start it with --cncf-config %s and then use canonical cncf/textus server commands.\n' "$server_config"
