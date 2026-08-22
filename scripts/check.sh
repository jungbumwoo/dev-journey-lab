#!/usr/bin/env bash

# shellcheck source=lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/common.sh"

section CHECK "Compose configuration"
sentinel_compose config --quiet
cluster_compose config --quiet

section CHECK "shell syntax"
while IFS= read -r script; do
    bash -n "$script"
done < <(find "$LAB_ROOT/scripts" "$LAB_ROOT/compose" -type f -name '*.sh' -print | sort)

section CHECK "Java build"
if [[ -x "$LAB_ROOT/gradlew" ]]; then
    "$LAB_ROOT/gradlew" --no-daemon :client-lab:test
else
    log "Gradle wrapper is not generated yet; skipping Java build"
fi

section RESULT "static checks passed"
