#!/bin/bash
set -euo pipefail

fail() { printf '%s\n' "$*" >&2; exit 1; }

script_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
output_root=""
output_seen=false
while (($#)); do
    case "$1" in
        --output-root)
            [[ "$output_seen" == false ]] || fail '--output-root may appear only once'
            (($# >= 2)) || fail '--output-root requires an absolute path'
            output_seen=true
            output_root="$2"
            shift 2
            ;;
        *) fail "Unknown argument: $1" ;;
    esac
done
[[ "$output_seen" == true && "$output_root" == /* ]] || fail 'Usage: scripts/test/build_phase5_presentation_diagnostics.sh --output-root ABS'
[[ "$(uname -s)" == Darwin && "$(uname -m)" == arm64 ]] || fail 'Requires a Darwin arm64 development host'
case "$output_root/" in
    */./*|*/../*) fail 'Output path must not contain dot components' ;;
esac
while [[ "$output_root" != / && "$output_root" == */ ]]; do
    output_root="${output_root%/}"
done
[[ ! -e "$output_root" && ! -L "$output_root" ]] || fail 'Output destination already exists'

# Resolve the existing parent before creating any output; reject symlink escapes.
phase_root="$script_root/.codex-workflow/phases/PHASE-5"
[[ -d "$phase_root" ]] || fail 'The private PHASE-5 directory must already exist'
private_root="$(cd "$phase_root" && pwd -P)"
[[ "$private_root" == "$phase_root" ]] || fail 'The private PHASE-5 directory must be canonical'
pending="$output_root"
suffix=""
while [[ ! -e "$pending" && ! -L "$pending" ]]; do
    part="$(basename "$pending")"
    suffix="/$part$suffix"
    pending="$(dirname "$pending")"
done
[[ -d "$pending" ]] || fail 'Output parent must be an existing directory'
resolved_parent="$(cd "$pending" && pwd -P)"
output_root="$resolved_parent$suffix"
[[ "$output_root" == "$private_root/"* ]] || fail 'Output must be strictly beneath the private PHASE-5 directory'
[[ ! -e "$output_root" && ! -L "$output_root" ]] || fail 'Resolved output destination already exists'
umask 077
mkdir -p "$(dirname "$output_root")"
mkdir "$output_root"
mkdir "$output_root/tmp"
export TMPDIR="$output_root/tmp"

# Use normal Apple SDK/SwiftPM caches, with separate normal and diagnostic builds.
swift test --package-path "$script_root/native/macos" --scratch-path "$output_root/normal-build" \
    --triple arm64-apple-macosx13.0
swift build --package-path "$script_root/native/macos" --scratch-path "$output_root/product-build" \
    --configuration release --triple arm64-apple-macosx13.0 --product TextusControlCenterMenuBar \
    -Xswiftc -DCC5_UI_DIAGNOSTICS
binary_directory="$(swift build --package-path "$script_root/native/macos" --scratch-path "$output_root/product-build" \
    --configuration release --triple arm64-apple-macosx13.0 --product TextusControlCenterMenuBar \
    -Xswiftc -DCC5_UI_DIAGNOSTICS --show-bin-path)"
production_app="$output_root/production/Textus Control Center.app"
minimal_app="$output_root/minimal/CC5 Minimal.app"
mkdir -p "$production_app/Contents/MacOS" "$minimal_app/Contents/MacOS"
cp "$binary_directory/TextusControlCenterMenuBar" "$production_app/Contents/MacOS/TextusControlCenterMenuBar"
xcrun swiftc -swift-version 5 -parse-as-library -O -target arm64-apple-macosx13.0 \
    -DCC5_UI_DIAGNOSTICS -DCC5_MINIMAL_REPRO \
    "$script_root/native/macos/Sources/ControlCenterMenuBar/PresentationDiagnostics.swift" \
    "$script_root/scripts/test/fixtures/phase5_minimal_menu_bar.swift" \
    -o "$minimal_app/Contents/MacOS/CC5MinimalMenuBar"

cat > "$production_app/Contents/Info.plist" <<'PLIST'
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
cat > "$minimal_app/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleIdentifier</key><string>org.simplemodeling.textus.controlcenter.cc5.minimal</string>
  <key>CFBundleName</key><string>CC5 Minimal</string>
  <key>CFBundleDisplayName</key><string>CC5 Minimal</string>
  <key>CFBundleExecutable</key><string>CC5MinimalMenuBar</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>CFBundleShortVersionString</key><string>0.0.1</string>
  <key>LSMinimumSystemVersion</key><string>13.0</string>
  <key>LSUIElement</key><true/>
</dict></plist>
PLIST
codesign --force --sign - "$production_app"
codesign --verify --strict --verbose=2 "$production_app"
codesign --force --sign - "$minimal_app"
codesign --verify --strict --verbose=2 "$minimal_app"
printf 'Production diagnostic app: %s\nMinimal diagnostic app: %s\n' "$production_app" "$minimal_app"
