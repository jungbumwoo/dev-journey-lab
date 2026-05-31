package com.jungbum.bean;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;

/*
 * BeanDefinition은 "빈 객체"가 아니라 "빈을 만들기 위한 설계도"입니다.
 *
 * 이 예제는 @Bean, @Component 없이 BeanDefinition을 직접 등록합니다.
 * 컨테이너는 refresh() 시점에 이 설계도를 읽고 실제 객체를 생성합니다.
 */
public class BeanDefinitionDemo {

    public static void main(String[] args) {
        System.out.println("\n===== BeanDefinition 데모 시작 =====");

        try (GenericApplicationContext context = new GenericApplicationContext()) {
            BeanDefinition orderServiceDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(OrderService.class)
                    .addPropertyValue("prefix", "NORMAL")
                    .addPropertyValue("discountRate", 10)
                    .setScope(BeanDefinition.SCOPE_SINGLETON)
                    .setLazyInit(false)
                    .getBeanDefinition();

            BeanDefinition auditServiceDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(AuditService.class)
                    .setScope(BeanDefinition.SCOPE_PROTOTYPE)
                    .setLazyInit(true)
                    .getBeanDefinition();

            context.registerBeanDefinition("orderService", orderServiceDefinition);
            context.registerBeanDefinition("auditService", auditServiceDefinition);

            System.out.println("\n[refresh 전] BeanDefinition만 등록됨. 아직 빈 객체는 생성되지 않았습니다.");
            printBeanDefinition("orderService", orderServiceDefinition);
            printBeanDefinition("auditService", auditServiceDefinition);

            System.out.println("\n[refresh 호출]");
            context.refresh();
            System.out.println("[refresh 후] singleton + lazyInit=false 빈은 컨테이너 초기화 중 생성됩니다.");

            System.out.println("\n1) BeanDefinition의 propertyValues가 실제 빈에 주입된 결과");
            OrderService orderService = context.getBean("orderService", OrderService.class);
            System.out.println("   " + orderService.createOrder("keyboard", 100_000));

            System.out.println("\n2) prototype BeanDefinition은 getBean 할 때마다 새 객체를 만듭니다.");
            AuditService auditService1 = context.getBean("auditService", AuditService.class);
            AuditService auditService2 = context.getBean("auditService", AuditService.class);
            System.out.println("   auditService1 == auditService2 ? " + (auditService1 == auditService2));
            auditService1.record("first event");
            auditService2.record("second event");
        }

        System.out.println("\n===== BeanDefinition 데모 종료 =====");
    }

    private static void printBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        System.out.println("   beanName: " + beanName);
        System.out.println("     beanClassName: " + beanDefinition.getBeanClassName());
        System.out.println("     scope: " + beanDefinition.getScope());
        System.out.println("     lazyInit: " + beanDefinition.isLazyInit());
        System.out.println("     propertyValues: " + beanDefinition.getPropertyValues());
    }

    static class OrderService {
        private String prefix;
        private int discountRate;

        public OrderService() {
            System.out.println("[constructor] OrderService 생성");
        }

        public void setPrefix(String prefix) {
            System.out.println("[setter] OrderService.prefix = " + prefix);
            this.prefix = prefix;
        }

        public void setDiscountRate(int discountRate) {
            System.out.println("[setter] OrderService.discountRate = " + discountRate);
            this.discountRate = discountRate;
        }

        String createOrder(String itemName, int price) {
            int discountedPrice = price * (100 - discountRate) / 100;
            return prefix + " order: " + itemName + ", price=" + discountedPrice;
        }
    }

    static class AuditService {
        public AuditService() {
            System.out.println("[constructor] AuditService 생성");
        }

        void record(String message) {
            System.out.println("   audit: " + message + " / instance=" + System.identityHashCode(this));
        }
    }
}
