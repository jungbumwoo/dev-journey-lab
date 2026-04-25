package com.jungbum.memory.pc;

/**
 * [Program Counter (PC Register) 학습]
 *
 * PC Register는 현재 스레드가 실행 중인 바이트코드의 주소(줄 번호)를 저장한다.
 *
 * 특징:
 *   - 스레드마다 독립적으로 존재 (Thread-private)
 *   - JVM에서 유일하게 OutOfMemoryError가 발생하지 않는 영역
 *   - Java 메서드 실행 시: 현재 바이트코드 명령어 주소를 저장
 *   - Native 메서드 실행 시: undefined (비어 있음)
 *
 *  Thread-1              Thread-2
 *  ┌──────────┐          ┌──────────┐
 *  │ PC: 0x0A │          │ PC: 0x1F │
 *  │ (현재 실행│          │ (현재 실행│
 *  │  위치)    │          │  위치)    │
 *  └──────────┘          └──────────┘
 *
 * PC Register를 직접 접근하는 Java API는 없지만,
 * 멀티스레드 환경에서 왜 필요한지를 이해할 수 있다.
 *
 * 실행 방법:
 *   java com.jungbum.memory.pc.ProgramCounterExample
 */
public class ProgramCounterExample {

    public static void main(String[] args) {
        System.out.println("=== Program Counter (PC Register) 학습 ===\n");

        // 1. PC Register가 필요한 이유: 스레드 컨텍스트 스위칭
        contextSwitchDemo();

        // 2. 바이트코드 주소 개념
        bytecodeAddressDemo();
    }

    /**
     * CPU 코어가 1개라면 여러 스레드를 번갈아 실행(컨텍스트 스위칭)한다.
     * 이때 각 스레드가 "어디까지 실행했는지"를 기억해야 하는데,
     * 그 역할을 하는 것이 PC Register이다.
     *
     * Thread-1이 10번째 명령어를 실행하다가 Thread-2로 전환되면:
     *   Thread-1의 PC = 10 (저장)
     *   Thread-2의 PC를 로드하여 Thread-2의 실행 위치부터 계속
     *
     * 다시 Thread-1로 돌아오면:
     *   Thread-1의 PC = 10을 로드하여 그 위치부터 다시 실행
     */
    static void contextSwitchDemo() {
        System.out.println("--- 1. 컨텍스트 스위칭과 PC Register ---");

        // 두 스레드가 각각 카운팅하는 예제
        // 각 스레드는 자신만의 PC Register를 가지므로
        // 컨텍스트 스위칭 후에도 정확한 위치에서 재개할 수 있다
        Thread counter1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("  [Counter-1] i=" + i + " (PC Register가 현재 위치 추적)");
                Thread.yield(); // 다른 스레드에게 CPU 양보 (컨텍스트 스위칭 유도)
            }
        }, "Counter-1");

        Thread counter2 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("  [Counter-2] i=" + i + " (PC Register가 현재 위치 추적)");
                Thread.yield();
            }
        }, "Counter-2");

        counter1.start();
        counter2.start();

        try {
            counter1.join();
            counter2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("두 스레드가 번갈아 실행되어도 각자의 PC Register 덕분에");
        System.out.println("정확한 위치에서 재개할 수 있었음\n");
    }

    /**
     * PC Register는 바이트코드의 줄 번호(주소)를 저장한다.
     * javap -c 명령으로 바이트코드를 확인하면 각 명령어에 번호가 매겨져 있다.
     *
     * 예) 아래 simpleMethod()의 바이트코드:
     *   0: iconst_1       <- PC가 0일 때 이 명령어 실행
     *   1: istore_0       <- PC가 1일 때
     *   2: iconst_2       <- PC가 2일 때
     *   3: istore_1       <- PC가 3일 때
     *   4: iload_0        <- PC가 4일 때
     *   5: iload_1        <- PC가 5일 때
     *   6: iadd           <- PC가 6일 때
     *   7: istore_2       <- PC가 7일 때
     *   8: iload_2        <- PC가 8일 때
     *   9: ireturn        <- PC가 9일 때
     */
    static void bytecodeAddressDemo() {
        System.out.println("--- 2. 바이트코드 주소 ---");

        int result = simpleMethod();
        System.out.println("simpleMethod() 결과: " + result);
        System.out.println();
        System.out.println("바이트코드를 확인하려면:");
        System.out.println("  javap -c -p com.jungbum.memory.pc.ProgramCounterExample");
        System.out.println();
        System.out.println("PC Register는 위 바이트코드의 주소(0, 1, 2, ...)를");
        System.out.println("순서대로 가리키며 실행을 진행합니다.");
    }

    static int simpleMethod() {
        int a = 1;       // iconst_1, istore_0
        int b = 2;       // iconst_2, istore_1
        int c = a + b;   // iload_0, iload_1, iadd, istore_2
        return c;         // iload_2, ireturn
    }
}
