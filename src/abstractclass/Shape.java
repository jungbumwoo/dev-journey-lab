package abstractclass;

/**
 * Abstract Class - 바이트코드 분석 포인트
 *
 * [1] class 선언
 *   javap -verbose Shape.class 출력:
 *     flags: (0x0421) ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT
 *   -> abstract class는 ACC_ABSTRACT 플래그를 가짐
 *   -> 인터페이스와 달리 ACC_INTERFACE는 없음
 *
 * [2] abstract 메서드
 *   일반 메서드: Code attribute (실제 바이트코드 존재)
 *   abstract 메서드: Code attribute 없음, ACC_ABSTRACT 플래그만 존재
 *
 *   abstract 메서드 바이트코드:
 *     public abstract double area();
 *       descriptor: ()D
 *       flags: (0x0401) ACC_PUBLIC, ACC_ABSTRACT
 *       <- Code 속성이 없음!
 *
 * [3] constructor
 *   abstract class도 constructor를 가짐
 *   서브클래스 생성자에서 super() 호출 시 -> invokespecial
 *
 * 확인 명령어:
 *   javap -verbose out/abstractclass/Shape.class
 */
public abstract class Shape {

    protected String color;
    protected double x, y;

    // abstract class도 constructor를 가짐
    // 서브클래스가 super(color, x, y)로 호출 -> invokespecial
    public Shape(String color, double x, double y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }

    // abstract method: Code attribute 없음, ACC_ABSTRACT만 존재
    public abstract double area();

    public abstract double perimeter();

    public abstract String shapeName();

    // concrete method: Code attribute 있음, ACC_ABSTRACT 없음
    // 런타임에 invokevirtual로 호출 -> vtable 통해 실제 구현 호출
    public void printInfo() {
        System.out.printf("[%s] color=%s, area=%.2f, perimeter=%.2f%n",
            shapeName(), color, area(), perimeter());
    }

    // Template Method Pattern: abstract 메서드로 알고리즘 골격 정의
    public String summary() {
        return String.format("%s at (%.1f,%.1f): area=%.2f",
            shapeName(), x, y, area());
    }

    public String getColor() { return color; }
}
