package com.jungbum.memory.methodarea;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * [Method Area (메서드 영역 / Metaspace) 학습]
 *
 * Method Area는 클래스 수준의 정보를 저장하는 영역이다.
 * JDK 8부터는 PermGen이 제거되고 Metaspace(네이티브 메모리)로 대체되었다.
 *
 * 저장되는 정보:
 *   - 클래스 메타데이터 (클래스 이름, 부모 클래스, 인터페이스 목록)
 *   - 필드 정보 (이름, 타입, 접근 제한자)
 *   - 메서드 정보 (이름, 반환 타입, 매개변수, 바이트코드)
 *   - 런타임 상수 풀 (Runtime Constant Pool)
 *   - static 변수
 *
 *  ┌─────────────────────────────────────────┐
 *  │           Method Area (Metaspace)        │
 *  │  ┌─────────────────┐ ┌───────────────┐  │
 *  │  │  Class Metadata  │ │ Static Fields │  │
 *  │  │  - 클래스 이름    │ │ - static 변수 │  │
 *  │  │  - 부모 클래스    │ │               │  │
 *  │  │  - 메서드 정보    │ └───────────────┘  │
 *  │  │  - 필드 정보      │ ┌───────────────┐  │
 *  │  └─────────────────┘ │ Runtime        │  │
 *  │                       │ Constant Pool │  │
 *  │                       └───────────────┘  │
 *  └─────────────────────────────────────────┘
 *
 * 실행 방법:
 *   java -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=64m \
 *        com.jungbum.memory.methodarea.MethodAreaExample
 */
public class MethodAreaExample {

    // static 변수 -> Method Area에 저장
    static int staticCounter = 0;
    static final String CONSTANT = "상수값"; // 컴파일 타임 상수

    // 인스턴스 변수 -> Heap에 저장 (Method Area가 아님!)
    int instanceVar = 10;

    public static void main(String[] args) {
        System.out.println("=== Method Area 학습 ===\n");

        // 1. 클래스 메타데이터 확인
        classMetadataDemo();

        // 2. static 변수의 저장 위치
        staticFieldDemo();

        // 3. 클래스 로딩 과정
        classLoadingDemo();
    }

    /**
     * 클래스가 로드되면 Method Area에 클래스 메타데이터가 저장된다.
     * Reflection API로 이 정보에 접근할 수 있다.
     */
    static void classMetadataDemo() {
        System.out.println("--- 1. 클래스 메타데이터 (Method Area에 저장) ---");

        Class<?> clazz = MethodAreaExample.class;

        // 클래스 이름
        System.out.println("클래스 이름: " + clazz.getName());
        System.out.println("부모 클래스: " + clazz.getSuperclass().getName());

        // 필드 정보
        System.out.println("\n필드 목록 (Method Area에 저장):");
        for (Field f : clazz.getDeclaredFields()) {
            System.out.println("  " + f.getType().getSimpleName() + " " + f.getName()
                    + (java.lang.reflect.Modifier.isStatic(f.getModifiers()) ? " [static -> Method Area]" : " [instance -> Heap]"));
        }

        // 메서드 정보
        System.out.println("\n메서드 목록 (바이트코드가 Method Area에 저장):");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println("  " + m.getReturnType().getSimpleName() + " " + m.getName() + "()");
        }
        System.out.println();
    }

    /**
     * static 변수는 클래스에 속하며, 모든 인스턴스가 공유한다.
     * JDK 7+에서는 static 변수의 참조가 Heap의 Class 객체에 저장되지만,
     * 개념적으로는 Method Area의 일부로 간주된다.
     */
    static void staticFieldDemo() {
        System.out.println("--- 2. static 변수 ---");

        MethodAreaExample obj1 = new MethodAreaExample();
        MethodAreaExample obj2 = new MethodAreaExample();

        staticCounter = 1;
        System.out.println("staticCounter = 1 설정 후:");
        System.out.println("  obj1에서 접근: " + MethodAreaExample.staticCounter);
        System.out.println("  obj2에서 접근: " + MethodAreaExample.staticCounter);
        System.out.println("  (같은 Method Area의 값을 참조하므로 동일)");

        System.out.println("\n인스턴스 변수와의 차이:");
        obj1.instanceVar = 100;
        obj2.instanceVar = 200;
        System.out.println("  obj1.instanceVar = " + obj1.instanceVar + " (Heap - obj1 고유)");
        System.out.println("  obj2.instanceVar = " + obj2.instanceVar + " (Heap - obj2 고유)");
        System.out.println();
    }

    /**
     * 클래스 로딩 과정:
     *   1. Loading: .class 파일을 읽어 바이너리 데이터 생성
     *   2. Linking:
     *      - Verification: 바이트코드 검증
     *      - Preparation: static 변수에 기본값 할당
     *      - Resolution: 심볼릭 레퍼런스를 직접 레퍼런스로 변환
     *   3. Initialization: static 초기화 블록과 static 변수 초기화 실행
     */
    static void classLoadingDemo() {
        System.out.println("--- 3. 클래스 로딩 ---");

        System.out.println("LazyClass를 참조하기 전...");
        System.out.println("(아직 LazyClass는 로드되지 않음)");

        // 클래스를 처음 사용할 때 로드됨
        System.out.println("\nLazyClass.value 접근:");
        System.out.println("  값: " + LazyClass.value);
        System.out.println("(이 시점에 LazyClass가 로드되고 static 블록이 실행됨)");
    }

    static class LazyClass {
        static int value = 42;

        static {
            System.out.println("  [LazyClass static 초기화 블록 실행!]");
            System.out.println("  [Method Area에 클래스 메타데이터 저장 완료]");
        }
    }
}
