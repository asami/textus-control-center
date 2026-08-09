#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"
component_descriptor="$project_root/src/main/car/component-descriptor.json"
assembly_descriptor="$project_root/src/main/car/assembly-descriptor.yaml"
standalone_assembly="$project_root/conf/cncf/assembly-standalone.yaml"

rg -F -- '"schemaVersion": 3' "$component_descriptor" >/dev/null
rg -F -- '"namespace": "org.simplemodeling.textus"' "$component_descriptor" >/dev/null
rg -F -- '"id": "ControlCenter"' "$component_descriptor" >/dev/null
rg -F -- '"textus.component.org.simplemodeling.textus.ControlCenter.datastores.application.policy": "local-default"' "$component_descriptor" >/dev/null
rg -F -- 'namespace: org.simplemodeling.textus' "$assembly_descriptor" >/dev/null
rg -F -- 'id: ControlCenter' "$assembly_descriptor" >/dev/null
rg -F -- 'id: Supervisor' "$assembly_descriptor" >/dev/null
rg -F -- 'textus.component.org.simplemodeling.textus.ControlCenter.datastores.application.policy: local-default' "$assembly_descriptor" >/dev/null
rg -F -- 'contract: supervisor' "$assembly_descriptor" >/dev/null
rg -F -- 'id: textus-control-center-local-operator' "$assembly_descriptor" >/dev/null
rg -F -- 'namespace: org.simplemodeling.textus' "$standalone_assembly" >/dev/null
rg -F -- 'id: ControlCenter' "$standalone_assembly" >/dev/null
rg -F -- 'id: Supervisor' "$standalone_assembly" >/dev/null
rg -F -- 'textus.subsystem.user-mode: standalone' "$standalone_assembly" >/dev/null
rg -F -- 'textus.component.org.simplemodeling.textus.ControlCenter.datastores.application.policy: local-default' "$standalone_assembly" >/dev/null
rg -F -- 'contract: supervisor' "$standalone_assembly" >/dev/null
rg -F -- 'id: textus-control-center-local-operator' "$standalone_assembly" >/dev/null
rg -F -- 'name: textus-control-center-launcher-registration' "$standalone_assembly" >/dev/null
! rg -n -- 'token:|credentialRef:|credential-ref:' "$component_descriptor" "$assembly_descriptor" "$standalone_assembly"

printf 'Textus Control Center standalone assembly binds the Supervisor provider and declares local durable state without embedding credentials.\n'
