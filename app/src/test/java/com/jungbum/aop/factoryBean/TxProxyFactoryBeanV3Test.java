package com.jungbum.aop.factoryBean;

import com.jungbum.dao.UserDao;
import com.jungbum.factoryBean.AppConfig;
import com.jungbum.factoryBean.NameMatchClassMethodPointcut;
import com.jungbum.factoryBean.TransactionAdvice;
import com.jungbum.service.UserService;
import com.jungbum.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { AppConfig.class, TransactionAdvice.class, TxProxyFactoryBeanV2Test.txProxyConfig.class })
public class TxProxyFactoryBeanV3Test {

    @Autowired
    ApplicationContext context;

    @TestConfiguration
    static class txProxyConfig {
        @Bean
        public PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }


//        @Bean
//        public NameMatchMethodPointcut transactionPointcut() {
//            NameMatchMethodPointcut nmmp = new NameMatchMethodPointcut();
//            nmmp.setMappedName("upgrade*");
//            return nmmp;
//        }
//
        // V2에서 썻던 NameMatchMethodPointcut 를 아래 NameMatchClassMethodPointcut 로 대체
        // class도 구분하기 위함
        @Bean
        public NameMatchClassMethodPointcut transactionPointcut() {
            NameMatchClassMethodPointcut nameMatchClassMethodPointcut = new NameMatchClassMethodPointcut();
            nameMatchClassMethodPointcut.setMappedClassName("*ServiceImpl");
            nameMatchClassMethodPointcut.setMappedName("upgrade*");
            return nameMatchClassMethodPointcut;
        }

        // TransactionAdvice 사용 여부는 UserServiceTest에서 검증함
        @Bean
        public DefaultPointcutAdvisor defaultPointcutAdvisor(Pointcut pointcut, TransactionAdvice transactionAdvice) {
            // TxProxyFactoryBeanTest와 달리 pointcut, advice가 분리되어있음
            return new DefaultPointcutAdvisor(pointcut, transactionAdvice);
        }

        // proxyFactoryBean 주석처리. 명시적으로 팩토리 빈을 생성하지 않음
//        @Bean
//        ProxyFactoryBean proxyFactoryBean() {
//            ProxyFactoryBean pfb = new ProxyFactoryBean();
//            pfb.setTarget(new UserServiceImpl());
//            pfb.setInterceptorNames("defaultPointcutAdvisor");
//            return pfb;
//        }
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

