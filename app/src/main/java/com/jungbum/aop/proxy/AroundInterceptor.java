package com.jungbum.aop.proxy;

/** Spring의 AOP Alliance MethodInterceptor를 학습에 필요한 형태로 줄인 인터페이스다. */
@FunctionalInterface
public interface AroundInterceptor {

    Object invoke(MiniReflectiveMethodInvocation invocation) throws Throwable;
}
