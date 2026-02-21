package generics;

/**
 * Generic Class - Box<T>
 *
 * [Type Erasure 핵심]
 * 컴파일 후 T는 Object로 교체됨 (상한이 없는 경우)
 *
 * 소스 코드:          컴파일 후 바이트코드 (erasure):
 *   Box<String>     ->  Box (raw type과 동일)
 *   T get()         ->  Object get()
 *   void set(T v)   ->  void set(Object v)
 *
 * [Signature attribute]
 * javap -verbose Box.class 의 출력:
 *   Signature: #N  // <T:Ljava/lang/Object;>Ljava/lang/Object;
 *   -> 컴파일러가 제네릭 정보를 Signature 속성에 보존
 *   -> 런타임 리플렉션이나 IDE에서 제네릭 타입 정보를 읽을 때 사용
 *
 * [호출 측 checkcast]
 * Box<String> box = new Box<>("hello");
 * String s = box.get();  // 컴파일 후:
 *   invokevirtual Box.get:()Ljava/lang/Object;
 *   checkcast String        <- 컴파일러가 자동 삽입!
 *
 * 확인: ./scripts/inspect.sh Box -verbose
 */
public class Box<T> {

    private T value;  // 바이트코드: private java.lang.Object value;

    public Box(T value) {
        this.value = value;
    }

    // 바이트코드: public java.lang.Object get()
    public T get() {
        return value;
    }

    // 바이트코드: public void set(java.lang.Object)
    public void set(T value) {
        this.value = value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return "Box[" + value + "]";
    }
}
