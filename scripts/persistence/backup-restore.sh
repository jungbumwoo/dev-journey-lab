#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/replication-sentinel/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir persistence-backup)"
restore_container="redis-restore-check-$(date '+%Y%m%d%H%M%S')"

cleanup() {
    docker rm -f "$restore_container" >/dev/null 2>&1 || true
}
trap cleanup EXIT

primary_service() {
    local node role
    for node in redis-primary redis-replica-1 redis-replica-2; do
        role="$(sentinel_cli -h "$node" ROLE 2>/dev/null | head -n1 | tr -d '\r' || true)"
        [[ "$role" == "master" ]] && { printf '%s\n' "$node"; return 0; }
    done
    return 1
}

key="lab:persistence:restore-check"
value="backup-$(date '+%s')"
primary="$(primary_service)"

section GIVEN "AOF everysec with a replicated write"
sentinel_cli -h "$primary" SET "$key" "$value" >/dev/null
acknowledged="$(sentinel_cli -h "$primary" WAIT 1 5000 | tr -d '\r')"
[[ "$acknowledged" -ge 1 ]] || die "write was not acknowledged by a replica"
sentinel_cli -h "$primary" INFO persistence >"$artifact_dir/before-restart.info"

section WHEN "restart the current primary while preserving its named volume"
sentinel_compose restart "$primary" >/dev/null
"$LAB_ROOT/compose/replication-sentinel/scripts/healthcheck.sh"
current_primary="$(primary_service)"
restored_value="$(sentinel_cli -h "$current_primary" GET "$key" | tr -d '\r')"
assert_equals "$value" "$restored_value" "AOF/replication restart check failed"

section WHEN "create an RDB backup and restore it in an isolated container"
sentinel_cli -h "$current_primary" BGSAVE >/dev/null
rdb_ready() {
    local persistence
    persistence="$(sentinel_cli -h "$current_primary" INFO persistence 2>/dev/null | tr -d '\r')"
    grep -q 'rdb_bgsave_in_progress:0' <<<"$persistence" && grep -q 'rdb_last_bgsave_status:ok' <<<"$persistence"
}
wait_until 60 "successful BGSAVE" rdb_ready

primary_container="$(sentinel_compose ps -q "$current_primary")"
docker cp "$primary_container:/data/dump.rdb" "$artifact_dir/dump.rdb"
[[ -s "$artifact_dir/dump.rdb" ]] || die "RDB backup was not copied"

docker run -d --name "$restore_container" --network none \
    -v "$artifact_dir:/backup:ro" "$REDIS_IMAGE" \
    redis-server --dir /backup --dbfilename dump.rdb --appendonly no --save "" --bind 127.0.0.1 >/dev/null

restore_ready() {
    docker exec "$restore_container" redis-cli PING 2>/dev/null | grep -q PONG
}
wait_until 30 "isolated restore Redis" restore_ready

verified_value="$(docker exec "$restore_container" redis-cli --raw GET "$key" | tr -d '\r')"
assert_equals "$value" "$verified_value" "restored RDB does not contain the expected value"

section THEN "the volume survived restart and the external RDB passed a restore test"
sentinel_cli -h "$current_primary" INFO persistence >"$artifact_dir/after-restart.info"
log "backup=$artifact_dir/dump.rdb"
log "evidence=$artifact_dir"
sentinel_cli -h "$current_primary" DEL "$key" >/dev/null
