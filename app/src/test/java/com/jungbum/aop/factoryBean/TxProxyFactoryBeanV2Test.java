package com.jungbum.aop.factoryBean;

import com.jungbum.factoryBean.AppConfig;
import com.jungbum.factoryBean.TransactionAdvice;
import com.jungbum.service.UserService;
import com.jungbum.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.NameMatchMethodPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


// TxProxyFactoryBeanTest는 직접 정의한 커스텀 FactoryBean을 사용하는 방식 -> 재사용성 저하, jdk proxy 기술 의존성
// V2는 스프링이 제공하는 범용적인 ProxyFactoryBean을 사용하는 방식 -> 타겟, advise, poincut을 설정만으로 조립. jdk외 CGLIB도 사용가능
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AppConfig.class, TransactionAdvice.class, TxProxyFactoryBeanV2Test.txProxyConfig.class })
public class TxProxyFactoryBeanV2Test {

    @Autowired
    ApplicationContext context;

    @TestConfiguration
    static class txProxyConfig {
        @Bean
        public PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        public NameMatchMethodPointcut transactionPointcut() {
            NameMatchMethodPointcut nmmp = new NameMatchMethodPointcut();
            nmmp.setMappedName("upgrade*");
            return nmmp;
        }

        @Bean
        public DefaultPointcutAdvisor defaultPointcutAdvisor(Pointcut pointcut, TransactionAdvice transactionAdvice) {
            // TxProxyFactoryBeanTest와 달리 pointcut, advice가 분리되어있음
            return new DefaultPointcutAdvisor(pointcut, transactionAdvice);
        }

        // 여기 Bean 생성 시 파라미터로 받아서 설정하려했더니 cycle이 발생하였음.
        // 파라미터로 받는게 아니라 set 시 명시적으로 넣어줘서 해결됨.
        @Bean
        ProxyFactoryBean proxyFactoryBean() {
            ProxyFactoryBean pfb = new ProxyFactoryBean();
            pfb.setTarget(new UserServiceImpl());
            pfb.setInterceptorNames("defaultPointcutAdvisor");
            return pfb;
        }
    }

    @Test
    public void getMessageFromFactoryBean() {
        Object proxy = context.getBean("proxyFactoryBean");
        assertThat(proxy).isInstanceOf(UserService.class);

        assertThat(org.springframework.aop.support.AopUtils.isAopProxy(proxy)).isTrue();
    }

    @Test
    public void getFactoryBean() throws Exception {
        // & - 스프링 문법
        // 팩토리 빈이 만들어준 결과물이 아니라, 팩토리 빈 객체.
        Object factory = context.getBean("&proxyFactoryBean");
        assertThat(factory).isInstanceOf(org.springframework.aop.framework.ProxyFactoryBean.class);    }
}

