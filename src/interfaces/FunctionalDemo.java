package interfaces;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Functional Interface + Lambda -> invokedynamic
 *
 * [invokedynamic (Java 7+, Lambda Java 8+)]
 * 람다는 새로운 클래스를 생성하지 않고 invokedynamic 인스트럭션으로 컴파일됨.
 *
 * 람다 컴파일 과정:
 * 1. 람다 본체 -> private static synthetic 메서드로 추출
 *    (ex: lambda$main$0, lambda$main$1, ...)
 * 2. 호출 지점 -> invokedynamic 인스트럭션
 * 3. 런타임에 LambdaMetafactory.metafactory()가 함수형 인터페이스 구현체 생성
 *    (첫 호출 시 생성, 이후 캐시)
 *
 * [소스 코드 -> 바이트코드]
 *   Transformer upper = s -> s.toUpperCase();
 *   ->
 *   invokedynamic #N, 0  // InvokeDynamic #0:transform:()Linterfaces/FunctionalDemo$Transformer;
 *   Bootstrap: LambdaMetafactory.metafactory
 *
 * [메서드 레퍼런스]
 *   String::trim    -> invokedynamic (인스턴스 메서드 ref)
 *   Math::abs       -> invokedynamic (static 메서드 ref)
 *   String::new     -> invokedynamic (constructor ref)
 *
 * [람다 vs 익명 클래스]
 *   익명 클래스: 컴파일 시 별도 .class 파일 생성 (FunctionalDemo$1.class)
 *   람다:        invokedynamic - 별도 .class 파일 없음 (런타임 동적 생성)
 *
 * 확인:
 *   ./gradlew inspect -PclassName=FunctionalDemo -Pflags="-p -c"
 *   -> lambda$main$N 합성 메서드 확인
 *   -> invokedynamic 인스트럭션 확인
 */
public class FunctionalDemo {

    // @FunctionalInterface: 추상 메서드 정확히 1개 강제 (컴파일 체크)
    // default/static 메서드는 여러 개 허용
    @FunctionalInterface
    interface Transformer {
        String transform(String input);  // 추상 메서드 1개

        // default 메서드는 함수형 인터페이스에 허용
        default Transformer andThen(Transformer after) {
            return s -> after.transform(this.transform(s));
        }
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║     Functional Interface & Lambda        ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── 1. Lambda -> invokedynamic ────────────────────────────────
        System.out.println("── 1. Lambda: invokedynamic ────────────────");
        // 각 람다 -> invokedynamic + 별도 lambda$main$N 합성 메서드
        Transformer upper   = s -> s.toUpperCase();             // lambda$main$0
        Transformer lower   = s -> s.toLowerCase();             // lambda$main$1
        Transformer exclaim = s -> s + "!";                     // lambda$main$2
        Transformer trim    = String::trim;                     // 메서드 레퍼런스

        System.out.println("  upper(\"hello\")   = " + upper.transform("hello"));
        System.out.println("  lower(\"WORLD\")   = " + lower.transform("WORLD"));
        System.out.println("  exclaim(\"hi\")    = " + exclaim.transform("hi"));
        System.out.println("  trim(\" hello \") = " + trim.transform(" hello "));
        System.out.println();

        // ── 2. default 메서드로 함수 합성 ────────────────────────────
        System.out.println("── 2. Default 메서드 함수 합성 ─────────────");
        Transformer pipeline = upper.andThen(exclaim).andThen(trim);
        System.out.println("  upper -> exclaim -> trim: " + pipeline.transform(" hi "));
        System.out.println();

        // ── 3. java.util.function 표준 인터페이스 ────────────────────
        System.out.println("── 3. 표준 함수형 인터페이스 ───────────────");
        Function<Integer, Integer> doubler  = x -> x * 2;
        Predicate<Integer>         isEven   = x -> x % 2 == 0;
        Consumer<String>           printer  = System.out::println;  // 메서드 레퍼런스
        Supplier<List<Integer>>    supplier = ArrayList::new;       // 생성자 레퍼런스

        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
        List<Integer> result  = numbers.stream()
            .filter(isEven)
            .map(doubler)
            .collect(Collectors.toList());
        System.out.print("  짝수만 2배: ");
        printer.accept(result.toString());

        List<Integer> newList = supplier.get();
        newList.add(42);
        System.out.println("  ArrayList::new supplier: " + newList);
        System.out.println();

        // ── 4. 람다 vs 익명 클래스 비교 ──────────────────────────────
        System.out.println("── 4. Lambda vs 익명 클래스 ────────────────");

        // 익명 클래스: 컴파일 시 FunctionalDemo$1.class 생성
        Transformer anonClass = new Transformer() {
            @Override
            public String transform(String input) {
                return "[anon] " + input;
            }
        };

        // 람다: invokedynamic, 별도 .class 파일 없음
        Transformer lambda = input -> "[lambda] " + input;

        System.out.println("  익명 클래스 getClass(): " + anonClass.getClass());
        System.out.println("  람다        getClass(): " + lambda.getClass());
        System.out.println("  익명 클래스 결과: " + anonClass.transform("test"));
        System.out.println("  람다        결과: " + lambda.transform("test"));
        System.out.println();

        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./gradlew inspect -PclassName=FunctionalDemo -Pflags=\"-p -c\"");
        System.out.println("  -> invokedynamic 인스트럭션 확인");
        System.out.println("  -> lambda$main$N 합성 메서드 확인");
    }
}
