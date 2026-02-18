package io.jungbum.nioserver.http;

import io.jungbum.nioserver.Message;
import io.jungbum.nioserver.MessageBuffer;
import io.jungbum.nioserver.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpMessageReaderTest {

    private Socket mockSocket;
    private HttpMessageReader reader;

    @BeforeEach
    void setUp() {
        mockSocket = mock(Socket.class);
        reader = new HttpMessageReader();
        reader.init(new MessageBuffer());
    }

    @Test
    @DisplayName("한 번의 읽기로 두 개의 요청이 들어왔을 때 모두 파싱해야 함")
    void shouldParseMultipleMessagesInOneRead() throws IOException {
        // 1. 두 개의 HTTP GET 요청 준비
        String rawRequests = "GET /1 HTTP/1.1\r\n\r\nGET /2 HTTP/1.1\r\n\r\n";
        ByteBuffer byteBuffer = ByteBuffer.wrap(rawRequests.getBytes());

        // socket.read가 전체 데이터를 한 번에 준다고 가정
        when(mockSocket.read(any(ByteBuffer.class))).thenAnswer(inv -> {
            ByteBuffer out = inv.getArgument(0);
            out.put(rawRequests.getBytes());
            return rawRequests.length();
        });

        // 2. 실행
        reader.read(mockSocket, ByteBuffer.allocate(1024));

        // 3. 검증
        List<Message> results = reader.getMessages();
        assertEquals(2, results.size(), "두 개의 메시지가 생성되어야 함");

        String firstMsg = new String(results.get(0).sharedArray, results.get(0).offset, results.get(0).length);
        assertTrue(firstMsg.contains("/1"));
        assertFalse(firstMsg.contains("/2"), "첫 번째 메시지에 두 번째 요청이 포함되면 안 됨");
    }

    @Test
    @DisplayName("메시지가 잘려서 들어와도 최종적으로 합쳐져야 함")
    void shouldHandleFragmentedMessages() throws IOException {
        String part1 = "GET /test HTT";
        String part2 = "P/1.1\r\n\r\n";
        ByteBuffer tempBuffer = ByteBuffer.allocate(1024);

        // 첫 번째 부분 전송
        when(mockSocket.read(any())).thenAnswer(inv -> {
            ((ByteBuffer)inv.getArgument(0)).put(part1.getBytes());
            return part1.length();
        });
        reader.read(mockSocket, tempBuffer);
        assertEquals(0, reader.getMessages().size());

        // 두 번째 부분 전송
        when(mockSocket.read(any())).thenAnswer(inv -> {
            ((ByteBuffer)inv.getArgument(0)).put(part2.getBytes());
            return part2.length();
        });
        reader.read(mockSocket, tempBuffer);

        // 최종 완성 확인
        assertEquals(1, reader.getMessages().size());
    }
}