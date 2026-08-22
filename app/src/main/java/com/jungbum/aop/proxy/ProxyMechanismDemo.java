package com.jungbum.aop.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/** JDK 동적 프록시, CGLIB 서브클래스, interceptor chain을 한 번에 비교한다. */
public final class ProxyMechanismDemo {

    private ProxyMechanismDemo() {
    }

    public static void main(String[] args) throws Throwable {
        CalculatorService target = new CalculatorService();

        List<String> jdkTrace = new ArrayList<>();
        Calculator jdkProxy = (Calculator) Proxy.newProxyInstance(
                Calculator.class.getClassLoader(),
                new Class<?>[] {Calculator.class},
                new TracingInvocationHandler(target, jdkTrace::add));

        List<String> cglibTrace = new ArrayList<>();
        CalculatorService cglibProxy = CglibProxyFactory.create(CalculatorService.class, cglibTrace::add);

        System.out.println("[JDK dynamic proxy]");
        System.out.println("class      : " + jdkProxy.getClass().getName());
        System.out.println("interfaces : " + List.of(jdkProxy.getClass().getInterfaces()));
        System.out.println("2 + 3      : " + jdkProxy.sumLeftRight(2, 3));
        System.out.println("trace      : " + jdkTrace);

        System.out.println("\n[CGLIB proxy]");
        System.out.println("class      : " + cglibProxy.getClass().getName());
        System.out.println("superclass : " + cglibProxy.getClass().getSuperclass().getName());
        System.out.println("2 + 3      : " + cglibProxy.sumLeftRight(2, 3));
        System.out.println("trace      : " + cglibTrace);

        System.out.println("\n[ReflectiveMethodInvocation-style chain]");
        List<String> chainTrace = new ArrayList<>();
        CalculatorService chainTarget = new CalculatorService() {
            @Override
            public int sumLeftRight(int left, int right) {
                chainTrace.add("target");
                return super.sumLeftRight(left, right);
            }
        };
        Method sumLR = Calculator.class.getMethod("sumLeftRight", int.class, int.class);
        MiniReflectiveMethodInvocation invocation = new MiniReflectiveMethodInvocation(
                chainTarget,
                sumLR,
                new Object[] {2, 3},
                List.of(
                        around("outer", chainTrace),
                        around("inner", chainTrace)));
        System.out.println("result     : " + invocation.proceed());
        System.out.println("trace      : " + chainTrace);
    }

    private static AroundInterceptor around(String name, List<String> trace) {
        return  invocation -> {
            trace.add(name + ".before");
            Object result = invocation.proceed();
            trace.add(name + ".after");
            return result;
        };
    }
}
