package com.jungbum.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class UpperCaseHandlerV2 implements InvocationHandler {
//    Hello target;
    Object target;

//    public UpperCaseHandlerV2(Hello target) {
    // 어떤 종류의 인터페이스를 구현한 타킷에도 적용가능해짐 (Hello -> Object)
    public UpperCaseHandlerV2(Object target) {
        this.target = target;
    }

    // return type이 String이 아니어도 핸들링이 가능해짐.
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object ret = method.invoke(target, args);
        if (ret instanceof String) {
            return ((String) ret).toUpperCase();
        }

        // method 명 조건도 추가 가능
        // if (ret instanceof String && method.getName().startsWith("say")) {}
        return ret;
    }
}