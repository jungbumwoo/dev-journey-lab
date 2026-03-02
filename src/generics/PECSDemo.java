package generics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class PECSDemo {
    public static void main(String[] args) throws Exception {

        // ** ok **
        List unboundedList = new ArrayList<Integer>();
        List<?> unboundedList2 = new ArrayList<Integer>();

        // ** not ok **
        // Wildcard type '?' cannot be instantiated directly
        // List<?> unboundedList3 = new ArrayList<?>();
        // Required type: List<Object>, Provided: ArrayList <Integer>
        // List<Object> unboundedList4 = new ArrayList<Integer>();
        List<Object> unboundedList4 = Collections.singletonList(new ArrayList<Integer>());

        // ** array 보다 generic + List 를 실무에서 권하는 이유: 타입에러를 runtime에서 겪을 것인가, 컴파일 타임에 미리 확인할 것 인가? **
        // arrays are covariant, which has 2 implications
        // 1.
        Number[] nums = new Number[2];
        nums[0] = Integer.valueOf(1);
        nums[1] = Double.valueOf(2.0);

        // 2.
        // Integer 가 Number의 자식이라면, Integer[]도 Number[] 의 자식으로 간주.
        Integer[] intArr = new Integer[5];
        Number[] numArr = intArr; // Ok

        // 컴파일 시점에는 에러 없지만 런타임에 아래 에러 발생
        // Storing element of type 'java.lang.Double' to array of 'java.lang.Integer' elements will produce 'ArrayStoreException'
        // numArr[0] = 1.23;

        // generics are invariant.
        ArrayList<Integer> intArrList = new ArrayList<>();
        // ArrayList<Number> numArrList = intArrList; // compile error.

        // With wildcards, it’s possible for generics to support covariance and contravariance.
        ArrayList<Integer> intArrListWithWildCard = new ArrayList<>();
        ArrayList<? super Integer> numArrList = intArrListWithWildCard;

        // ****

        // ** Producer-Extends (Read-Only) **
        final List<? extends Number> numberList = DoubleStream.of(1.23, 2.34, 3.45)
                .boxed().collect(Collectors.toList());

//         numberList.add(new Integer(1)); //Cannot add integer to this list, even though integer is a subtype of number.
        numberList.add(null); //Cannot add anything to numberList other than null
        final Number number = numberList.get(0); //Can only get values as Number type
        System.out.println(number.doubleValue());

        // ** Consumer-Super (Write-Only) **
        // Integer만 안전하게 받을 수 produce 되도록 수 있음을 보장.
        final List<? super Integer> integerList = IntStream.of(1, 2, 3).boxed()
                .collect(Collectors.toList());

        final Integer integer = 123;
        integerList.add(integer); // Integers can be added
        final Number num = 321;
        // integerList.add(num);
        // Q. integerList.add(num); <- Number는 Integar super에 해당 하는데 add 왜 안되나?
        // A. X ∈ { Integer, Number, Object }. 어떤 값인지 모르는데 add 할 수 없음
        // ex ArrayList<Integer>인 경우: 여기에 Number(예: 3.14 같은 Double)을 넣는 것은 불가능.
        // ex ArrayList<Number> pr ArrayList<Object >인 경우: 여기에 Integer를 넣는 것은 가능. 고로 Integer만 write 할 수 있다.
        final List<? super Integer> integerArrList = new ArrayList<Object>();
        final Integer integer123 = 123;
        integerArrList.add(integer123);

        // 근데 아래 <? super Number>에 Integer add 는 된다. Integer의 경우 final이라 하위 상속하는게 없지만 Number는 Integer가 상속할 수 있으니.
        final List<? super Number> numberArrList = new ArrayList<Object>();
        final Integer integerNumber = 123;
        numberArrList.add(integerNumber);
        // numberArrList.add(new Object()); <- 는 안됨. Number만 add 가능.

        // Q. 아래 read 들은 왜 안되고 write only 상태가 되나? 어차피 Integer를 넣었으니 꺼낼 때도 당연히 Integer로 형변환해서 주면 되는 거 아냐?
        // 변수에 타입 지정을 해줬는데 컴파일러에서 타입캐스팅 해주면 되는 것 아닌가?
        // final Integer integer1 = integerList.get(0);
        // final Number integer1 = integerList.get(0); //Cannot get values as Integer or Number
        // final Object integer = integerList.get(0); //Can only get value in an  Object reference

        // A. 맞는 말인데, 런타임에 다른 타입 값이 오염되었을 경우 runtime error가 발생하기 때문.
    }

    // PECS 예제 코드. CollectionUtils의 copy method
    // src(Producer): 데이터를 꺼내서 제공하므로 extends
    // dest(Consumer): 데이터를 받아서 저장하므로 super
    public static <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) {
            dest.add(item); // src에서 꺼낸 T를 dest에 소비(저장)
        }
    }
}
