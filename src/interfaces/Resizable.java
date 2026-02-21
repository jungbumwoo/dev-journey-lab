package interfaces;

/**
 * 두 번째 인터페이스 - 다중 구현 및 Diamond Problem 시연용
 *
 * [Diamond Problem]
 * 두 인터페이스가 같은 시그니처의 default 메서드를 가질 경우:
 *   interface A { default void info() { ... } }
 *   interface B { default void info() { ... } }
 *   class C implements A, B { }  // 컴파일 에러!
 *   class C implements A, B {
 *     public void info() { A.super.info(); }  // 명시적 오버라이드 필요
 *   }
 *
 * [바이트코드 - diamond 해결 시]
 *   A.super.info() 호출 ->
 *   invokespecial A.info:()V  (invokeinterface가 아님!)
 *   -> super interface의 default 메서드를 직접 호출
 *
 * 확인:
 *   ./gradlew inspect -PclassName=Widget -Pflags="-c"
 *   -> invokespecial로 super interface 호출하는 부분 확인
 */
public interface Resizable {

    void resize(double factor);

    // Drawable.drawInfo()와 같은 이름 -> Widget에서 diamond 해결 필요
    default String drawInfo() {
        return "[Resizable] " + getClass().getSimpleName();
    }

    default void doubleSize() {
        resize(2.0);
    }

    default void halfSize() {
        resize(0.5);
    }
}
