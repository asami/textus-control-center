#!/usr/bin/env bash
set -euo pipefail

cncf_home="${HOME:?HOME is required}/.cncf"
port="18013"
host_label="local"
rotate="false"

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
endpoint: http://127.0.0.1:$port/rest/v1/textus-control-center/subsystem-inventory
credentialRef: credentials/launcher-registration.token
timeout: 2s
heartbeatInterval: 30s
hostLabel: $host_label
EOF
cat > "$server_config_tmp" <<EOF
"textus-control-center.registration.authentication.token": "$token"
"textus-control-center.registration.authentication.principal-id": "textus-control-center-launcher"
"textus-control-center.home": "$root"
"textus.component.textus-control-center.datastores.application.policy": "local-default"
"textus.local-data.textus-control-center.application.path": "$state_dir/registry.sqlite"
EOF

chmod 600 "$credential_tmp" "$locator_tmp" "$server_config_tmp"
mv -f "$credential_tmp" "$credential"
mv -f "$locator_tmp" "$locator"
mv -f "$server_config_tmp" "$server_config"
trap - EXIT

printf 'Initialized standalone Textus Control Center at %s.\n' "$root"
printf 'Start it with --cncf-config %s and then use canonical cncf/textus server commands.\n' "$server_config"
