package com.jungbum.aop.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Spring {@code ReflectiveMethodInvocation}의 proceed 알고리즘을 축약한 학습용 구현이다.
 *
 * <p>각 interceptor가 {@link #proceed()}를 호출할 때 인덱스가 하나 전진한다. 체인의 끝에서는
 * reflection으로 실제 target 메서드를 실행하므로 around advice가 양파처럼 중첩된다.</p>
 */
public final class MiniReflectiveMethodInvocation {

    private final Object target;
    private final Method method;
    private final Object[] arguments;
    private final List<AroundInterceptor> interceptors;
    private int currentInterceptorIndex = -1;

    public MiniReflectiveMethodInvocation(
            Object target, Method method, Object[] arguments, List<AroundInterceptor> interceptors) {

        this.target = Objects.requireNonNull(target);
        this.method = Objects.requireNonNull(method);
        this.arguments = arguments != null ? arguments.clone() : new Object[0];
        this.interceptors = List.copyOf(interceptors);
    }

    public Object proceed() throws Throwable {
        if (currentInterceptorIndex == interceptors.size() - 1) {
            return invokeJoinpoint();
        }

        AroundInterceptor next = interceptors.get(++currentInterceptorIndex);
        return next.invoke(this);
    }

    private Object invokeJoinpoint() throws Throwable {
        try {
            return method.invoke(target, arguments);
        }
        catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArguments() {
        return arguments.clone();
    }
}
