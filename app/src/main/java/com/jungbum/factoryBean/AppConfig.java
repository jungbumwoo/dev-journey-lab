package com.jungbum.factoryBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    public MessageFactoryBean message() {
        MessageFactoryBean factory = new MessageFactoryBean();
        factory.setText("factoryBean: Muyaho!!");
        return factory;
    }
}
