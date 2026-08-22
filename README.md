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

### Proxy 내부 구조 데모

[`ProxyMechanismDemo`](app/src/main/java/com/jungbum/aop/proxy/ProxyMechanismDemo.java)는 같은
`CalculatorService` 호출을 JDK Dynamic Proxy, CGLIB, 축약한 Spring interceptor chain으로 각각 실행한다.

```console
./gradlew :app:proxyMechanismDemo
./gradlew :app:test --tests 'com.jungbum.aop.proxy.*'
```

#### JDK Dynamic Proxy와 InvocationHandler

JDK 프록시는 `java.lang.reflect.Proxy`의 서브클래스이면서 요청한 인터페이스를 구현하는 클래스를
런타임에 만든다. 생성된 각 인터페이스 메서드는 대략 다음 일을 한다.

1. 호출 인자를 `Object[]`로 만든다.
2. 프록시가 가진 `InvocationHandler`에 `proxy`, `Method`, 인자를 전달한다.
3. 이 예제의 [`TracingInvocationHandler`](app/src/main/java/com/jungbum/aop/proxy/TracingInvocationHandler.java)는
   전후 동작을 수행한 뒤 `Method.invoke(target, args)`로 실제 객체를 호출한다.

따라서 프록시 생성 코드는 메서드마다 필요하지 않고 핸들러 하나에 모인다. 반면 인터페이스가
필수이고, `equals`, `hashCode`, `toString`도 핸들러에 들어오기 때문에 그 정책을 명시해야 한다.
`InvocationHandler` 자체가 target을 자동으로 호출하는 것은 아니다. 테스트처럼 값을 바로 반환할 수도
있으며, 이 예제는 일반적인 위임 프록시를 보여주기 위해 핸들러 안에서 reflection을 선택했다.

#### CGLIB은 바이트코드를 어떻게 조작하는가

`Enhancer`는 target class의 생성자와 오버라이드 가능한 메서드를 조사하고, Spring에 포함된 ASM으로
새 `.class` 바이트 배열을 만든 뒤 ClassLoader에 정의한다. 생성된 클래스는 개념적으로 다음 모양이다.

```java
class CalculatorService$$EnhancerByCGLIB extends CalculatorService {
    MethodInterceptor callback;

    @Override
    public int add(int left, int right) {
        return (int) callback.intercept(this, ADD_METHOD, new Object[] {left, right}, ADD_METHOD_PROXY);
    }

    final int CGLIB$add$0(int left, int right) {
        return super.add(left, right); // MethodProxy.invokeSuper가 도달하는 경로
    }
}
```

실제 이름과 세부 구현은 버전에 따라 달라지지만 핵심은 "서브클래스 + override + callback"이다.
그래서 `final` 클래스는 프록시를 만들 수 없고 `final`/`private` 메서드는 가로챌 수 없다.
[`CglibBytecodeDemo`](app/src/main/java/com/jungbum/aop/proxy/CglibBytecodeDemo.java)는 CGLIB의
generation strategy에서 실제 바이트 배열을 캡처한다.

```console
./gradlew :app:cglibBytecodeDemo
# 데모가 마지막에 출력한 inspect command를 그대로 실행한다.
```

raw CGLIB 예제의 `MethodProxy.invokeSuper(proxy, args)`는 같은 프록시 인스턴스의 부모 구현으로 간다.
반면 Spring의 `CglibAopProxy.DynamicAdvisedInterceptor`는 프록시와 별도로 target을 얻고 advisor chain을
구한다. chain이 있으면 `ReflectiveMethodInvocation.proceed()`가 interceptor를 하나씩 실행하고,
마지막 joinpoint에서 reflection으로 target을 호출한다. 즉 CGLIB은 Spring AOP에서 "호출을 낚아채는
입구"이고, 그 뒤의 advice chain 및 target 호출 방식과는 별개다.

#### Reflection API는 왜 느릴 수 있는가

직접 호출은 수신 타입과 인자/반환 타입이 bytecode에 구체적으로 들어 있어 JIT가 인라이닝과
특수화를 하기 쉽다. `Method.invoke(Object, Object...)` 경계에는 다음 비용이 추가될 수 있다.

- 가변 인자 `Object[]` 구성과 primitive boxing/unboxing
- 접근 권한, 인자 개수, 런타임 타입 호환성 검사
- target이 던진 예외를 `InvocationTargetException`으로 감싸고 다시 해석하는 경로
- 동적으로 바뀔 수 있는 `Method` 때문에 직접 호출보다 어려운 인라이닝/최적화

JDK 18 이후 core reflection은 내부적으로 MethodHandle 위에 다시 구현되었으므로, 오래된 설명처럼
항상 JNI/native 호출을 거친다고 이해하면 안 된다. 고정된 `Method`와 충분한 워밍업에서는 JVM이
상당 부분 최적화할 수 있고, 실제 차이는 호출 모양과 JVM 버전에 따라 달라진다.
[`ReflectionCostDemo`](app/src/main/java/com/jungbum/aop/proxy/ReflectionCostDemo.java)는 워밍업과 결과
소비를 포함한 관찰용 코드다. 숫자를 신뢰해야 하는 성능 판단에는 JMH를 사용해야 한다.

```console
./gradlew :app:reflectionCostDemo
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
