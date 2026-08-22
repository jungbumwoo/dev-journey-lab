package com.jungbum.aop.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JDK Dynamic Proxy와 InvocationHandler")
class JdkDynamicProxyLearningTest {

    @Test
    @DisplayName("생성 클래스는 Proxy를 상속하고 요청한 인터페이스를 구현한다")
    void generatedProxyShape() {
        Calculator proxy = createProxy(new CalculatorService(), new ArrayList<>());

        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        assertSame(Proxy.class, proxy.getClass().getSuperclass());
        assertTrue(proxy instanceof Calculator);
        assertFalse(CalculatorService.class.isInstance(proxy));
        assertTrue(proxy.getClass().getName().contains("$Proxy"));
    }

    @Test
    @DisplayName("인터페이스 메서드 호출 정보가 InvocationHandler 하나로 모인다")
    void invocationIsDispatchedToHandler() {
        AtomicReference<Object> receivedProxy = new AtomicReference<>();
        AtomicReference<Method> receivedMethod = new AtomicReference<>();
        AtomicReference<Object[]> receivedArguments = new AtomicReference<>();
        InvocationHandler handler = (proxy, method, args) -> {
            receivedProxy.set(proxy);
            receivedMethod.set(method);
            receivedArguments.set(args);
            return 42;
        };
        Calculator proxy = (Calculator) Proxy.newProxyInstance(
                Calculator.class.getClassLoader(), new Class<?>[] {Calculator.class}, handler);

        assertEquals(42, proxy.sumLeftRight(10, 20));
        assertSame(proxy, receivedProxy.get());
        assertEquals("sumLeftRight", receivedMethod.get().getName());
        assertEquals(List.of(10, 20), List.of(receivedArguments.get()));
        assertSame(handler, Proxy.getInvocationHandler(proxy));
    }

    @Test
    @DisplayName("핸들러가 target 호출 전후에 부가기능을 실행한다")
    void handlerAddsCrossCuttingBehavior() {
        List<String> trace = new ArrayList<>();
        Calculator proxy = createProxy(new CalculatorService(), trace);

        assertEquals(5, proxy.sumLeftRight(2, 3));
        assertEquals(List.of("before:sumLeftRight", "after:sumLeftRight"), trace);
    }

    @Test
    @DisplayName("Method.invoke가 감싼 InvocationTargetException에서 원래 예외를 꺼낸다")
    void targetExceptionIsUnwrapped() {
        List<String> trace = new ArrayList<>();
        Calculator proxy = createProxy(new CalculatorService(), trace);

        ArithmeticException thrown = assertThrows(ArithmeticException.class, () -> proxy.divideLeftRight(1, 0));

        assertEquals("/ by zero", thrown.getMessage());
        assertEquals(List.of("before:divideLeftRight", "throw:divideLeftRight"), trace);
    }

    @Test
    @DisplayName("Object 메서드도 핸들러로 들어오므로 identity 정책을 명시한다")
    void objectMethodsNeedAnExplicitPolicy() {
        Calculator proxy = createProxy(new CalculatorService(), new ArrayList<>());
        Calculator anotherProxy = createProxy(new CalculatorService(), new ArrayList<>());

        assertEquals(proxy, proxy);
        assertNotEquals(proxy, anotherProxy);
        assertEquals(System.identityHashCode(proxy), proxy.hashCode());
        assertTrue(proxy.toString().startsWith("JdkProxy("));
    }

    @Test
    @DisplayName("JDK Proxy에는 인터페이스만 지정할 수 있다")
    void concreteClassCannotBeAProxyContract() {
        assertThrows(IllegalArgumentException.class, () -> Proxy.newProxyInstance(
                CalculatorService.class.getClassLoader(),
                new Class<?>[] {CalculatorService.class},
                (proxy, method, args) -> null));
    }

    private Calculator createProxy(CalculatorService target, List<String> trace) {
        return (Calculator) Proxy.newProxyInstance(
                Calculator.class.getClassLoader(),
                new Class<?>[] {Calculator.class},
                new TracingInvocationHandler(target, trace::add));
    }
}
