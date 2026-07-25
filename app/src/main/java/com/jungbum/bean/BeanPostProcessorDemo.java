package com.jungbum.bean;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

public class BeanPostProcessorDemo {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoConfig.class)) {
            System.out.println("\n===== BeanPostProcessor 데모 시작 =====");

            GreetingService greetingService = context.getBean(GreetingService.class);
            System.out.println("1) 조작된 빈 호출: " + greetingService.greet("spring"));

            PaymentService paymentService = context.getBean(PaymentService.class);
            System.out.println("2) 바꿔치기된 빈 타입: " + paymentService.getClass().getName());
            paymentService.pay(10_000);

            System.out.println("===== BeanPostProcessor 데모 종료 =====");
        }
    }

    static class DemoConfig {
        @Bean
        static BeanPostProcessor learningBeanPostProcessor() {
            return new LearningBeanPostProcessor();
        }

        @Bean
        GreetingService greetingService() {
            return new DefaultGreetingService();
        }

        @Bean
        PaymentService paymentService() {
            return new DefaultPaymentService();
        }
    }

    interface GreetingService {
        String greet(String name);
    }

    static class DefaultGreetingService implements GreetingService {
        private String prefix = "hello";

        void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String greet(String name) {
            return prefix + ", " + name;
        }
    }

    interface PaymentService {
        void pay(int amount);
    }

    static class DefaultPaymentService implements PaymentService {
        @Override
        public void pay(int amount) {
            System.out.println("   실제 결제 로직 실행: " + amount + "원");
        }
    }

    static class LearningBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            System.out.println("[before init] " + beanName + " -> " + readableType(bean));

            if (bean instanceof DefaultGreetingService greetingService) {
                greetingService.setPrefix("muyaho~ you are hacked.");
            }

            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            System.out.println("[after init ] " + beanName + " -> " + readableType(bean));

            if (bean instanceof PaymentService paymentService) {
                // JDK 동적 프록시
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        new Class<?>[] {PaymentService.class},
                        (proxy, method, args) -> {
                            System.out.println("muyaho~ 프록시 부가기능 시작: " + method.getName());
                            Object result = method.invoke(paymentService, args);
                            System.out.println("muyaho~ 프록시 부가기능 종료: " + method.getName());
                            return result;
                        }
                );
            }

            return bean;
        }

        private String readableType(Object bean) {
            Class<?> beanClass = bean.getClass();
            Class<?>[] interfaces = beanClass.getInterfaces();

            if (interfaces.length == 0) {
                return beanClass.getSimpleName();
            }

            String interfaceNames = Arrays.stream(interfaces)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));

            return beanClass.getSimpleName() + " implements " + interfaceNames;
        }
    }
}
