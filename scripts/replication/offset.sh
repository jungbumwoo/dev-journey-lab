#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/replication-sentinel/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir replication-offset)"
lab_key="lab:replication:sequence"
paused_replica=""
replica_paused=false

recover() {
    if $replica_paused && [[ -n "$paused_replica" ]]; then
        sentinel_compose unpause "$paused_replica" >/dev/null 2>&1 || true
    fi
}
trap recover EXIT

primary="$(current_sentinel_primary)"
for node in redis-primary redis-replica-1 redis-replica-2; do
    [[ "$node" == "$primary" ]] && continue
    role="$(sentinel_cli -h "$node" ROLE 2>/dev/null | head -n 1 | tr -d '\r' || true)"
    if [[ "$role" == "slave" || "$role" == "replica" ]]; then
        paused_replica="$node"
        break
    fi
done
assert_nonempty "$paused_replica" "could not find a replica to pause"

section GIVEN "healthy primary with two replicas"
log "primary=$primary, pause target=$paused_replica"
sentinel_cli -h "$primary" INFO replication >"$artifact_dir/before-primary.info"

section WHEN "write a monotonic sequence and wait for two replica acknowledgements"
for sequence in {1..50}; do
    sentinel_cli -h "$primary" SET "$lab_key" "$sequence" >/dev/null
done
acknowledged="$(sentinel_cli -h "$primary" WAIT 2 5000 | tr -d '\r')"
[[ "$acknowledged" -ge 2 ]] || die "expected two replica acknowledgements, got $acknowledged"

readonly_reply="$(sentinel_cli -h "$paused_replica" SET lab:replication:readonly-test value 2>&1 || true)"
assert_contains "READONLY" "$readonly_reply" "replica unexpectedly accepted a write"

section WHEN "pause one replica and continue writing through the remaining replica"
sentinel_compose pause "$paused_replica" >/dev/null
replica_paused=true
for sequence in {51..250}; do
    sentinel_cli -h "$primary" SET "$lab_key" "$sequence" >/dev/null
done
sentinel_cli -h "$primary" INFO replication >"$artifact_dir/during-pause-primary.info"

section RECOVERY "unpause the replica and wait until it catches up"
sentinel_compose unpause "$paused_replica" >/dev/null
replica_paused=false

replica_caught_up() {
    [[ "$(sentinel_cli -h "$paused_replica" GET "$lab_key" 2>/dev/null | tr -d '\r')" == "250" ]]
}
wait_until 30 "$paused_replica catching up to sequence 250" replica_caught_up

for node in redis-primary redis-replica-1 redis-replica-2; do
    sentinel_cli -h "$node" INFO replication >"$artifact_dir/after-$node.info"
    value="$(sentinel_cli -h "$node" GET "$lab_key" | tr -d '\r')"
    assert_equals "250" "$value" "$node did not converge to the final value"
done

section THEN "replicas converged after asynchronous lag"
log "WAIT acknowledgements=$acknowledged"
log "evidence=$artifact_dir"
sentinel_cli -h "$primary" DEL "$lab_key" >/dev/null
