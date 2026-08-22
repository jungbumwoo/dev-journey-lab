#!/usr/bin/env bash

set -Eeuo pipefail

LAB_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

if [[ -f "$LAB_ROOT/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$LAB_ROOT/.env"
    set +a
fi

REDIS_IMAGE="${REDIS_IMAGE:-redis:8.8.1-trixie}"
CLIENT_IMAGE="${CLIENT_IMAGE:-redis-operations-client:local}"
SENTINEL_PROJECT="${SENTINEL_PROJECT:-redis-sentinel-lab}"
CLUSTER_PROJECT="${CLUSTER_PROJECT:-redis-cluster-lab}"
SENTINEL_COMPOSE="$LAB_ROOT/compose/replication-sentinel/compose.yml"
CLUSTER_COMPOSE="$LAB_ROOT/compose/cluster/compose.yml"

log() {
    printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

section() {
    printf '\n[%s] %s\n' "$1" "$2"
}

die() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "required command not found: $1"
}

new_artifact_dir() {
    local scenario="$1"
    local created="$LAB_ROOT/artifacts/$scenario/$(date '+%Y%m%d-%H%M%S')"
    mkdir -p "$created"
    printf '%s\n' "$created"
}

wait_until() {
    local timeout_seconds="$1"
    local description="$2"
    shift 2
    local deadline=$((SECONDS + timeout_seconds))

    until "$@" >/dev/null 2>&1; do
        if (( SECONDS >= deadline )); then
            die "timeout waiting for: $description (${timeout_seconds}s)"
        fi
        sleep 1
    done
}

sentinel_compose() {
    docker compose -p "$SENTINEL_PROJECT" -f "$SENTINEL_COMPOSE" "$@"
}

cluster_compose() {
    docker compose -p "$CLUSTER_PROJECT" -f "$CLUSTER_COMPOSE" "$@"
}

sentinel_cli() {
    sentinel_compose exec -T toolbox redis-cli --raw "$@"
}

cluster_cli() {
    cluster_compose exec -T toolbox redis-cli --raw "$@"
}

current_sentinel_primary() {
    sentinel_cli -h sentinel-1 -p 26379 SENTINEL get-master-addr-by-name redis-main | head -n 1 | tr -d '\r'
}

container_running() {
    local container_id="$1"
    [[ "$(docker inspect -f '{{.State.Running}}' "$container_id" 2>/dev/null)" == "true" ]]
}
