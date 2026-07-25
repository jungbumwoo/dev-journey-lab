package com.jungbum.bean;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

/*
 * BeanFactoryPostProcessor 계열의 실행 순서를 관찰하는 데모입니다.
 *
 * 핵심 순서:
 * 1. 직접 등록한 BeanDefinitionRegistryPostProcessor의 postProcessBeanDefinitionRegistry
 * 2. BeanDefinitionRegistryPostProcessor 빈
 *    - PriorityOrdered
 *    - Ordered
 *    - order 없는 나머지
 * 3. 위 BeanDefinitionRegistryPostProcessor들의 postProcessBeanFactory
 * 4. 직접 등록한 일반 BeanFactoryPostProcessor
 * 5. BeanFactoryPostProcessor 빈
 *    - PriorityOrdered
 *    - Ordered
 *    - order 없는 나머지
 *
 * AnnotationConfigApplicationContext는 내부적으로 ConfigurationClassPostProcessor를 등록합니다.
 * 이 클래스가 @Configuration, @ComponentScan, @Bean, @Import를 해석해서
 * 추가 BeanDefinition을 등록합니다.
 */
public class BeanFactoryPostProcessorOrderDemo {

    public static void main(String[] args) {
        System.out.println("\n===== BeanFactoryPostProcessor 실행 순서 데모 시작 =====");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.addBeanFactoryPostProcessor(new DirectRegistryPostProcessor());
            context.addBeanFactoryPostProcessor(new DirectRegularBeanFactoryPostProcessor());

            context.register(AppConfig.class);

            System.out.println("\n[refresh 전] AnnotationConfigApplicationContext에 직접 등록된 BeanDefinition");
            printBeanNames("refresh 전", context.getBeanDefinitionNames());

            System.out.println("\n[refresh 호출] invokeBeanFactoryPostProcessors() 내부 순서 관찰");
            context.refresh();

            System.out.println("\n[refresh 후] ConfigurationClassPostProcessor가 추가로 등록한 BeanDefinition 확인");
            printBeanNames("refresh 후", context.getBeanDefinitionNames());

            System.out.println("\n[현재 컨텍스트의 Spring 기본 BeanFactoryPostProcessor 구현체]");
            printSpringDefaultPostProcessors(context);

            System.out.println("\n[빈 사용]");
            context.getBean(AppService.class).run();
            context.getBean(ImportedService.class).run();
            context.getBean(ScannedService.class).run();
        }

