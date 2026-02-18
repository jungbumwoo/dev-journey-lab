# NIO Non-Blocking Server 리뷰

review by claude-opus-4-6

## 전체 아키텍처

이 프로젝트는 Jakob Jenkov의 Java NIO Server 패턴을 기반으로 한 **Non-Blocking I/O 서버**이다. 핵심 설계는 **2개의 스레드**로 수천 개의 동시 연결을 처리하는 것이다.

```
┌─────────────────────────────────────────────────────────────────┐
│                         Server.start()                          │
│                                                                 │
│   Thread 1 (Accepter)          Thread 2 (Processor)             │
│   ┌──────────────────┐         ┌──────────────────────────┐     │
│   │  SocketAccepter   │         │    SocketProcessor        │     │
│   │                  │  Queue   │                          │     │
│   │  accept()        ├────────►│  takeNewSockets()        │     │
│   │  (blocking)      │ Socket  │  readFromSockets()       │     │
│   │                  │         │  writeToSockets()        │     │
│   └──────────────────┘         └──────────────────────────┘     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 스레드 모델: 왜 2개만으로 충분한가

### 전통적인 Blocking I/O (Thread-per-Connection)

```
Client 1 ──► Thread 1 (read에서 블로킹... 대기... 대기...)
Client 2 ──► Thread 2 (read에서 블로킹... 대기... 대기...)
Client 3 ──► Thread 3 (read에서 블로킹... 대기... 대기...)
  ...1000개 연결 = 1000개 스레드 = 메모리 폭발
```

### 이 프로젝트의 Non-Blocking I/O

```
Client 1 ─┐
Client 2 ─┼──► Selector ──► Thread 1 (Processor)
Client 3 ─┘    "누가 준비됐나?"   → 준비된 것만 처리
  ...1000개 연결 = 2개 스레드
```

### Thread 1 - SocketAccepter

`SocketAccepter.java:32`

- `serverSocket.accept()`는 **blocking** 호출 — 새 연결이 올 때까지 이 스레드는 대기
- 연결이 오면 `Socket` 객체를 만들어 `ArrayBlockingQueue`에 넣음
- 이 스레드는 **연결 수락만** 담당하고 데이터 I/O는 하지 않음

### Thread 2 - SocketProcessor

`SocketProcessor.java:57-71`

- `while(true)` 루프에서 100ms마다 3단계 사이클 반복:

```java
executeCycle() {
    takeNewSockets();    // 1. 큐에서 새 소켓 꺼내기
    readFromSockets();   // 2. Selector로 읽기 가능한 소켓에서 읽기
    writeToSockets();    // 3. Selector로 쓰기 가능한 소켓에 쓰기
}
```

### 두 스레드 간 통신

`ArrayBlockingQueue<Socket>` (thread-safe)

```
Accepter Thread                    Processor Thread
     │                                   │
     │  socketQueue.add(socket)          │
     ├──────────────────────────────────►│  socketQueue.poll()
     │         (thread-safe)             │
```

---

## NIO 핵심 원리: Selector

Selector는 이 서버의 핵심이다. **여러 채널 중 I/O 준비된 것만 골라주는 멀티플렉서**이다.

```
                    readSelector
                   ┌───────────┐
  Socket A (대기중) │           │
  Socket B (읽기OK)│  selectNow│──► "B, D가 읽기 가능"
  Socket C (대기중) │           │
  Socket D (읽기OK)│           │
                   └───────────┘
```

### SocketProcessor는 Selector를 2개 사용

| Selector | 용도 | 등록 시점 |
|---|---|---|
| `readSelector` | OP_READ - 읽을 데이터가 있는 소켓 감지 | `takeNewSockets()`에서 등록 |
| `writeSelector` | OP_WRITE - 쓸 수 있는 소켓 감지 | `registerNonEmptySockets()`에서 등록 |

### 왜 Selector를 읽기/쓰기로 분리했는가?

쓰기 가능한 소켓은 대부분의 시간 동안 항상 writable 상태이다 (커널 송신 버퍼가 비어있으므로). 하나의 Selector에 OP_READ|OP_WRITE를 같이 등록하면, **보낼 데이터가 없는데도 매번 writable로 감지**되어 불필요한 CPU 소모가 발생한다. 분리하면 보낼 데이터가 있을 때만 writeSelector에 등록/해제할 수 있다.

---

## 데이터 흐름 상세

### 읽기 흐름 (클라이언트 → 서버)

```
1. takeNewSockets()
   socketQueue.poll() → socket
   socket.socketChannel.configureBlocking(false)   ← Non-blocking 설정
   socket.socketChannel.register(readSelector, OP_READ)  ← Selector에 등록
   key.attach(socket)                              ← SelectionKey에 Socket 부착

