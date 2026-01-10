package com.jungbum.factoryBean;

import org.springframework.beans.factory.FactoryBean;

// Message class 처럼 생성자가 private이어서 스프링이 직접 new를 할 수 없거나, 객체 생성 로직이 복잡할 때 FactoryBean을 사용함
public class MessageFactoryBean implements FactoryBean<Message> {
    String text;
    public void setText(String text) {
        this.text = text;
    }

    @Override
    public Message getObject() throws Exception {
        return Message.newMessage(this.text);
    }

    @Override
    public Class<? extends Message> getObjectType() {
        return Message.class;
    }
}
