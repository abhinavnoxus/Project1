#!/usr/bin/env bash
# Structural source checks that run before Gradle, so obvious syntax slips cost seconds
# instead of a five-minute CI round trip.
set -uo pipefail
cd "$(dirname "$0")/.."
fail=0

# 1. Enum bodies: within ONE enum declaration, the entry list must come before any member
#    `fun`. Scoped per-enum (not per-file) because a file may hold several enums and data
#    classes, and a data class legitimately has functions before any enum in the file.
while IFS= read -r f; do
    awk -v F="$f" '
        /^enum class / { in_enum = 1; seen_entry = 0; next }
        in_enum && /^}/ { in_enum = 0; next }
        in_enum && /^    [A-Z][A-Za-z0-9_]*\(/ { seen_entry = 1 }
        in_enum && /^    fun / && !seen_entry {
            printf "::error file=%s line=%d::function declared before the enum entry list\n", F, NR
            found = 1
        }
        END { exit 0 }
    ' "$f"
done < <(grep -rlE '^enum class' app/src/main/java --include='*.kt')
# 2. Duplicate imports in one file.
while IFS= read -r f; do
    dups=$(grep -E '^import ' "$f" | sort | uniq -d)
    if [ -n "$dups" ]; then
        echo "::error file=$f::duplicate imports"
        echo "$dups"
        fail=1
    fi
done < <(find app/src -name '*.kt')

# 3. Unbalanced braces, a blunt but effective smoke test.
while IFS= read -r f; do
    o=$(tr -cd '{' < "$f" | wc -c)
    c=$(tr -cd '}' < "$f" | wc -c)
    if [ "$o" -ne "$c" ]; then
        echo "::error file=$f::brace mismatch open=$o close=$c"
        fail=1
    fi
done < <(find app/src -name '*.kt')

if [ "$fail" -eq 0 ]; then echo "static checks passed"; fi
exit "$fail"