2. readFromSockets()
   readSelector.selectNow()          ← 블로킹 없이 즉시 확인
   for (ready keys):
     socket = key.attachment()       ← 부착된 Socket 꺼내기
     socket.messageReader.read(socket, readByteBuffer)
     │
     └─► Socket.read() → socketChannel.read(byteBuffer)
         └─► non-blocking: 데이터 있으면 읽고, 없으면 즉시 0 반환

     messages = messageReader.getMessages()
     for (message):
       messageProcessor.process(message, writeProxy)  ← 비즈니스 로직 호출
```

### 쓰기 흐름 (서버 → 클라이언트)

```
3. writeToSockets()

   3-1. takeNewOutboundMessages()
        outboundMessageQueue.poll() → message
        socket = socketMap.get(message.socketId)
        socket.messageWriter.enqueue(message)
        emptyToNonEmptySockets.add(socket)    ← "이 소켓은 쓸 게 생겼다"

   3-2. cancelEmptySockets()
        for (nonEmptyToEmptySockets):
          key.cancel()                        ← 다 보낸 소켓은 Selector에서 해제

   3-3. registerNonEmptySockets()
        for (emptyToNonEmptySockets):
          socket.register(writeSelector, OP_WRITE)  ← 보낼 게 있는 소켓만 등록

   3-4. writeSelector.selectNow()
        for (ready keys):
          socket.messageWriter.write(socket, writeByteBuffer)
          if (writer.isEmpty()):
            nonEmptyToEmptySockets.add(socket) ← "다 보냈으니 다음 사이클에서 해제"
```

쓰기의 소켓 상태 전환:

```
보낼 데이터 없음 ──(enqueue)──► emptyToNonEmptySockets ──(register)──► writeSelector에 등록
                                                                            │
보낼 데이터 없음 ◄──(cancel)── nonEmptyToEmptySockets ◄──(다 보냄)─────────┘
```

---

## 메모리 관리: MessageBuffer 풀링

이 서버는 **메시지마다 새로운 byte[]를 할당하지 않고**, 미리 할당된 거대한 byte 배열을 분할하여 사용한다.

```
smallMessageBuffer (4MB)
┌──────┬──────┬──────┬──────┬─────────┐
│ 4KB  │ 4KB  │ 4KB  │ 4KB  │ ...x1024│
└──────┴──────┴──────┴──────┴─────────┘
  ▲                    ▲
  msg1                 msg2     ← 같은 배열 내 다른 영역

mediumMessageBuffer (16MB): 128KB x 128개
largeMessageBuffer  (16MB): 1MB x 16개
```

메시지가 4KB를 초과하면 `expandMessage()`로 128KB 블록으로 승격, 그래도 부족하면 1MB로 승격한다. `QueueIntFlip`은 사용 가능한 블록의 시작 offset을 관리하는 **lock-free 원형 큐**이다.

**장점**: GC 압력 최소화. 수천 개의 동시 메시지를 처리해도 heap allocation이 거의 발생하지 않는다.

---

## Partial Read/Write 처리

Non-blocking I/O의 핵심 과제는 **데이터가 한 번에 다 오지 않는다**는 것이다.

### Partial Read (`HttpMessageReader.java`)

```
첫 번째 read: "GET /test HTT"        ← 불완전한 HTTP 요청
  → parseHttpRequest → \r\n\r\n 없음 → 대기
  → nextMessage에 13바이트 축적

두 번째 read: "P/1.1\r\n\r\n"         ← 나머지 도착
  → nextMessage에 이어붙임: "GET /test HTTP/1.1\r\n\r\n"
  → parseHttpRequest → 완성! → completeMessages에 추가
```

### Partial Write (`MessageWriter.java`)

```
전송할 메시지: 11바이트 "Hello World"

첫 번째 write: 커널 버퍼에 5바이트만 들어감
  → bytesWritten = 5, isEmpty() = false

두 번째 write: 나머지 6바이트 전송
  → bytesWritten = 11, isEmpty() = true → 다음 메시지로 이동
