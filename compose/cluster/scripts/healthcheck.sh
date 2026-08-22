#!/usr/bin/env bash

# shellcheck source=../../../scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)/scripts/lib/common.sh"

verbose=false
[[ "${1:-}" == "--verbose" ]] && verbose=true

cluster_info_ready() {
    cluster_cli -h redis-cluster-1 CLUSTER INFO | grep -q 'cluster_state:ok'
}
wait_until 60 "healthy Cluster" cluster_info_ready

info="$(cluster_cli -h redis-cluster-1 CLUSTER INFO | tr -d '\r')"
value_of() {
    local field="$1"
    awk -F: -v wanted="$field" '$1 == wanted {print $2}' <<<"$info"
}

[[ "$(value_of cluster_slots_assigned)" == "16384" ]] || die "not all 16384 slots are assigned"
[[ "$(value_of cluster_slots_ok)" == "16384" ]] || die "not all slots are healthy"
[[ "$(value_of cluster_slots_pfail)" == "0" ]] || die "Cluster has PFAIL slots"
[[ "$(value_of cluster_slots_fail)" == "0" ]] || die "Cluster has FAIL slots"

nodes="$(cluster_cli -h redis-cluster-1 CLUSTER NODES | tr -d '\r')"
primary_count="$(awk '$3 ~ /(^|,)master(,|$)/ && $3 !~ /fail/ {count++} END {print count+0}' <<<"$nodes")"
replica_count="$(awk '$3 ~ /(^|,)(slave|replica)(,|$)/ && $3 !~ /fail/ {count++} END {print count+0}' <<<"$nodes")"
disconnected_count="$(awk '$8 != "connected" {count++} END {print count+0}' <<<"$nodes")"

[[ "$primary_count" == "3" ]] || die "expected 3 primaries, found $primary_count"
[[ "$replica_count" == "3" ]] || die "expected 3 replicas, found $replica_count"
[[ "$disconnected_count" == "0" ]] || die "found $disconnected_count disconnected Cluster links"

for node in {1..6}; do
    pong="$(cluster_cli -h "redis-cluster-$node" PING | tr -d '\r')"
    [[ "$pong" == "PONG" ]] || die "redis-cluster-$node did not answer PONG"
done

if $verbose; then
    section STATUS "Cluster info"
    printf '%s\n' "$info"
    section STATUS "Cluster nodes"
    printf '%s\n' "$nodes"
fi

log "Cluster healthy: primaries=3, replicas=3, slots=16384"
