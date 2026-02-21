package generics;

import java.lang.reflect.Method;
import java.util.StringJoiner;

/**
 * Bridge Method (브리지 메서드) 분석
 *
 * 제네릭 오버라이딩 시 Type Erasure 이후에도 다형성을 유지하기 위해
 * 컴파일러가 자동으로 합성(synthetic) bridge method를 생성한다.
 *
 * [생성 시나리오]
 * 1. 제네릭 인터페이스/클래스를 구체 타입으로 구현/상속할 때
 * 2. 공변 반환 타입(covariant return type)이 있을 때
 *
 * [시나리오 1: 제네릭 인터페이스 구현]
 *   interface Transformer<T> { T transform(T input); }
 *   Erasure 후: Object transform(Object input)
 *
 *   class UpperCaseTransformer implements Transformer<String> {
 *     public String transform(String input) { ... }  // 구체 메서드
 *   }
 *
 *   컴파일러가 자동 생성 (bridge):
 *     public Object transform(Object input) {
 *       return transform((String) input);  // 구체 메서드 위임
 *     }
 *
 * [시나리오 2: 공변 반환 타입]
 *   class Animal { public Animal create() { ... } }
 *   class Dog extends Animal {
 *     @Override public Dog create() { ... }  // 공변 반환
 *   }
 *   컴파일러가 자동 생성 (bridge):
 *     public Animal create() { return create(); }  // Dog.create() 위임
 *
 * [바이트코드 플래그]
 *   브리지 메서드: ACC_BRIDGE | ACC_SYNTHETIC 플래그 설정
 *   javap -p -verbose UpperCaseTransformer.class 에서 확인 가능
 *
 * 실행: ./scripts/run.sh bridge
 * 바이트코드: ./scripts/inspect.sh UpperCaseTransformer -p
 */
public class BridgeMethodDemo {

    // ── Case 1: 제네릭 인터페이스 구현 ─────────────────────────────

    interface Transformer<T> {
        T transform(T input);
    }

    // Erasure 전: String transform(String input)
    // Erasure 후: Transformer의 Object transform(Object input) 도 구현해야 함
    // -> 컴파일러가 bridge method 자동 생성
    static class UpperCaseTransformer implements Transformer<String> {
        @Override
        public String transform(String input) {
            return input.toUpperCase();
        }
    }

    // ── Case 2: 공변 반환 타입 ──────────────────────────────────────

    static class Animal {
        public Animal create() {
            return new Animal();
        }
        @Override public String toString() { return "Animal"; }
    }

    static class Dog extends Animal {
        // 공변 반환 타입: Dog create() -> Animal create() 브리지 자동 생성
        @Override
        public Dog create() {
            return new Dog();
        }
        @Override public String toString() { return "Dog"; }
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║          Bridge Method Demo              ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── Case 1 시연 ────────────────────────────────────────────
        System.out.println("── 1. 제네릭 인터페이스 구현 ───────────────");
        UpperCaseTransformer transformer = new UpperCaseTransformer();

        // 구체 타입으로 직접 호출: String transform(String) 호출
        System.out.println("직접 호출 (String):     " + transformer.transform("hello"));

        // 인터페이스로 호출: erasure 후 Object transform(Object) 호출
        // -> bridge method -> String transform(String) 위임
        Transformer<String> t = transformer;
        System.out.println("인터페이스 통해 호출:   " + t.transform("world"));
        System.out.println();

        System.out.println("UpperCaseTransformer 메서드 목록 (bridge 포함):");
        printMethods(UpperCaseTransformer.class);
        System.out.println();

        // ── Case 2 시연 ────────────────────────────────────────────
        System.out.println("── 2. 공변 반환 타입 ───────────────────────");
        Dog dog = new Dog();

        // 컴파일 타임 타입 Dog -> Dog create() 직접 호출
        Dog d1 = dog.create();
        System.out.println("dog.create() = " + d1 + " (" + d1.getClass().getSimpleName() + ")");

        // 컴파일 타임 타입 Animal -> bridge method Animal create() 호출
        // bridge method가 Dog create()를 호출하고 Dog 반환
        Animal a = dog;
        Animal d2 = a.create();
        System.out.println("((Animal)dog).create() = " + d2 + " (" + d2.getClass().getSimpleName() + ")");
        System.out.println();

        System.out.println("Dog 메서드 목록 (bridge 포함):");
        printMethods(Dog.class);

        System.out.println();
        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./scripts/inspect.sh UpperCaseTransformer -p");
        System.out.println("  ./scripts/inspect.sh Dog -p");
        System.out.println("  -> bridge method의 ACC_BRIDGE, ACC_SYNTHETIC 플래그 확인");
    }

    static void printMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.printf("  %-35s bridge=%-5b synthetic=%b%n",
                m.getReturnType().getSimpleName() + " " + m.getName() + paramStr(m),
                m.isBridge(),
                m.isSynthetic());
        }
    }

    static String paramStr(Method m) {
        StringJoiner sj = new StringJoiner(", ", "(", ")");
        for (Class<?> p : m.getParameterTypes()) sj.add(p.getSimpleName());
        return sj.toString();
    }
}
