package nested;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Nested class (Inner vs Static Nested) 종합 데모.
 *
 * 학습 포인트:
 *   1. 생성 문법 차이 (outer.new Inner vs new Outer.StaticNested)
 *   2. Outer 멤버 접근 차이 (Inner는 instance까지, StaticNested는 static까지)
 *   3. 여러 번 생성하면 각각 다른 인스턴스인가?
 *      -> 그렇다. == 비교, identityHashCode 로 확인
 *   4. 합성 필드 this$0: Inner에는 있지만 StaticNested에는 없음 (reflection으로 확인)
 *   5. GC 동작:
 *      - Outer에 대한 강한 참조를 끊어도 Inner가 살아있으면 Outer는 회수되지 않음
 *      - StaticNested는 Outer 참조가 없으므로 Outer가 즉시 회수 대상이 됨
 *
 * 실행: ./gradlew runNested
 * 바이트코드 확인: ./gradlew inspect -PclassName=Outer\$Inner -Pflags="-p -verbose"
 */
public class NestedClassDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Nested Class (Inner vs Static)         ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        section1_Instantiation();
        section2_MemberAccess();
        section3_DistinctInstances();
        section4_SyntheticThisField();
        section5_GarbageCollection();

        System.out.println();
        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./gradlew inspect -PclassName=Outer\\$Inner        -Pflags=\"-p -verbose\"");
        System.out.println("  ./gradlew inspect -PclassName=Outer\\$StaticNested -Pflags=\"-p -verbose\"");
        System.out.println("  -> Inner.class 의 <init>(Lnested/Outer;...) 첫 파라미터, this$0 필드 확인");
    }

    // ─────────────────────────────────────────────────────────────
    // Section 1. 생성 문법
    // ─────────────────────────────────────────────────────────────
    static void section1_Instantiation() {
        System.out.println("── 1. 생성 문법 ─────────────────────────────");

        // StaticNested: Outer 인스턴스가 없어도 만들 수 있다.
        Outer.StaticNested sn = new Outer.StaticNested("standalone");
        System.out.println("  " + sn.describe());

        // Inner: 반드시 enclosing Outer 인스턴스가 먼저 있어야 한다.
        Outer outer = new Outer("outer-1");
        Outer.Inner inner = outer.new Inner("from-outer-1");
        System.out.println("  " + inner.describe());
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Section 2. Outer 멤버 접근 차이
    // ─────────────────────────────────────────────────────────────
    static void section2_MemberAccess() {
        System.out.println("── 2. Outer 멤버 접근 ───────────────────────");
        Outer outer = new Outer("outer-access");
        Outer.Inner inner = outer.new Inner("inner-access");
        Outer.StaticNested sn = new Outer.StaticNested("sn-access");

        System.out.println("  Inner -> Outer.private greet()  : OK");
        System.out.println("    " + inner.describe());

        System.out.println("  StaticNested -> Outer.instance  : 불가 (컴파일 에러)");
        System.out.println("  StaticNested -> Outer.static    : OK");
        System.out.println("    " + sn.describe());
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Section 3. 여러 번 new 하면 각각 다른 인스턴스인가?
    // ─────────────────────────────────────────────────────────────
    static void section3_DistinctInstances() {
        System.out.println("── 3. 여러 번 생성 -> 서로 다른 인스턴스 ────");

        Outer outerA = new Outer("A");
        Outer outerB = new Outer("B");

        Outer.Inner inner1 = outerA.new Inner("a-1");
        Outer.Inner inner2 = outerA.new Inner("a-2");   // 같은 outer 에서 두 개
        Outer.Inner inner3 = outerB.new Inner("b-1");   // 다른 outer 에서 하나

        Outer.StaticNested sn1 = new Outer.StaticNested("sn-1");
        Outer.StaticNested sn2 = new Outer.StaticNested("sn-2");

        // == 비교: 모두 다른 객체
        System.out.println();
        System.out.println("  inner1 == inner2 : " + (inner1 == inner2)
            + "   (같은 Outer에서 만들었지만 별개 인스턴스)");
        System.out.println("  inner1 == inner3 : " + (inner1 == inner3));
        System.out.println("  sn1    == sn2    : " + (sn1 == sn2));

        // identityHashCode 로 JVM 관점 동일성 확인
        System.out.println();
        System.out.println("  identityHashCode:");
        System.out.printf("    inner1 = %d%n", System.identityHashCode(inner1));
        System.out.printf("    inner2 = %d%n", System.identityHashCode(inner2));
        System.out.printf("    inner3 = %d%n", System.identityHashCode(inner3));
        System.out.printf("    sn1    = %d%n", System.identityHashCode(sn1));
        System.out.printf("    sn2    = %d%n", System.identityHashCode(sn2));

        // class-level static counter는 모든 인스턴스가 공유
        System.out.println();
        System.out.println("  공유되는 static 카운터:");
        System.out.println("    Outer.outerInstanceCount             = " + Outer.outerInstanceCount);
        System.out.println("    Outer.Inner.innerInstanceCount       = " + Outer.Inner.innerInstanceCount);
        System.out.println("    Outer.StaticNested.staticNestedCount = " + Outer.StaticNested.staticNestedCount);
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Section 4. 합성 필드 this$0 확인 (reflection)
    // ─────────────────────────────────────────────────────────────
    static void section4_SyntheticThisField() throws Exception {
        System.out.println("── 4. 합성 필드 this$0 (Reflection) ─────────");

        System.out.println("  Inner 의 declared fields:");
        for (Field f : Outer.Inner.class.getDeclaredFields()) {
            System.out.printf("    %-12s type=%-20s synthetic=%-5b static=%b%n",
                f.getName(),
                f.getType().getSimpleName(),
                f.isSynthetic(),
                Modifier.isStatic(f.getModifiers()));
        }

        System.out.println("  Inner 의 declared constructors:");
        for (Constructor<?> c : Outer.Inner.class.getDeclaredConstructors()) {
            StringBuilder sb = new StringBuilder("    Inner(");
            Class<?>[] params = c.getParameterTypes();
            for (int i = 0; i < params.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(params[i].getSimpleName());
            }
            sb.append(")  <- 첫 파라미터가 Outer 임을 주목");
            System.out.println(sb);
        }

        System.out.println();
        System.out.println("  StaticNested 의 declared fields:");
        for (Field f : Outer.StaticNested.class.getDeclaredFields()) {
            System.out.printf("    %-12s type=%-20s synthetic=%-5b static=%b%n",
                f.getName(),
                f.getType().getSimpleName(),
                f.isSynthetic(),
                Modifier.isStatic(f.getModifiers()));
        }
        System.out.println("    -> this$0 없음. Outer 인스턴스 참조를 갖지 않음.");
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────────
    // Section 5. GC 동작
    //   WeakReference로 Outer 회수 여부를 추적.
    //   - Inner가 살아있으면 Outer는 회수 안 됨 (this$0이 강한 참조)
    //   - StaticNested는 Outer를 참조하지 않으므로 Outer가 회수 가능
    // ─────────────────────────────────────────────────────────────
    static void section5_GarbageCollection() throws Exception {
        System.out.println("── 5. GC 동작 (WeakReference 추적) ──────────");
        System.out.println();

        // ── Scenario A: Inner 살아있음 -> Outer 회수 불가 ──
        System.out.println("  [A] Inner 가 Outer 를 붙잡고 있는 경우");
        WeakReference<Outer> weakA;
        Outer.Inner aliveInner;
        {
            Outer outer = new Outer("GC-A");
            aliveInner = outer.new Inner("inner-A");
            weakA = new WeakReference<>(outer);
            outer = null; // 강한 참조 제거. 그러나 aliveInner.this$0이 여전히 Outer를 가리킴
        }
        forceGc();
        System.out.println("    Inner 살아있음? " + (aliveInner != null));
        System.out.println("    Outer 회수됨?  " + (weakA.get() == null)
            + "  -> false 기대 (Inner의 this$0이 막고 있음)");

        // 이제 Inner 도 놓아주면 Outer 도 회수 대상
        aliveInner = null;
        forceGc();
        System.out.println("    Inner=null 후 Outer 회수됨? " + (weakA.get() == null)
            + "  -> true 기대");
        System.out.println();

        // ── Scenario B: StaticNested 살아있음 -> Outer 즉시 회수 가능 ──
        System.out.println("  [B] StaticNested 만 살아있는 경우 (Outer 참조 없음)");
        WeakReference<Outer> weakB;
        Outer.StaticNested aliveSn;
        {
            Outer outer = new Outer("GC-B");
            aliveSn = new Outer.StaticNested("sn-B");
            weakB = new WeakReference<>(outer);
            outer = null;
        }
        forceGc();
        System.out.println("    StaticNested 살아있음? " + (aliveSn != null));
        System.out.println("    Outer 회수됨?         " + (weakB.get() == null)
            + "  -> true 기대 (StaticNested는 Outer를 잡지 않음)");
        System.out.println();

        System.out.println("  요약:");
        System.out.println("    Inner -> Outer 강한 참조 보유 -> Outer 의 GC를 막음 (메모리 누수 위험)");
        System.out.println("    StaticNested -> Outer 참조 없음 -> Outer 와 독립적으로 회수됨");
        System.out.println("    교훈: Outer 인스턴스가 필요 없다면 static nested 로 선언하라.");
    }

    /**
     * System.gc()는 힌트일 뿐이지만, 짧은 데모 객체에 대해 WeakReference 회수를 유도하기엔 충분.
     * 여러 번 + 소량 대기로 회수 확률을 높임.
     */
    static void forceGc() throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(50);
        }
    }
}
