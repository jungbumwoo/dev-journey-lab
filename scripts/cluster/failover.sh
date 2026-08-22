#!/usr/bin/env bash

# shellcheck source=../lib/assertions.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/lib/assertions.sh"

"$LAB_ROOT/compose/cluster/scripts/healthcheck.sh"
artifact_dir="$(new_artifact_dir cluster-failover)"
client_container=""
killed_service=""

recover() {
    if [[ -n "$killed_service" ]]; then
        cluster_compose up -d "$killed_service" >/dev/null 2>&1 || true
    fi
    if [[ -n "$client_container" ]]; then
        docker rm -f "$client_container" >/dev/null 2>&1 || true
    fi
}
trap recover EXIT

primary_service=""
for node in {1..6}; do
    role="$(cluster_cli -h "redis-cluster-$node" ROLE | head -n1 | tr -d '\r')"
    if [[ "$role" == "master" ]]; then
        primary_service="redis-cluster-$node"
        break
    fi
done
assert_nonempty "$primary_service" "could not find a Cluster primary"

lab_key=""
for index in {1..500}; do
    candidate="lab:failover:$index"
    reply="$(cluster_cli -h "$primary_service" SET "$candidate" 0 2>&1 || true)"
    if [[ "$reply" == "OK" ]]; then
        lab_key="$candidate"
        break
    fi
done
assert_nonempty "$lab_key" "could not find a key owned by $primary_service"

observer="redis-cluster-1"
[[ "$observer" == "$primary_service" ]] && observer="redis-cluster-2"

section GIVEN "Cluster-aware workload writing a slot owned by $primary_service"
client_container="$(cluster_compose --profile client run -d -e LAB_KEY="$lab_key" client-lab cluster-failover 25)"
client_ready() {
    docker logs "$client_container" 2>&1 | grep -q 'WORKLOAD_READY'
}
wait_until 90 "Java Cluster workload startup" client_ready

section WHEN "kill one shard primary and wait for its replica promotion"
cluster_compose kill -s SIGKILL "$primary_service" >/dev/null
killed_service="$primary_service"

cluster_available() {
    cluster_cli -h "$observer" CLUSTER INFO 2>/dev/null | grep -q 'cluster_state:ok'
}
wait_until "${CLUSTER_FAILOVER_TIMEOUT_SECONDS:-45}" "Cluster replica promotion" cluster_available

section RECOVERY "restart the failed node and wait for six-node convergence"
cluster_compose up -d "$primary_service" >/dev/null
killed_service=""
"$LAB_ROOT/compose/cluster/scripts/healthcheck.sh" --verbose >"$artifact_dir/recovered-topology.txt"

client_exit="$(docker wait "$client_container")"
docker logs "$client_container" >"$artifact_dir/client.log" 2>&1 || true
docker rm "$client_container" >/dev/null 2>&1 || true
client_container=""
assert_equals "0" "$client_exit" "Cluster workload failed"

final_value="$(cluster_compose exec -T toolbox redis-cli -c --raw -h "$observer" GET "$lab_key" | tr -d '\r')"
assert_nonempty "$final_value" "workload key disappeared after failover"

section THEN "the shard recovered and the bounded-retry client made progress"
log "failed primary=$primary_service, key=$lab_key, final value=$final_value"
log "evidence=$artifact_dir"
cluster_compose exec -T toolbox redis-cli -c --raw -h "$observer" DEL "$lab_key" >/dev/null
