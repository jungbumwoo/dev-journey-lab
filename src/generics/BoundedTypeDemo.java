package generics;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 제네릭 타입 경계 & PECS 원칙
 *
 * [Bounded Type Parameters와 Erasure]
 *   <T>                    -> Object
 *   <T extends Comparable<T>> -> Comparable  (upper bound로 erasure)
 *   <T extends Foo & Bar>  -> Foo            (첫 번째 bound로 erasure)
 *
 * [PECS: Producer Extends, Consumer Super]
 *   ? extends T : 읽기(get)만 가능 -> 데이터를 '생산'하는 컬렉션
 *   ? super T   : 쓰기(add)만 가능 -> 데이터를 '소비'하는 컬렉션
 *
 * [공변성 (Covariance) 문제]
 *   Integer는 Number의 서브타입이지만
 *   List<Integer>는 List<Number>의 서브타입이 아님!
 *   -> 와일드카드(?)로 해결
 *
 * 실행: ./scripts/run.sh bounded
 * 바이트코드: ./scripts/inspect.sh BoundedTypeDemo -c
 */
public class BoundedTypeDemo {

    // <T extends Comparable<T>> -> erasure 후 Comparable로 교체
    // 바이트코드: public static java.lang.Comparable max(java.util.List)
    public static <T extends Comparable<T>> T max(List<T> list) {
        T result = list.get(0);
        for (T item : list) {
            if (item.compareTo(result) > 0) result = item;
        }
        return result;
    }

    // ? extends Number: Number나 하위 타입 List를 읽기 전용으로 받음
    // Integer, Double, Long 등 모두 가능
    public static double sum(List<? extends Number> numbers) {
        double total = 0;
        for (Number n : numbers) total += n.doubleValue();
        // numbers.add(1.0);  // 컴파일 에러! ? extends -> 쓰기 불가
        return total;
    }

    // ? super Integer: Integer 또는 상위 타입 List에 쓰기 가능
    // List<Integer>, List<Number>, List<Object> 모두 수용
    public static void fillWithSequence(List<? super Integer> list, int count) {
        for (int i = 1; i <= count; i++) list.add(i);
        // Integer n = list.get(0);  // 컴파일 에러! ? super -> 특정 타입 읽기 불가 (Object는 가능)
    }

    // 복합 bounded: 읽고 필터링해서 반환
    public static <T extends Comparable<T>> List<T> filterAbove(List<T> list, T threshold) {
        List<T> result = new ArrayList<>();
        for (T item : list) {
            if (item.compareTo(threshold) >= 0) result.add(item);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      Bounded Type Parameters & PECS      ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── 1. Upper Bounded <T extends Comparable<T>> ──────────────
        System.out.println("── 1. <T extends Comparable<T>> ────────────");
        System.out.println("  erasure 후 파라미터 타입이 Comparable로 변경됨");
        List<Integer> ints = Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6);
        List<String>  strs = Arrays.asList("banana", "apple", "cherry");

        System.out.println("  max(ints) = " + max(ints));
        System.out.println("  max(strs) = " + max(strs));
        System.out.println();

        // ── 2. PECS - Producer Extends ──────────────────────────────
        System.out.println("── 2. PECS: Producer Extends (? extends Number) ──");
        System.out.println("  읽기(get)만 가능, 쓰기(add) 불가");
        List<Integer> intList    = Arrays.asList(1, 2, 3);
        List<Double>  doubleList = Arrays.asList(1.5, 2.5, 3.5);
        List<Long>    longList   = Arrays.asList(100L, 200L, 300L);

        System.out.println("  sum(intList)    = " + sum(intList));
        System.out.println("  sum(doubleList) = " + sum(doubleList));
        System.out.println("  sum(longList)   = " + sum(longList));
        System.out.println();

        // ── 공변성 문제 시연 ─────────────────────────────────────────
        System.out.println("── 3. 공변성 문제: List<Integer> ≠ List<Number> ──");
        // List<Number> fromInts = intList;  // 컴파일 에러!
        // -> List<? extends Number>로 해결
        List<? extends Number> covariant = intList;  // OK
        System.out.println("  List<? extends Number> = intList -> OK");
        // covariant.add(1);  // 컴파일 에러! 쓰기 불가
        System.out.println("  covariant.add(1) -> 컴파일 에러 (주석 처리됨)");
        System.out.println();

        // ── 3. PECS - Consumer Super ─────────────────────────────────
        System.out.println("── 4. PECS: Consumer Super (? super Integer) ──");
        System.out.println("  쓰기(add)는 가능, 특정 타입 읽기 불가");
        List<Integer> intDest  = new ArrayList<>();
        List<Number>  numDest  = new ArrayList<>();
        List<Object>  objDest  = new ArrayList<>();

        fillWithSequence(intDest, 3);
        fillWithSequence(numDest, 3);
        fillWithSequence(objDest, 3);

        System.out.println("  List<Integer> = " + intDest);
        System.out.println("  List<Number>  = " + numDest);
        System.out.println("  List<Object>  = " + objDest);
        // fillWithSequence(new ArrayList<Double>(), 3);  // 컴파일 에러! Double은 Integer의 super 아님
        System.out.println();

        // ── 4. filter 사용 예시 ──────────────────────────────────────
        System.out.println("── 5. filterAbove: <T extends Comparable<T>> ──");
        List<Integer> filtered = filterAbove(ints, 5);
        System.out.println("  filterAbove([3,1,4,1,5,9,2,6], 5) = " + filtered);
        System.out.println();

        // ── 5. Erasure된 시그니처 확인 ──────────────────────────────
        System.out.println("── 6. Erasure 결과: 메서드 파라미터 타입 ───");
        for (Method m : BoundedTypeDemo.class.getDeclaredMethods()) {
            if (m.getTypeParameters().length > 0 && m.getParameterCount() > 0) {
                System.out.printf("  %-15s erased 파라미터[0] = %s%n",
                    m.getName() + "()",
                    m.getParameterTypes()[0].getSimpleName());
            }
        }

        System.out.println();
        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./scripts/inspect.sh BoundedTypeDemo -c");
        System.out.println("  -> max() 메서드가 Comparable 파라미터로 컴파일됨 확인");
    }
}
