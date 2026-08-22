#!/usr/bin/env bash

# shellcheck source=../../../scripts/lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)/scripts/lib/common.sh"

verbose=false
[[ "${1:-}" == "--verbose" ]] && verbose=true

toolbox_ready() {
    sentinel_cli -h sentinel-1 -p 26379 PING | grep -q PONG
}
wait_until 60 "Sentinel toolbox" toolbox_ready

primary_ready() {
    primary="$(current_sentinel_primary 2>/dev/null || true)"
    [[ -n "$primary" ]]
}
primary=""
wait_until 60 "Sentinel master discovery" primary_ready

role_of() {
    sentinel_cli -h "$1" ROLE 2>/dev/null | head -n 1 | tr -d '\r'
}

discovered_primary_has_master_role() {
    # 장애 전환 중 get-master-addr-by-name 결과가 바뀔 수 있으므로 매번 재조회한다.
    primary="$(current_sentinel_primary 2>/dev/null || true)"
    [[ -n "$primary" && "$(role_of "$primary" || true)" == "master" ]]
}
wait_until 60 "discovered primary role" discovered_primary_has_master_role

count_roles() {
    master_count=0
    replica_count=0
    local node role
    for node in redis-primary redis-replica-1 redis-replica-2; do
        role="$(role_of "$node" || true)"
        case "$role" in
            master) ((master_count += 1)) ;;
            slave|replica) ((replica_count += 1)) ;;
        esac
    done
}

roles_converged() {
    count_roles
    [[ "$master_count" -eq 1 && "$replica_count" -eq 2 ]]
}

# 재기동된 구 primary는 Sentinel 명령을 받기 전 잠시 master로 보일 수 있다.
# 이 과도 상태를 장애로 오판하지 않고 전체 역할이 수렴할 때까지 기다린다.
wait_until 60 "one primary and two replicas" roles_converged
count_roles

[[ "$master_count" -eq 1 ]] || die "expected one primary, found $master_count"
[[ "$replica_count" -eq 2 ]] || die "expected two replicas, found $replica_count"

quorum="$(sentinel_cli -h sentinel-1 -p 26379 SENTINEL ckquorum redis-main | tr -d '\r')"
[[ "$quorum" == OK* ]] || die "Sentinel quorum is not healthy: $quorum"

if $verbose; then
    section STATUS "Sentinel"
    sentinel_cli -h sentinel-1 -p 26379 SENTINEL master redis-main
    section STATUS "Redis roles"
    for node in redis-primary redis-replica-1 redis-replica-2; do
        printf '%-18s %s\n' "$node" "$(role_of "$node")"
    done
fi

log "Sentinel topology healthy: primary=$primary, replicas=2, quorum=OK"
