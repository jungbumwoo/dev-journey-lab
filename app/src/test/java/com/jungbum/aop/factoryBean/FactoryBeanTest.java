package com.jungbum.aop.factoryBean;

import com.jungbum.factoryBean.AppConfig;
import com.jungbum.factoryBean.Message;
import com.jungbum.factoryBean.MessageFactoryBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
public class FactoryBeanTest {

    @Autowired
    ApplicationContext context;

    @Test
    public void getMessageFromFactoryBean() {
        Object message = context.getBean("message");
        // Q. 왜 AppConfig에서 MessageFactoryBean를 bean으로 등록했는데 MessageFactoryBean가 아닌 Message을 return 할까?
        // A. 컨테이너를 초기화할 때 빈의 타입이 FactoryBean 인터페이스를 구현하였음으로
        // 팩토리 빈 자체를 주는 게 아니라, 그 안에 정의된 getObject() 메소드를 대신 호출해서 줌.
        assertThat(message).isInstanceOf(Message.class);
        assertThat(((Message)message).getText()).isEqualTo("factoryBean: Muyaho!!");
    }

    @Test
    public void getFactoryBean() throws Exception {
        // & - 스프링 문법
        // 팩토리 빈이 만들어준 결과물이 아니라, 팩토리 빈 객체.
        Object factory = context.getBean("&message");
        assertThat(factory).isInstanceOf(MessageFactoryBean.class);
    }
}
