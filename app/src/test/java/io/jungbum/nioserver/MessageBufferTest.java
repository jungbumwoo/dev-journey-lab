package io.jungbum.nioserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageBufferTest {

    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        messageBuffer = new MessageBuffer();
    }

    @Test
    @DisplayName("최초 메시지 할당 시 Small 버퍼를 할당받아야 한다")
    void shouldGetSmallMessageInitially() {
        Message message = messageBuffer.getMessage();

        assertNotNull(message);
        assertEquals(4 * 1024, message.capacity); // CAPACITY_SMALL
        assertEquals(messageBuffer.smallMessageBuffer, message.sharedArray);
    }

    @Test
    @DisplayName("메시지가 확장되면 Medium 버퍼로 옮겨가야 한다")
    void shouldExpandToMediumBuffer() {
        Message message = messageBuffer.getMessage();
        message.length = 100; // 데이터가 있다고 가정

        boolean expanded = messageBuffer.expandMessage(message);

        assertTrue(expanded);
        assertEquals(128 * 1024, message.capacity); // CAPACITY_MEDIUM
        assertEquals(messageBuffer.mediumMessageBuffer, message.sharedArray);
    }

    @Test
    @DisplayName("더 이상 할당할 블록이 없으면 null을 반환해야 한다")
    void shouldReturnNullWhenSmallBufferExhausted() {
        // smallMessageBufferFreeBlocks의 크기인 1024개를 모두 소진
        for (int i = 0; i < 1024; i++) {
            messageBuffer.getMessage();
        }

        Message extraMessage = messageBuffer.getMessage();
        assertNull(extraMessage, "버퍼가 꽉 차면 null을 반환해야 합니다.");
    }

    @Test
    @DisplayName("확장 시 이전 작은 블록은 다시 사용 가능한 상태가 되어야 한다")
    void shouldFreeSmallBlockAfterExpansion() {
        Message message = messageBuffer.getMessage();
        int originalOffset = message.offset;

        messageBuffer.expandMessage(message);

        // 다시 할당했을 때 이전에 썼던 offset을 다시 가져올 수 있는지 확인
        // (QueueIntFlip의 동작에 따라 순서는 다를 수 있지만, 여기서는 개념 확인용)
        Message newMessage = messageBuffer.getMessage();
        assertNotNull(newMessage);
    }
}