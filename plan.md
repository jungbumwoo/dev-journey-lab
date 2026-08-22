# Docker 기반 Redis 운영 실습 프로젝트 계획

## 1. 목표

로컬 Docker 환경에서 Redis의 복제와 고가용성, Cluster 샤딩을 직접 구성하고,
정상 동작뿐 아니라 장애·복구·데이터 유실 가능성·클라이언트 동작까지 코드로
재현하는 실습 프로젝트를 만든다.

이 프로젝트의 핵심 목표는 다음과 같다.

1. Redis의 primary-replica 복제가 비동기라는 사실을 replication offset과
   장애 실험으로 확인한다.
2. 일반 복제만으로는 자동 failover가 되지 않으며, 비분산 Redis의 HA에는
   Sentinel이 필요하다는 점을 확인한다.
3. Redis Cluster의 16,384 hash slot, replica 승격, `MOVED`/`ASK`, hash tag,
   resharding을 실제 명령과 클라이언트 코드로 관찰한다.
4. 장애 상황에서 클라이언트 재연결·timeout·retry·중복 실행·stale read가
   애플리케이션에 어떤 영향을 주는지 확인한다.
5. persistence, backup, memory, latency, security, monitoring 등 실무 운영에서
   놓치기 쉬운 항목을 재현 가능한 runbook 형태로 정리한다.

문서와 코드에서는 기존 용어인 `master/slave` 대신 Redis가 현재 사용하는
`primary/replica`를 사용한다. 명령이나 설정 이름에 `master`가 남아 있는 경우
(`masterauth`, Sentinel의 master name 등)에만 원래 이름을 유지한다.

## 2. 중요한 설계 결정

### 2.1 토폴로지를 두 종류로 분리한다

Sentinel과 Redis Cluster는 서로 다른 문제를 해결한다. 한 구성에 섞지 않는다.

| 토폴로지 | 구성 | 학습 목적 |
|---|---|---|
| Replication + Sentinel | primary 1, replica 2, Sentinel 3 | 샤딩 없는 단일 keyspace의 복제와 자동 failover |
| Redis Cluster | primary 3, replica 3 | hash slot 기반 샤딩과 shard별 자동 failover |

Redis Cluster 자체가 replica와 failover 기능을 제공하므로 Cluster 앞에 Sentinel을
추가하지 않는다.

### 2.2 Docker 내부 네트워크를 기본 실행 환경으로 사용한다

Cluster와 Sentinel은 자신이 알고 있는 노드 주소를 클라이언트와 다른 노드에
알려준다. Docker Desktop의 host port와 container address를 섞으면 최초 노드에는
접속되지만 redirect된 주소에는 접속하지 못하는 문제가 쉽게 발생한다.

따라서 기본 실습은 다음 원칙을 사용한다.

- Redis 노드, Java 예제 앱, `redis-cli` toolbox를 같은 Docker network에 둔다.
- 노드 사이에서는 Compose service name과 container port를 사용한다.
- 호스트에서는 `docker compose exec` 또는 `make cli-*`를 통해 toolbox를 사용한다.
- host에서 직접 Cluster에 접속하는 예제는 별도의 `nat-trap` 실습으로 둔다.
- production에서는 모든 노드의 client port와 cluster bus port가 상호 도달
  가능해야 하며 NAT 뒤의 주소 광고 문제를 반드시 검증한다.

### 2.3 버전과 실행 결과를 재현 가능하게 고정한다

- 작성 시점의 Docker Official Image인 `redis:8.8.1-trixie`를 기본값으로 둔다.
- `latest`, `8`, `8.8` 같은 이동 tag는 사용하지 않는다.
- 실제 구현 시 image digest까지 기록하고, 업그레이드는 별도 PR/commit으로 한다.
- Java 21과 Gradle Wrapper를 사용한다.
- Java dependency는 version catalog와 dependency locking으로 고정한다.
- Redis 7.x와의 차이를 확인하는 호환성 profile은 기본 실습 완료 후 추가한다.

### 2.4 CLI 실습과 애플리케이션 실습을 모두 제공한다

- 서버 내부 상태와 운영 명령은 `redis-cli` 기반 shell script로 관찰한다.
- topology 변경을 애플리케이션이 어떻게 처리하는지는 Java + Lettuce로 확인한다.
- 처음에는 Lettuce를 직접 사용해 동작을 드러내고, Spring Data Redis 예제는
  선택적인 adapter module로 추가한다.
- 모든 실습은 사람이 읽을 수 있는 로그와 자동 assertion을 함께 제공한다.

## 3. 제안 디렉터리 구조

