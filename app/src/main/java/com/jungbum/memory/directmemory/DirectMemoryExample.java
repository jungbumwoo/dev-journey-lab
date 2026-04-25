package com.jungbum.memory.directmemory;

import java.nio.ByteBuffer;

/**
 * [Direct Memory (직접 메모리) 학습]
 *
 * Direct Memory는 JVM Heap 밖의 네이티브 메모리를 직접 사용하는 방식이다.
 * NIO(New I/O)의 DirectByteBuffer를 통해 할당한다.
 *
 * Heap Buffer vs Direct Buffer:
 *  ┌─────────┐         ┌─────────┐
 *  │  Java   │         │  Java   │
 *  │  Heap   │         │  Heap   │
 *  │ ┌─────┐ │         │ ┌─────┐ │
 *  │ │Heap │ │         │ │참조 ├─┼──┐
 *  │ │Buf  │ │         │ └─────┘ │  │
 *  │ └─────┘ │         └─────────┘  │  ┌──────────┐
 *  └─────────┘                      └──│ Direct   │
 *  HeapByteBuffer                      │ Memory   │
 *                                      └──────────┘
 *                                    DirectByteBuffer
 *
 * Direct Memory의 장점:
 *   - I/O 작업 시 Heap <-> Native 복사 비용이 없음 (Zero-copy)
 *   - GC 대상이 아니므로 GC 부담 감소
 *
 * Direct Memory의 단점:
 *   - 할당/해제 비용이 더 큼
 *   - OutOfMemoryError 발생 가능 (-XX:MaxDirectMemorySize로 제한)
 *
 * 실행 방법:
 *   java -XX:MaxDirectMemorySize=100m com.jungbum.memory.directmemory.DirectMemoryExample
 */
public class DirectMemoryExample {

    public static void main(String[] args) {
        System.out.println("=== Direct Memory 학습 ===\n");

        // 1. Heap Buffer vs Direct Buffer
        bufferComparisonDemo();

        // 2. Direct Memory 할당과 사용
        directMemoryUsageDemo();

        // 3. 성능 비교
        performanceComparisonDemo();
    }

    /**
     * Heap Buffer: JVM Heap에 할당
     * Direct Buffer: 네이티브 메모리에 할당
     */
    static void bufferComparisonDemo() {
        System.out.println("--- 1. Heap Buffer vs Direct Buffer ---");

        // Heap Buffer
        ByteBuffer heapBuffer = ByteBuffer.allocate(1024);
        System.out.println("Heap Buffer:");
        System.out.println("  isDirect: " + heapBuffer.isDirect());    // false
        System.out.println("  capacity: " + heapBuffer.capacity() + " bytes");
        System.out.println("  (JVM Heap에 할당됨, GC 대상)");

        // Direct Buffer
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);
        System.out.println("\nDirect Buffer:");
        System.out.println("  isDirect: " + directBuffer.isDirect());  // true
        System.out.println("  capacity: " + directBuffer.capacity() + " bytes");
        System.out.println("  (네이티브 메모리에 할당됨, GC 대상 아님)");
        System.out.println();
    }

    /**
     * Direct Buffer에 데이터를 쓰고 읽는 예제.
     */
    static void directMemoryUsageDemo() {
        System.out.println("--- 2. Direct Memory 사용 ---");

        ByteBuffer buffer = ByteBuffer.allocateDirect(256);

        // 데이터 쓰기
        buffer.putInt(42);
        buffer.putDouble(3.14);
        buffer.putChar('A');

        // flip: 쓰기 모드 -> 읽기 모드
        buffer.flip();

        // 데이터 읽기
        System.out.println("putInt -> getInt: " + buffer.getInt());
        System.out.println("putDouble -> getDouble: " + buffer.getDouble());
        System.out.println("putChar -> getChar: " + buffer.getChar());
        System.out.println("(Direct Memory에서 직접 읽기/쓰기 수행)");
        System.out.println();
    }

    /**
     * 대량의 I/O 작업에서 Direct Buffer가 유리할 수 있다.
     * 반복적인 할당/해제에서는 Heap Buffer가 유리하다.
     */
    static void performanceComparisonDemo() {
        System.out.println("--- 3. 성능 비교 ---");

        int iterations = 1_000_000;
        int bufferSize = 1024;

        // Heap Buffer 읽기/쓰기 성능
        ByteBuffer heapBuf = ByteBuffer.allocate(bufferSize);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            heapBuf.clear();
            heapBuf.putInt(i);
            heapBuf.flip();
            heapBuf.getInt();
        }
        long heapTime = System.nanoTime() - start;

        // Direct Buffer 읽기/쓰기 성능
        ByteBuffer directBuf = ByteBuffer.allocateDirect(bufferSize);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            directBuf.clear();
            directBuf.putInt(i);
            directBuf.flip();
            directBuf.getInt();
        }
        long directTime = System.nanoTime() - start;

        System.out.println("Heap Buffer (" + iterations + " 반복): " + heapTime / 1_000_000 + " ms");
        System.out.println("Direct Buffer (" + iterations + " 반복): " + directTime / 1_000_000 + " ms");
        System.out.println();
        System.out.println("참고: Direct Buffer는 I/O 채널과 함께 사용할 때 진정한 성능 이점이 있음");
        System.out.println("(커널 버퍼 <-> JVM Heap 간 복사를 건너뛰는 Zero-copy 효과)");
    }
}
