
## Branch: `spring/dive`
 토비의 스프링 3.1 Vol. 1 - 6장(AOP)의 내용을 학습함

### 학습 자료
- **교재**: [토비의 스프링 3.1 (이일민 저)](https://product.kyobobook.co.kr/detail/S000000935360)
- **참조 소스**: [AcornPublishing/toby-spring3-1](https://github.com/AcornPublishing/toby-spring3-1)

### 변경 사항
- **코드 상세 주석**: 각 클래스와 Test 코드에 주석을 추가함
- **최신 스택 적용**: Java 25, Spring Boot 4 기반으로 환경 코드 최신화.
- **의존성 주입 방식 변경**: 예제의 기본 `Setter 주입` 방식을 `생성자 주입(Constructor Injection)`으로 변경함.

### 본 브랜치에서 다루는 개념
- **JDK Dynamic Proxy**: 인터페이스 기반의 런타임 프록시 생성
- **FactoryBean**: 스프링 빈 생성 로직이 복잡할 때 사용하는 커스텀 팩토리 인터페이스.
- **Transaction Abstraction**: `PlatformTransactionManager`를 통한 트랜잭션 관리.
- **AOP (Aspect Oriented Programming)**: 부가기능과 핵심 로직의 분리.