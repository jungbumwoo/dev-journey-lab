package com.jungbum.aop.proxy;

/**
 * 두 종류의 프록시가 호출을 위임할 실제 객체(target)다.
 *
 * <p>CGLIB은 이 클래스를 상속한 새 클래스를 런타임에 생성하므로 final 클래스일 수 없고,
 * 가로채야 하는 메서드 역시 final이면 안 된다.</p>
 */
public class CalculatorService implements Calculator {

    @Override
    public int sumLeftRight(int left, int right) {
        return left + right;
    }

    @Override
    public int divideLeftRight(int dividend, int divisor) {
        return dividend / divisor;
    }

    /** CGLIB 서브클래스가 오버라이드할 수 없음을 확인하기 위한 메서드다. */
    public final String finalOperation() {
        return "final method";
    }
}