```text
dev-journey-lab/
├── README.md
├── plan.md
├── Makefile
├── .env.example
├── compose/
│   ├── replication-sentinel/
│   │   ├── compose.yml
│   │   ├── conf/
│   │   │   ├── redis.common.conf      # 모든 노드 공통 설정
│   │   │   ├── replica.conf           # replicaof만 추가
│   │   │   └── sentinel.template.conf
│   │   └── scripts/
│   │       ├── init-sentinels.sh
│   │       └── healthcheck.sh
│   ├── cluster/
│   │   ├── compose.yml
│   │   ├── conf/redis-cluster.conf
│   │   └── scripts/
│   │       ├── create-cluster.sh
│   │       ├── healthcheck.sh
│   │       └── reset-cluster.sh
│   └── observability/
│       ├── compose.yml
│       ├── prometheus.yml
│       └── grafana/
├── scripts/
│   ├── lib/
│   │   ├── common.sh
│   │   ├── wait-until.sh
│   │   └── assertions.sh
│   ├── replication/
│   ├── sentinel/
│   ├── cluster/
│   ├── persistence/
│   ├── memory/
│   └── security/
├── client-lab/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/.../redis/
│       │   ├── common/
│       │   ├── replication/
│       │   ├── sentinel/
│       │   ├── cluster/
│       │   └── workload/
│       └── test/java/.../redis/
├── spring-client-lab/             # 선택 사항, 후반 단계
├── labs/
│   ├── 00-environment.md
│   ├── 10-replication.md
│   ├── 20-sentinel-failover.md
│   ├── 30-cluster-basics.md
│   ├── 40-cluster-failover.md
│   ├── 50-resharding.md
│   ├── 60-persistence-backup.md
│   ├── 70-memory-latency.md
│   └── 80-security.md
├── runbooks/
│   ├── failover.md
│   ├── add-remove-node.md
│   ├── backup-restore.md
│   ├── diagnose-latency.md
│   └── incident-checklist.md
└── artifacts/                     # 실행 결과, gitignore
```

## 4. 공통 실행 인터페이스

학습자가 Compose 파일의 세부 명령을 외우지 않아도 되도록 Make target을
프로젝트의 표준 인터페이스로 제공한다.

```bash
make doctor                 # Docker, Compose, Java, port, resource 확인

make sentinel-up            # primary + replicas + Sentinels 기동
make sentinel-status
make sentinel-down          # volume 보존

make cluster-up             # 6개 노드 기동 + idempotent cluster 생성
make cluster-status
make cluster-down           # volume 보존

make lab LAB=replication-offset
make lab LAB=sentinel-failover
make lab LAB=cluster-redirect

make down                   # 모든 컨테이너 중지, 데이터 보존
make destroy                # 확인 문구 입력 후에만 전용 volume 제거
```

`down`과 `destroy`를 분리한다. `docker compose down -v`는 학습 데이터와
persistence 결과를 지우므로 일반 cleanup 명령에서 절대 암묵적으로 실행하지
않는다.

모든 실습 script는 다음 계약을 따른다.

1. 시작 전 필요한 topology와 상태를 검증한다.
2. `GIVEN / WHEN / THEN / RECOVERY` 형식으로 수행 내용을 출력한다.
3. 관찰한 `INFO`, role, offset, slot map을 `artifacts/<lab>/<timestamp>/`에 저장한다.
4. 예상 조건을 만족하지 않으면 non-zero exit code를 반환한다.
5. 장애를 주입한 실습은 shell `trap`으로 가능한 범위에서 topology를 복구한다.
6. 재실행 가능해야 하며 이미 완료된 bootstrap을 안전하게 감지한다.

## 5. 인프라 구성 계획

### 5.1 Replication + Sentinel

구성:

- `redis-primary`
- `redis-replica-1`, `redis-replica-2`
- `sentinel-1`, `sentinel-2`, `sentinel-3`
- `toolbox`
- `client-lab` profile

주요 설정:

```conf
# 모든 노드 공통 (redis.common.conf)
# Sentinel failover 후에는 어떤 replica든 primary가 될 수 있으므로
# replicaof를 제외한 설정은 primary/replica 구분 없이 동일하게 유지한다.
# min-replicas-*를 primary에만 두면 failover 직후 write safety가 사라진다.
replica-read-only yes
min-replicas-to-write 1        # best-effort write safety 실습용 profile
min-replicas-max-lag 5

# replica에만 추가 (replica.conf)
replicaof redis-primary 6379

# Sentinel 개념 예시 (sentinel.conf 지시어는 소문자)
sentinel resolve-hostnames yes
sentinel announce-hostnames yes
sentinel monitor redis-main redis-primary 6379 2
sentinel down-after-milliseconds redis-main 5000
sentinel failover-timeout redis-main 30000
sentinel parallel-syncs redis-main 1
```

Sentinel은 실행 중 상태를 자신의 설정 파일에 다시 기록한다. read-only bind
mount를 직접 전달하지 않고 각 Sentinel의 writable volume에 template을 복사한
후 실행한다. Sentinel별 설정 파일과 volume을 공유하지 않는다.

