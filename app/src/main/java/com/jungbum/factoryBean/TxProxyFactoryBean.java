package com.jungbum.factoryBean;

import com.jungbum.aop.TransactionHandler;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Proxy;

@RequiredArgsConstructor
public class TxProxyFactoryBean implements FactoryBean<Object> {
    private final Object target;
    private final PlatformTransactionManager transactionManager;
    private final String pattern;
    private final Class<?> serviceInterface;

    @Override
    public @Nullable Object getObject() throws Exception {
        TransactionHandler txHandler = new TransactionHandler(target, transactionManager, pattern);

        return Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ serviceInterface },
                txHandler
        );
    }

    @Override
    public @Nullable Class<?> getObjectType() {
        return serviceInterface;
    }
}
