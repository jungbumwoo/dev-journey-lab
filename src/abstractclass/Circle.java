package abstractclass;

/**
 * Shape를 상속한 concrete class
 *
 * [바이트코드 포인트]
 *
 * 1. constructor에서 super() 호출
 *    바이트코드:
 *      aload_0
 *      aload_1         (color)
 *      dload_2         (x)
 *      dload 4         (y)
 *      dload 6         (radius)
 *      invokespecial Shape.<init>:(Ljava/lang/String;DD)V   <- super()
 *
 * 2. 오버라이딩된 abstract 메서드들은 Code attribute를 가짐
 *    area():
 *      getstatic Math.PI:D
 *      aload_0
 *      getfield Circle.radius:D
 *      dup2
 *      dmul
 *      dmul
 *      dreturn
 *
 * 확인: javap -c out/abstractclass/Circle.class
 */
public class Circle extends Shape {

    private double radius;

    public Circle(String color, double x, double y, double radius) {
        super(color, x, y);  // invokespecial Shape.<init>
        this.radius = radius;
    }

    @Override
    public double area() {
        return Math.PI * radius * radius;
    }

    @Override
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    @Override
    public String shapeName() {
        return "Circle(r=" + radius + ")";
    }
}

/*
*
해석: L0, L1, L2 (Labels)
의미: 바이트코드 내의 **특정 지점(위치)**을 가리키는 '라벨'입니다.

역할:
점프 문(goto, ifeq 등)이 어디로 이동해야 할지 알려주는 표시
변수의 유효 범위(Scope)를 지정하는 시작과 끝점
소스 코드의 특정 행이 시작되는 지점을 표시 한 것.

LINENUMBER 34 L0
해석: "자바 소스 파일(.java)의 34번째 줄에 해당하는 실행 코드가 여기서부터(L0) 시작된다"는 뜻

존재 이유:
StackTrace: 프로그램 실행 중 예외(Exception)가 발생했을 때,
at abstractclass.Circle.<init>(Circle.java:34) 처럼 몇 번째 줄에서 에러가 났는지 정확히 알려줄 수 있는 근거
Debugger: 개발자가 IDE(IntelliJ, Eclipse 등)에서 34행에 브레이크포인트를 걸면, 디버거는 이 정보를 보고 바이트코드의 L0 지점에서 실행을 멈춤

---
*
* ALOAD, DLOAD 무슨 차이?
*
* A (Address/Reference): 객체 참조 (예: ALOAD)
* D (Double): 64비트 실수 (예: DLOAD, DMUL)
* I (Integer): 32비트 정수
* L (Long): 64비트 정수
*
* ---
*
* PUTFIELD : 스택의 값을 필드에 저장
* MAXSTACK / MAXLOCALS: 메서드 실행에 필요한 스택의 최대 깊이와 지역 변수 슬롯 개수가 미리 계산
* JVM이 런타임에 메모리를 효율적으로 관리
*
* ---
*
* 왜 DLOAD 2 다음 DLOAD 3이 아닌 DLOAD 4?
* ALOAD 0
  ALOAD 1
  DLOAD 2
  DLOAD 4
*
* JVM은 메서드가 호출될 때 필요한 변수들을 Slot이라는 단위로 관리. 각 슬롯은 기본적으로 32비트 크기
* 32비트 타입 (int, float, reference 등): 슬롯 1개 사용.
* 64비트 타입 (double, long): 슬롯 2개 사용.
* ALOAD 0      // 슬롯 0 (this) 로드
ALOAD 1      // 슬롯 1 (String color) 로드
DLOAD 2      // 슬롯 2~3 (double x) 로드
DLOAD 4      // 슬롯 4~5 (double y) 로드
*
*
* ---
// class version 65.0 (65)
// access flags 0x21
public class abstractclass/Circle extends abstractclass/Shape {

  // compiled from: Circle.java
  // access flags 0x19
  public final static INNERCLASS java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles Lookup

  // access flags 0x2
  private D radius

  // access flags 0x1
  public <init>(Ljava/lang/String;DDD)V
   L0
    LINENUMBER 34 L0
    ALOAD 0 // 'this' 참조를 스택에 올림
    ALOAD 1 // 첫 번째 인자(color)를 스택에 올림
    DLOAD 2 // 두 번째 인자(x, double)를 올림
    DLOAD 4
    INVOKESPECIAL abstractclass/Shape.<init> (Ljava/lang/String;DD)V
   L1
    LINENUMBER 35 L1
    ALOAD 0
    DLOAD 6
    PUTFIELD abstractclass/Circle.radius : D
   L2
    LINENUMBER 36 L2
    RETURN
    MAXSTACK = 6
    MAXLOCALS = 8

  // access flags 0x1
  public area()D
   L0
    LINENUMBER 40 L0
    LDC 3.141592653589793
    ALOAD 0
    GETFIELD abstractclass/Circle.radius : D
    DMUL
    ALOAD 0
    GETFIELD abstractclass/Circle.radius : D
    DMUL
    DRETURN
    MAXSTACK = 4
    MAXLOCALS = 1

  // access flags 0x1
  public perimeter()D
   L0
    LINENUMBER 45 L0
    LDC 6.283185307179586
    ALOAD 0
    GETFIELD abstractclass/Circle.radius : D
    DMUL
    DRETURN
    MAXSTACK = 4
    MAXLOCALS = 1

  // access flags 0x1
  public shapeName()Ljava/lang/String;
   L0
    LINENUMBER 50 L0
    ALOAD 0
    GETFIELD abstractclass/Circle.radius : D
    INVOKEDYNAMIC makeConcatWithConstants(D)Ljava/lang/String; [
      // handle kind 0x6 : INVOKESTATIC
      java/lang/invoke/StringConcatFactory.makeConcatWithConstants(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
      // arguments:
      "Circle(r=\u0001)"
    ]
    ARETURN
    MAXSTACK = 2
    MAXLOCALS = 1
}

*
* */