모든 Redis/Sentinel service는 `restart: "no"`로 둔다. restart policy가 걸려
있으면 `SIGKILL` 장애 주입 직후 컨테이너가 자동 재기동되어 failover가
관찰되지 않거나 타이밍이 왜곡된다. 장애 주입과 정지는 실습 script에서
`docker compose kill`/`stop`으로 명시적으로 수행한다. 이 원칙은 Cluster
구성에도 동일하게 적용한다.

비밀번호를 사용하는 profile에서는 primary의 ACL, replica의 `masteruser`와
`masterauth`, Sentinel의 Redis 인증, Sentinel 자체 ACL을 모두 설정한다.
`requirepass` 하나만 추가하고 복제 인증을 빼먹는 잘못된 예제도 별도 실습으로
재현한다.

### 5.2 Redis Cluster

구성:

- `redis-cluster-1` ~ `redis-cluster-6`
- primary 3개, primary당 replica 1개
- `toolbox`
- `client-lab` profile

최소 설정:

```conf
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
```

각 노드는 고유한 data volume을 사용한다. `nodes.conf`는 Redis가 관리하는
노드 identity와 topology 상태이므로 사람이 template으로 만들거나 여러 노드가
공유해서는 안 된다.

Compose에서는 공통 config와 함께 각 service에 고유한
`cluster-announce-hostname redis-cluster-N`을 전달하고,
`cluster-preferred-endpoint-type hostname`을 사용한다. 이렇게 하면 Docker 내부의
cluster-aware client가 container IP 대신 안정적인 service name을 redirect
endpoint로 받는다. cluster bus의 기본 port는 client port + 10000이다. bus port는
전용 바이너리 프로토콜이라 외부에서 직접 검증하기 어려우므로, 노드 간 통신
상태는 `CLUSTER NODES`의 link state와 pfail/fail 카운터로 간접 확인한다.

`create-cluster.sh`는 다음 순서로 동작한다.

1. 6개 노드의 `PING` 응답을 기다린다.
2. 이미 `cluster_state:ok`이면 성공으로 종료한다.
3. 일부만 구성된 상태이면 자동 삭제하지 않고 진단 정보와 복구 방법을 출력한다.
4. 완전히 비어 있을 때만 다음 형태로 cluster를 만든다.

```bash
redis-cli --cluster create \
  redis-cluster-1:6379 redis-cluster-2:6379 redis-cluster-3:6379 \
  redis-cluster-4:6379 redis-cluster-5:6379 redis-cluster-6:6379 \
  --cluster-replicas 1 --cluster-yes
```

health check는 단순 `PING`이 아니라 아래 조건을 확인한다.

- `cluster_state:ok`
- 16,384 slots가 모두 할당됨
- primary 3개와 replica 3개
- 모든 primary에 정상 연결된 replica가 한 개 이상 있음
- `cluster_slots_fail=0`, `cluster_slots_pfail=0`
- replica의 replication link가 up 상태

## 6. 실습 시나리오

각 실습 문서는 `목적 → 사전 조건 → 실행 명령 → 관찰 지점 → 기대 결과 →
운영 주의사항 → 복구` 순서를 사용한다.

### LAB 00. 환경과 baseline

실행할 내용:

- Docker Desktop/Engine의 CPU와 memory 제한 확인
- Redis image version, OS, config 출력
- `PING`, `ROLE`, `INFO server`, `INFO memory` 수집
- `redis-cli --latency`로 container network의 baseline 측정
- Compose container가 모두 한 물리 host에 있다는 한계 기록

확인할 점:

- 로컬 3 Sentinel/6 Cluster node는 프로세스 장애 실습이지, 실제 독립 AZ나
  물리 host 장애를 보장하는 구성이 아니다.
- 느린 Docker Desktop I/O 결과를 실제 production storage 성능으로 해석하지 않는다.

### LAB 10. 기본 복제와 replication offset

실행할 내용:

- primary에 연속 쓰기
- `INFO replication`, `ROLE`로 primary/replica offset 비교
- replica에서 쓰기를 시도해 `READONLY` 확인
- replica process를 잠시 멈춘 상태에서 primary에 쓰고 replica lag 관찰
- 재개 후 offset이 따라잡는 과정 관찰

코드 예제:

- `ReplicationOffsetExample`: 일정한 sequence를 가진 key를 기록하고 각 노드에서
  마지막 sequence와 replication offset을 수집한다.
- `ReplicaReadStalenessExample`: replica가 멈춘 동안 primary write 직후 replica
  read가 이전 값을 반환할 수 있음을 assertion한다.

운영 포인트:

- Redis replication은 비동기이므로 primary의 성공 응답이 replica 반영을
  의미하지 않는다.
