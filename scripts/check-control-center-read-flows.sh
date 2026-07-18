#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_root"

run_command() {
  sbt "runMain org.goldenport.cncf.CncfMain --discover=classes command $1" 2>&1
}

tree="$(run_command 'meta.tree')"
rg -F -- 'TextusControlCenter:' <<<"$tree" >/dev/null
rg -F -- 'SubsystemInventory:' <<<"$tree" >/dev/null
rg -F -- '- listSubsystems' <<<"$tree" >/dev/null
rg -F -- '- getSubsystem' <<<"$tree" >/dev/null

openapi="$(run_command 'TextusControlCenter.meta.openapi')"
rg -F -- '"/textus-control-center/subsystem-inventory/list-subsystems"' <<<"$openapi" >/dev/null
rg -F -- '"/textus-control-center/subsystem-inventory/get-subsystem"' <<<"$openapi" >/dev/null
rg -F -- '"/textus-control-center/subsystem-inventory/register-subsystem"' <<<"$openapi" >/dev/null
rg -F -- '"/textus-control-center/subsystem-inventory/heartbeat-subsystem"' <<<"$openapi" >/dev/null
rg -F -- '"/textus-control-center/subsystem-inventory/deregister-subsystem"' <<<"$openapi" >/dev/null

denied="$(run_command 'textus-control-center.subsystem-inventory.list-subsystems')"
rg -F -- 'status=403' <<<"$denied" >/dev/null
rg -F -- 'Subsystem inventory requires administrative authorization.' <<<"$denied" >/dev/null

printf 'Textus Control Center Command and automatic REST projections are available.\n'
