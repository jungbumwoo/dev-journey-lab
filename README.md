# dev-journey-lab



@Transactional 어노테이션이 적용된 메소드가 호출될 때 스프링이 기본적으로 어떻게 동작하여 트랜잭션을 관리하나요?

스프링이 런타임에 해당 객체의 프록시를 생성하여 트랜잭션 처리 로직을 추가함


스프링에서 트랜잭션 추상화를 위해 제공하는 핵심 인터페이스?
PlatformTransactionManager


스프링에서 트랜잭션 동기화를 위해 주로 사용하는 메커니즘은 무엇인가요?
ThreadLocal과 TransactionSynchronizationManager

스프링 애플리케이션 개발 시 트랜잭션 관리 방식으로 일반적으로 가장 많이 사용되고 권장되는 방식?
선언적(Declarative) 트랜잭션 관리