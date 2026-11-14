#!/usr/bin/env bash
# Builds the NEXORA CI report markdown body from the logs and test XML produced by CI.
set -u
A="${1:-unknown}"; U="${2:-unknown}"; L="${3:-unknown}"; R="${4:-unknown}"
OUT="${5:-/tmp/report.md}"

{
  echo "## NEXORA build report"
  echo
  echo "| step | outcome |"
  echo "|---|---|"
  echo "| assembleDebug | $A |"
  echo "| unitTest | $U |"
  echo "| lint | $L |"
  echo "| assembleRelease | $R |"
  echo

  echo "### Build errors"
  echo
  echo '```'
  grep -nE "^e: |^w: .*error|error: |What went wrong|Execution failed for task|> Task .* FAILED|FAILURE:" /tmp/assemble.log 2>/dev/null | head -n 80 || echo "no error lines found"
  echo "---- context around first error ----"
  first=$(grep -nE "^e: " /tmp/assemble.log 2>/dev/null | head -1 | cut -d: -f1)
  if [ -n "${first:-}" ]; then
    start=$((first))
    end=$((first+8))
    sed -n "${start},${end}p" /tmp/assemble.log
  fi
  echo '```'
  echo

  echo "### Unit test summary"
  if ls app/build/test-results/testDebugUnitTest/*.xml >/dev/null 2>&1; then
    python3 scripts/ci-summarise-tests.py
  else
    echo "_no test result xml produced_"
  fi
  echo

  echo "<details><summary>assembleDebug log (tail 100)</summary>"
  echo
  echo '```'
  tail -n 100 /tmp/assemble.log 2>/dev/null || true
  echo '```'
  echo "</details>"
  echo

  if [ -d app/build/reports/tests/testDebugUnitTest/classes ]; then
    echo "### Failing assertions"
    echo
    echo '```'
    grep -hE "FAILED|expected:|AssertionError|at com.nexora" app/build/reports/tests/testDebugUnitTest/classes/*.html 2>/dev/null \
      | sed -e 's/<[^>]*>//g' -e 's/&quot;/\"/g' -e 's/&gt;/>/g' -e 's/&lt;/</g' \
      | head -n 80 || true
    echo '```'
    echo
  fi

  echo "<details><summary>unit test log (tail 60)</summary>"
  echo
  echo '```'
  tail -n 60 /tmp/unit.log 2>/dev/null || true
  echo '```'
  echo "</details>"
  echo

  if [ "$A" = "success" ]; then
    echo "<details><summary>lint log (tail 40)</summary>"
    echo
    echo '```'
    tail -n 40 /tmp/lint.log 2>/dev/null || true
    echo '```'
    echo "</details>"
  fi
} > "$OUT"
cat "$OUT"