```

`MessageWriter`가 `bytesWritten` 오프셋을 유지하면서, 다음 `write()` 호출 때 이어서 전송한다.

---

## 개선 가능한 포인트

### 1. SocketAccepter의 blocking accept

현재 `accept()`가 blocking이라 별도 스레드가 필요하다. `ServerSocketChannel`을 non-blocking으로 설정하고 Selector에 `OP_ACCEPT`로 등록하면 Processor 스레드 하나로 통합할 수 있다.

### 2. 100ms 고정 sleep

`SocketProcessor.java:66`

부하가 낮을 때는 100ms마다 불필요하게 깨어나고, 부하가 높을 때는 100ms 지연이 생긴다. `selectNow()` 대신 `select(timeout)`을 사용하면 이벤트가 있을 때 즉시 깨어나면서도 CPU를 절약할 수 있다.

### 3. 단일 Processor 스레드 병목

현재 모든 읽기/쓰기/비즈니스 로직이 하나의 스레드에서 실행된다. `messageProcessor.process()`가 무거운 작업이면 다른 소켓의 I/O도 지연된다. 비즈니스 로직을 별도 스레드 풀로 분리하는 것을 고려할 수 있다.

### 4. 스레드 안전성

`outboundMessageQueue`는 `LinkedList`인데 (`SocketProcessor.java:19`), `messageProcessor.process()`에서 `writeProxy.enqueue()`를 통해 메시지를 넣고, 같은 Processor 스레드의 `takeNewOutboundMessages()`에서 꺼낸다. 현재는 단일 스레드라 문제없지만, 비즈니스 로직을 별도 스레드로 분리하면 thread-safe 큐로 교체해야 한다.

---

Q.  Selector는 어떤 스레드가 실행하나요? Selector는 어떤 스레드가 실행하고, read나 write가 필요하여 blocking이 발생하는 부분은 어떤 스레드가 실행하나요? os 커널 스레드일까요?

## Selector와 스레드, 커널의 관계

### Selector를 실행하는 스레드

**Processor 스레드(Thread 2)가 직접 실행한다.** 별도의 스레드가 아니다.

```
Processor Thread (Thread 2)
    │
    ├─ readSelector.selectNow()    ← 이 스레드가 직접 호출
    ├─ socket.read(byteBuffer)     ← 이 스레드가 직접 호출
    ├─ writeSelector.selectNow()   ← 이 스레드가 직접 호출
    └─ socket.write(byteBuffer)    ← 이 스레드가 직접 호출
```

### selectNow() 내부에서 일어나는 일

`selectNow()`는 **시스템 콜**이다. 별도의 커널 스레드가 감시하는 것이 아니다.

```
┌─ User Space ──────────────────────────────────────┐
│                                                    │
│  Processor Thread                                  │
│      │                                             │
│      ▼                                             │
│  readSelector.selectNow()                          │
│      │                                             │
│      ▼                                             │
│  SelectorImpl → native method 호출                  │
│      │                                             │
├──────┼─────────── system call boundary ────────────┤
│      ▼                                             │
│  ┌─ Kernel Space ───────────────────────────┐      │
│  │                                          │      │
│  │  macOS: kqueue(kq, changelist, ...)      │      │
│  │  Linux: epoll_wait(epfd, events, ...)    │      │
│  │                                          │      │
│  │  커널이 내부 자료구조를 확인:                │      │
│  │  "fd 5: 수신 버퍼에 데이터 있음 → READY"    │      │
│  │  "fd 7: 수신 버퍼 비어있음 → NOT READY"     │      │
│  │                                          │      │
│  │  → ready 목록을 반환                      │      │
│  └──────────────────────────────────────────┘      │
│      │                                             │
│      ▼                                             │
│  Processor Thread 계속 실행                          │
│  "fd 5가 ready구나 → 읽기 시작"                      │
│                                                    │
└────────────────────────────────────────────────────┘
```

**핵심: 같은 Processor 스레드가 user space → kernel space로 모드 전환(context switch)했다가 돌아오는 것이지, 커널이 별도 스레드를 만들어 감시하는 것이 아니다.**

### 커널은 소켓 상태를 언제 알게 되는가?

커널이 소켓의 ready 상태를 아는 것은 **하드웨어 인터럽트** 때문이다.

```
1. 네트워크 패킷 도착
       │
       ▼
