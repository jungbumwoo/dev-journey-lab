package com.jungbum.bean;

import java.util.Arrays;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

/*
 * 1. BeanFactoryPostProcessor는 빈 인스턴스 생성 전에 실행되어 BeanDefinition을 수정합니다.
 * 2. orderService.prefix 값을 NORMAL에서 BFPP로 바꾸고, reportService를 lazy-init으로 변경해서 생성 시점 차이를 확인합니다.
 * 3. BeanPostProcessor와 달리 "이미 만들어진 객체"가 아니라 "객체를 만들 설계도"를 고치는 지점이 핵심입니다.
 */
public class BeanFactoryPostProcessorDemo {

    public static void main(String[] args) {
        System.out.println("\n===== BeanFactoryPostProcessor 데모 시작 =====");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoConfig.class)) {
            System.out.println("\n[main] ApplicationContext 초기화 완료");
            System.out.println("[main] orderService 는 singleton 이라 context 초기화 중 이미 생성되었습니다.");
            System.out.println("[main] reportService 는 BeanDefinition 이 lazy-init 으로 바뀌어서 아직 생성되지 않았습니다.");

            OrderService orderService = context.getBean(OrderService.class);
            System.out.println("\n1) BeanDefinition propertyValues 변경 결과");
            System.out.println("   " + orderService.createOrder("keyboard"));

            System.out.println("\n2) BeanDefinition lazyInit 변경 결과");
            ReportService reportService = context.getBean(ReportService.class);
            System.out.println("   " + reportService.createReport());
        }

        System.out.println("\n===== BeanFactoryPostProcessor 데모 종료 =====");
    }

    static class DemoConfig {
        @Bean
        static BeanFactoryPostProcessor learningBeanFactoryPostProcessor() {
            return new LearningBeanFactoryPostProcessor();
        }

        @Bean
        OrderService orderService() {
            OrderService orderService = new OrderService();
            orderService.setPrefix("NORMAL");
            return orderService;
        }

        @Bean
        ReportService reportService() {
            return new ReportService();
        }
    }

    static class OrderService {
        private String prefix;

        public OrderService() {
            System.out.println("[constructor] OrderService 생성");
        }

        public void setPrefix(String prefix) {
            System.out.println("[setter] OrderService.prefix = " + prefix);
            this.prefix = prefix;
        }

        String createOrder(String itemName) {
            return prefix + " order: " + itemName;
        }
    }

    static class ReportService {
        public ReportService() {
            System.out.println("[constructor] ReportService 생성");
        }

        String createReport() {
            return "daily report";
        }
    }

    static class LearningBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("[BeanFactoryPostProcessor] BeanDefinition 수정 시작");
            System.out.println("   등록된 BeanDefinition: " + Arrays.toString(beanFactory.getBeanDefinitionNames()));

            BeanDefinition orderServiceDefinition = beanFactory.getBeanDefinition("orderService");
            System.out.println("   변경 전 orderService propertyValues: " + orderServiceDefinition.getPropertyValues());
            orderServiceDefinition.getPropertyValues().add("prefix", "MUYAHO!");
            System.out.println("   변경 후 orderService propertyValues: " + orderServiceDefinition.getPropertyValues());
            System.out.println("   @Bean 메서드가 NORMAL 을 넣더라도, BeanDefinition 후처리 값인 MUYAHO! 가 한 번 더 주입됩니다.");

            BeanDefinition reportServiceDefinition = beanFactory.getBeanDefinition("reportService");
            System.out.println("   변경 전 reportService.lazyInit: " + reportServiceDefinition.isLazyInit());
            reportServiceDefinition.setLazyInit(true);
            System.out.println("   변경 후 reportService.lazyInit: " + reportServiceDefinition.isLazyInit());

            System.out.println("[BeanFactoryPostProcessor] BeanDefinition 수정 종료");
        }
    }
}
