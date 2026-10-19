#!/bin/bash
set -euo pipefail

fail() { printf '%s\n' "$*" >&2; exit 1; }

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
output_root=""
while (($#)); do
    case "$1" in
        --output-root)
            (($# >= 2)) || fail '--output-root requires an absolute path'
            [[ -z "$output_root" ]] || fail '--output-root may appear only once'
            output_root="$2"
            shift 2
            ;;
        *) fail "Unknown argument: $1" ;;
    esac
done
[[ "$output_root" == /* ]] || fail 'Usage: scripts/build-macos-menu-bar.sh --output-root ABS'
[[ "$(uname -s)" == Darwin && "$(uname -m)" == arm64 ]] || fail 'Requires a Darwin arm64 development host'

# Resolve the existing parent before creating output; reject symlink escapes.
private_root="$(cd "$script_root/.codex-workflow" && pwd -P)"
[[ "$private_root" == "$script_root/.codex-workflow" ]] || fail '.codex-workflow must not escape through a symlink'
pending="$output_root"
suffix=""
while [[ ! -e "$pending" ]]; do
    part="$(basename "$pending")"
    [[ "$part" != . && "$part" != .. ]] || fail 'Output path must not contain unresolved dot segments'
    suffix="/$part$suffix"
    pending="$(dirname "$pending")"
done
[[ -d "$pending" ]] || fail 'Output parent must be a directory'
resolved_parent="$(cd "$pending" && pwd -P)"
output_root="$resolved_parent$suffix"
[[ "$output_root" == "$private_root/"* ]] || fail 'Output must be beneath the primary .codex-workflow directory'
app="$output_root/Textus Control Center.app"
[[ ! -e "$app" && ! -L "$app" ]] || fail "App destination already exists: $app"
[[ ! -L "$output_root/build" ]] || fail 'Build destination must not be a symlink'
[[ ! -L "$output_root/tmp" ]] || fail 'Temporary destination must not be a symlink'
mkdir -p "$output_root"
mkdir -p "$output_root/tmp"
export TMPDIR="$output_root/tmp"

swift test --package-path "$script_root/native/macos" --scratch-path "$output_root/build" \
    --triple arm64-apple-macosx13.0
swift build --package-path "$script_root/native/macos" --scratch-path "$output_root/build" \
    --configuration release --triple arm64-apple-macosx13.0 --product TextusControlCenterMenuBar
binary_directory="$(swift build --package-path "$script_root/native/macos" --scratch-path "$output_root/build" \
    --configuration release --triple arm64-apple-macosx13.0 --show-bin-path)"
mkdir -p "$app/Contents/MacOS"
cp "$binary_directory/TextusControlCenterMenuBar" "$app/Contents/MacOS/TextusControlCenterMenuBar"
cat > "$app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleIdentifier</key><string>org.simplemodeling.textus.controlcenter.menubar</string>
  <key>CFBundleName</key><string>Textus Control Center</string>
  <key>CFBundleDisplayName</key><string>Textus Control Center</string>
  <key>CFBundleExecutable</key><string>TextusControlCenterMenuBar</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>CFBundleShortVersionString</key><string>0.1.1</string>
  <key>LSMinimumSystemVersion</key><string>13.0</string>
  <key>LSUIElement</key><true/>
</dict></plist>
PLIST
codesign --force --sign - "$app"
codesign --verify --strict --verbose=2 "$app"
printf 'Built app: %s\nShared diagnostic: %s --summary-once --server-url http://127.0.0.1:PORT\n' \
    "$app" "$app/Contents/MacOS/TextusControlCenterMenuBar"