- replica read는 부하 분산에 유용하지만 stale read, read-your-write 위반을
  애플리케이션이 허용하는 경우에만 사용한다.

### LAB 11. Full sync와 partial resynchronization

실행할 내용:

- 짧은 연결 단절 후 `PSYNC` partial resync 관찰
- 작은 replication backlog profile에서 긴 단절을 만들어 full sync 유도
- `client-output-buffer-limit replica`를 작게 설정하고 dataset을 키워
  full sync 도중 replica 연결이 끊기고 sync가 반복 실패하는 루프 재현
- `INFO stats`의 sync 관련 counter와 Redis log 비교
- full sync 중 primary latency와 network/memory 사용량 수집

운영 포인트:

- backlog가 너무 작으면 일시적인 단절도 전체 dataset 재전송으로 이어진다.
- replica output buffer limit이 dataset 대비 너무 작으면 full sync가
  `sync → buffer 초과로 연결 끊김 → 다시 full sync`로 무한 반복될 수 있다.
- 큰 dataset의 full sync와 fork/COW는 network, CPU, memory를 동시에 압박한다.
- cascading replica는 장애 반경과 복구 시간을 별도로 평가해야 한다.

### LAB 12. `WAIT`, `WAITAOF`, min replicas

실행할 내용:

- 동일 write 뒤 `WAIT 1 <timeout>` 유무에 따른 replica ack 확인
- 지원 버전에서 `WAITAOF`로 local/replica AOF 반영 수 관찰
- replica 2개를 **모두** 중지하고 `min-replicas-to-write=1`일 때 write 거부 확인
  (replica가 하나라도 남아 있으면 조건을 충족해 write가 거부되지 않는다.
  하나만 중지하는 실습을 원하면 `min-replicas-to-write 2`를 사용한다)
- 설정을 끈 경우 availability는 높아지지만 유실 window가 커짐을 비교

코드 예제:

- `WaitForReplicaExample`
- `WriteAvailabilityTradeoffExample`

운영 포인트:

- `WAIT`와 `WAITAOF`는 유실 가능성을 낮추지만 Redis를 strong consistency
  시스템으로 만들지는 않는다.
- timeout은 명령 실패/성공의 의미와 retry 정책까지 함께 설계해야 한다.

### LAB 20. Sentinel discovery와 자동 failover

실행할 내용:

- `SENTINEL master`, `replicas`, `sentinels`, `get-master-addr-by-name` 관찰
- primary container를 `SIGKILL`로 종료
- `SDOWN → ODOWN → replica promotion → 나머지 replica 재설정` 이벤트 기록
- 이전 primary를 재기동하고 replica로 편입되는지 확인
- failover 전후 endpoint와 replication offset 비교

코드 예제:

- `SentinelDiscoveryExample`: 특정 Redis 주소를 고정하지 않고 Sentinel에서
  현재 primary를 찾는다.
- `SentinelFailoverLoop`: sequence write를 계속하며 failover 시간, 실패 횟수,
  성공 응답 후 사라진 sequence, 중복 retry를 기록한다.

운영 포인트:

- Sentinel quorum은 `ODOWN` 판정 수이고, 실제 failover 승인에는 Sentinel
  과반수의 투표가 필요하다.
- robust deployment에는 최소 3 Sentinel과 독립적인 failure domain이 필요하다.
- 애플리케이션이 Sentinel-aware client를 쓰지 않으면 새 primary를 발견하지 못한다.

### LAB 21. Sentinel quorum과 network partition

실행할 내용:

- Sentinel 하나만 primary와 통신할 수 없게 만들어 `SDOWN`만 발생함을 확인
- 두 Sentinel의 관점을 분리해 quorum과 majority 차이 관찰
- minority Sentinel만 남았을 때 failover가 수행되지 않는지 확인
- Docker NAT/잘못 광고된 replica 주소 때문에 승격 후보를 찾지 못하는 상황 재현

주의:

- `docker network disconnect`를 사용하는 script는 정확한 Compose project와
  container ID를 검증하고, 종료 시 반드시 다시 연결한다.
- production의 partial partition은 로컬 Docker disconnect보다 복잡하므로
  이 실습 결과를 네트워크 설계의 완전한 검증으로 간주하지 않는다.

### LAB 30. Cluster slot과 redirect

실행할 내용:

- `CLUSTER INFO`, `CLUSTER NODES`, `CLUSTER SLOTS` 출력
- `CLUSTER KEYSLOT key`로 여러 key의 slot 계산
- cluster mode가 아닌 client로 잘못된 node에 요청해 `MOVED` 확인
- `redis-cli -c`와 cluster-aware Java client가 redirect를 처리하는 방식 비교
- replica read가 `READONLY` 명령으로 활성화됨을 확인
  (Lettuce에서는 `ReadFrom` 설정이 이 명령 전송을 담당한다)
