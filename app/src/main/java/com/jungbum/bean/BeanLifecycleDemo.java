package com.jungbum.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/*
 * 스프링 빈의 전체 생명주기(lifecycle)를 한 번에 "관찰"하기 위한 교육용 데모입니다.
 *
 *   주의: 실무 애플리케이션 빈은 이렇게 작성하지 않습니다.
 *   - 여기서는 호출 순서를 한 화면에서 보려고 한 클래스가 6개 콜백 인터페이스를 전부 구현합니다.
 *   - Aware 계열은 코드를 스프링에 강하게 결합시켜 비즈니스 빈에서는 거의 쓰지 않고,
 *     주로 프레임워크/인프라 빈에서만 사용합니다.
 *   - 초기화/소멸도 InitializingBean/DisposableBean 대신 표준 애너테이션
 *     @PostConstruct / @PreDestroy 를 권장합니다.
 *
 * 실행하면 아래 순서대로 콜백이 호출되는 것을 출력으로 확인할 수 있습니다.
 *
 *  [생성]
 *   1. Constructor                                   - 객체 생성(인스턴스화)
 *   2. Setter (의존성/프로퍼티 주입)                  - DI 단계
 *
 *  [Aware - 컨테이너 인프라 정보 주입]
 *   3. BeanNameAware.setBeanName                     - 컨테이너가 직접 호출
 *   4. BeanClassLoaderAware.setBeanClassLoader       - 컨테이너가 직접 호출
 *   5. BeanFactoryAware.setBeanFactory               - 컨테이너가 직접 호출
 *   6. ApplicationContextAware.setApplicationContext - 내부 BeanPostProcessor(ApplicationContextAwareProcessor)가 호출
 *
 *  [초기화]
 *   7. BeanPostProcessor.postProcessBeforeInitialization
 *   8. @PostConstruct                                - CommonAnnotationBeanPostProcessor가 before-init 시점에 호출
 *   9. InitializingBean.afterPropertiesSet
 *  10. custom init-method (@Bean(initMethod = ...))
 *  11. BeanPostProcessor.postProcessAfterInitialization
 *
 *  --- 빈 사용 가능 ---
 *
 *  [소멸] - 컨테이너 종료(close) 시, 등록 역순으로 호출
 *  12. @PreDestroy                                   - CommonAnnotationBeanPostProcessor가 호출
 *  13. DisposableBean.destroy
 *  14. custom destroy-method (@Bean(destroyMethod = ...))
 */
public class BeanLifecycleDemo {

    public static void main(String[] args) {
        System.out.println("\n===== Bean Lifecycle 데모 시작 =====");

        System.out.println("\n[main] ApplicationContext 생성 → 빈 생성/초기화 콜백이 차례로 호출됩니다.");
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DemoConfig.class)) {

            System.out.println("\n[main] === 초기화 완료. 이제 빈을 사용할 수 있습니다. ===");
            LifecycleService service = context.getBean(LifecycleService.class);
            System.out.println("[main] 빈 사용: " + service.doWork());

            System.out.println("\n[main] context.close() 호출 → 소멸 콜백이 차례로 호출됩니다.");
        }

        System.out.println("\n===== Bean Lifecycle 데모 종료 =====");
    }

    static class DemoConfig {
        @Bean
        static BeanPostProcessor lifecycleBeanPostProcessor() {
            return new LifecycleBeanPostProcessor();
        }

        // initMethod / destroyMethod 로 커스텀 초기화/소멸 메서드를 지정합니다.
        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        LifecycleService lifecycleService() {
            LifecycleService service = new LifecycleService();
            service.setMessage("hello lifecycle"); // 2. Setter 주입
            return service;
        }
    }

    /**
     * 생명주기 콜백 인터페이스/애너테이션을 모두 구현하여 호출 순서를 관찰합니다.
     */
    static class LifecycleService
            implements BeanNameAware, BeanClassLoaderAware, BeanFactoryAware,
                       ApplicationContextAware, InitializingBean, DisposableBean {

        private String message;

        public LifecycleService() {
            System.out.println(" 1. [Constructor] LifecycleService 생성");
        }

        public void setMessage(String message) {
            System.out.println(" 2. [Setter] message = " + message);
            this.message = message;
        }

        // --- Aware 콜백 ---
        @Override
        public void setBeanName(String name) {
            System.out.println(" 3. [BeanNameAware] beanName = " + name);
        }

        @Override
        public void setBeanClassLoader(ClassLoader classLoader) {
            System.out.println(" 4. [BeanClassLoaderAware] classLoader = " + classLoader.getClass().getSimpleName());
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            System.out.println(" 5. [BeanFactoryAware] beanFactory = " + beanFactory.getClass().getSimpleName());
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            System.out.println(" 6. [ApplicationContextAware] applicationContext = " + applicationContext.getClass().getSimpleName());
        }

        // --- 초기화 콜백 ---
        @PostConstruct
        public void postConstruct() {
            System.out.println(" 8. [@PostConstruct] postConstruct()");
        }

        @Override
        public void afterPropertiesSet() {
            System.out.println(" 9. [InitializingBean] afterPropertiesSet()");
        }

        public void customInit() {
            System.out.println("10. [init-method] customInit()");
        }

        String doWork() {
            return "doWork() -> " + message;
        }

        // --- 소멸 콜백 ---
        @PreDestroy
        public void preDestroy() {
            System.out.println("12. [@PreDestroy] preDestroy()");
        }

        @Override
        public void destroy() {
            System.out.println("13. [DisposableBean] destroy()");
        }

        public void customDestroy() {
            System.out.println("14. [destroy-method] customDestroy()");
        }
    }

    /**
     * 초기화 콜백(@PostConstruct, afterPropertiesSet, init-method) 을 감싸는
     * before/after 두 지점을 출력하여 초기화 콜백과의 상대적 순서를 보여줍니다.
     */
    static class LifecycleBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof LifecycleService) {
                System.out.println(" 7. [BeanPostProcessor] postProcessBeforeInitialization (" + beanName + ")");
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof LifecycleService) {
                System.out.println("11. [BeanPostProcessor] postProcessAfterInitialization (" + beanName + ")");
            }
            return bean;
        }
    }
}
