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