- 일반 Pub/Sub은 Cluster 전 노드에 broadcast되고, `SSUBSCRIBE`/`SPUBLISH`
  sharded Pub/Sub은 slot 기반으로 특정 shard에만 전달됨을 비교
- Cluster에서는 DB 0만 사용하며 `SELECT`가 불가능함을 확인

코드 예제:

- `NaiveClusterClientExample`: 단일 endpoint client가 `MOVED`를 그대로 받는다.
- `ClusterAwareClientExample`: slot map을 가진 client가 올바른 primary로 요청한다.
- `TopologySnapshotExample`: client가 알고 있는 slot map과 서버 map을 출력한다.

운영 포인트:

- Cluster endpoint 목록 중 하나는 bootstrap 주소일 뿐이다. client가 전체
  topology와 주소에 도달할 수 있어야 한다.
- redirect 문자열을 애플리케이션이 직접 임시 parsing하지 말고 검증된
  cluster-aware client를 사용한다.

### LAB 31. Hash tag와 multi-key 명령

실행할 내용:

- `order:1`, `order:2`에 `MGET`/transaction/Lua를 실행해 `CROSSSLOT` 확인
- `order:{customer-1}:1`, `order:{customer-1}:2`가 같은 slot에 배치됨을 확인
- empty braces와 여러 braces 등 hash tag 경계 사례 확인
- 한 hash tag에 지나치게 많은 key를 모아 hot shard가 되는 예제 측정

코드 예제:

- `CrossSlotExample`
- `HashTagTransactionExample`

운영 포인트:

- multi-key command, transaction, Lua/function의 관련 key는 같은 slot이어야 한다.
- hash tag는 원자성을 가능하게 하지만 잘못 사용하면 분산을 무너뜨린다.
- key naming convention은 애플리케이션 설계 단계에서 정한다.

### LAB 40. Cluster failover와 client retry

실행할 내용:

- 한 primary를 중지하고 해당 shard replica의 promotion 관찰
- 영향받지 않은 shard와 영향받은 shard의 요청 성공률을 분리 측정
- primary를 복구해 replica로 합류하는지 확인
- 정상 maintenance용 `CLUSTER FAILOVER`와 장애 시 자동 failover 비교
- `FORCE`와 특히 `TAKEOVER`는 설명만 한 뒤 격리된 실습에서만 실행

코드 예제:

- `ClusterFailoverLoadGenerator`: key를 slot별로 분류해 latency/error를 기록한다.
- `UnsafeRetryExample`: timeout 뒤 무조건 재시도한 increment가 중복될 수 있음을 보인다.
- `IdempotentWriteExample`: request ID와 Lua script로 중복 실행을 방지한다.
- `TopologyRefreshComparisonExample`: Lettuce 기본값(periodic refresh 비활성)과
  periodic + adaptive refresh를 켠 설정의 failover 후 회복 시간을 비교한다.

운영 포인트:

- timeout은 서버가 명령을 실행하지 않았다는 증거가 아니다.
- failover retry는 횟수 제한, exponential backoff + jitter, 전체 deadline,
  idempotency를 함께 설계한다.
- Cluster도 async replication 기반이므로 acknowledged write가 failover에서
  유실될 가능성을 완전히 제거하지 못한다.

### LAB 41. Cluster majority와 slot coverage

실행할 내용:

- primary 하나와 replica가 동시에 unavailable일 때 해당 slot의 동작 관찰
- primary 과반수와 통신할 수 없는 partition에서 failover가 제한되는지 확인
- `cluster-require-full-coverage yes/no` profile의 availability 차이 비교
- `cluster_state`, `cluster_slots_fail`, client error를 함께 기록

운영 포인트:

- `cluster-require-full-coverage no`는 일부 shard 장애 중 다른 shard 요청을
  허용할 수 있지만, 애플리케이션이 partial availability를 올바르게 처리해야 한다.
- local 3-primary 구성에서 동시에 여러 노드를 중단하면 의도보다 cluster 전체가
  unavailable해질 수 있으므로 실습마다 중단 대상을 slot map으로 먼저 확인한다.

### LAB 50. Resharding과 노드 증설/축소

실행할 내용:

- 지속적인 read/write load 중 일부 slot reshard
- migration 중 `ASK`, 완료 후 `MOVED` 관찰
- cluster-aware client topology refresh 시간 측정
- 새 primary와 replica 추가, slot 할당, replica 관계 확인
- key가 남아 있는 node 제거가 거부되는지 확인 후 안전하게 drain/remove

코드 예제:

- `ReshardingObserver`: redirect 종류와 slot map 변경을 시간순으로 저장한다.

운영 포인트:

