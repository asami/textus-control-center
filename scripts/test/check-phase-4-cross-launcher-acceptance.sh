#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/../.." && pwd)"
exec uv run --no-cache --managed-python --no-python-downloads --script \
  "$project_root/scripts/test/phase4_cross_launcher_acceptance.py" "$@"
