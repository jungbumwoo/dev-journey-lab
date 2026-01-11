package com.jungbum.aop.factoryBean;

import com.jungbum.aop.Hello;
import com.jungbum.aop.HelloTarget;
import com.jungbum.aop.UpperCaseHandlerV2;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactoryBean;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamicProxyTest {
    @Test
    public void simpleProxy() {
        Hello proxiedHello = (Hello) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {Hello.class},
                new UpperCaseHandlerV2(new HelloTarget())
        );

        assertEquals("HELLO TOBY", proxiedHello.sayHello("Toby"));
        assertEquals("HI TOBY", proxiedHello.sayHi("Toby"));
        assertEquals("THANK YOU TOBY", proxiedHello.sayThankYou("Toby"));
    }

    static class UppercaseAdvice implements MethodInterceptor {
        public Object invoke(MethodInvocation invocation) throws Throwable {
            String ret = (String) invocation.proceed();
            return ret.toUpperCase();
        }
    }

    @Test
    public void proxyFactoryBean() {
        /* simpleProxy 방식은 타겟이 바뀔 때마다 프록시 생성 코드도 바뀌어야 하지만
         proxyFactoryBean 방식은 1. 무엇을 할 것인가(Advice)와 2. 누구에게 할 것인가(Target) 를 완전히 독립시킴.
        */
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());
        pfBean.addAdvice(new UppercaseAdvice()); // 여러 개를 추가할 수 있음

        Hello proxiedHello = (Hello) pfBean.getObject();
        assertEquals("HELLO TOBY", proxiedHello.sayHello("Toby"));
        assertEquals("HI TOBY", proxiedHello.sayHi("Toby"));
        assertEquals("THANK YOU TOBY", proxiedHello.sayThankYou("Toby"));
    }
}
