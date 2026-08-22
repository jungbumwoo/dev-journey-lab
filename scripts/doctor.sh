#!/usr/bin/env bash

# shellcheck source=lib/common.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/common.sh"

section CHECK "required commands"
for command_name in docker make java; do
    require_command "$command_name"
    log "$command_name: $(command -v "$command_name")"
done

docker compose version >/dev/null 2>&1 || die "Docker Compose v2+ is required"
docker info >/dev/null 2>&1 || die "Docker daemon is not reachable. Start Docker Desktop/Engine."

section CHECK "versions"
docker --version
docker compose version
java -version 2>&1 | head -n 1

java_major="$(java -version 2>&1 | awk -F'[\".]' '/version/ {print $2; exit}')"
[[ "$java_major" =~ ^[0-9]+$ ]] || die "could not parse Java version"
(( java_major >= 21 )) || die "Java 21 or newer is required (found $java_major)"

section CHECK "Docker resources"
docker_cpus="$(docker info --format '{{.NCPU}}')"
docker_memory="$(docker info --format '{{.MemTotal}}')"
docker_memory_gib="$((docker_memory / 1024 / 1024 / 1024))"
log "CPU: $docker_cpus"
log "Memory: ${docker_memory_gib} GiB"
if (( docker_cpus < 4 || docker_memory_gib < 6 )); then
    log "WARNING: 4 CPU and 6 GiB are recommended for the 6-node Cluster lab"
fi

section CHECK "pinned images"
log "Redis: $REDIS_IMAGE"
log "Java client: $CLIENT_IMAGE"
if [[ "$REDIS_IMAGE" == "redis:latest" || "$REDIS_IMAGE" == "redis:8" ]]; then
    die "REDIS_IMAGE must use an exact version tag"
fi

section RESULT "environment is ready"
