package com.jungbum.factoryBean;

import com.jungbum.aop.TransactionHandler;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Proxy;

// transaction proxy를 생성하는 FactoryBean
@RequiredArgsConstructor
public class TxProxyFactoryBean implements FactoryBean<Object> {
    private final Object target;
    private final PlatformTransactionManager transactionManager;
    private final String pattern;
    private final Class<?> serviceInterface;

    @Override
    public @Nullable Object getObject() throws Exception {
        TransactionHandler txHandler = new TransactionHandler(target, transactionManager, pattern);

        // Proxy.newProxyInstance: JDK가 제공하는 동적 프록시 생성 도구
        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ serviceInterface },  // 인터페이스를 전달받아 해당 인터페이스를 구현하는 프록시 객체를 구현. CGLIB 는 다른 방식을 사용.
                txHandler // 얘가 가로채는 애. 프록시의 메소드가 호출되면 얘가 가로채서 처리함
        );
    }

    @Override
    public @Nullable Class<?> getObjectType() {
        return serviceInterface;
    }
}
