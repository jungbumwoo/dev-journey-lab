package com.jungbum.memory.heap;

/**
 * [Heap - Young/Old Generation 학습]
 *
 * Heap은 세대별(Generational)로 나뉜다:
 *
 *  ┌─────────────────────────────────────────────┐
 *  │                   Heap                       │
 *  │  ┌──────────────────────┐  ┌──────────────┐ │
 *  │  │   Young Generation   │  │     Old      │ │
 *  │  │ ┌─────┬─────┬──────┐│  │  Generation  │ │
 *  │  │ │Eden │ S0  │  S1  ││  │  (Tenured)   │ │
 *  │  │ └─────┴─────┴──────┘│  └──────────────┘ │
 *  │  └──────────────────────┘                    │
 *  └─────────────────────────────────────────────┘
 *
 * - 새 객체는 Eden에 할당
 * - Minor GC가 발생하면 살아남은 객체는 Survivor로 이동
 * - 여러 번 살아남은 객체는 Old Generation으로 승격(Promotion)
 *
 * 실행 방법:
 *   java -Xms40m -Xmx40m -Xmn20m -XX:+PrintGCDetails \
 *        -XX:SurvivorRatio=8 com.jungbum.memory.heap.YoungOldGenerationExample
 *
 * VM 매개변수 설명:
 *   -Xmn: Young Generation 크기
 *   -XX:SurvivorRatio=8: Eden:S0:S1 = 8:1:1
 */
public class YoungOldGenerationExample {

    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        System.out.println("=== Young/Old Generation 학습 ===\n");

        // 1. 객체 할당과 Minor GC
        minorGcDemo();

        // 2. 큰 객체는 Old Generation에 직접 할당
        largeObjectDemo();

        // 3. 객체 승격(Promotion) 과정
        promotionDemo();
    }

    /**
     * Eden이 가득 차면 Minor GC가 발생한다.
     * 살아남은 객체는 Survivor 영역으로 복사된다.
     */
    static void minorGcDemo() {
        System.out.println("--- 1. Minor GC 시뮬레이션 ---");

        // Eden 영역을 채워서 Minor GC 유도
        for (int i = 0; i < 5; i++) {
            byte[] allocation = new byte[2 * _1MB];
            System.out.println("2MB 할당 #" + (i + 1) + " (Eden에 할당, GC 발생 가능)");
        }
        System.out.println();
    }

    /**
     * -XX:PretenureSizeThreshold 이상의 큰 객체는
     * Young Generation을 거치지 않고 바로 Old Generation에 할당된다.
     * (Serial, ParNew 컬렉터에서 지원)
     */
    static void largeObjectDemo() {
        System.out.println("--- 2. 큰 객체 할당 ---");
        // 큰 배열은 Old Generation에 직접 할당될 수 있음
        byte[] bigObject = new byte[4 * _1MB];
        System.out.println("4MB 객체 생성 (큰 객체 -> Old Generation 직접 할당 가능)");
        System.out.println();
    }

    /**
     * 객체가 Minor GC를 여러 번 살아남으면 Old Generation으로 승격된다.
     * 기본 임계값은 -XX:MaxTenuringThreshold=15
     */
    static void promotionDemo() {
        System.out.println("--- 3. 객체 승격(Promotion) ---");

        // 장수 객체 (참조를 유지하여 GC에서 살아남게 함)
        Object longLived = new byte[_1MB];

        // 단명 객체를 반복 생성하여 Minor GC 유도
        for (int i = 0; i < 10; i++) {
            byte[] shortLived = new byte[_1MB];
            // shortLived는 매 루프마다 참조가 끊김 -> GC 대상
        }

        // longLived는 여러 번의 GC에서 살아남아 Old Generation으로 승격
        System.out.println("longLived 객체: 여러 번의 GC를 살아남아 Old Generation으로 승격됨");
        System.out.println("shortLived 객체들: 참조가 끊겨 Young Generation에서 수거됨");
        System.out.println("longLived 해시: " + System.identityHashCode(longLived));
    }
}
