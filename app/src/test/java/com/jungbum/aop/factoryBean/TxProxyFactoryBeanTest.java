package com.jungbum.aop.factoryBean;

import com.jungbum.aop.TransactionHandler;
import com.jungbum.aop.example.service.UserService;
import com.jungbum.aop.example.service.UserServiceImpl;
import com.jungbum.factoryBean.TxProxyFactoryBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration()
public class TxProxyFactoryBeanTest {

    @Autowired
    ApplicationContext context;

    @TestConfiguration
    static class txProxyConfig {
        @Bean
        public PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        // 타겟이 바뀔 때마다 프록시 생성 코드도 추가되어야하고, 여러 타겟이 필요하면 그만큼 FactoryBean도 추가가 필요해보임
        @Bean
        public TxProxyFactoryBean txProxyFactoryBean(PlatformTransactionManager transactionManager) {
            UserService userService = new UserServiceImpl();

            TxProxyFactoryBean txProxyFactoryBean = new TxProxyFactoryBean(
                    userService, transactionManager,
                    null,
                    UserService.class
            );
            return txProxyFactoryBean;
        }
    }

    @Test
    public void getMessageFromFactoryBean() {
        Object proxy = context.getBean("txProxyFactoryBean");
        assertThat(proxy).isInstanceOf(UserService.class);

        // 프록시에서 핸들러를 꺼내옴
        Object handler = Proxy.getInvocationHandler(proxy);
        assertThat(handler).isInstanceOf(TransactionHandler.class);
    }

    @Test
    public void getFactoryBean() throws Exception {
        // & - 스프링 문법
        // 팩토리 빈이 만들어준 결과물이 아니라, 팩토리 빈 객체.
        Object factory = context.getBean("&txProxyFactoryBean");
        assertThat(factory).isInstanceOf(TxProxyFactoryBean.class);
    }
}
