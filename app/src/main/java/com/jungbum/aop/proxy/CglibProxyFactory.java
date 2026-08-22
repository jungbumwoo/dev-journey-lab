package com.jungbum.aop.proxy;

import java.util.function.Consumer;

import org.springframework.cglib.proxy.Enhancer;

/** CGLIB의 핵심 설정만 남긴 학습용 프록시 팩토리다. */
public final class CglibProxyFactory {

    private CglibProxyFactory() {
    }

    public static <T> T create(Class<T> targetClass, Consumer<String> trace) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new CglibTracingInterceptor(trace));
        return targetClass.cast(enhancer.create());
    }
}
