#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/cluster/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir cluster-redirect)"
moved_key=""
moved_reply=""

section GIVEN "a non-cluster-aware client connected only to redis-cluster-1"
for index in {1..200}; do
    candidate="lab:moved:$index"
    reply="$(cluster_cli -h redis-cluster-1 SET "$candidate" direct 2>&1 || true)"
    if [[ "$reply" == MOVED* ]]; then
        moved_key="$candidate"
        moved_reply="$reply"
        break
    fi
    cluster_cli -h redis-cluster-1 DEL "$candidate" >/dev/null 2>&1 || true
done
assert_nonempty "$moved_key" "could not find a key owned by another node"

section WHEN "send the same key through redis-cli -c and Lettuce ClusterClient"
cluster_reply="$(cluster_compose exec -T toolbox redis-cli -c --raw -h redis-cluster-1 SET "$moved_key" cluster-aware | tr -d '\r')"
assert_equals "OK" "$cluster_reply" "cluster-aware redis-cli did not follow MOVED"

cluster_compose --profile client run --rm client-lab cluster-redirect "$moved_key" >"$artifact_dir/java-client.log"

section THEN "the direct client receives MOVED while cluster-aware clients succeed"
printf '%s\n' "$moved_reply" | tee "$artifact_dir/moved.txt"
log "key=$moved_key"
log "evidence=$artifact_dir"
cluster_compose exec -T toolbox redis-cli -c --raw -h redis-cluster-1 DEL "$moved_key" >/dev/null
