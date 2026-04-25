package com.jungbum.memory.nativestack;

/**
 * [Native Method Stack (네이티브 메서드 스택) 학습]
 *
 * Native Method Stack은 JNI(Java Native Interface)를 통해 호출되는
 * 네이티브 메서드(C/C++ 등)를 위한 스택이다.
 *
 * VM Stack vs Native Method Stack:
 *  ┌─────────────────┐  ┌─────────────────────┐
 *  │   VM Stack       │  │ Native Method Stack  │
 *  │ (Java 메서드용)  │  │ (Native 메서드용)     │
 *  │                  │  │                      │
 *  │ methodA() 프레임 │  │ native_func() 프레임  │
 *  │ methodB() 프레임 │  │                      │
 *  └─────────────────┘  └─────────────────────┘
 *
 * 참고: HotSpot VM에서는 VM Stack과 Native Method Stack을 구분하지 않고
 *       하나의 스택으로 통합하여 사용한다.
 *
 * 실행 방법:
 *   java com.jungbum.memory.nativestack.NativeMethodStackExample
 */
public class NativeMethodStackExample {

    public static void main(String[] args) {
        System.out.println("=== Native Method Stack 학습 ===\n");

        // 1. Java 표준 라이브러리의 native 메서드 확인
        nativeMethodDemo();

        // 2. native 메서드 호출 흐름
        nativeCallFlowDemo();
    }

    /**
     * Java 표준 라이브러리에서 자주 사용되는 native 메서드들.
     * 이 메서드들이 호출될 때 Native Method Stack이 사용된다.
     */
    static void nativeMethodDemo() {
        System.out.println("--- 1. 자주 사용되는 native 메서드들 ---");

        // Object.hashCode() -> native 메서드
        Object obj = new Object();
        int hash = obj.hashCode();
        System.out.println("Object.hashCode() [native]: " + hash);

        // System.currentTimeMillis() -> native 메서드
        long time = System.currentTimeMillis();
        System.out.println("System.currentTimeMillis() [native]: " + time);

        // System.arraycopy() -> native 메서드
        int[] src = {1, 2, 3, 4, 5};
        int[] dst = new int[5];
        System.arraycopy(src, 0, dst, 0, src.length);
        System.out.print("System.arraycopy() [native]: ");
        for (int v : dst) System.out.print(v + " ");
        System.out.println();

        // Thread.currentThread() -> native 메서드
        Thread current = Thread.currentThread();
        System.out.println("Thread.currentThread() [native]: " + current.getName());

        System.out.println("\n위 메서드들은 모두 native 키워드로 선언된 메서드이며,");
        System.out.println("호출 시 Native Method Stack에 프레임이 생성된다.");
        System.out.println();
    }

    /**
     * native 메서드 호출 시의 스택 변화:
     *
     * 1. Java 메서드 실행 중 -> VM Stack 사용
     * 2. native 메서드 호출 -> Native Method Stack으로 전환
     * 3. native 메서드 반환 -> VM Stack으로 복귀
     *
     * HotSpot VM에서는 이 두 스택이 합쳐져 있으므로
     * 스택 트레이스에서 native 메서드를 확인할 수 있다.
     */
    static void nativeCallFlowDemo() {
        System.out.println("--- 2. Native 메서드 호출 흐름 ---");

        System.out.println("Java 메서드에서 native 메서드 호출 흐름:");
        System.out.println("  javaMethod()          <- VM Stack 프레임");
        System.out.println("    -> Object.hashCode() <- Native Method Stack 프레임");
        System.out.println("    <- 반환              <- VM Stack으로 복귀");
        System.out.println();

        // 스택 트레이스로 확인
        System.out.println("스택 트레이스에서 native 메서드 확인:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String marker = element.isNativeMethod() ? " [NATIVE]" : "";
            System.out.println("  " + element.getClassName() + "." + element.getMethodName() + marker);
        }
        System.out.println("\n(getStackTrace() 자체가 native 메서드이므로 [NATIVE] 표시가 보임)");
    }
}
