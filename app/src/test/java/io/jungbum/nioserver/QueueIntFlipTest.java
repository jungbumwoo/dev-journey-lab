package io.jungbum.nioserver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueueIntFlipTest {

    private QueueIntFlip queue;
    private final int CAPACITY = 10;

    @BeforeEach
    void setUp() {
        queue = new QueueIntFlip(CAPACITY);
    }

    @Test
    @DisplayName("기본 삽입 및 추출 테스트")
    void testBasicPutAndTake() {
        assertTrue(queue.put(100));
        assertTrue(queue.put(200));
        assertEquals(2, queue.available());

        assertEquals(100, queue.take());
        assertEquals(200, queue.take());
        assertEquals(0, queue.available());
        assertEquals(-1, queue.take()); // 빈 큐 처리
    }

    @Test
    @DisplayName("큐가 가득 찼을 때 삽입 실패 확인")
    void testQueueFull() {
        for (int i = 0; i < CAPACITY; i++) {
            assertTrue(queue.put(i));
        }
        assertFalse(queue.put(11), "가득 찬 상태에서는 삽입에 실패해야 함");
        assertEquals(0, queue.remainingCapacity());
    }

    @Test
    @DisplayName("Flip 발생 시 로직 검증 (Write Pos가 0으로 돌아가는 상황)")
    void testFlipLogic() {
        // 1. 8개 채움 (WritePos: 8, ReadPos: 0, Flipped: false)
        for (int i = 0; i < 8; i++) queue.put(i);

        // 2. 5개 꺼냄 (WritePos: 8, ReadPos: 5, Flipped: false)
        for (int i = 0; i < 5; i++) queue.take();

        // 3. 2개 더 채움 (WritePos: 10 -> 0으로 Flip, Flipped: false)
        assertTrue(queue.put(8));
        assertTrue(queue.put(9));
        assertFalse(queue.flipped, "WritePos가 끝에 도달해도 다음 put 전까지 flipped는 false");
        assertEquals(10, queue.writePos);
        assertEquals(queue.writePos, queue.capacity);

        // 4. 이제 다시 0번 인덱스부터 채울 수 있음 (ReadPos인 5 전까지)
        assertTrue(queue.put(10)); // index 0
        assertTrue(queue.flipped, "flipped true");
        assertEquals(1, queue.writePos);
        assertEquals(6, queue.available()); // (10-5) + 1 = 6
    }

    @Test
    @DisplayName("벌크(Bulk) Put 테스트 - 경계 지점에서 나뉘어 저장되는지 확인")
    void testBulkPutWithFlip() {
        // 7개 채움
        for (int i = 0; i < 7; i++) queue.put(i);

        // 5개 벌크 삽입 시도 (7, 8, 9, 10, 11)
        // 남은 공간은 3개(끝부분) + @
        int[] newElements = {7, 8, 9, 10, 11};
        int putCount = queue.put(newElements, 5);

        assertEquals(3, putCount, "ReadPos가 0이므로 Flip되어 들어갈 공간이 없어 3개만 들어가야 함");
        assertTrue(queue.flipped);
        assertEquals(0, queue.writePos);
    }

    @Test
    @DisplayName("벌크(Bulk) Take 테스트 - Flip된 데이터를 연속적으로 가져오는지 확인")
    void testBulkTakeWithFlip() {
        // 1. Flip 상황 강제 조성
        for (int i = 0; i < 10; i++) queue.put(i); // 꽉 채움
        for (int i = 0; i < 5; i++) queue.take();  // 0~4 제거 (ReadPos: 5)
        queue.put(10); // index 0에 추가 (WritePos: 1, Flipped: true)
        queue.put(11); // index 1에 추가 (WritePos: 2, Flipped: true)

        // 2. 현재 상태: ReadPos 5, WritePos 2, Flipped true (총 7개 존재)
        int[] target = new int[10];
        int takenCount = queue.take(target, 7);

        assertEquals(7, takenCount);
        assertEquals(5, target[0]); // 원래 뒤쪽에 있던 데이터부터
        assertEquals(6, target[1]);
        assertEquals(7, target[2]);
        assertEquals(8, target[3]);
        assertEquals(9, target[4]);
        assertEquals(10, target[5]); // Flip되어 앞쪽으로 온 데이터
        assertEquals(11, target[6]);
        assertFalse(queue.flipped, "데이터를 다 읽으면 flipped가 다시 false가 되어야 함");

        // 다 읽은 상태 확인
        assertEquals(queue.readPos, queue.writePos);
        assertEquals( 2, queue.readPos);

        for (int j = 2; j < 10; j++) queue.put(j);
        assertFalse(queue.flipped, "CAP 까지 채운 상태로 flipped 되기 전");
        queue.put(0);
        assertTrue(queue.flipped, "flipped 됨");
    }

    @Test
    @DisplayName("Reset 기능 테스트")
    void testReset() {
        queue.put(1);
        queue.flipped = true;
        queue.reset();

        assertEquals(0, queue.writePos);
        assertEquals(0, queue.readPos);
        assertFalse(queue.flipped);
        assertEquals(0, queue.available());
    }
}