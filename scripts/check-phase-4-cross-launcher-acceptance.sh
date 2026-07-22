#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
cncf_launcher_dir="${CNCF_LAUNCHER_DIR:-/Users/asami/src/dev2026/cncf-launcher}"
textus_launcher_dir="${TEXTUS_LAUNCHER_DIR:-/Users/asami/src/dev2026/textus-launcher}"
cncf_command="${CNCF_COMMAND:-cncf}"
textus_command="${TEXTUS_COMMAND:-textus}"
if [ -n "${COURSIER_CACHE:-}" ]; then
  coursier_cache="$COURSIER_CACHE"
elif [ "$(uname)" = "Darwin" ]; then
  coursier_cache="$HOME/Library/Caches/Coursier/v1"
else
  coursier_cache="${XDG_CACHE_HOME:-$HOME/.cache}/coursier/v1"
fi
test -d "$coursier_cache"
textus_artifact="textus-control-center:0.1.0-SNAPSHOT"
runtime_home="$(mktemp -d -t textus-control-center-phase4)"
runtime_repository="$runtime_home/repository"
runtime_config="$runtime_home/textus-launcher.yaml"
evidence_command="$runtime_home/cncf-evidence"
cncf_log="$runtime_home/cncf-server.log"
textus_log="$runtime_home/textus-server.log"
cncf_pid=""
textus_pid=""

terminate_launcher() {
  local pid="$1"
  local child
  for child in $(pgrep -P "$pid" 2>/dev/null || true); do
    terminate_launcher "$child"
  done
  if kill -0 "$pid" 2>/dev/null; then kill -TERM "$pid" 2>/dev/null || true; fi
}

cleanup() {
  if [ -n "$cncf_pid" ]; then terminate_launcher "$cncf_pid"; fi
  if [ -n "$textus_pid" ]; then terminate_launcher "$textus_pid"; fi
  wait "$cncf_pid" 2>/dev/null || true
  wait "$textus_pid" 2>/dev/null || true
  if [ "${PHASE4_KEEP_WORK:-false}" = "true" ]; then
    printf 'Phase 4 cross-launcher work directory retained: %s\n' "$runtime_home"
  else
    rm -rf "$runtime_home"
  fi
}
trap cleanup EXIT INT TERM

cd "$project_root"
scripts/update-runtime-classpath.sh
sbt --batch cozyBuildCar

car_file="$project_root/target/textus-control-center-0.1.0-SNAPSHOT.car"
test -f "$car_file"
mkdir -p "$runtime_repository/textus-control-center/0.1.0-SNAPSHOT"
cp "$car_file" "$runtime_repository/textus-control-center/0.1.0-SNAPSHOT/textus-control-center-0.1.0-SNAPSHOT.car"
printf 'repositories:\n  car:\n    - %s\n' "$runtime_repository" > "$runtime_config"
printf '#!/usr/bin/env bash\nexec %q --launcher-home %q "$@"\n' "$cncf_command" "$runtime_home" > "$evidence_command"
chmod +x "$evidence_command"

export COURSIER_CACHE="$coursier_cache"
export CNCF_LAUNCHER_DEV_DIR="$cncf_launcher_dir"
export TEXTUS_LAUNCHER_DEV_DIR="$textus_launcher_dir"

exec "$cncf_command" --launcher-home "$runtime_home" server --textus.server.port=18113 >"$cncf_log" 2>&1 &
cncf_pid=$!
exec "$textus_command" --launcher-home "$runtime_home" --config "$runtime_config" "$textus_artifact" server --cncf.server.port=18114 >"$textus_log" 2>&1 &
textus_pid=$!

matched=false
for attempt in $(seq 1 30); do
  evidence="$($evidence_command launcher evidence list --format json 2>/dev/null || true)"
  if python3 -c 'import json, sys; rows=json.load(sys.stdin).get("entries", []); assert {x.get("launcherKind") for x in rows} >= {"cncf", "textus"}; assert all(x.get("stoppedAt") is None for x in rows)' <<<"$evidence" 2>/dev/null; then
    matched=true
    break
  fi
  sleep 1
done

if [ "$matched" != "true" ]; then
  printf 'Phase 4 cross-launcher evidence did not retain both Launcher kinds as current lifecycle facts.\n' >&2
  printf 'CNCF Launcher log:\n' >&2
  cat "$cncf_log" >&2 || true
  printf 'Textus Launcher log:\n' >&2
  cat "$textus_log" >&2 || true
  printf 'Last evidence output: %s\n' "$evidence" >&2
  exit 1
fi

terminate_launcher "$textus_pid"
wait "$textus_pid" 2>/dev/null || true

matched=false
for attempt in $(seq 1 30); do
  evidence="$($evidence_command launcher evidence list --format json 2>/dev/null || true)"
  if python3 -c 'import json, sys; rows=json.load(sys.stdin).get("entries", []); assert len(rows) == 2; assert {x.get("launcherKind") for x in rows} == {"cncf", "textus"}; assert any(x.get("launcherKind") == "cncf" and x.get("stoppedAt") is None for x in rows); assert any(x.get("launcherKind") == "textus" and x.get("stoppedAt") is not None for x in rows)' <<<"$evidence" 2>/dev/null; then
    matched=true
    break
  fi
  sleep 1
done

if [ "$matched" != "true" ]; then
  printf 'Phase 4 cross-launcher evidence did not retain normal Textus termination.\n' >&2
  printf 'CNCF Launcher log:\n' >&2
  cat "$cncf_log" >&2 || true
  printf 'Textus Launcher log:\n' >&2
  cat "$textus_log" >&2 || true
  printf 'Last evidence output: %s\n' "$evidence" >&2
  exit 1
fi

python3 -c 'import json, sys; rows=json.load(sys.stdin).get("entries", []); assert len(rows) == 2; assert {x.get("launcherKind") for x in rows} == {"cncf", "textus"}; assert all("developmentDirectory" not in x for x in rows)' <<<"$evidence"
TEXTUS_CONTROL_CENTER_PHASE4_EVIDENCE_COMMAND="$evidence_command" sbt --batch "testOnly org.simplemodeling.textus.controlcenter.SubsystemInventoryActionSpec"

printf 'Phase 4 cross-launcher acceptance passed: canonical cncf server and textus <artifact> server wrote shared evidence before Control Center reconciliation.\n'