2. NIC(네트워크 카드)가 하드웨어 인터럽트 발생
       │
       ▼
3. 커널의 인터럽트 핸들러 실행 (현재 실행 중인 스레드를 잠시 중단)
       │
       ▼
4. 커널이 해당 소켓의 수신 버퍼에 데이터 복사
       │
       ▼
5. epoll/kqueue의 ready list 업데이트
       │
   (여기까지는 Processor 스레드와 무관하게 일어남)
       │
       ▼
6. 나중에 Processor 스레드가 selectNow() 호출
       │
       ▼
7. 커널이 ready list를 확인해서 즉시 반환
```

인터럽트 핸들러는 스레드가 아니다. CPU가 현재 하던 일을 잠시 중단하고 커널 코드를 실행하는 것이다.

### selectNow() vs select()의 차이

이 프로젝트에서 사용하는 방식:

```java
// SocketProcessor.java
while (true) {
    executeCycle();          // selectNow() 사용 → 즉시 반환
    Thread.sleep(100);       // 100ms 대기
}
```

| 메서드 | 동작 | Processor 스레드 상태 |
|---|---|---|
| `selectNow()` | ready 소켓이 없어도 **즉시 0 반환** | 계속 실행 (CPU 사용) |
| `select()` | ready 소켓이 생길 때까지 **스레드 sleep** | 커널이 재움, 이벤트 시 깨움 |
| `select(100)` | 최대 100ms 대기, 이벤트 있으면 즉시 반환 | 대기 중 CPU 미사용 |

현재 프로젝트는 `selectNow()` + `Thread.sleep(100)` 조합인데, `select(100)`으로 바꾸면 동일한 효과를 하나의 호출로 얻을 수 있다. `select(100)` 중에 스레드가 sleep 상태일 때, 데이터가 도착하면 커널이 인터럽트로 ready list를 갱신하고, sleep 중인 스레드를 **즉시 깨운다.** 100ms를 다 기다릴 필요가 없다.

### read()/write()도 blocking이 아닌가?

이 프로젝트에서는 아니다. `takeNewSockets()`에서:

```java
newSocket.socketChannel.configureBlocking(false);  // ← 핵심
```

이 설정 이후 모든 I/O 호출은 non-blocking 시스템 콜이 된다:

```
socketChannel.read(buffer)
    │
    ▼
커널: "수신 버퍼에 데이터 있나?"
    ├─ 있음 → 데이터 복사, 읽은 바이트 수 반환 (즉시)
    └─ 없음 → 0 반환 (즉시, 기다리지 않음)

socketChannel.write(buffer)
    │
    ▼
커널: "송신 버퍼에 공간 있나?"
    ├─ 있음 → 데이터 복사, 쓴 바이트 수 반환 (즉시)
    └─ 없음 → 0 반환 (즉시, 기다리지 않음)
