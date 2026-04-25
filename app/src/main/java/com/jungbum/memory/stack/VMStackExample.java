package com.jungbum.memory.stack;

/**
 * [VM Stack (가상 머신 스택) 학습]
 *
 * 각 스레드마다 독립적인 VM Stack이 생성된다.
 * 메서드가 호출될 때마다 스택 프레임(Stack Frame)이 push되고,
 * 메서드가 종료되면 pop된다.
 *
 * 스택 프레임 구조:
 *  ┌─────────────────────┐
 *  │    Stack Frame       │
 *  │ ┌─────────────────┐ │
 *  │ │ 지역 변수 테이블  │ │  <- 매개변수, 지역 변수 저장
 *  │ │ (Local Variables)│ │
 *  │ ├─────────────────┤ │
 *  │ │  오퍼랜드 스택    │ │  <- 연산 중간 결과 저장
 *  │ │ (Operand Stack) │ │
 *  │ ├─────────────────┤ │
 *  │ │  동적 링크        │ │  <- 런타임 상수 풀 참조
 *  │ │ (Dynamic Link)  │ │
 *  │ ├─────────────────┤ │
 *  │ │  반환 주소        │ │  <- 메서드 종료 후 돌아갈 위치
 *  │ │ (Return Address) │ │
 *  │ └─────────────────┘ │
 *  └─────────────────────┘
 *
 * 실행 방법:
 *   java -Xss256k com.jungbum.memory.stack.VMStackExample
 *
 * VM 매개변수 설명:
 *   -Xss: 스레드당 스택 크기
 */
public class VMStackExample {

    public static void main(String[] args) {
        System.out.println("=== VM Stack 학습 ===\n");

        // 1. 스택 프레임과 지역 변수
        localVariableDemo();

        // 2. 오퍼랜드 스택 연산
        operandStackDemo();

        // 3. 메서드 호출 체인과 스택 프레임
        callChainDemo();

        // 4. 스레드별 독립 스택
        threadStackDemo();
    }

    /**
     * 지역 변수 테이블(Local Variable Table):
     * - 메서드의 매개변수와 지역 변수를 저장
     * - 기본 타입(int, long 등)은 값 자체를 저장
     * - 참조 타입은 Heap 객체에 대한 참조를 저장
     * - 컴파일 시점에 크기가 결정됨
     */
    static void localVariableDemo() {
        System.out.println("--- 1. 지역 변수 테이블 ---");

        // 이 변수들은 모두 현재 스택 프레임의 지역 변수 테이블에 저장됨
        int a = 10;             // slot 0: int 값 10 (4 bytes)
        long b = 20L;           // slot 1-2: long 값 20 (8 bytes, 2 슬롯 사용)
        double c = 3.14;        // slot 3-4: double 값 3.14 (8 bytes, 2 슬롯 사용)
        String s = "hello";     // slot 5: String 객체의 참조 (Heap에 있는 객체를 가리킴)

        System.out.println("int a = " + a + " (스택에 값 저장)");
        System.out.println("long b = " + b + " (스택에 값 저장, 2 슬롯)");
        System.out.println("double c = " + c + " (스택에 값 저장, 2 슬롯)");
        System.out.println("String s = " + s + " (스택에 참조 저장, 실제 객체는 Heap)");
        System.out.println();
    }

    /**
     * 오퍼랜드 스택(Operand Stack):
     * - 바이트코드 명령어의 연산에 사용되는 임시 스택
     * - 산술 연산, 메서드 호출의 인자 전달 등에 사용
     *
     * 예) int result = 3 + 5 의 바이트코드:
     *   iconst_3    // 오퍼랜드 스택에 3 push
     *   iconst_5    // 오퍼랜드 스택에 5 push
     *   iadd        // 두 값을 pop하고 더한 결과(8)를 push
     *   istore_1    // 오퍼랜드 스택에서 pop하여 지역 변수 슬롯 1에 저장
     */
    static void operandStackDemo() {
        System.out.println("--- 2. 오퍼랜드 스택 ---");

        // 이 연산은 오퍼랜드 스택을 통해 수행됨
        int x = 3;
        int y = 5;
        int result = x + y;  // 오퍼랜드 스택: push x -> push y -> iadd -> store result

        System.out.println(x + " + " + y + " = " + result);
        System.out.println("(바이트코드 레벨에서 오퍼랜드 스택을 통해 연산됨)");
        System.out.println("javap -c 명령으로 바이트코드를 확인할 수 있습니다:");
        System.out.println("  javap -c -p com.jungbum.memory.stack.VMStackExample");
        System.out.println();
    }

    /**
     * 메서드 호출 시 스택 프레임이 쌓이고, 반환 시 제거된다.
     *
     * 호출 순서: main -> methodA -> methodB -> methodC
     *
     * Stack 상태:
     *  |             |     |             |     |  methodC()  |     |             |
     *  |             |  -> |  methodB()  |  -> |  methodB()  |  -> |             |
     *  |  methodA()  |     |  methodA()  |     |  methodA()  |     |  methodA()  |
     *  |   main()    |     |   main()    |     |   main()    |     |   main()    |
     *  └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
     *     methodA          B 호출 시점        C 호출 시점         C,B 반환 후
     */
    static void callChainDemo() {
        System.out.println("--- 3. 메서드 호출 체인과 스택 프레임 ---");
        methodA();
        System.out.println();
    }

    static void methodA() {
        System.out.println("methodA 시작 (스택 프레임 push)");
        methodB(42);
        System.out.println("methodA 종료 (스택 프레임 pop)");
    }

    static void methodB(int param) {
        System.out.println("  methodB 시작 (스택 프레임 push, param=" + param + ")");
        int localVar = param * 2;
        methodC(localVar);
        System.out.println("  methodB 종료 (스택 프레임 pop)");
    }

    static String methodC(int value) {
        System.out.println("    methodC 시작 (스택 프레임 push, value=" + value + ")");
        String result = "결과: " + value;
        System.out.println("    methodC 종료 (스택 프레임 pop, 반환값을 오퍼랜드 스택에 push)");
        return result;
    }

    /**
     * 각 스레드는 독립적인 VM Stack을 가진다.
     * 따라서 스레드 간에 지역 변수는 공유되지 않는다.
     */
    static void threadStackDemo() {
        System.out.println("--- 4. 스레드별 독립 스택 ---");

        Thread t1 = new Thread(() -> {
            int threadLocal = 100;
            System.out.println("[Thread-1] 지역 변수: " + threadLocal + " (Thread-1의 Stack)");
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            int threadLocal = 200;
            System.out.println("[Thread-2] 지역 변수: " + threadLocal + " (Thread-2의 Stack)");
        }, "Thread-2");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("(각 스레드의 threadLocal은 서로 독립적)");
    }
}
