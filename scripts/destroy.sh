#!/usr/bin/env bash

# shellcheck source=lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/common.sh"

confirmation="${CONFIRM_DESTROY:-}"
if [[ "$confirmation" != "DESTROY" ]]; then
    [[ -t 0 ]] || die "non-interactive use requires CONFIRM_DESTROY=DESTROY"
    printf 'This deletes named volumes for %s and %s. Type DESTROY: ' "$SENTINEL_PROJECT" "$CLUSTER_PROJECT"
    read -r confirmation
fi

[[ "$confirmation" == "DESTROY" ]] || die "confirmation did not match; nothing was deleted"

sentinel_compose down -v --remove-orphans
cluster_compose down -v --remove-orphans
log "removed only the two lab Compose projects and their named volumes"
log "artifacts/ was preserved"
