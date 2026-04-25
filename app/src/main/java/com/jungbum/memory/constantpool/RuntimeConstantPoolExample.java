package com.jungbum.memory.constantpool;

/**
 * [Runtime Constant Pool (런타임 상수 풀) 학습]
 *
 * 런타임 상수 풀은 Method Area의 일부로,
 * .class 파일의 상수 풀 테이블(Constant Pool Table)이 클래스 로딩 후
 * 런타임에 저장되는 영역이다.
 *
 * 저장되는 내용:
 *   - 리터럴(Literal): 문자열 리터럴, 숫자 상수
 *   - 심볼릭 레퍼런스: 클래스/인터페이스 이름, 필드/메서드 이름과 디스크립터
 *
 * String Pool (문자열 풀):
 *   - JDK 7+에서는 Heap 영역으로 이동
 *   - String.intern()으로 풀에 등록 가능
 *
 * 실행 방법:
 *   java com.jungbum.memory.constantpool.RuntimeConstantPoolExample
 */
public class RuntimeConstantPoolExample {

    public static void main(String[] args) {
        System.out.println("=== Runtime Constant Pool 학습 ===\n");

        // 1. String Pool과 intern()
        stringPoolDemo();

        // 2. 컴파일 타임 상수 vs 런타임 상수
        compileTimeConstantDemo();

        // 3. Integer Cache (상수 풀과 유사한 개념)
        integerCacheDemo();
    }

    /**
     * String Pool:
     * - 문자열 리터럴은 String Pool에 저장된다.
     * - 같은 리터럴은 동일한 객체를 참조한다.
     * - new String()으로 생성하면 Heap에 새 객체가 만들어진다.
     * - intern()을 호출하면 String Pool에 등록하고 그 참조를 반환한다.
     */
    static void stringPoolDemo() {
        System.out.println("--- 1. String Pool ---");

        // 리터럴은 String Pool에서 같은 객체를 참조
        String s1 = "hello";
        String s2 = "hello";
        System.out.println("s1 == s2 (리터럴): " + (s1 == s2));  // true
        System.out.println("  -> 같은 String Pool 객체를 참조");

        // new로 생성하면 Heap에 새 객체
        String s3 = new String("hello");
        System.out.println("\ns1 == s3 (new): " + (s1 == s3));     // false
        System.out.println("  -> s3는 Heap에 별도 객체");

        // intern()으로 String Pool의 참조 획득
        String s4 = s3.intern();
        System.out.println("\ns1 == s4 (intern): " + (s1 == s4));  // true
        System.out.println("  -> intern()이 String Pool의 기존 참조를 반환");

        // JDK 7+ 에서의 intern() 동작 변화
        String s5 = new StringBuilder("ja").append("va").toString();
        String s6 = s5.intern();
        System.out.println("\ns5 == s6 (\"java\"): " + (s5 == s6));
        System.out.println("  -> \"java\"는 JVM 내부에서 이미 사용하는 문자열이므로 false일 수 있음");

        String s7 = new StringBuilder("hello").append("world").toString();
        String s8 = s7.intern();
        System.out.println("\ns7 == s8 (\"helloworld\"): " + (s7 == s8));
        System.out.println("  -> JDK 7+: intern()이 Heap 객체의 참조를 Pool에 등록하므로 true");
        System.out.println();
    }

    /**
     * 컴파일 타임 상수(static final + 리터럴)는
     * 상수 풀에 직접 저장되며, 사용하는 클래스에 인라인된다.
     */
    static void compileTimeConstantDemo() {
        System.out.println("--- 2. 컴파일 타임 상수 vs 런타임 값 ---");

        // 컴파일 타임 상수: 상수 풀에 직접 저장
        final String compiletimeConst = "COMPILE_TIME";
        System.out.println("컴파일 타임 상수: " + compiletimeConst);
        System.out.println("  -> .class 파일의 상수 풀에 저장됨");

        // 런타임에 결정되는 값
        String runtimeValue = "RUNTIME" + System.currentTimeMillis();
        System.out.println("런타임 값: " + runtimeValue);
        System.out.println("  -> 실행 시점에 Heap에서 생성됨");

        // 리터럴끼리의 연산은 컴파일 타임에 최적화
        String concat1 = "hello" + " " + "world"; // 컴파일 시 "hello world"로 최적화
        String concat2 = "hello world";
        System.out.println("\n\"hello\" + \" \" + \"world\" == \"hello world\": " + (concat1 == concat2));
        System.out.println("  -> 컴파일러가 리터럴 연산을 미리 계산(Constant Folding)");
        System.out.println();
    }

    /**
     * Integer Cache: -128 ~ 127 범위의 Integer는 캐시된다.
     * String Pool과 유사한 메모리 최적화 기법이다.
     */
    static void integerCacheDemo() {
        System.out.println("--- 3. Integer Cache ---");

        Integer a = 127;
        Integer b = 127;
        System.out.println("127 == 127: " + (a == b));  // true (캐시 범위 내)
        System.out.println("  -> -128~127 범위는 Integer Cache에서 동일 객체 반환");

        Integer c = 128;
        Integer d = 128;
        System.out.println("128 == 128: " + (c == d));  // false (캐시 범위 밖)
        System.out.println("  -> 범위 밖이므로 각각 새 객체 생성");
        System.out.println("\n(Integer Cache도 상수 풀과 유사한 메모리 절약 패턴)");
    }
}
