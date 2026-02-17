package io.jungbum.nioserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    private MessageBuffer messageBuffer;
    private Message message;

    @BeforeEach
    void setUp() {
        // 실제 MessageBuffer를 사용하여 상호작용 테스트
        messageBuffer = new MessageBuffer();
        message = messageBuffer.getMessage(); // 최초 4KB(Small) 할당
    }

    @Test
    @DisplayName("데이터를 쓸 때 공간이 부족하면 자동으로 버퍼가 확장되어야 한다")
    void shouldExpandAutomaticallyWhenWriting() {
        // Small(4KB)보다 큰 5KB 데이터를 준비
        byte[] bigData = new byte[5 * 1024];

        int written = message.writeToMessage(bigData);

        assertEquals(bigData.length, written);
        assertEquals(bigData.length, message.length);
        // CAPACITY_MEDIUM(128KB)으로 확장되었는지 확인
        assertEquals(128 * 1024, message.capacity);
    }

    @Test
    @DisplayName("Partial Message 복사 시 위치와 길이가 정확해야 한다")
    void testWritePartialMessage() {

        // 1. 원본 메시지에 100바이트 데이터 기록
        byte[] sourceData = new byte[100];
        for (int i = 0; i < 100; i++) {
            sourceData[i] = (byte) i;
        }

        Message message = messageBuffer.getMessage();
        message.writeToMessage(sourceData);

        // 2. 60바이트 지점부터가 다음 메시지라고 가정
        Message nextMessage = messageBuffer.getMessage();
        nextMessage.writePartialMessageToMessage(message, 60);

        // 3. length 검증
        assertEquals(40, nextMessage.length);

        // 4. 실제 데이터 검증
        for (int i = 0; i < 40; i++) {
            byte expected = sourceData[60 + i];
            byte actual = nextMessage.sharedArray[nextMessage.offset + i];
            assertEquals(expected, actual);
        }
    }

    @Test
    @DisplayName("확장이 불가능한 크기일 경우 -1을 반환해야 한다")
    void shouldReturnMinusOneWhenExpansionFails() {
        // 매우 큰 데이터 (16MB 이상)
        byte[] tooBigData = new byte[20 * 1024 * 1024];

        int result = message.writeToMessage(tooBigData);

        assertEquals(-1, result);
    }
}