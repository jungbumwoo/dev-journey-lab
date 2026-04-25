package com.jungbum.memory.heap;

import java.util.ArrayList;
import java.util.List;

/**
 * [Heap 영역 학습]
 *
 * Heap은 JVM이 관리하는 가장 큰 메모리 영역으로, 모든 객체 인스턴스와 배열이 할당됩니다.
 * GC(Garbage Collection)의 주요 대상이 되는 영역입니다.
 *
 * Heap 구조 (Generational):
 *   - Young Generation (Eden + Survivor0 + Survivor1)
 *   - Old Generation (Tenured)
 *
 * 실행 방법:
 *   java -Xms50m -Xmx50m -verbose:gc com.jungbum.memory.heap.HeapMemoryExample
 *
 * VM 매개변수 설명:
 *   -Xms: 힙 초기 크기
 *   -Xmx: 힙 최대 크기
 *   -verbose:gc: GC 로그 출력
 */
public class HeapMemoryExample {

    public static void main(String[] args) {
        System.out.println("=== Heap 메모리 영역 학습 ===\n");

        // 1. 객체는 Heap에 할당된다
        objectAllocationDemo();

        // 2. 배열도 Heap에 할당된다
        arrayAllocationDemo();

        // 3. GC에 의해 Heap이 관리된다
        gcDemo();

        // 4. Heap 메모리 사용량 확인
        heapMemoryInfoDemo();
    }

    /**
     * new 키워드로 생성된 모든 객체는 Heap에 할당된다.
     * 지역 변수(참조)는 Stack에, 실제 객체는 Heap에 존재한다.
     *
     * Stack: [obj 참조] ----> Heap: [실제 Object 데이터]
     */
    static void objectAllocationDemo() {
        System.out.println("--- 1. 객체 할당 ---");

        // obj는 Stack의 지역 변수(참조), new Object()의 실체는 Heap에 존재
        Object obj = new Object();
        System.out.println("Object 생성됨: " + obj);
        System.out.println("해시코드(Heap 주소 기반): " + System.identityHashCode(obj));

        // 같은 클래스에서 생성해도 각각 별도의 Heap 공간을 차지
        Object obj2 = new Object();
        System.out.println("두 객체는 서로 다른 Heap 공간: " + (obj != obj2));
        System.out.println();
    }

    /**
     * 배열도 객체이므로 Heap에 할당된다.
     * 큰 배열일수록 많은 Heap 공간을 차지한다.
     */
    static void arrayAllocationDemo() {
        System.out.println("--- 2. 배열 할당 ---");

        Runtime rt = Runtime.getRuntime();
        long before = rt.freeMemory();

        // 1MB 크기의 배열 할당 (1024 * 1024 bytes)
        byte[] largeArray = new byte[1024 * 1024];

        long after = rt.freeMemory();
        System.out.println("1MB 배열 할당 전 여유 메모리: " + before / 1024 + " KB");
        System.out.println("1MB 배열 할당 후 여유 메모리: " + after / 1024 + " KB");
        System.out.println("실제 사용된 Heap: " + (before - after) / 1024 + " KB");
        System.out.println();
    }

    /**
     * 참조가 없어진 객체는 GC의 대상이 된다.
     * System.gc()는 GC를 '요청'할 뿐, 즉시 실행을 보장하지 않는다.
     */
    static void gcDemo() {
        System.out.println("--- 3. GC 동작 확인 ---");

        Runtime rt = Runtime.getRuntime();

        // 많은 객체를 생성
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(new byte[1024 * 100]); // 100KB씩 10개
        }
        System.out.println("객체 생성 후 사용 메모리: " + (rt.totalMemory() - rt.freeMemory()) / 1024 + " KB");

        // 참조 제거 -> GC 대상이 됨
        list = null;

        // GC 요청
        System.gc();

        System.out.println("GC 후 사용 메모리: " + (rt.totalMemory() - rt.freeMemory()) / 1024 + " KB");
        System.out.println("(참조가 끊긴 객체들이 수거되어 메모리가 회복됨)");
        System.out.println();
    }

    /**
     * Runtime을 통해 현재 Heap 메모리 상태를 확인할 수 있다.
     */
    static void heapMemoryInfoDemo() {
        System.out.println("--- 4. Heap 메모리 정보 ---");

        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();       // -Xmx 값
        long totalMemory = rt.totalMemory();   // 현재 할당된 Heap
        long freeMemory = rt.freeMemory();     // 현재 사용 가능한 Heap
        long usedMemory = totalMemory - freeMemory;

        System.out.println("최대 Heap (-Xmx): " + maxMemory / 1024 / 1024 + " MB");
        System.out.println("현재 할당된 Heap: " + totalMemory / 1024 / 1024 + " MB");
        System.out.println("사용 중인 Heap: " + usedMemory / 1024 / 1024 + " MB");
        System.out.println("여유 Heap: " + freeMemory / 1024 / 1024 + " MB");
    }
}
