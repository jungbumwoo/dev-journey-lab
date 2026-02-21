package generics;

import java.lang.reflect.*;
import java.util.*;

/**
 * Type Erasure (타입 소거) 심층 분석
 *
 * Java 제네릭은 컴파일 타임에만 존재하며, 런타임에는 타입 정보가 사라짐.
 * 이를 Type Erasure라고 한다.
 *
 * Erasure 규칙:
 *   <T>                -> Object
 *   <T extends Foo>    -> Foo  (upper bound로 erasure)
 *   List<String>       -> List
 *   Map<K, V>          -> Map
 *
 * [왜 Type Erasure를 사용하나?]
 * Java 5에서 제네릭 도입 시 하위 호환성 유지를 위해 선택.
 * C#의 제네릭과 달리, 런타임에 새로운 클래스를 생성하지 않음.
 *
 * 실행: ./scripts/run.sh erasure
 * 바이트코드: ./scripts/inspect.sh TypeErasureDemo -c
 */
public class TypeErasureDemo {

    // 필드에 generic type 정보 -> 바이트코드 Signature attribute에 보존
    private List<String>              stringList  = new ArrayList<>();
    private List<Integer>             intList     = new ArrayList<>();
    private Map<String, List<Integer>> complexMap = new HashMap<>();
    private Box<Double>               doubleBox   = new Box<>(3.14);

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║           Type Erasure Demo              ║");
        System.out.println("╚══════════════════════════════════════════╝");
        System.out.println();

        // ── 1. 런타임에 제네릭 타입 구분 불가 ──────────────────────
        System.out.println("── 1. List<String> vs List<Integer>: 런타임 동일 ──");
        List<String>  strings  = new ArrayList<>();
        List<Integer> integers = new ArrayList<>();

        System.out.println("strings.getClass()  = " + strings.getClass());
        System.out.println("integers.getClass() = " + integers.getClass());
        System.out.println("동일 클래스?         = " + (strings.getClass() == integers.getClass()));
        System.out.println();

        // ── 2. instanceof는 제네릭 타입 지정 불가 ───────────────────
        System.out.println("── 2. instanceof + 제네릭 ──────────────────");
        // strings instanceof List<String>  // 컴파일 에러!
        System.out.println("strings instanceof List (raw) = " + (strings instanceof List));

        // Java 16+ pattern matching - List<?>만 가능
        if (strings instanceof List<?> list) {
            System.out.println("List<?> 패턴 매칭 성공, 크기 = " + list.size());
        }
        System.out.println();

        // ── 3. Reflection으로 Signature attribute 읽기 ──────────────
        System.out.println("── 3. Field Signature attribute (바이트코드 보존 정보) ──");
        System.out.println("  (javap -verbose에서 확인 가능한 Signature 속성)");
        TypeErasureDemo demo = new TypeErasureDemo();

        String[] fields = {"stringList", "intList", "complexMap", "doubleBox"};
        for (String name : fields) {
            Field f = TypeErasureDemo.class.getDeclaredField(name);
            System.out.println("  " + name + ":");
            System.out.println("    getType()        = " + f.getType().getSimpleName()
                + "  <- erasure 결과 (런타임 타입)");
            System.out.println("    getGenericType() = " + f.getGenericType()
                + "  <- Signature에서 읽음");
            if (f.getGenericType() instanceof ParameterizedType pt) {
                System.out.println("    rawType          = " + pt.getRawType());
                System.out.println("    typeArgs         = " + Arrays.toString(pt.getActualTypeArguments()));
            }
        }
        System.out.println();

        // ── 4. checkcast: raw type 사용 시 heap pollution ───────────
        System.out.println("── 4. checkcast 인스트럭션 시연 ────────────");
        System.out.println("  Box<String>에 Integer를 raw type으로 삽입:");
        Box<String> stringBox = new Box<>("hello");

        @SuppressWarnings("unchecked")
        Box rawBox = stringBox;  // raw type (컴파일 경고)
        rawBox.set(42);          // 런타임에 타입 체크 없음

        System.out.println("  rawBox.set(42) 성공 - 아직 ClassCastException 없음");
        System.out.println("  stringBox.get() 시도...");
        try {
            // 바이트코드:
            //   invokevirtual Box.get:()Ljava/lang/Object;
            //   checkcast     #N // class java/lang/String  <- 여기서 에러!
            String s = stringBox.get();
            System.out.println("  결과: " + s);
        } catch (ClassCastException e) {
            System.out.println("  ClassCastException 발생!");
            System.out.println("  -> get()의 checkcast 인스트럭션에서 Integer != String");
        }
        System.out.println();

        // ── 5. 제네릭 메서드 타입 파라미터 ─────────────────────────
        System.out.println("── 5. 제네릭 메서드 TypeVariable ───────────");
        for (Method m : TypeErasureDemo.class.getDeclaredMethods()) {
            TypeVariable<?>[] tps = m.getTypeParameters();
            if (tps.length > 0) {
                System.out.println("  Method: " + m.getName());
                for (TypeVariable<?> tp : tps) {
                    System.out.println("    TypeParam: " + tp.getName()
                        + "  bounds=" + Arrays.toString(tp.getBounds()));
                }
                System.out.println("    erased param[0]: "
                    + m.getParameterTypes()[0].getSimpleName());
            }
        }

        System.out.println();
        System.out.println("━━ 바이트코드 확인 ━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  ./scripts/inspect.sh Box -verbose     # Signature attribute 확인");
        System.out.println("  ./scripts/inspect.sh TypeErasureDemo  # checkcast 확인");
    }

    // <T> -> Object로 erasure
    public <T> T identity(T value) {
        return value;
    }

    // <T extends Comparable<T>> -> Comparable으로 erasure
    public <T extends Comparable<T>> T findMax(List<T> list) {
        return list.stream().max(Comparator.naturalOrder()).orElseThrow();
    }
}
