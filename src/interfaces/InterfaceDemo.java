package interfaces;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;

/**
 * Interface 내부 동작 종합 데모
 *
 * 학습 포인트:
 *   1. Interface 바이트코드 플래그 (ACC_INTERFACE + ACC_ABSTRACT)
 *   2. abstract / default / static 메서드 구분
 *   3. 인터페이스 상수 (ACC_STATIC + ACC_FINAL)
 *   4. invokeinterface vs invokevirtual
 *   5. Diamond Problem과 invokespecial 해결
 *   6. Lambda -> invokedynamic
 *
 * 실행: ./gradlew runInterface
 * 바이트코드:
 *   ./gradlew inspect -PclassName=Drawable -Pflags="-verbose"  # 인터페이스 플래그
 *   ./gradlew inspect -PclassName=Widget   -Pflags="-c"        # invokespecial diamond
 *   ./gradlew inspect -PclassName=InterfaceDemo -Pflags="-c"   # invokeinterface vs invokevirtual
 */
public class InterfaceDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║         Interface Internals              ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── 1. Interface 플래그 & 메서드 분석 ────────────────────────
        System.out.println("── 1. Drawable 인터페이스 분석 (Reflection) ─");
        inspectInterface(Drawable.class);
        System.out.println();

        // ── 2. 다중 인터페이스 구현 + Diamond 해결 ───────────────────
        System.out.println("── 2. 다중 인터페이스 + Diamond Problem ─────");
        Widget w = new Widget("button", 100, 50);
        w.draw();
        // drawInfo()는 Drawable.super와 Resizable.super 모두 호출
        // 바이트코드: invokespecial Drawable.drawInfo + invokespecial Resizable.drawInfo
        System.out.println("  drawInfo(): " + w.drawInfo());
        System.out.println();

        // ── 3. invokeinterface vs invokevirtual ──────────────────────
        System.out.println("── 3. invokeinterface vs invokevirtual ──────");
        System.out.println("  (javap -c InterfaceDemo.class 에서 확인)");

        // 인터페이스 참조 -> invokeinterface
        Drawable  d = w;
        Resizable r = w;
        d.draw();    // invokeinterface
        r.resize(1.5); // invokeinterface

        // 클래스 참조 -> invokevirtual
        w.draw();    // invokevirtual
        w.resize(1.0); // invokevirtual

        System.out.println();

        // ── 4. default 메서드 & static 메서드 ───────────────────────
        System.out.println("── 4. Default / Static 메서드 ───────────────");
        System.out.println("  default drawInfo(): " + w.drawInfo());
        System.out.println("  default doubleSize():");
        w.doubleSize();           // Resizable의 default 메서드 -> invokevirtual (구체 클래스 없으면 인터페이스 default 실행)
        Drawable.printDefault();  // static 메서드 -> invokestatic
        System.out.println();

        // ── 5. instanceof + 인터페이스 ───────────────────────────────
        System.out.println("── 5. instanceof ────────────────────────────");
        System.out.println("  w instanceof Drawable   : " + (w instanceof Drawable));
        System.out.println("  w instanceof Resizable  : " + (w instanceof Resizable));
        System.out.println("  w.getClass().getInterfaces():");
        for (Class<?> iface : w.getClass().getInterfaces()) {
            System.out.println("    " + iface.getName());
        }
        System.out.println();

        // ── 6. Abstract Class vs Interface 비교 ─────────────────────
        System.out.println("── 6. Abstract Class vs Interface 비교 ──────");
        printComparison();
        System.out.println();

        // ── 7. Functional Interface & Lambda ─────────────────────────
        System.out.println("── 7. Functional Interface & Lambda ─────────");
        FunctionalDemo.main(args);
    }

    static void inspectInterface(Class<?> iface) throws Exception {
        int mod = iface.getModifiers();
        System.out.println("  인터페이스  : " + iface.getName());
        System.out.println("  isInterface : " + iface.isInterface());
        System.out.println("  isAbstract  : " + Modifier.isAbstract(mod));
        System.out.println("  메서드 :");
        for (Method m : iface.getDeclaredMethods()) {
            int mMod = m.getModifiers();
            String kind;
            if (Modifier.isStatic(mMod))    kind = "static ";
            else if (m.isDefault())          kind = "default";
            else                             kind = "abstract";
            System.out.printf("    [%-8s] %-20s  Code: %s%n",
                kind, m.getName() + "()",
                (kind.equals("abstract") ? "없음" : "있음"));
        }
        System.out.println("  상수 (static final 필드) :");
        for (Field f : iface.getDeclaredFields()) {
            System.out.printf("    %-16s = %s  [%s]%n",
                f.getName(), f.get(null),
                Modifier.toString(f.getModifiers()));
        }
    }

    static void printComparison() {
        String fmt = "  %-28s %-20s %-20s%n";
        System.out.printf(fmt, "항목", "Abstract Class", "Interface");
        System.out.printf(fmt, "─".repeat(27), "─".repeat(19), "─".repeat(19));
        System.out.printf(fmt, "ACC 플래그", "ACC_ABSTRACT", "ACC_INTERFACE + ACC_ABSTRACT");
        System.out.printf(fmt, "다중 상속/구현", "단일 상속만", "다중 구현 가능");
        System.out.printf(fmt, "인스턴스 필드", "가능", "불가 (상수만)");
        System.out.printf(fmt, "생성자", "가능 (super 호출)", "불가");
        System.out.printf(fmt, "abstract 메서드", "있음", "있음");
        System.out.printf(fmt, "concrete 메서드", "있음", "default 메서드 (Java 8+)");
        System.out.printf(fmt, "static 메서드", "있음", "있음 (Java 8+)");
        System.out.printf(fmt, "메서드 호출", "invokevirtual", "invokeinterface");
        System.out.printf(fmt, "super 호출", "invokespecial", "invokespecial (A.super.m())");
    }
}
