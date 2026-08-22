#!/usr/bin/env bash

# shellcheck source=../../../scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)/scripts/lib/common.sh"

toolbox_ready() {
    cluster_cli -h redis-cluster-1 PING | grep -q PONG
}
wait_until 60 "Cluster toolbox" toolbox_ready

all_nodes_ready() {
    local node
    for node in {1..6}; do
        cluster_cli -h "redis-cluster-$node" PING | grep -q PONG || return 1
    done
}
wait_until 60 "all six Redis nodes" all_nodes_ready

cluster_state="$(cluster_cli -h redis-cluster-1 CLUSTER INFO 2>/dev/null | awk -F: '$1 == "cluster_state" {gsub("\\r", "", $2); print $2}')"
if [[ "$cluster_state" == "ok" ]]; then
    log "Cluster already initialized; keeping existing node IDs and data"
    exit 0
fi

partially_configured=false
for node in {1..6}; do
    known_nodes="$(cluster_cli -h "redis-cluster-$node" CLUSTER INFO | awk -F: '$1 == "cluster_known_nodes" {gsub("\\r", "", $2); print $2}')"
    if [[ -n "$known_nodes" && "$known_nodes" -gt 1 ]]; then
        partially_configured=true
    fi
done

if $partially_configured; then
    cluster_cli -h redis-cluster-1 CLUSTER NODES >&2 || true
    die "partially configured Cluster detected; inspect it and run make destroy only if data can be discarded"
fi

section WHEN "create 3-primary + 3-replica Cluster"
cluster_compose exec -T toolbox redis-cli --cluster create \
    redis-cluster-1:6379 redis-cluster-2:6379 redis-cluster-3:6379 \
    redis-cluster-4:6379 redis-cluster-5:6379 redis-cluster-6:6379 \
    --cluster-replicas 1 --cluster-yes

cluster_ok() {
    cluster_cli -h redis-cluster-1 CLUSTER INFO | grep -q 'cluster_state:ok'
}
wait_until 60 "cluster_state:ok" cluster_ok
log "Cluster initialization completed"
