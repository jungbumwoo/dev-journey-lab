package com.jungbum.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

// HelloUppderCase의 경우 추가되는 method 마다 추가 구현이 필요했지만 여기서는 그럴 필요가 없어짐
public class UpperCaseHandler implements InvocationHandler {
    Hello target;

    public UpperCaseHandler(Hello target) {
        this.target = target;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String ret = (String)method.invoke(target, args);
        return ret.toUpperCase();
    }
}
