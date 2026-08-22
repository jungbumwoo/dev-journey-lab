# Redis Operations Lab

Docker에서 Redis replication/Sentinel과 3-primary + 3-replica Cluster를 직접
운영하며 장애, redirect, 데이터 안전성, client retry를 관찰하는 실습 프로젝트다.
전체 설계와 후속 범위는 [plan.md](plan.md)에 정리되어 있다.

## Prerequisites

- Docker Engine/Desktop와 Docker Compose v2 이상
- GNU Make
- Java 21 (로컬 Java build를 실행할 때)
- 권장 Docker 자원: 4 CPU, 6 GiB memory 이상

Redis와 Java client는 host port를 사용하지 않는다. Cluster가 광고하는 Docker
hostname을 그대로 사용할 수 있도록 `redis-cli`와 Java client도 같은 Compose
network 안에서 실행한다.

## Quick start

```bash
cp .env.example .env
make doctor

# Replication + Sentinel
make sentinel-up
make sentinel-status
make lab LAB=replication-offset
make lab LAB=sentinel-failover

# Redis Cluster
make cluster-up
make cluster-status
make lab LAB=cluster-redirect
make lab LAB=cluster-crossslot
make lab LAB=cluster-failover

# Persistence
make lab LAB=persistence-backup
```

실습 목록은 `./scripts/run-lab.sh --list`로 확인한다. 실행 증거는
`artifacts/<lab>/<timestamp>/`에 저장되며 Git에는 포함되지 않는다.

## Safe cleanup

```bash
make down       # container만 내리고 named volume은 보존
make destroy    # DESTROY 확인 후 이 프로젝트의 named volume까지 삭제
```

`make destroy`는 되돌릴 수 없는 학습 데이터 삭제 작업이다. 일반적인 종료에는
`make down`을 사용한다.

## Topologies

| Topology | Services | Purpose |
|---|---|---|
| Replication + Sentinel | primary 1, replica 2, Sentinel 3 | 비분산 HA와 자동 failover |
| Redis Cluster | primary 3, replica 3 | slot 기반 샤딩과 shard failover |

Sentinel과 Cluster는 결합하지 않는다. Cluster는 자체 replica election과
failover를 사용한다.

## Java examples

Java/Lettuce 예제는 Docker image로 실행하므로 host에서 Docker hostname을
해석하기 위한 별도 설정이 필요 없다.

```bash
make client-image
make client-run TOPOLOGY=cluster SCENARIO=cluster-redirect
make client-run TOPOLOGY=cluster SCENARIO=cluster-crossslot
make client-run TOPOLOGY=sentinel SCENARIO=sentinel-discovery
```

## Safety notes

- 이 구성은 한 Docker host에서 실행되므로 실제 독립 AZ 장애를 모사하지 않는다.
- Redis replication, `WAIT`, `WAITAOF`는 strong consistency를 보장하지 않는다.
- timeout 후 write retry는 중복 실행될 수 있다.
- `replication-offset` 실습은 container pause를 사용하며 종료 trap으로 복구한다.
- 각 node의 `nodes.conf`, AOF, RDB는 서로 다른 named volume에 저장한다.
