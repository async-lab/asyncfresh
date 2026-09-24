#!/usr/bin/env bash
set -euo pipefail
export PATH="/usr/bin:/bin:$PATH"

script_dir="$(cd "$(dirname "$0")" && pwd)"
target="${1:?usage: ROLLBACK.sh TARGET_FILE}"
cp "$script_dir/BASELINE.vue" "$target"
cmp "$script_dir/BASELINE.vue" "$target"
printf 'RESTORED baseline\n'
