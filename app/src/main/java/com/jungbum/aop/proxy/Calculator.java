package com.jungbum.aop.proxy;

/**
 * JDK 동적 프록시가 구현할 계약이다.
 *
 * <p>JDK 동적 프록시는 구체 클래스가 아니라 이런 인터페이스를 기준으로 프록시 클래스를 만든다.</p>
 */
public interface Calculator {

    int sumLeftRight(int left, int right);

    int divideLeftRight(int dividend, int divisor);
}
