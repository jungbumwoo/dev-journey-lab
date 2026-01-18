package com.jungbum.aop.factoryBean;

import com.jungbum.aop.Hello;
import com.jungbum.aop.HelloTarget;
import com.jungbum.aop.UpperCaseHandlerV2;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;

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

        MethodInterceptor를 구현한 UppercaseAdvice에 target이 없다.
        MethodInvocation 은 target object의 method를 실행할 수 있는 기능이 있기 때문에 MethodInterceptor가 부가기능을 제공하는데만 포커싱
        MethodInvocation은 일종의 callback object. proceed() method 실행 시, target object method를 내부적으로 실행
        이러한 점이 jdk dynamic proxy와 ProxyFactoryBean의 차이이자 ProxyFactoryBean 의 장점.

        jdk dynamic proxy에서는 필수로 필요했던 Hello Interface를 제공하지 않았음. ProxyFactoryBean 찾음
        todo: ProxyFactoryBean이 JDK dynamic proxy <> CGLib 바이트코드 생성 프레임워크로 프록시로 만드는 차이 확인

        pattern을 주입받아서 메소드 선정하던건? -> pointcut으로 대체
        메소드 선정 알고리즘을 담은 object: pointcut. 부가기능 제공 object: Advise.
        */
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());
        pfBean.addAdvice(new UppercaseAdvice()); // 여러 개를 추가할 수 있음

        Hello proxiedHello = (Hello) pfBean.getObject();
        assertEquals("HELLO TOBY", proxiedHello.sayHello("Toby"));
        assertEquals("HI TOBY", proxiedHello.sayHi("Toby"));
        assertEquals("THANK YOU TOBY", proxiedHello.sayThankYou("Toby"));
    }

    @Test
    public void pointcutAdvisor() {
        ProxyFactoryBean pfBean = new ProxyFactoryBean();
        pfBean.setTarget(new HelloTarget());

        NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();
        pointcut.setMappedName("sayH*"); // 이름 비교 조건 설정.

        // advice + point cut 을 같이 advisor 로 등록
        // advisor = pointcut(메소드 선정 알고리즘 + advice(부가기능)
        // advice, pointcut을 각각 등록하면 어떤 어드바이스에 어떤 포인트컷을 적용할지 애매
        pfBean.addAdvisor(new DefaultPointcutAdvisor(pointcut, new UppercaseAdvice()));

        Hello proxiedHello = (Hello) pfBean.getObject();
        assertEquals("HELLO TOBY", proxiedHello.sayHello("Toby"));
        assertEquals("HI TOBY", proxiedHello.sayHi("Toby"));

        // pointcut 조건에 해당하지 않음으로 변환 적용이 되지 않음
        assertEquals("Thank You Toby", proxiedHello.sayThankYou("Toby"));
    }
}
