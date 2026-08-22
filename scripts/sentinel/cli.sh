#!/usr/bin/env bash

# shellcheck source=../lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/common.sh"

primary="$(current_sentinel_primary)"
assert_message="Sentinel did not return the current primary"
[[ -n "$primary" ]] || die "$assert_message"
log "connecting to current primary: $primary:6379"
exec docker compose -p "$SENTINEL_PROJECT" -f "$SENTINEL_COMPOSE" exec toolbox redis-cli -h "$primary"
