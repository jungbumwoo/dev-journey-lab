package com.jungbum.aop;

import com.jungbum.aop.example.service.UserService;
import com.jungbum.aop.example.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



public class TransactionHandlerTest {

    private PlatformTransactionManager transactionManager;
    private TransactionStatus transactionStatus;

    @BeforeEach
    void setUp() {
        transactionManager = mock(PlatformTransactionManager.class);
        transactionStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    }

    @Test
    void transactionCommitTest() {
        // given
        UserService target = new UserServiceImpl();
        TransactionHandler handler = new TransactionHandler(target, transactionManager, "add");

        UserService proxy = (UserService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{UserService.class},
                handler
        );

        // when
        proxy.add("jungbum");

        // then
        verify(transactionManager, times(1)).commit(transactionStatus);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    void transactionRollbackTest() {
        // given
        UserService target = new UserServiceImpl();
        TransactionHandler handler = new TransactionHandler(target, transactionManager, "add");

        UserService proxy = (UserService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{UserService.class},
                handler
        );

        // when & then
        assertThrows(RuntimeException.class, () -> proxy.add("error"));

        verify(transactionManager, times(1)).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    @Test
    void pattenNotMatchMethodTest() {
        // given
        UserService target = new UserServiceImpl();
        TransactionHandler handler = new TransactionHandler(target, transactionManager, "add");

        UserService proxy = (UserService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{UserService.class},
                handler
        );

        // when
        proxy.delete("jungbum");

        // then
        verify(transactionManager, never()).getTransaction(any());
    }
}