- reshard와 failover를 동시에 수행하지 않는다.
- 이동 전후 key count, slot coverage, application error rate를 검증한다.
- `redis-cli --cluster check` 결과만 보지 말고 client 관점의 성공률도 확인한다.

### LAB 60. Persistence, restart, backup/restore

profile:

- no persistence
- RDB only
- AOF `appendfsync everysec`
- AOF + RDB

실행할 내용:

- graceful shutdown과 `SIGKILL` 후 데이터 차이 비교
- volume 보존 restart와 volume 삭제의 차이 확인
- `LASTSAVE`, `BGSAVE`, `BGREWRITEAOF`, `INFO persistence` 관찰
- AOF rewrite 중 latency와 memory/COW 측정
- RDB를 외부 backup directory로 복사하고 새 instance에 restore
- 잘린 AOF는 원본을 복사한 뒤 `redis-check-aof`로 검사하는 절차 연습

운영 포인트:

- replication은 backup이 아니다. 잘못된 `DEL`/`FLUSHALL`도 복제된다.
- Cluster backup은 shard별 snapshot과 동일 시점/slot topology 정보가 필요하다.
- backup 파일 생성뿐 아니라 별도 위치 restore 성공까지 정기적으로 검증한다.
- RDB/AOF fork와 rewrite 중에는 평상시보다 큰 memory headroom이 필요하다.

### LAB 70. Memory, eviction, big key, latency

실행할 내용:

- 작은 `maxmemory`로 `noeviction`과 `allkeys-lru` 동작 비교
- application error와 eviction count 확인
- `MEMORY USAGE`, `MEMORY STATS`, `INFO memory` 비교
- 큰 collection에 O(N) 명령을 실행해 `SLOWLOG`와 latency spike 관찰
- `LATENCY LATEST`, `LATENCY DOCTOR`, `redis-cli --latency` 사용
- pipelining 전후 throughput과 tail latency 비교

운영 포인트:

- `used_memory`뿐 아니라 RSS, fragmentation, replication/client buffer,
  persistence COW까지 포함해 host memory를 계획한다.
- swap은 심각한 latency를 만들 수 있으며 production Linux에서는 THP와
  overcommit 설정을 함께 검토한다.
- `KEYS`, 큰 `DEL`, 큰 Lua script, 무제한 `LRANGE` 같은 명령은 작은 데이터로
  기능 테스트한 결과만 보고 production에서 안전하다고 판단하면 안 된다.
- cluster-wide `SCAN`, memory 조사, backup은 각 primary를 대상으로 수행해야 한다.

### LAB 71. 관측성과 alert 조건

1단계에서는 Redis 내장 명령만 사용한다.

- `INFO replication`, `INFO cluster`, `INFO persistence`
- `INFO memory`, `INFO stats`, `INFO commandstats`
- `SLOWLOG GET`, `LATENCY LATEST/DOCTOR`
- `ROLE`, `CLUSTER INFO/NODES`, Sentinel API

2단계에서 Prometheus + Redis exporter + Grafana profile을 추가한다.

최소 alert 후보:

- primary/replica role 변경
- replica link down, replication lag 증가
- connected replica 수가 요구치 미만
- `cluster_state != ok`, failed/pfail slot 발생
- rejected connection, error reply, eviction 증가
- memory 사용률과 RSS 증가
- latest fork time, RDB/AOF 실패, AOF rewrite 장기화
- p95/p99 client latency와 timeout 증가

각 alert에는 `왜 위험한가`, `즉시 확인할 명령`, `안전한 첫 대응`, `금지 행동`을
runbook으로 연결한다.

### LAB 80. ACL과 network security

실행할 내용:

- default user 비활성화와 application/operator 계정 분리
- key pattern과 command category 제한
- application 계정으로 `CONFIG`, `FLUSHALL`, `CLUSTER`가 거부되는지 확인
- replica와 Sentinel 인증 누락 시 발생하는 증상 확인
- credential rotation 절차 실습
- 선택적으로 TLS profile 추가

운영 포인트:

- Redis port와 cluster bus port를 public network에 노출하지 않는다.
- 비밀번호를 image, compose 파일, git에 직접 기록하지 않는다.
- `CONFIG SET`, `DEBUG`, `MODULE`, `FLUSHALL`, failover 계열 명령은 별도
  operator 권한으로 제한한다.
- `MONITOR`는 트래픽과 민감한 값을 노출하고 부하를 만들 수 있으므로 일반
  모니터링 수단으로 사용하지 않는다.

## 7. Java 클라이언트 예제 설계

### 7.1 공통 구조

MVP 단계에서는 아래 공통 프레임워크를 먼저 만들지 않는다. 각 예제를 단순한
`main` + 로그로 시작하고, 예제가 3~4개 쌓여 반복 패턴이 확인된 뒤에
interface와 공통 기능을 추출한다.

각 실행 예제는 공통 `LabScenario` interface를 구현한다.

