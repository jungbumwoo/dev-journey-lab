package com.jungbum.aop.proxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ReflectiveMethodInvocation의 축약형")
class MiniReflectiveMethodInvocationTest {

    @Test
    @DisplayName("proceed가 interceptor를 순서대로 전진시켜 around chain을 만든다")
    void proceedBuildsNestedAroundChain() throws Throwable {
        List<String> trace = new ArrayList<>();
        ProbeTarget target = new ProbeTarget(trace);
        Method method = ProbeTarget.class.getMethod("greet", String.class);
        MiniReflectiveMethodInvocation invocation = new MiniReflectiveMethodInvocation(
                target,
                method,
                new Object[] {"Toby"},
                List.of(around("first", trace), around("second", trace)));

        Object result = invocation.proceed();

        assertEquals("Hello Toby", result);
        assertEquals(List.of(
                "first.before",
                "second.before",
                "target",
                "second.after",
                "first.after"), trace);
    }

    @Test
    @DisplayName("interceptor는 proceed를 호출하지 않고 target 실행을 단락할 수 있다")
    void interceptorCanShortCircuitTheChain() throws Throwable {
        List<String> trace = new ArrayList<>();
        ProbeTarget target = new ProbeTarget(trace);
        Method method = ProbeTarget.class.getMethod("greet", String.class);
        MiniReflectiveMethodInvocation invocation = new MiniReflectiveMethodInvocation(
                target,
                method,
                new Object[] {"Toby"},
                List.of(ignored -> "cached value"));

        assertEquals("cached value", invocation.proceed());
        assertEquals(List.of(), trace);
    }

    @Test
    @DisplayName("joinpoint의 예외는 InvocationTargetException을 벗겨 원형 그대로 전달한다")
    void joinpointExceptionIsUnwrapped() throws NoSuchMethodException {
        ProbeTarget target = new ProbeTarget(new ArrayList<>());
        Method method = ProbeTarget.class.getMethod("fail");
        MiniReflectiveMethodInvocation invocation = new MiniReflectiveMethodInvocation(
                target, method, new Object[0], List.of());

        IllegalStateException thrown = assertThrows(IllegalStateException.class, invocation::proceed);

        assertEquals("target failure", thrown.getMessage());
    }

    private AroundInterceptor around(String name, List<String> trace) {
        return invocation -> {
            trace.add(name + ".before");
            Object result = invocation.proceed();
            trace.add(name + ".after");
            return result;
        };
    }

    public static class ProbeTarget {

        private final List<String> trace;

        ProbeTarget(List<String> trace) {
            this.trace = trace;
        }

        public String greet(String name) {
            trace.add("target");
            return "Hello " + name;
        }

        public void fail() {
            throw new IllegalStateException("target failure");
        }
    }
}
