package abstractclass;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Abstract Class 내부 동작 확인 데모
 *
 * 핵심 JVM 개념:
 *
 * [invokevirtual vs invokespecial]
 *   invokevirtual : 런타임 타입 기반으로 메서드 결정 (다형성)
 *                   -> shape.area() 같은 일반 인스턴스 메서드 호출
 *   invokespecial : 컴파일 타임에 메서드 결정 (정적 바인딩)
 *                   -> super(), private 메서드, constructor 호출
 *
 * [Virtual Method Table (vtable)]
 *   JVM은 각 클래스마다 vtable을 유지
 *   Circle의 vtable: area() -> Circle.area, perimeter() -> Circle.perimeter, ...
 *   invokevirtual 시 객체의 실제 타입의 vtable에서 메서드 주소를 조회
 *
 * 실행: ./scripts/run.sh abstract
 * 바이트코드: ./scripts/inspect.sh AbstractClassDemo
 */
public class AbstractClassDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║       Abstract Class Internals           ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── 1. 다형성 (Polymorphism) ────────────────────────────────
        System.out.println("── 1. 다형성 ──────────────────────────────");
        // 컴파일 타임 타입: Shape (인터페이스처럼 사용)
        // 런타임 타입: Circle, Rectangle
        Shape circle = new Circle("red", 0, 0, 5.0);
        Shape rect   = new Rectangle("blue", 1, 2, 4.0, 6.0);

        // area() 호출: invokevirtual -> 런타임에 실제 구현 호출
        circle.printInfo();
        rect.printInfo();
        System.out.println();

        // ── 2. Reflection으로 클래스 내부 확인 ─────────────────────
        System.out.println("── 2. Shape 클래스 분석 (Reflection) ──────");
        inspectClass(Shape.class);
        System.out.println();

        System.out.println("── 3. Circle 클래스 분석 (Reflection) ─────");
        inspectClass(Circle.class);
        System.out.println();

        // ── 3. instanceof ────────────────────────────────────────────
        System.out.println("── 4. instanceof ───────────────────────────");
        // instanceof -> JVM의 checkcast / instanceof 인스트럭션
        System.out.println("circle instanceof Shape     : " + (circle instanceof Shape));
        System.out.println("circle instanceof Circle    : " + (circle instanceof Circle));
        System.out.println("circle instanceof Rectangle : " + (circle instanceof Rectangle));
        System.out.println();

        // ── 4. 런타임 타입 확인 ─────────────────────────────────────
        System.out.println("── 5. 런타임 타입 ──────────────────────────");
        System.out.println("circle.getClass()              : " + circle.getClass());
        System.out.println("circle.getClass().getSuperclass: " + circle.getClass().getSuperclass());
        System.out.println("Shape.class.getSuperclass()    : " + Shape.class.getSuperclass());
        System.out.println("Shape는 abstract class?        : " + Modifier.isAbstract(Shape.class.getModifiers()));
        System.out.println();

        // ── 5. 동적 디스패치 시뮬레이션 ─────────────────────────────
        System.out.println("── 6. 동적 디스패치 (invokevirtual) ────────");
        System.out.println("Shape 배열에 다양한 객체 -> 각각 올바른 area() 호출");
        Shape[] shapes = {
            new Circle("red",   0, 0, 3.0),
            new Rectangle("blue", 0, 0, 4.0, 5.0),
            new Circle("green", 0, 0, 1.5),
        };
        for (Shape s : shapes) {
            System.out.println("  " + s.summary());
        }

        System.out.println();
        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./scripts/inspect.sh Shape    # ACC_ABSTRACT 확인");
        System.out.println("  ./scripts/inspect.sh Circle   # invokespecial super() 확인");
        System.out.println("  ./scripts/inspect.sh AbstractClassDemo -c  # invokevirtual 확인");
    }

    /**
     * Reflection으로 클래스 내부 메서드 정보 출력
     * abstract flag 포함
     */
    static void inspectClass(Class<?> clazz) {
        int mod = clazz.getModifiers();
        System.out.println("  클래스  : " + clazz.getName());
        System.out.println("  abstract: " + Modifier.isAbstract(mod));
        System.out.println("  super   : " + clazz.getSuperclass());
        System.out.println("  메서드  :");
        for (Method m : clazz.getDeclaredMethods()) {
            boolean isAbstract = Modifier.isAbstract(m.getModifiers());
            System.out.printf("    %-20s abstract=%-5b [%s]%n",
                m.getName() + "()",
                isAbstract,
                isAbstract ? "Code 없음" : "Code 있음");
        }
    }
}