```java
public interface LabScenario {
    String name();
    void arrange(LabContext context) throws Exception;
    LabResult run(LabContext context) throws Exception;
    void verify(LabResult result);
    void cleanup(LabContext context) throws Exception;
}
```

공통 기능:

- correlation/request ID
- command timeout과 전체 operation deadline 분리
- bounded retry와 backoff/jitter
- client-side latency histogram
- 성공/timeout/redirect/reconnect 횟수 기록
- key별 sequence와 checksum
- topology snapshot
- JSON 결과를 `artifacts/`에 저장

### 7.2 connection 종류

다음 connection factory를 별도로 둔다.

- direct primary connection
- direct replica connection
- Sentinel-discovered primary connection
- Redis Cluster connection
- Cluster replica-read connection

설정 이름과 타입을 분리해 실수로 standalone client를 Cluster에 연결하거나,
write path가 replica-read connection을 사용하는 것을 방지한다.

### 7.3 반드시 보여줄 client 함정

- startup node는 살아 있지만 redirect endpoint에 연결할 수 없는 경우
- topology refresh가 늦어 failover 후 이전 primary를 계속 보는 경우
  (Lettuce는 기본으로 periodic topology refresh가 꺼져 있으므로
  periodic refresh와 adaptive refresh trigger를 명시적으로 켜야 한다)
- replica read의 stale value
- connection pool이 stale connection을 보유하는 경우
- command timeout 후 retry가 중복 write를 만드는 경우
- unbounded retry가 장애를 증폭하는 경우
- hash tag가 없는 multi-key operation의 `CROSSSLOT`
- failover 동안 transaction/Lua script가 실패했을 때 결과를 알 수 없는 경우
- Pub/Sub connection 끊김과 재구독 사이의 message 유실
- Cluster에서 일반 Pub/Sub을 대규모로 사용해 cluster bus에 broadcast 부하를
  만드는 경우 (sharded Pub/Sub과의 차이)

## 8. 자동 검증 전략

### 8.1 Shell 검증

인프라 상태와 CLI 결과는 shell assertion으로 검증한다.

```bash
assert_equals "master" "$(role_of redis-primary)"
assert_replication_link_up redis-replica-1
assert_cluster_state_ok redis-cluster-1
assert_slot_coverage 16384
```

가능하면 문자열 전체 비교보다 RESP 출력을 안정적으로 parsing하고, timeout이
있는 polling을 사용한다. 고정된 `sleep 10`에만 의존하지 않는다.

### 8.2 JUnit integration test

- Compose topology가 이미 실행 중이라는 전제의 black-box test로 시작한다.
- test마다 key prefix와 hash tag를 격리한다.
- failover test는 resource lock을 걸어 병렬 실행하지 않는다.
- Awaitility 같은 bounded polling으로 eventual condition을 검사한다.
- 성공 기준에 failover 소요 시간과 허용 error 수를 명시한다.
- Testcontainers 기반 완전 자동 bootstrap은 Compose 실습이 안정화된 후 추가한다.

### 8.3 결과 보고서

각 실습은 다음 JSON과 Markdown summary를 생성한다.

```json
{
  "scenario": "sentinel-failover",
  "redisVersion": "8.8.1",
  "startedAt": "...",
  "failoverDurationMs": 0,
  "writesAttempted": 0,
  "writesAcknowledged": 0,
  "writesMissingAfterRecovery": 0,
  "timeouts": 0,
  "retries": 0,
  "duplicateEffects": 0
}
```

숫자를 hard-code된 정답으로 단정하기보다 실행 환경별 결과를 남기고, 반드시
성립해야 하는 불변식과 허용 범위를 test assertion으로 구분한다.

## 9. 실무 운영 체크리스트에 포함할 내용

### Topology와 장애 대응

- Cluster와 Sentinel 중 어떤 모델을 사용하는지 명확한가?
- replica가 primary와 같은 host/AZ에 몰려 있지 않은가?
- client가 모든 광고 주소에 연결할 수 있는가?
- failover timeout보다 application timeout/retry budget이 적절한가?
- planned failover와 unplanned failover 절차를 모두 검증했는가?
- 이전 primary가 돌아왔을 때 split-brain처럼 사용되지 않는가?

### 데이터 안전성

- 허용 가능한 RPO/RTO가 수치로 정의되어 있는가?
- async replication의 유실 window를 수용할 수 있는가?
- `WAIT`, min replicas, persistence 조합의 availability tradeoff를 정했는가?
- backup이 Redis node 밖에 있으며 restore test가 성공하는가?
- Cluster의 모든 shard가 backup 대상인가?

### 용량과 성능

