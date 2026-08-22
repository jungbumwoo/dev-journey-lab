package com.jungbum.aop.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * JDK 동적 프록시의 모든 메서드 호출이 도착하는 단일 진입점이다.
 *
 * <p>{@link Method#invoke(Object, Object...)}는 대상 메서드가 던진 예외를
 * {@link InvocationTargetException}으로 감싼다. 프록시 바깥에서는 원래 예외가 보여야 하므로
 * 이 핸들러는 cause를 꺼내 다시 던진다.</p>
 */
public final class TracingInvocationHandler implements InvocationHandler {

    private final Object target;
    private final Consumer<String> trace;

    public TracingInvocationHandler(Object target, Consumer<String> trace) {
        this.target = Objects.requireNonNull(target);
        this.trace = Objects.requireNonNull(trace);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // equals/hashCode/toString도 InvocationHandler로 들어온다. target에 그대로 위임하면
        // proxy.equals(proxy)가 false가 될 수 있어 프록시 자신의 identity로 처리한다.
        if (method.getDeclaringClass() == Object.class) {
            return invokeObjectMethod(proxy, method, args);
        }

        trace.accept("before:" + method.getName());
        try {
            Object result = method.invoke(target, args);
            trace.accept("after:" + method.getName());
            return result;
        }
        catch (InvocationTargetException ex) {
            trace.accept("throw:" + method.getName());
            throw ex.getTargetException();
        }
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "JdkProxy(" + target + ")";
            default -> throw new IllegalStateException("Unexpected Object method: " + method);
        };
    }
}
