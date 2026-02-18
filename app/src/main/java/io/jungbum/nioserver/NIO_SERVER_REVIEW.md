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
