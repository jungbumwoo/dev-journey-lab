package nested;

/**
 * Nested class 학습용 Outer 클래스.
 *
 * 두 종류의 nested class를 한 곳에 모아 비교합니다.
 *
 *   [Inner (non-static)]
 *     - 컴파일러가 합성 필드 this$0 (Outer 참조)를 자동으로 추가
 *     - constructor 첫 파라미터에 enclosing Outer 인스턴스를 받음
 *     - 생성: outer.new Inner(...)
 *     - Outer의 private 멤버까지 접근 가능 (Outer.this 로 명시적 접근도 가능)
 *     - Outer 인스턴스가 살아있어야 의미가 있음 -> Inner 인스턴스는 Outer를 GC 못 하게 막음
 *
 *   [StaticNested]
 *     - this$0 없음. 사실상 namespace만 Outer 안에 있는 top-level 클래스
 *     - 생성: new Outer.StaticNested(...)
 *     - Outer의 instance 멤버에 직접 접근 불가 (static 멤버만 가능)
 *     - Outer 인스턴스 수명과 완전히 독립
 *
 * 바이트코드 확인:
 *   ./gradlew inspect -PclassName=Outer\$Inner          -Pflags="-p -verbose"
 *   ./gradlew inspect -PclassName=Outer\$StaticNested   -Pflags="-p -verbose"
 */
public class Outer {

    /** 지금까지 만들어진 Outer 인스턴스 총 개수 (class-level, 모든 Outer가 공유) */
    static int outerInstanceCount = 0;

    final int id;
    final String name;

    public Outer(String name) {
        this.id = ++outerInstanceCount;
        this.name = name;
        System.out.println("    [Outer ctor]         id=" + id + " name=" + name);
    }

    /** private 메서드 - Inner는 접근 가능, 외부 클래스는 불가 */
    private String greet() {
        return "hello from Outer #" + id + "(" + name + ")";
    }

    // ─────────────────────────────────────────────────────────────
    // 1) Non-static Inner class
    //    - 합성 필드 this$0: Outer 인스턴스 참조
    //    - Outer.this 키워드로 enclosing 인스턴스에 명시 접근 가능
    // ─────────────────────────────────────────────────────────────
    public class Inner {
        /** Inner 인스턴스 총 개수. inner class는 Java 16부터 static 멤버 허용. */
        static int innerInstanceCount = 0;

        final int id;
        final String label;

        public Inner(String label) {
            this.id = ++innerInstanceCount;
            this.label = label;
            // Outer.this 로 enclosing Outer 명시
            System.out.println("    [Inner ctor]         id=" + id
                + " label=" + label
                + " (enclosing Outer #" + Outer.this.id + ")");
        }

        /** Outer의 private greet() 까지 접근 가능 */
        public String describe() {
            return "Inner #" + id + "[" + label + "] in " + greet();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2) Static nested class
    //    - this$0 없음. Outer 인스턴스와 무관
    // ─────────────────────────────────────────────────────────────
    public static class StaticNested {
        static int staticNestedCount = 0;

        final int id;
        final String label;

        public StaticNested(String label) {
            this.id = ++staticNestedCount;
            this.label = label;
            System.out.println("    [StaticNested ctor]  id=" + id + " label=" + label);
        }

        public String describe() {
            // Outer.this 사용 불가. instance 멤버 접근 불가.
            // 가능한 것은 Outer.outerInstanceCount 같은 static 멤버 뿐.
            return "StaticNested #" + id + "[" + label + "]"
                + " (총 Outer 인스턴스=" + Outer.outerInstanceCount + ")";
        }
    }
}