```

**모든 I/O가 즉시 반환되기 때문에 Processor 스레드 하나로 수천 개의 소켓을 돌아가며 처리할 수 있는 것이다.**

### 정리

```
별도 커널 스레드가 감시?          → 아니오
Selector에 별도 스레드?          → 아니오
Processor 스레드가 직접 시스템 콜? → 예
커널이 소켓 상태를 아는 방법?      → 하드웨어 인터럽트
read/write가 블로킹?            → 아니오 (configureBlocking(false))
```

Selector의 본질은 "커널아, 이 소켓들 중에 지금 당장 ready인 게 뭐야?"라고 **Processor 스레드가 커널에게 묻는 것**이고, 커널은 인터럽트를 통해 이미 알고 있는 상태를 즉시 알려주는 것이다.

---

읽기: 네트워크 → 애플리케이션

                          누가 하나?                                                                                                                                                                                                                                                                                     
                          ─────────                                                                                                                                                                                                                                                                                      
1. 네트워크 패킷 도착
   │
   ▼
2. NIC가 커널 수신 버퍼에 복사    ← NIC 하드웨어 (DMA). 스레드 아님, CPU도 안 씀
   │
   ▼
   ┌──────────────────────┐
   │  Kernel recv buffer   │    ← 소켓당 하나, 커널이 관리
   │  "GET /test HTTP..."  │
   └──────────┬───────────┘
   │
   │  socketChannel.read(byteBuffer) ← Processor 스레드가 시스템 콜
   │
   │  커널 모드에서 memcpy 수행       ← 같은 Processor 스레드가 실행
   │
   ▼
   ┌──────────────────────┐
   │  User-space           │
   │  ByteBuffer (1MB)     │    ← SocketProcessor.readByteBuffer
   │  "GET /test HTTP..."  │
   └──────────┬───────────┘
   │
   │  message.writeToMessage(byteBuffer) ← Processor 스레드가 Java 코드로
   ▼
   ┌──────────────────────┐
   │  Message              │
   │  (MessageBuffer 내)    │    ← 공유 byte[] 배열의 한 구간
   └──────────────────────┘


쓰기: 애플리케이션 → 네트워크

     ┌──────────────────────┐
     │  Message              │
     │  "HTTP/1.1 200 OK..." │
     └──────────┬───────────┘
                │
                │  byteBuffer.put(message.sharedArray, ...) ← Processor 스레드가 Java 코드로
                ▼
     ┌──────────────────────┐
     │  User-space           │
     │  ByteBuffer (1MB)     │    ← SocketProcessor.writeByteBuffer
     │  "HTTP/1.1 200 OK..." │
     └──────────┬───────────┘
                │
                │  socketChannel.write(byteBuffer)  ← Processor 스레드가 시스템 콜
                │
                │  커널 모드에서 memcpy 수행          ← 같은 Processor 스레드가 실행
                │
                ▼
     ┌──────────────────────┐
     │  Kernel send buffer   │    ← 소켓당 하나, 커널이 관리
     │  "HTTP/1.1 200 OK..." │
     └──────────┬───────────┘
                │
                ▼
     NIC가 네트워크로 전송           ← NIC 하드웨어 (DMA). 스레드 아님, CPU도 안 씀


핵심: socketChannel.read(byteBuffer)가 하는 일

이 한 줄의 호출 안에서 일어나는 일을 풀어보면:

// Socket.java:22
int bytesRead = this.socketChannel.read(byteBuffer);

Processor Thread
│
│ [User Mode - Java]
│  socketChannel.read(byteBuffer)
│      → sun.nio.ch.SocketChannelImpl.read()
│          → native method 호출
│
│ ──── syscall boundary (mode switch) ────
│
│ [Kernel Mode - 같은 스레드]
│  sys_read(fd, buf, count)
│      → 커널이 recv buffer 확인
│      │
│      ├─ 데이터 있음:
│      │    memcpy(user_buf, kernel_recv_buf, n)  ← 이 복사도 Processor 스레드가 함
│      │    return n (읽은 바이트 수)
│      │
│      └─ 데이터 없음 (non-blocking이므로):
│           return EAGAIN → Java에서 0 반환
│
│ ──── syscall return (mode switch) ────
│
│ [User Mode - Java]
│  bytesRead = 결과값


커널 recv buffer → User ByteBuffer 복사를 수행하는 것은 Processor 스레드 자신입니다. 커널 모드로 전환된 상태에서 같은 스레드가 memcpy를 실행합니다. 커널이 별도 스레드를 만들어 복사해주는 것이 아닙니다.

정리: 각 구간별 누가 처리하나

┌────────────────────────────────────┬──────────────────────────────┬──────────────────┐
│                구간                │             누가             │     스레드?      │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ 네트워크 → 커널 recv buffer        │ NIC 하드웨어 (DMA)           │ 스레드 아님      │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ 커널 recv buffer → User ByteBuffer │ Processor 스레드 (커널 모드) │ Processor 스레드 │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ User ByteBuffer → Message          │ Processor 스레드 (유저 모드) │ Processor 스레드 │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ Message → User ByteBuffer          │ Processor 스레드 (유저 모드) │ Processor 스레드 │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ User ByteBuffer → 커널 send buffer │ Processor 스레드 (커널 모드) │ Processor 스레드 │
├────────────────────────────────────┼──────────────────────────────┼──────────────────┤
│ 커널 send buffer → 네트워크        │ NIC 하드웨어 (DMA)           │ 스레드 아님      │
└────────────────────────────────────┴──────────────────────────────┴──────────────────┘

중간의 4단계 복사를 전부 Processor 스레드 하나가 직접 수행합니다. non-blocking이기 때문에 커널 버퍼에 데이터가 없거나 공간이 없으면 기다리지 않고 즉시 반환하므로, 한 스레드가 수천 개의 소켓을 돌아가며 처리할 수 있는 것입니다.
