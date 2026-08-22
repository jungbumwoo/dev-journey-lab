#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/replication-sentinel/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir sentinel-failover)"
client_container=""
killed_service=""

primary_service() {
    local node role
    for node in redis-primary redis-replica-1 redis-replica-2; do
        role="$(sentinel_cli -h "$node" ROLE 2>/dev/null | head -n 1 | tr -d '\r' || true)"
        if [[ "$role" == "master" ]]; then
            printf '%s\n' "$node"
            return 0
        fi
    done
    return 1
}

recover() {
    if [[ -n "$killed_service" ]]; then
        sentinel_compose up -d "$killed_service" >/dev/null 2>&1 || true
    fi
    if [[ -n "$client_container" ]]; then
        docker rm -f "$client_container" >/dev/null 2>&1 || true
    fi
}
trap recover EXIT

old_primary="$(primary_service)"
old_endpoint="$(current_sentinel_primary)"
assert_nonempty "$old_primary" "could not identify current primary service"

section GIVEN "Sentinel-aware Java workload writing to $old_primary"
client_container="$(sentinel_compose --profile client run -d client-lab sentinel-failover 25)"

client_ready() {
    docker logs "$client_container" 2>&1 | grep -q 'WORKLOAD_READY'
}
wait_until 90 "Java Sentinel workload startup" client_ready

section WHEN "kill the current primary with SIGKILL"
sentinel_compose kill -s SIGKILL "$old_primary" >/dev/null
killed_service="$old_primary"

new_primary_elected() {
    local endpoint
    endpoint="$(current_sentinel_primary 2>/dev/null || true)"
    [[ -n "$endpoint" && "$endpoint" != "$old_endpoint" ]] || return 1
    [[ "$(sentinel_cli -h "$endpoint" ROLE 2>/dev/null | head -n1 | tr -d '\r')" == "master" ]]
}
wait_until "${SENTINEL_FAILOVER_TIMEOUT_SECONDS:-45}" "Sentinel failover" new_primary_elected
new_primary="$(current_sentinel_primary)"

client_exit="$(docker wait "$client_container")"
docker logs "$client_container" >"$artifact_dir/client.log" 2>&1 || true
docker rm "$client_container" >/dev/null 2>&1 || true
client_container=""
assert_equals "0" "$client_exit" "Java workload failed"

section RECOVERY "restart the old primary and let Sentinel reconfigure it"
sentinel_compose up -d "$old_primary" >/dev/null
killed_service=""
"$LAB_ROOT/compose/replication-sentinel/scripts/healthcheck.sh" --verbose >"$artifact_dir/recovered-topology.txt"

section THEN "Sentinel promoted a replica and the client rediscovered the primary"
log "old=$old_endpoint, new=$new_primary"
log "evidence=$artifact_dir"
