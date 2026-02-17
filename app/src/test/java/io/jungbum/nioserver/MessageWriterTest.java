package io.jungbum.nioserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageWriterTest {
    private MessageWriter writer;
    private Socket mockSocket;
    private ByteBuffer buffer;

    @BeforeEach
    void setUp() {
        writer = new MessageWriter();
        mockSocket = mock(Socket.class);
        buffer = ByteBuffer.allocate(1024);
    }

    @Test
    @DisplayName("메시지 하나를 여러 번에 걸쳐 부분 전송(Partial Write)하는 경우")
    void testPartialWrite() throws IOException {
        Message msg = new Message(null);
        msg.sharedArray = "Hello World".getBytes();
        msg.length = msg.sharedArray.length; // 11 bytes
        writer.enqueue(msg);

        // 1. 첫 번째 시도: 5바이트만 전송됨
        when(mockSocket.write(any(ByteBuffer.class))).thenReturn(5);
        writer.write(mockSocket, buffer);
        assertFalse(writer.isEmpty());

        // 2. 두 번째 시도: 나머지 6바이트 전송됨
        when(mockSocket.write(any(ByteBuffer.class))).thenReturn(6);
        writer.write(mockSocket, buffer);
        assertTrue(writer.isEmpty());
    }

    @Test
    @DisplayName("첫 번째 메시지 완료 후 두 번째 메시지가 0번 오프셋부터 시작되는지 확인")
    void testMessageSwitchingReset() throws IOException {
        // 1. 준비: 두 개의 메시지 설정
        Message msg1 = new Message(null);
        msg1.sharedArray = "ABC".getBytes();
        msg1.length = 3;

        Message msg2 = new Message(null);
        msg2.sharedArray = "DEF".getBytes();
        msg2.length = 3;

        writer.enqueue(msg1);
        writer.enqueue(msg2);

        // ArgumentCaptor 설정: socket.write()에 전달되는 ByteBuffer를 낚아챔
        ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);

        // 2. 실행: 첫 번째 메시지 전송 (3바이트 전송 완료 가정)
        when(mockSocket.write(any(ByteBuffer.class))).thenReturn(3);
        writer.write(mockSocket, buffer);

        // 3. 실행: 두 번째 메시지 전송
        when(mockSocket.write(any(ByteBuffer.class))).thenReturn(3);
        writer.write(mockSocket, buffer);

        // 4. 검증: socket.write가 총 2번 호출되었는지 확인하고, 두 번째 호출의 인자를 검사
        verify(mockSocket, times(2)).write(bufferCaptor.capture());

        // 모든 호출 중 두 번째(index 1)에 전달된 버퍼를 가져옴
        ByteBuffer capturedBuffer = bufferCaptor.getAllValues().get(1);

        // 버퍼의 내용을 확인하기 위해 읽기 모드로 되어 있는지 확인 (flip 호출 여부)
        // 주의: MessageWriter 내부에서 clear()를 호출하고 있어서 불안정해보임.
        // 실제 테스트 시에는 Mockito의 Answer를 이용해 호출 시점의 데이터를 복사하는 것이 더 정확할 수 있음

        // [보완 포인트] 아래는 write() 호출 시점의 데이터를 검증하기 위한 로직입니다.
        byte[] resultBytes = new byte[3];
        capturedBuffer.rewind(); // 검증을 위해 포인터를 앞으로 되돌림 -> 내부 clear() 호출 로직으로 인해 rewind()안해도 0으로 가있음
        capturedBuffer.get(resultBytes);

        assertArrayEquals("DEF".getBytes(), resultBytes, "두 번째 메시지는 'DEF'");
        assertTrue(writer.isEmpty(), "모든 메시지 전송 후에는 비어있어야 함.");
    }
}