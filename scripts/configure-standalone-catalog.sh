#!/usr/bin/env bash
set -euo pipefail

cncf_home="${HOME:?HOME is required}/.cncf"
development_root=""
local_catalog=""
public_catalog_base="https://www.simplemodeling.org/repository/catalog/car"
public_subscriptions=()

usage() {
  printf '%s\n' 'Usage: configure-standalone-catalog.sh --development-root <absolute-path> [--cncf-home <path>] [--local-repository-catalog <absolute-path>] [--public-catalog-base <https-url>] --public-subscription <artifact-id> [...]'
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cncf-home)
      cncf_home="$2"
      shift 2
      ;;
    --development-root)
      development_root="$2"
      shift 2
      ;;
    --local-repository-catalog)
      local_catalog="$2"
      shift 2
      ;;
    --public-catalog-base)
      public_catalog_base="$2"
      shift 2
      ;;
    --public-subscription)
      public_subscriptions+=("$2")
      shift 2
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

if [[ "$development_root" != /* ]]; then
  printf '%s\n' 'development root must be an absolute path' >&2
  exit 2
fi

if [[ -z "$local_catalog" ]]; then
  local_catalog="$cncf_home/local/repository/catalog/car"
fi

if [[ "$local_catalog" != /* ]]; then
  printf '%s\n' 'local repository catalog must be an absolute path' >&2
  exit 2
fi

if [[ ! "$public_catalog_base" =~ ^https://[^/]+(/.*)?$ ]]; then
  printf '%s\n' 'public catalog base must be an HTTPS URL' >&2
  exit 2
fi

if (( ${#public_subscriptions[@]} == 0 )); then
  printf '%s\n' 'at least one explicit public subscription is required' >&2
  exit 2
fi

validated_subscriptions=()
for artifact_id in "${public_subscriptions[@]}"; do
  if [[ ! "$artifact_id" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    printf '%s\n' 'public subscription artifact IDs must be safe artifact identifiers' >&2
    exit 2
  fi
  if [[ -n "${validated_subscriptions[*]:-}" ]]; then
    for existing_artifact_id in "${validated_subscriptions[@]}"; do
      if [[ "$artifact_id" == "$existing_artifact_id" ]]; then
        printf '%s\n' 'public subscription artifact IDs must be unique' >&2
        exit 2
      fi
    done
  fi
  validated_subscriptions+=("$artifact_id")
done

root="$cncf_home/textus-control-center"
catalog="$root/catalog.yaml"
server_config="$root/server-config.yaml"
if [[ ! -f "$server_config" ]]; then
  printf '%s\n' 'standalone Control Center is not bootstrapped; run bootstrap-standalone.sh first' >&2
  exit 1
fi

mkdir -p "$root"
umask 077
catalog_tmp="$(mktemp "$root/.catalog.yaml.XXXXXX")"
server_config_tmp="$(mktemp "$root/.server-config.yaml.XXXXXX")"

cleanup() {
  rm -f "$catalog_tmp" "$server_config_tmp"
}
trap cleanup EXIT

{
  printf '%s\n' 'schema: textus-control-center.catalog.v1'
  printf '%s\n' 'refresh:'
  printf '%s\n' '  timeout: 5s'
  printf '%s\n' 'development:'
  printf '%s\n' '  roots:'
  printf '%s\n' '    - id: dev2026'
  printf '      path: %s\n' "$development_root"
  printf '%s\n' '      include-prefix: textus-'
  printf '%s\n' 'local-repository:'
  printf '%s\n' '  id: local-cncf'
  printf '  catalog-root: %s\n' "$local_catalog"
  printf '%s\n' 'public-repositories:'
  printf '%s\n' '  - id: simplemodeling'
  printf '    catalog-base-url: %s\n' "$public_catalog_base"
  printf '%s\n' '    subscriptions:'
  for artifact_id in "${public_subscriptions[@]}"; do
    printf '      - %s\n' "$artifact_id"
  done
} > "$catalog_tmp"

chmod 600 "$catalog_tmp"
mv -f "$catalog_tmp" "$catalog"
awk -v control_center_home="$root" '
  /^"textus-control-center.home":/ {
    if (!written) {
      printf "\"textus-control-center.home\": \"%s\"\n", control_center_home
      written = 1
    }
    next
  }
  { print }
  END {
    if (!written) {
      printf "\"textus-control-center.home\": \"%s\"\n", control_center_home
    }
  }
' "$server_config" > "$server_config_tmp"
chmod 600 "$server_config_tmp"
mv -f "$server_config_tmp" "$server_config"
trap - EXIT

printf 'Configured standalone CAR catalog at %s. Restart Textus Control Center to apply it.\n' "$catalog"
