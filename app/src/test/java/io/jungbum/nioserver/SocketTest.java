package io.jungbum.nioserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SocketTest {

    private SocketChannel mockSocketChannel;
    private Socket socket;

    private ServerSocketChannel serverChannel;
    private SocketChannel clientChannel;
    private SocketChannel serverSideSocket;

    @BeforeEach
    void setup() throws IOException {
        serverChannel = ServerSocketChannel.open().bind(new InetSocketAddress(0)); // 빈 포트 할당
        int port = serverChannel.socket().getLocalPort();
        clientChannel = SocketChannel.open(new InetSocketAddress("localhost", port));
        serverSideSocket = serverChannel.accept();
        serverSideSocket.configureBlocking(false);

        // mock
        mockSocketChannel = Mockito.mock(SocketChannel.class);
        mockSocketChannel.configureBlocking(false);
        socket = new Socket(mockSocketChannel);
    }

    @AfterEach
    void tearDown() throws IOException {
        serverChannel.close();
        clientChannel.close();
        serverSideSocket.close();
    }

    @Test
    public void testRead() throws IOException {
        // Arrange
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        when(mockSocketChannel.read(any(ByteBuffer.class)))
                .thenAnswer(invocation -> {
                    ByteBuffer buffer = invocation.getArgument(0);
                    buffer.put("TestData".getBytes());
                    return 8; // Number of bytes read
                })
                .thenAnswer(invocation -> -1); // Simulate end of stream

        // Act
        int totalBytesRead = socket.read(byteBuffer);

        // Assert
        assertEquals(8, totalBytesRead);
        verify(mockSocketChannel, times(2)).read(any(ByteBuffer.class));
    }

    @Test
    @DisplayName("데이터를 쓸 때 쓴 바이트 수를 반환한다")
    public void testWrite() throws IOException {
        // Arrange
        ByteBuffer byteBuffer = ByteBuffer.wrap("TestData".getBytes());

        // Mock이 호출될 때 실제로 버퍼의 position을 끝으로 옮기도록 설정
        when(mockSocketChannel.write(byteBuffer)).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            int remaining = buffer.remaining();
            buffer.position(buffer.limit()); // 데이터를 모두 쓴 것처럼 position 이동
            return remaining; // 쓴 바이트 수 반환 (8)
        });

        // Act
        int totalBytesWritten = socket.write(byteBuffer);

        // Assert
        assertEquals(8, totalBytesWritten);
        verify(mockSocketChannel, times(1)).write(byteBuffer);
    }

    @Test
    @DisplayName("실제 네트워크 연결을 통해 데이터를 읽고 쓴다")
    void testRealNetworkRead() throws IOException {
        Socket socket = new Socket(serverSideSocket);
        String message = "Real Data";
        ByteBuffer writeBuf = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

        assertEquals(message.length(), writeBuf.remaining()); // 9
        clientChannel.write(writeBuf);
        assertEquals(0, writeBuf.remaining()); // write 전에는 9, write 후에는 0

        ByteBuffer readBuf = ByteBuffer.allocate(128);
        int readBytes = 0;
        long startTime = System.currentTimeMillis();
        while (readBytes < message.length() && System.currentTimeMillis() - startTime < 1000) { // 1초 타임아웃
            int justRead = socket.read(readBuf);
            if (justRead > 0) {
                readBytes += justRead;
            } else if (justRead == -1) {
                break; // EOF
            }
        }

        assertEquals(message.length(), readBytes);
        assertEquals(message.length(), readBuf.position(), "Read 후 position은 읽은 바이트 수와 같아야 함");

        readBuf.flip();
        assertEquals(message.length(), readBuf.remaining(), "Flip 후 remaining은 읽은 바이트 수와 같아야 함");

        String receivedMessage = StandardCharsets.UTF_8.decode(readBuf).toString();
        assertEquals(message, receivedMessage);
    }

    @Test
    public void testWriteWithPipe() throws IOException {
        // 1. 실제 데이터를 실어나를 Pipe 생성
        Pipe pipe = Pipe.open();
        Pipe.SinkChannel sink = pipe.sink();
        Pipe.SourceChannel source = pipe.source();

        // 2. SocketChannel Mock 생성
        SocketChannel mockSocketChannel = mock(SocketChannel.class);

        // 3. Mock의 write가 호출되면 실제 Pipe의 sink로 데이터를 넘기도록 설정
        when(mockSocketChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            return sink.write(buffer); // Pipe로 실제 데이터 전달
        });

        // 4. Socket 객체 생성 (수정 없이 그대로 사용)
        Socket socket = new Socket(mockSocketChannel);

        // 5. 실행
        ByteBuffer data = ByteBuffer.wrap("TestData".getBytes());
        int written = socket.write(data);

        // 6. 검증: Pipe의 source를 통해 실제로 데이터가 넘어갔는지 확인
        ByteBuffer checkBuffer = ByteBuffer.allocate(8);
        source.read(checkBuffer);
        checkBuffer.flip();

        assertEquals(8, written);
        assertEquals("TestData", StandardCharsets.UTF_8.decode(checkBuffer).toString());
    }

}