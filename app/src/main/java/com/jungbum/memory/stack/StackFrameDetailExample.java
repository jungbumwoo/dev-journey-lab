package com.jungbum.memory.stack;

/**
 * [스택 프레임 상세 학습 - 지역 변수 슬롯 재사용]
 *
 * 지역 변수 테이블의 슬롯(Slot)은 재사용될 수 있다.
 * 변수의 스코프가 끝나면 해당 슬롯을 다른 변수가 재사용할 수 있다.
 * 이는 GC에도 영향을 줄 수 있다.
 *
 * 실행 방법:
 *   java -verbose:gc com.jungbum.memory.stack.StackFrameDetailExample
 */
public class StackFrameDetailExample {

    public static void main(String[] args) {
        System.out.println("=== 스택 프레임 상세 학습 ===\n");

        // 1. 슬롯 재사용이 GC에 미치는 영향
        slotReuseGcEffect();

        // 2. this 참조와 인스턴스 메서드
        new StackFrameDetailExample().instanceMethodDemo(10, 20);
    }

    /**
     * 지역 변수 슬롯 재사용과 GC의 관계.
     *
     * placeholder의 스코프가 끝난 후에도, 슬롯이 재사용되지 않으면
     * GC Root에서 여전히 참조가 남아있어 GC되지 않을 수 있다.
     */
    static void slotReuseGcEffect() {
        System.out.println("--- 1. 슬롯 재사용과 GC ---");

        // Case A: 스코프 밖이지만 슬롯이 재사용되지 않은 경우
        {
            byte[] placeholder = new byte[64 * 1024 * 1024]; // 64MB
        }
        // 여기서 placeholder의 스코프는 끝났지만,
        // 슬롯이 재사용되지 않으면 GC가 수거하지 못할 수 있음
        System.gc();
        System.out.println("Case A: 슬롯 미재사용 - GC 후에도 64MB가 남아있을 수 있음");

        // Case B: 슬롯을 재사용하면 이전 참조가 끊김
        {
            byte[] placeholder = new byte[64 * 1024 * 1024]; // 64MB
        }
        int reuse = 0; // placeholder가 사용하던 슬롯을 재사용
        System.gc();
        System.out.println("Case B: 슬롯 재사용(int reuse=" + reuse + ") - GC가 64MB 수거 가능");
        System.out.println();
    }

    /**
     * 인스턴스 메서드의 지역 변수 테이블:
     * - slot 0: this 참조 (인스턴스 메서드에서 자동 추가)
     * - slot 1~: 매개변수
     * - 그 이후: 지역 변수
     *
     * static 메서드에는 this가 없으므로 slot 0부터 매개변수가 들어간다.
     */
    void instanceMethodDemo(int a, int b) {
        System.out.println("--- 2. 인스턴스 메서드의 지역 변수 테이블 ---");
        int sum = a + b;

        System.out.println("slot 0: this = " + this);
        System.out.println("slot 1: a = " + a);
        System.out.println("slot 2: b = " + b);
        System.out.println("slot 3: sum = " + sum);
        System.out.println("(인스턴스 메서드는 항상 slot 0에 this 참조를 가진다)");
    }
}
