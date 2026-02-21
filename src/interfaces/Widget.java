package interfaces;

/**
 * 다중 인터페이스 구현 - Diamond Problem 해결
 *
 * Drawable과 Resizable 모두 drawInfo()라는 default 메서드를 가짐
 * -> 컴파일 에러: class Widget inherits unrelated defaults
 * -> 명시적 오버라이드로 해결
 *
 * [Diamond 해결 바이트코드]
 * Drawable.super.drawInfo() 호출:
 *   invokespecial interfaces/Drawable.drawInfo:()Ljava/lang/String;
 *   ↑ invokeinterface가 아닌 invokespecial! (super 메서드 직접 지정)
 *
 * [invokeinterface vs invokevirtual 비교]
 *   Drawable d = new Widget(...);
 *   d.draw();    -> invokeinterface  (인터페이스 참조)
 *
 *   Widget w = new Widget(...);
 *   w.draw();    -> invokevirtual    (클래스 참조)
 *
 *   두 인스트럭션 모두 런타임에 실제 타입의 메서드를 호출하지만
 *   invokeinterface는 인터페이스 메서드 탐색 경로가 다름
 *
 * 확인:
 *   ./gradlew inspect -PclassName=Widget -Pflags="-c"
 *   ./gradlew inspect -PclassName=InterfaceDemo -Pflags="-c"
 *   -> d.draw() = invokeinterface, w.draw() = invokevirtual 비교
 */
public class Widget implements Drawable, Resizable {

    private String name;
    private double width, height;

    public Widget(String name, double width, double height) {
        this.name = name;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw() {
        System.out.printf("  Widget[%s] %.0fx%.0f%n", name, width, height);
    }

    @Override
    public void resize(double factor) {
        width *= factor;
        height *= factor;
        System.out.printf("  Widget[%s] resized -> %.0fx%.0f%n", name, width, height);
    }

    // Diamond Problem 해결: 두 인터페이스의 drawInfo() 충돌 -> 명시적 오버라이드
    // A.super.method() 문법: invokespecial로 특정 인터페이스의 default 메서드 호출
    @Override
    public String drawInfo() {
        return Drawable.super.drawInfo() + " | " + Resizable.super.drawInfo();
    }

    public String getName()  { return name; }
    public double getWidth() { return width; }
    public double getHeight(){ return height; }
}
