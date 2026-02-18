package io.jungbum.nioserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class SocketProcessorTest {

    private ServerSocketChannel serverChannel;
    private Queue<Socket> inboundSocketQueue;
    private MessageBuffer readMessageBuffer;
    private MessageBuffer writeMessageBuffer;
    private SocketProcessor processor;

    // 테스트용 메시지 리더: read 호출 시 데이터를 읽고 메시지를 생성
    private List<Message> testMessages;
    private boolean testReaderReadCalled;

    // 테스트용 메시지 프로세서: process 호출을 기록
    private List<Message> processedMessages;
    private List<WriteProxy> processedWriteProxies;

    // 연결된 클라이언트 채널 목록 (tearDown에서 정리용)
    private List<SocketChannel> clientChannels = new ArrayList<>();
    private List<SocketChannel> serverSideChannels = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        serverChannel = ServerSocketChannel.open().bind(new InetSocketAddress(0));

        inboundSocketQueue = new LinkedList<>();
        readMessageBuffer = new MessageBuffer();
        writeMessageBuffer = new MessageBuffer();

        testMessages = new ArrayList<>();
        testReaderReadCalled = false;
        processedMessages = new ArrayList<>();
        processedWriteProxies = new ArrayList<>();

        IMessageReaderFactory readerFactory = () -> new IMessageReader() {
            private MessageBuffer buffer;
            private final List<Message> messages = testMessages;

            @Override
            public void init(MessageBuffer readMessageBuffer) {
                this.buffer = readMessageBuffer;
            }

            @Override
            public void read(Socket socket, ByteBuffer byteBuffer) throws IOException {
                testReaderReadCalled = true;
                socket.read(byteBuffer);
                byteBuffer.clear();
            }

            @Override
            public List<Message> getMessages() {
                return messages;
            }
        };

        IMessageProcessor messageProcessor = (message, writeProxy) -> {
            processedMessages.add(message);
            processedWriteProxies.add(writeProxy);
        };

        processor = new SocketProcessor(
                inboundSocketQueue,
                readMessageBuffer,
                writeMessageBuffer,
                readerFactory,
                messageProcessor
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        for (SocketChannel ch : clientChannels) {
            if (ch.isOpen()) ch.close();
        }
        for (SocketChannel ch : serverSideChannels) {
            if (ch.isOpen()) ch.close();
        }
        serverChannel.close();
    }

    /**
     * 클라이언트-서버 소켓 쌍을 생성하고 서버 측 SocketChannel을 반환
     */
    private SocketChannel createConnectedChannel() throws IOException {
        int port = serverChannel.socket().getLocalPort();
        SocketChannel clientChannel = SocketChannel.open(new InetSocketAddress("localhost", port));
        SocketChannel serverSideChannel = serverChannel.accept();
        clientChannels.add(clientChannel);
        serverSideChannels.add(serverSideChannel);
        return serverSideChannel;
    }

    @Test
    @DisplayName("takeNewSockets: 큐에 있는 소켓을 꺼내 socketId를 할당하고 non-blocking으로 설정한다")
    void testTakeNewSockets() throws IOException {
        SocketChannel channel = createConnectedChannel();
        Socket socket = new Socket(channel);
        inboundSocketQueue.add(socket);

        processor.takeNewSockets();

        // socketId가 할당됨 (시작값: 16 * 1024 = 16384)
        assertEquals(16384, socket.socketId);
        // non-blocking으로 설정됨
        assertFalse(socket.socketChannel.isBlocking());
        // messageReader가 설정됨
        assertNotNull(socket.messageReader);
        // messageWriter가 설정됨
        assertNotNull(socket.messageWriter);
    }

    @Test
    @DisplayName("takeNewSockets: 여러 소켓에 증가하는 socketId를 할당한다")
    void testTakeNewSocketsIncrementsId() throws IOException {
        SocketChannel channel1 = createConnectedChannel();
        SocketChannel channel2 = createConnectedChannel();
        Socket socket1 = new Socket(channel1);
        Socket socket2 = new Socket(channel2);

        inboundSocketQueue.add(socket1);
        inboundSocketQueue.add(socket2);

        processor.takeNewSockets();

        assertEquals(16384, socket1.socketId);
        assertEquals(16385, socket2.socketId);
    }

    @Test
    @DisplayName("takeNewSockets: 큐가 비어있으면 아무것도 하지 않는다")
    void testTakeNewSocketsEmptyQueue() throws IOException {
        // 예외 없이 정상 실행되어야 함
        processor.takeNewSockets();
    }

    @Test
    @DisplayName("readFromSockets: 등록된 소켓에서 데이터를 읽으면 messageReader.read가 호출된다")
    void testReadFromSocketsCallsMessageReader() throws IOException, InterruptedException {
        SocketChannel serverSide = createConnectedChannel();
        Socket socket = new Socket(serverSide);
        inboundSocketQueue.add(socket);
        processor.takeNewSockets();

        // 클라이언트에서 데이터 전송
        SocketChannel clientChannel = this.clientChannels.get(0);
        clientChannel.write(ByteBuffer.wrap("Hello".getBytes()));

        // Selector가 데이터를 감지할 시간을 주기 위해 잠시 대기
        Thread.sleep(100);

        // read
        processor.readFromSockets();

        assertTrue(testReaderReadCalled, "messageReader.read()가 호출되어야 한다");
    }

    @Test
    @DisplayName("readFromSockets: 읽은 메시지가 있으면 messageProcessor.process가 호출된다")
    void testReadFromSocketsProcessesMessages() throws IOException, InterruptedException {
        SocketChannel serverSide = createConnectedChannel();
        Socket socket = new Socket(serverSide);
        inboundSocketQueue.add(socket);
        processor.takeNewSockets();

        // 테스트용 메시지를 미리 추가 (messageReader.getMessages()가 반환할 메시지)
        Message testMessage = readMessageBuffer.getMessage();
        testMessage.writeToMessage("test data".getBytes());
        testMessages.add(testMessage);

        // 클라이언트에서 데이터 전송 - Selector(readSelector.selectNow) 가 채널 감지하도록 호출하는 용도로 write 해줌
        SocketChannel clientChannel = this.clientChannels.get(0);
        clientChannel.write(ByteBuffer.wrap("Hello".getBytes()));

        // Selector가 데이터를 감지할 시간을 주기 위해 잠시 대기
        Thread.sleep(100);

        processor.readFromSockets();

        assertEquals(1, processedMessages.size(), "messageProcessor.process가 1번 호출되어야 한다");
        assertEquals(socket.socketId, processedMessages.get(0).socketId,
                "메시지에 소켓 ID가 설정되어야 한다");
    }

    @Test
    @DisplayName("readFromSockets: endOfStreamReached이면 소켓이 닫힌다")
    void testReadFromSocketsClosesOnEndOfStream() throws IOException {
        // endOfStreamReached를 트리거하기 위해, 클라이언트 연결을 먼저 닫는다
        SocketChannel serverSide = createConnectedChannel();
        Socket socket = new Socket(serverSide);
        inboundSocketQueue.add(socket);
        processor.takeNewSockets();

        // 클라이언트 소켓을 닫아 EOF를 발생시킨다
        clientChannels.get(0).close();

        // readFromSockets 호출 시 EOF를 감지하고 소켓을 정리해야 한다
        processor.readFromSockets();

        // EOF 시 채널이 닫혀야 한다
        assertFalse(serverSide.isOpen(), "EOF 시 서버 측 채널이 닫혀야 한다");
    }

    @Test
    @DisplayName("executeCycle: takeNewSockets, readFromSockets, writeToSockets를 순서대로 실행한다")
    void testExecuteCycle() throws IOException {
        SocketChannel serverSide = createConnectedChannel();
        Socket socket = new Socket(serverSide);
        inboundSocketQueue.add(socket);

        // executeCycle은 세 단계를 모두 실행
        processor.executeCycle();

        // takeNewSockets가 실행되어 socketId가 할당됨
        assertEquals(16384, socket.socketId);
        assertNotNull(socket.messageReader);
        assertNotNull(socket.messageWriter);
    }

    @Test
    @DisplayName("executeCycle: 여러 사이클 실행 시 새 소켓이 계속 처리된다")
    void testMultipleExecuteCycles() throws IOException {
        // 첫 번째 사이클: 첫 번째 소켓
        SocketChannel serverSide1 = createConnectedChannel();
        Socket socket1 = new Socket(serverSide1);
        inboundSocketQueue.add(socket1);
        processor.executeCycle();

        // 두 번째 사이클: 두 번째 소켓
        SocketChannel serverSide2 = createConnectedChannel();
        Socket socket2 = new Socket(serverSide2);
        inboundSocketQueue.add(socket2);
        processor.executeCycle();

        assertEquals(16384, socket1.socketId);
        assertEquals(16385, socket2.socketId);
    }

    @Test
    @DisplayName("writeToSockets: 읽기 데이터가 없을 때 예외 없이 실행된다")
    void testWriteToSocketsNoData() throws IOException {
        // 아무 소켓도 등록하지 않은 상태에서 writeToSockets 호출
        processor.writeToSockets();
        // 예외 없이 정상 실행되어야 함
    }
}
