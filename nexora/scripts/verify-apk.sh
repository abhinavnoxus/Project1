#!/usr/bin/env bash
# Inspects the built APK with the SDK's own tooling and prints facts about it.
set -u
APK=app/build/outputs/apk/debug/app-debug.apk
REL=app/build/outputs/apk/release/app-release.apk
AAPT=$(ls "$ANDROID_HOME"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1)

echo "## NEXORA APK verification"
echo
echo "| artefact | bytes | sha256 |"
echo "|---|---|---|"
for f in "$APK" "$REL"; do
  if [ -f "$f" ]; then
    echo "| $(basename "$f") | $(stat -c%s "$f") | $(sha256sum "$f" | cut -d' ' -f1) |"
  fi
done
echo

if [ -n "${AAPT:-}" ] && [ -f "$APK" ]; then
  echo "### Badging"
  echo '```'
  "$AAPT" dump badging "$APK" 2>/dev/null | head -25
  echo '```'
  echo

  echo "### Permissions actually declared"
  echo '```'
  "$AAPT" dump permissions "$APK" 2>/dev/null | head -30
  echo '```'
  echo

  echo "### Components"
  echo '```'
  "$AAPT" dump xmltree --file AndroidManifest.xml "$APK" 2>/dev/null \
    | grep -E "E: (activity|service|receiver|provider)|A: android:name" \
    | sed 's/^ *//' | head -40
  echo '```'
fi

if [ -f "$APK" ]; then
  echo "### Contents"
  echo '```'
  python3 - "$APK" <<'PY'
import sys, zipfile
z = zipfile.ZipFile(sys.argv[1])
names = z.namelist()
dex = [n for n in names if n.endswith('.dex')]
print(f"entries={len(names)} dex={len(dex)}")
for n in dex:
    print(f"  {n}: {z.getinfo(n).file_size} bytes")
print("resources.arsc:", any(n == 'resources.arsc' for n in names))
print("classes present sample:", [n for n in names if n.startswith('classes')][:5])
PY
  echo '```'
fi
