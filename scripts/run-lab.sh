#!/usr/bin/env bash

set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

list_labs() {
    printf '%s\n' \
        replication-offset \
        sentinel-failover \
        cluster-redirect \
        cluster-crossslot \
        cluster-failover \
        persistence-backup
}

if [[ "${1:-}" == "--list" ]]; then
    list_labs
    exit 0
fi

lab="${1:-}"
case "$lab" in
    replication-offset) exec "$ROOT/scripts/replication/offset.sh" ;;
    sentinel-failover) exec "$ROOT/scripts/sentinel/failover.sh" ;;
    cluster-redirect) exec "$ROOT/scripts/cluster/redirect.sh" ;;
    cluster-crossslot) exec "$ROOT/scripts/cluster/crossslot.sh" ;;
    cluster-failover) exec "$ROOT/scripts/cluster/failover.sh" ;;
    persistence-backup) exec "$ROOT/scripts/persistence/backup-restore.sh" ;;
    "") printf 'LAB is required. Available labs:\n' >&2; list_labs >&2; exit 2 ;;
    *) printf 'Unknown lab: %s\nAvailable labs:\n' "$lab" >&2; list_labs >&2; exit 2 ;;
esac