- `maxmemory`와 eviction policy가 업무 특성에 맞는가?
- fork/COW, buffer, allocator fragmentation을 위한 headroom이 있는가?
- big key, hot key, hot slot을 탐지할 수 있는가?
- O(N) 명령과 Lua/function 실행 시간을 통제하는가?
- replica full sync가 network와 primary latency에 미치는 영향을 시험했는가?

### 클라이언트

- Sentinel/Cluster-aware client인가?
- 모든 timeout에 명시적 값이 있는가?
- retry가 bounded되고 idempotent한가?
- replica read의 consistency 요구사항이 문서화되어 있는가?
- topology refresh, reconnect, shutdown을 관찰할 metric이 있는가?

### 보안과 변경 관리

- network 접근 제한, ACL, secret 관리가 적용되어 있는가?
- application 계정에서 위험한 admin command를 실행할 수 없는가?
- config 변경이 file과 runtime에 일관되게 반영되는가?
- Redis/image/client upgrade 전에 backup, compatibility, rolling failover를
  staging에서 검증하는가?

## 10. 구현 단계와 완료 조건

### Phase 1. 프로젝트 골격과 공통 도구

작업:

- Gradle/Java project, Makefile, `.env.example`, 공통 shell library
- `make doctor`, 안전한 cleanup, artifact 수집
- Redis image/version 고정

완료 조건:

- 새 clone에서 README 명령만으로 doctor가 성공한다.
- 잘못된 Docker resource/port 환경에서 이해 가능한 오류를 출력한다.

### Phase 2. Replication + Sentinel

작업:

- Compose, writable Sentinel config, health check
- LAB 10~21의 CLI script
- Sentinel Java connection과 failover workload

완료 조건:

- 한 명령으로 기동되고 role/replica/Sentinel quorum이 검증된다.
- primary 강제 종료 후 자동 승격과 client 재연결을 자동 assertion한다.
- 실습 종료 후 원래 사용 가능한 topology로 복구된다.

### Phase 3. Redis Cluster 기본 구성

작업:

- 6-node Compose와 idempotent bootstrap
- slot, redirect, hash tag 예제
- Cluster-aware Java client

완료 조건:

- 16,384 slots, 3 primary, 3 replica를 자동 검증한다.
- `MOVED`, `CROSSSLOT`, hash tag 성공 경로를 재현한다.

### Phase 4. Cluster 장애와 변경 작업

작업:

- failover load generator
- replica read/staleness와 retry/idempotency 예제
- reshard, add/remove node runbook

완료 조건:

- 장애 shard와 정상 shard의 client 결과를 분리해 보고한다.
- reshard 중 slot coverage를 잃지 않고 client workload가 완료된다.

### Phase 5. Persistence와 운영 관측

작업:

- persistence profile과 backup/restore
- memory/eviction/latency 실습
- Prometheus/Grafana optional profile과 alert/runbook

완료 조건:

- 생성된 backup으로 빈 instance restore를 자동 검증한다.
- memory limit, eviction, slow command, latency event를 각각 재현한다.

### Phase 6. Security와 문서 마무리

작업:

- ACL/auth profile, optional TLS
- incident checklist와 위험 명령 정리
- Spring Data Redis adapter 예제 여부 결정
- CI smoke test

완료 조건:

- application 계정으로 admin command가 거부된다.
- 모든 lab 문서에 기대 결과와 복구 절차가 있다.
- CI에서는 최소 bootstrap/status/basic client test가 통과한다.

## 11. 첫 번째 구현 범위(MVP)

처음부터 모든 실습을 만들기보다 아래 8개를 MVP로 완성한다.

1. `make doctor`
2. primary 1 + replica 2 기동과 offset 실습
3. Sentinel 3개와 primary failover 실습
4. Cluster 3 primary + 3 replica bootstrap
5. `MOVED`와 cluster-aware client 비교
6. `CROSSSLOT`과 hash tag 예제
7. Cluster primary 장애와 client retry 관찰
8. AOF `everysec` restart와 backup/restore

MVP 이후 resharding, network partition, observability, memory/latency, ACL/TLS를
순서대로 추가한다. 이렇게 해야 장애 주입 도구나 dashboard 구성보다 먼저
Redis의 핵심 동작과 client correctness를 검증할 수 있다.

## 12. 공식 참고 자료

구현 중 설정 의미와 기대 동작은 블로그 예제보다 아래 공식 문서를 우선한다.

- [Redis replication](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/)
- [High availability with Redis Sentinel](https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/)
- [Scale with Redis Cluster](https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/)
- [Redis Cluster specification](https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/)
- [Redis persistence](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/)
- [Redis latency monitoring](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency-monitor/)
- [Diagnosing latency issues](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/)
- [Memory optimization](https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/memory-optimization/)
- [Redis security](https://redis.io/docs/latest/operate/oss_and_stack/management/security/)
- [Redis Docker Official Image](https://hub.docker.com/_/redis)
