## Branch: `spring/dive`
 토비의 스프링 3.1 Vol. 1 - 6장(AOP)의 내용을 학습함

### 학습 자료
- **교재**: [토비의 스프링 3.1 (이일민 저)](https://product.kyobobook.co.kr/detail/S000000935360)
- **참조 소스**: [AcornPublishing/toby-spring3-1](https://github.com/AcornPublishing/toby-spring3-1)

### 변경 사항
- **코드 상세 주석**: 각 클래스와 Test 코드에 주석을 추가함
- **최신 스택 적용**: Java 25, Spring Boot 4 기반으로 환경 코드 최신화.
- **의존성 주입 방식 변경**: xml 사용 X, 예제의 기본 `Setter 주입` 방식을 `생성자 주입(Constructor Injection)`으로 변경함.

### 본 브랜치에서 다루는 개념
- **JDK Dynamic Proxy**: 인터페이스 기반의 런타임 프록시 생성
- **FactoryBean**: 스프링 빈 생성 로직이 복잡할 때 사용하는 커스텀 팩토리 인터페이스.
- **Transaction Abstraction**: `PlatformTransactionManager`를 통한 트랜잭션 관리.
- **AOP (Aspect Oriented Programming)**: 부가기능과 핵심 로직의 분리.
- **BeanDefinition**: 스프링 컨테이너가 빈을 생성하기 전에 사용하는 빈 메타데이터/설계도.
- **BeanPostProcessor**: 스프링 컨테이너가 빈 초기화 전후에 호출하는 확장 포인트. 빈 객체의 상태를 조작하거나 프록시 같은 다른 객체로 바꿔치기할 수 있음.

### BeanDefinition 데모 실행

[`BeanDefinitionDemo`](app/src/main/java/com/jungbum/bean/BeanDefinitionDemo.java)는 다음 흐름을 보여준다.

- `BeanDefinitionBuilder`로 빈의 클래스, property, scope, lazy-init 정보를 직접 등록함.
- `refresh()` 전에는 설계도만 있고, `refresh()` 이후 컨테이너가 설계도를 바탕으로 빈을 생성함.
- `singleton`과 `prototype` 스코프의 객체 생성 차이를 비교함.

```console
foo@bar:~$ ./gradlew :app:beanDefinitionDemo
```

### BeanPostProcessor 데모 실행

[`BeanPostProcessorDemo`](app/src/main/java/com/jungbum/bean/BeanPostProcessorDemo.java)는 다음 흐름을 보여준다.

- `postProcessBeforeInitialization`: `DefaultGreetingService`의 `prefix` 필드를 바꿔 빈 객체의 상태를 조작함.
- `postProcessAfterInitialization`: `DefaultPaymentService`를 JDK 동적 프록시 객체로 바꿔치기해 메서드 호출 전후 부가기능을 실행함.

```console
foo@bar:~$ ./gradlew :app:beanPostProcessorDemo
```

### BeanFactoryPostProcessor 실행 순서 데모

[`BeanFactoryPostProcessorOrderDemo`](app/src/main/java/com/jungbum/bean/BeanFactoryPostProcessorOrderDemo.java)는 다음 흐름을 보여준다.

- `BeanDefinitionRegistryPostProcessor`가 일반 `BeanFactoryPostProcessor`보다 먼저 실행됨.
- 각 그룹 안에서 `PriorityOrdered` → `Ordered` → order 없는 구현체 순서로 실행됨.
- `ConfigurationClassPostProcessor`가 `@Configuration`, `@ComponentScan`, `@Bean`, `@Import`를 해석해 새로운 `BeanDefinition`을 등록함.

```console
foo@bar:~$ ./gradlew :app:beanFactoryPostProcessorOrderDemo
```

### Test 실행
```console
foo@bar:~$ gradle clean test
```

# dev-journey-lab 🧪

A space to record my learnings by writing, running, and experimenting with code to satisfy my technical curiosity.

## Branches

| Branch | Topic | Details |
|:---|:---|:---|
| [**nio-server**](https://github.com/jungbumwoo/dev-journey-lab/tree/nio-server) | **Non-Blocking Server** | Updated the jenkov.com NIO server example code, applied minor improvements, and added tests. |
| [**coroutine**](https://github.com/jungbumwoo/dev-journey-lab/tree/coroutine) | **Coroutine** | Exploring coroutine programming paradigms and mechanics. |
| [**java-internal**](https://github.com/jungbumwoo/dev-journey-lab/tree/java-internal) | **Java Internals** | Bytecode analysis of Abstract Classes and Generics (Type Erasure, Bridge Methods). |
| [**spring/dive**](https://github.com/jungbumwoo/dev-journey-lab/tree/spring/dive) | **Spring Aop** | Exploring the principles of AOP, Proxy, and FactoryBean based on *"Toby's Spring"*, along with modernized example code. |
| [**feat/json_parser**](https://github.com/jungbumwoo/dev-journey-lab/tree/feat/json_parser) | **JSON Parser** | Implementing a JSON parser from scratch without external libraries to understand how it works. |