        System.out.println("\n===== BeanFactoryPostProcessor 실행 순서 데모 종료 =====");
    }

    private static void printBeanNames(String label, String[] beanNames) {
        Set<String> sortedNames = new TreeSet<>(Arrays.asList(beanNames));
        System.out.println("   [" + label + "] " + sortedNames);
    }

    private static void printSpringDefaultPostProcessors(AnnotationConfigApplicationContext context) {
        Map<String, BeanFactoryPostProcessor> processors = context.getBeansOfType(BeanFactoryPostProcessor.class);

        processors.forEach((beanName, processor) -> {
            String className = processor.getClass().getName();
            if (!className.startsWith("org.springframework.")) {
                return;
            }

            String processorType = processor instanceof BeanDefinitionRegistryPostProcessor ? "BDRPP" : "BFPP";
            String orderType = "non-ordered";
            if (processor instanceof PriorityOrdered) {
                orderType = "PriorityOrdered";
            } else if (processor instanceof Ordered) {
                orderType = "Ordered";
            }

            System.out.println("   " + beanName + " -> " + processorType + ", " + orderType + ", " + className);
        });
    }

    @Configuration
    @Import(ImportedConfig.class)
    @ComponentScan(
            basePackageClasses = ScannedService.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ScannedService.class)
    )
    static class AppConfig {
        @Bean
        AppService appService() {
            return new AppService();
        }

        @Bean
        static BeanDefinitionRegistryPostProcessor priorityRegistryPostProcessor() {
            return new PriorityOrderedRegistryPostProcessor("BDRPP Bean - PriorityOrdered", 1);
        }

        @Bean
        static BeanDefinitionRegistryPostProcessor orderedRegistryPostProcessor() {
            return new OrderedRegistryPostProcessor("BDRPP Bean - Ordered", 2);
        }

        @Bean
        static BeanDefinitionRegistryPostProcessor nonOrderedRegistryPostProcessor() {
            return new NonOrderedRegistryPostProcessor("BDRPP Bean - non-ordered");
        }

        @Bean
        static BeanFactoryPostProcessor priorityBeanFactoryPostProcessor() {
            return new PriorityOrderedBeanFactoryPostProcessor("BFPP Bean - PriorityOrdered", 1);
        }

        @Bean
        static BeanFactoryPostProcessor orderedBeanFactoryPostProcessor() {
            return new OrderedBeanFactoryPostProcessor("BFPP Bean - Ordered", 2);
        }

        @Bean
        static BeanFactoryPostProcessor nonOrderedBeanFactoryPostProcessor() {
            return new NonOrderedBeanFactoryPostProcessor("BFPP Bean - non-ordered");
        }
    }

    @Configuration
    static class ImportedConfig {
        @Bean
        ImportedService importedService() {
            return new ImportedService();
        }
    }

    static class AppService {
        void run() {
            System.out.println("   AppService 실행 (@Bean)");
        }
    }

    static class ImportedService {
        void run() {
            System.out.println("   ImportedService 실행 (@Import + @Bean)");
        }
    }

    @Component
    static class ScannedService {
        void run() {
            System.out.println("   ScannedService 실행 (@ComponentScan)");
        }
    }

    static class DirectRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            System.out.println(" 1. [직접 등록 BDRPP] postProcessBeanDefinitionRegistry");
            printBeanNames("직접 등록 BDRPP 시점", registry.getBeanDefinitionNames());
            System.out.println(" 2. [Spring 내부 BDRPP] ConfigurationClassPostProcessor가 곧 실행되어 설정 클래스를 해석합니다.");
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println(" 7. [직접 등록 BDRPP] postProcessBeanFactory");
        }
    }

    static class PriorityOrderedRegistryPostProcessor extends RegistryPostProcessorSupport implements PriorityOrdered {
        PriorityOrderedRegistryPostProcessor(String name, int order) {
            super(name, order);
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            System.out.println(" 3. [" + name + "] postProcessBeanDefinitionRegistry (PriorityOrdered)");
            System.out.println("    -> 이 시점에는 ConfigurationClassPostProcessor가 이미 @Bean/@Import/@ComponentScan을 해석했습니다.");
            printBeanNames("ConfigurationClassPostProcessor 처리 후", registry.getBeanDefinitionNames());
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println(" 8. [" + name + "] postProcessBeanFactory");
        }
    }

    static class OrderedRegistryPostProcessor extends RegistryPostProcessorSupport implements Ordered {
        OrderedRegistryPostProcessor(String name, int order) {
            super(name, order);
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            System.out.println(" 4. [" + name + "] postProcessBeanDefinitionRegistry (Ordered)");
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println(" 9. [" + name + "] postProcessBeanFactory");
        }
    }

    static class NonOrderedRegistryPostProcessor extends RegistryPostProcessorSupport {
        NonOrderedRegistryPostProcessor(String name) {
            super(name, Ordered.LOWEST_PRECEDENCE);
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
            System.out.println(" 5. [" + name + "] postProcessBeanDefinitionRegistry (non-ordered)");
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("10. [" + name + "] postProcessBeanFactory");
        }
    }

    abstract static class RegistryPostProcessorSupport implements BeanDefinitionRegistryPostProcessor {
        protected final String name;
        private final int order;

        RegistryPostProcessorSupport(String name, int order) {
            this.name = name;
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    static class DirectRegularBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("11. [직접 등록 BFPP] postProcessBeanFactory");
        }
    }

    static class PriorityOrderedBeanFactoryPostProcessor extends BeanFactoryPostProcessorSupport implements PriorityOrdered {
        PriorityOrderedBeanFactoryPostProcessor(String name, int order) {
            super(name, order);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("12. [" + name + "] postProcessBeanFactory (PriorityOrdered)");
        }
    }

    static class OrderedBeanFactoryPostProcessor extends BeanFactoryPostProcessorSupport implements Ordered {
        OrderedBeanFactoryPostProcessor(String name, int order) {
            super(name, order);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("13. [" + name + "] postProcessBeanFactory (Ordered)");
        }
    }

    abstract static class BeanFactoryPostProcessorSupport implements BeanFactoryPostProcessor {
        protected final String name;
        private final int order;

        BeanFactoryPostProcessorSupport(String name, int order) {
            this.name = name;
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    }

    static class NonOrderedBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
        private final String name;

        NonOrderedBeanFactoryPostProcessor(String name) {
            this.name = name;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            System.out.println("14. [" + name + "] postProcessBeanFactory");
        }
    }
}
