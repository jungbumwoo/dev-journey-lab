package interfaces;

/**
 * Interface 바이트코드 분석 - 기본 인터페이스
 *
 * [1] class 선언 (javap -verbose Drawable.class)
 *   abstract class : flags: (0x0421) ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT
 *   interface      : flags: (0x0601) ACC_PUBLIC, ACC_INTERFACE, ACC_ABSTRACT
 *                           ↑ ACC_INTERFACE + ACC_ABSTRACT 두 플래그 모두 설정
 *
 * [2] 메서드 종류별 바이트코드
 *   abstract 메서드: Code attribute 없음, ACC_ABSTRACT 플래그
 *   default 메서드 : Code attribute 있음, ACC_PUBLIC (ACC_ABSTRACT 없음) - Java 8+
 *   static 메서드  : Code attribute 있음, ACC_PUBLIC, ACC_STATIC - Java 8+
 *
 * [3] 인터페이스 상수
 *   int DEFAULT_COLOR = 0xFFFFFF;
 *   -> 컴파일 후: ACC_PUBLIC, ACC_STATIC, ACC_FINAL
 *   -> abstract class의 static final 필드와 동일한 바이트코드
 *
 * [4] invokeinterface
 *   인터페이스 참조로 메서드 호출 시 invokeinterface 인스트럭션 사용
 *   일반 클래스 참조 -> invokevirtual
 *   static 메서드   -> invokestatic
 *
 * 확인:
 *   ./gradlew inspect -PclassName=Drawable -Pflags="-verbose"
 */
public interface Drawable {

    // 인터페이스 상수 -> ACC_PUBLIC, ACC_STATIC, ACC_FINAL
    // 명시적으로 쓰지 않아도 컴파일러가 자동으로 세 modifier를 추가
    int DEFAULT_COLOR = 0xFFFFFF;
    String VERSION = "1.0";

    // abstract 메서드: Code attribute 없음, ACC_ABSTRACT
    void draw();

    // default 메서드: Code attribute 있음 (Java 8+)
    // abstract class의 concrete 메서드와 바이트코드 구조 동일
    default String drawInfo() {
        return "[Drawable] " + getClass().getSimpleName();
    }

    // default 메서드끼리 서로 호출 가능
    default void printDrawInfo() {
        System.out.println(drawInfo());
    }

    // static 메서드: Code attribute 있음, ACC_STATIC (Java 8+)
    // 인터페이스명으로 직접 호출: Drawable.printDefault()
    // 오버라이드 불가 (클래스에 숨겨짐 - hiding)
    static void printDefault() {
        System.out.println("DEFAULT_COLOR = #" + Integer.toHexString(DEFAULT_COLOR));
    }
}
