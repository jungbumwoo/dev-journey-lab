package com.jungbum.aop.proxy;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * CGLIB이 생성한 오버라이드 메서드에서 호출되는 callback이다.
 */
public final class CglibTracingInterceptor implements MethodInterceptor {

    private final Consumer<String> trace;

    public CglibTracingInterceptor(Consumer<String> trace) {
        this.trace = Objects.requireNonNull(trace);
    }

    @Override
    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        trace.accept("before:" + method.getName());
        try {
            // invokeSuper는 생성된 서브클래스의 부모 구현을 직접 호출한다.
            // method.invoke(proxy, args)를 사용하면 오버라이드 메서드로 다시 들어와 무한 재귀한다.
            Object result = methodProxy.invokeSuper(proxy, args);
            trace.accept("after:" + method.getName());
            return result;
        }
        catch (Throwable ex) {
            trace.accept("throw:" + method.getName());
            throw ex;
        }
    }
}
