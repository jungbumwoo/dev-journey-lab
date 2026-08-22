#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/cluster/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir cluster-crossslot)"

key_a="order:1"
key_b="order:2"
tagged_a="order:{customer-1}:1"
tagged_b="order:{customer-1}:2"

slot_a="$(cluster_cli -h redis-cluster-1 CLUSTER KEYSLOT "$key_a" | tr -d '\r')"
slot_b="$(cluster_cli -h redis-cluster-1 CLUSTER KEYSLOT "$key_b" | tr -d '\r')"
tagged_slot_a="$(cluster_cli -h redis-cluster-1 CLUSTER KEYSLOT "$tagged_a" | tr -d '\r')"
tagged_slot_b="$(cluster_cli -h redis-cluster-1 CLUSTER KEYSLOT "$tagged_b" | tr -d '\r')"

[[ "$slot_a" != "$slot_b" ]] || die "test keys unexpectedly share a slot"
assert_equals "$tagged_slot_a" "$tagged_slot_b" "hash-tagged keys do not share a slot"

section WHEN "run multi-key commands across different and identical slots"
crossslot_reply="$(cluster_compose exec -T toolbox redis-cli -c --raw -h redis-cluster-1 MGET "$key_a" "$key_b" 2>&1 || true)"
assert_contains "CROSSSLOT" "$crossslot_reply" "cross-slot MGET unexpectedly succeeded"

tagged_reply="$(cluster_compose exec -T toolbox redis-cli -c --raw -h redis-cluster-1 MSET "$tagged_a" one "$tagged_b" two | tr -d '\r')"
assert_equals "OK" "$tagged_reply" "hash-tagged MSET failed"

cluster_compose --profile client run --rm client-lab cluster-crossslot >"$artifact_dir/java-client.log"

section THEN "hash tags colocate related keys but can create hot shards"
{
    printf '%s slot=%s\n' "$key_a" "$slot_a"
    printf '%s slot=%s\n' "$key_b" "$slot_b"
    printf '%s slot=%s\n' "$tagged_a" "$tagged_slot_a"
    printf '%s slot=%s\n' "$tagged_b" "$tagged_slot_b"
    printf 'error=%s\n' "$crossslot_reply"
} | tee "$artifact_dir/result.txt"

cluster_compose exec -T toolbox redis-cli -c --raw -h redis-cluster-1 DEL "$tagged_a" "$tagged_b" >/dev/null
log "evidence=$artifact_dir"
