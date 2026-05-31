package com.jungbum.bean;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/*
 * 실무에 가까운 생명주기 작성법을 보여주는 데모입니다.
 * (콜백 호출 "순서" 자체를 관찰하려면 BeanLifecycleDemo 를 보세요.)
 *
 * 실무 비즈니스 빈의 일반적인 모습:
 *   - 의존성은 "생성자 주입"으로 받는다 (final 필드 → 불변, 누락 시 컴파일/기동 단계에서 발견).
 *   - 초기화/정리는 표준 애너테이션 @PostConstruct / @PreDestroy 로 처리한다.
 *     → 스프링 인터페이스(InitializingBean/DisposableBean)에 의존하지 않아 결합도가 낮다.
 *   - BeanNameAware / BeanFactoryAware / ApplicationContextAware 같은 Aware 계열은 쓰지 않는다.
 *     필요한 협력 객체는 그냥 주입받으면 되기 때문이다.
 *
 * 즉, 한 클래스가 여러 콜백 인터페이스를 동시에 구현할 일은 사실상 없습니다.
 */
public class RealisticLifecycleDemo {

    public static void main(String[] args) {
        System.out.println("\n===== Realistic Lifecycle 데모 시작 =====");

        System.out.println("\n[main] ApplicationContext 생성");
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(AppConfig.class)) {

            System.out.println("\n[main] === 초기화 완료. 빈 사용 ===");
            OrderService orderService = context.getBean(OrderService.class);
            System.out.println("[main] " + orderService.placeOrder("keyboard"));

            System.out.println("\n[main] context.close() → @PreDestroy 가 등록 역순으로 호출됩니다.");
        }

        System.out.println("\n===== Realistic Lifecycle 데모 종료 =====");
    }

    @Configuration
    @ComponentScan(basePackageClasses = RealisticLifecycleDemo.class)
    static class AppConfig {
    }

    /**
     * 외부 자원(결제 게이트웨이 커넥션)을 다루는 협력 빈.
     * @PostConstruct 에서 자원을 열고, @PreDestroy 에서 닫는 전형적인 패턴.
     */
    @Component
    static class PaymentGateway {

        private boolean connected;

        @PostConstruct
        void connect() {
            this.connected = true;
            System.out.println("[PaymentGateway] @PostConstruct - 결제 게이트웨이 커넥션 open");
        }

        String charge(String item) {
            if (!connected) {
                throw new IllegalStateException("아직 연결되지 않은 게이트웨이입니다.");
            }
            return "charged for " + item;
        }

        @PreDestroy
        void disconnect() {
            this.connected = false;
            System.out.println("[PaymentGateway] @PreDestroy - 결제 게이트웨이 커넥션 close");
        }
    }

    /**
     * 비즈니스 빈. PaymentGateway 를 "생성자 주입"으로 받는다.
     * 생성자가 하나뿐이면 @Autowired 도 생략 가능.
     */
    @Component
    static class OrderService {

        private final PaymentGateway paymentGateway; // final → 주입 후 변경 불가

        OrderService(PaymentGateway paymentGateway) {
            this.paymentGateway = paymentGateway;
            System.out.println("[OrderService] 생성자 주입 완료 (PaymentGateway)");
        }

        @PostConstruct
        void init() {
            System.out.println("[OrderService] @PostConstruct - 초기화 (의존성 주입이 끝난 뒤 호출됨)");
        }

        String placeOrder(String item) {
            return "order ok: " + paymentGateway.charge(item);
        }

        @PreDestroy
        void cleanup() {
            System.out.println("[OrderService] @PreDestroy - 정리");
        }
    }
}
