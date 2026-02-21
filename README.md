# dev-journey-lab 🧪

궁금했던 것을 코드로 작성, 실행, 접하면서 확인한 것들을 기록합니다.

## Branches

| Branch                                                                                                  | Topic                   | Details                                                |
|:--------------------------------------------------------------------------------------------------------|:------------------------|:-------------------------------------------------------|
| [**java-internal**](https://github.com/jungbumwoo/dev-journey-lab/tree/java-internal)                  | **Java Internals**      | Abstract Class, Generics(Type Erasure, Bridge Method) 바이트코드 분석 |
| [**nio-server**](https://github.com/jungbumwoo/dev-journey-lab/tree/nio-server)                         | **Non-Blocking Server** | jenkov.com NIO server 예제 코드 최신화, minor 개선 및 테스트 추가     |
| [**coroutine**](https://github.com/jungbumwoo/dev-journey-lab/tree/coroutine)                           | **Coroutine**           | coroutine programming 방식 확인                            |
| [**spring/dive**](https://github.com/jungbumwoo/dev-journey-lab/tree/spring/dive)                       | **Spring Aop**          | 『토비의 스프링』 기반 AOP, Proxy, FactoryBean 원리 확인 및 예제 코드 최신화 |
| [**feat/json_parser**](https://github.com/jungbumwoo/dev-journey-lab/tree/feat/json_parser)             | **JSON Parser**         | 라이브러리 없이 직접 구현하며 json parser 확인                        |

---

# Java Internals (`java-internal` branch)

Abstract class와 Generics의 내부 동작을 바이트코드 수준에서 이해하는 예제 프로젝트.

## 프로젝트 구조

```
src/
├── abstractclass/
│   ├── Shape.java           # abstract class (ACC_ABSTRACT, abstract 메서드)
│   ├── Circle.java          # concrete subclass (invokespecial super())
│   ├── Rectangle.java       # concrete subclass
│   └── AbstractClassDemo.java  # 다형성, reflection, vtable 시연
└── generics/
    ├── Box.java             # generic class (Type Erasure, Signature attribute)
    ├── TypeErasureDemo.java # 타입 소거, checkcast, Signature 읽기
    ├── BridgeMethodDemo.java# bridge method (ACC_BRIDGE, ACC_SYNTHETIC)
    └── BoundedTypeDemo.java # bounded type, PECS (? extends / ? super)
scripts/
├── compile.sh   # javac로 전체 컴파일
├── run.sh       # 데모 실행
└── inspect.sh   # javap로 바이트코드 확인
```

## 빠른 시작

```bash
# 1. 컴파일
./scripts/compile.sh

# 2. 전체 데모 실행
./scripts/run.sh all

# 3. 바이트코드 확인
./scripts/inspect.sh help
```

---

## 학습 내용

### 1. Abstract Class 내부

| 개념 | 바이트코드 |
|------|------------|
| `abstract class` | `ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT` 플래그 |
| `abstract` 메서드 | `Code` attribute 없음, `ACC_ABSTRACT` 플래그만 존재 |
| `super()` 호출 | `invokespecial Shape.<init>` |
| 가상 메서드 호출 | `invokevirtual` → vtable로 런타임 타입의 메서드 결정 |

```bash
# 확인 명령어
./scripts/inspect.sh Shape -verbose      # ACC_ABSTRACT, abstract 메서드 (Code 없음)
./scripts/inspect.sh Circle -c           # invokespecial super() 호출
./scripts/inspect.sh AbstractClassDemo -c  # invokevirtual 다형성 호출
```

**핵심 바이트코드 예시:**
```
// Shape.class
flags: (0x0421) ACC_PUBLIC, ACC_SUPER, ACC_ABSTRACT

public abstract double area();
  flags: (0x0401) ACC_PUBLIC, ACC_ABSTRACT
  <- Code 속성 없음!

// Circle.class - constructor
invokespecial #1  // Method Shape."<init>":(Ljava/lang/String;DD)V
```

---

### 2. Generics - Type Erasure

Java 제네릭은 컴파일 타임에만 존재. 런타임에는 타입 정보가 소거됨.

| 소스 코드 | 컴파일 후 (erasure) |
|-----------|---------------------|
| `Box<T>` | `Box` (raw) |
| `T get()` | `Object get()` |
| `<T extends Comparable<T>>` | `Comparable` (upper bound) |
| `String s = box.get()` | `invokevirtual get()` + `checkcast String` |

```bash
./scripts/inspect.sh Box -verbose        # Signature: <T:Ljava/lang/Object;>...
./scripts/inspect.sh TypeErasureDemo -c  # Box.get()후 checkcast 인스트럭션
```

**핵심 바이트코드 예시:**
```
// Box.class
Signature: #38  // <T:Ljava/lang/Object;>Ljava/lang/Object;  <- 원래 제네릭 타입 보존

public T get();
  descriptor: ()Ljava/lang/Object;  <- T가 Object로 erasure
  Signature: ()TT;                  <- 원래 타입은 Signature에 보존

// 호출 측 (TypeErasureDemo)
invokevirtual #N  // Method generics/Box.get:()Ljava/lang/Object;
checkcast     #N  // class java/lang/String  <- 컴파일러 자동 삽입
```

---

### 3. Generics - Bridge Method

Type Erasure 이후에도 다형성을 유지하기 위해 컴파일러가 자동 생성.

**플래그:** `ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC`

```bash
./scripts/inspect.sh BridgeMethodDemo\$UpperCaseTransformer -p  # bridge method 확인
./scripts/inspect.sh BridgeMethodDemo\$Dog -p                    # 공변 반환 타입 bridge
```

**핵심 바이트코드 예시:**
```
// UpperCaseTransformer.class

// 실제 구현 메서드
public java.lang.String transform(java.lang.String);
  flags: (0x0001) ACC_PUBLIC

// 컴파일러 자동 생성 bridge method
public java.lang.Object transform(java.lang.Object);
  flags: (0x1041) ACC_PUBLIC, ACC_BRIDGE, ACC_SYNTHETIC
  Code:
    checkcast String      // (String) 캐스트
    invokevirtual transform(String)  // 실제 메서드 위임
```

---

### 4. Generics - PECS (Producer Extends, Consumer Super)

| 와일드카드 | 읽기 | 쓰기 | 사용 목적 |
|------------|------|------|-----------|
| `? extends T` | ✅ | ❌ | 데이터 생산 (읽기 전용) |
| `? super T` | ❌ (Object만) | ✅ | 데이터 소비 (쓰기 전용) |

```java
// Producer Extends: 읽기 전용
double sum(List<? extends Number> list)  // Integer, Double, Long 모두 수용

// Consumer Super: 쓰기 전용
void fill(List<? super Integer> list)    // List<Integer>, List<Number>, List<Object> 수용
```

---

## JVM 인스트럭션 요약

| 인스트럭션 | 의미 |
|------------|------|
| `invokevirtual` | 런타임 타입 기반 메서드 호출 (다형성) |
| `invokespecial` | 컴파일 타임 결정: `super()`, `private`, constructor |
| `invokeinterface` | 인터페이스 메서드 호출 |
| `invokestatic` | static 메서드 호출 |
| `checkcast` | 런타임 타입 캐스트 검사 (type erasure로 컴파일러 삽입) |
| `instanceof` | 타입 검사 |

---

# 컴파일
./gradlew compileJava

# 개별 데모 실행
./gradlew runAbstract                                                                                                                                                                                                                                                                                                  
./gradlew runErasure
./gradlew runBridge
./gradlew runBounded

# 전체 실행 (순서대로)
./gradlew runAll

# 바이트코드 검사
./gradlew inspect -PclassName=Shape
./gradlew inspect -PclassName=Shape           -Pflags="-verbose"
./gradlew inspect -PclassName=Box             -Pflags="-verbose"
./gradlew inspect -PclassName=UpperCaseTransformer  -Pflags="-p"
./gradlew inspect -PclassName=TypeErasureDemo -Pflags="-c"

# 사용 가능한 클래스 목록 확인
./gradlew inspect

build/classes/ 에 컴파일 결과가 저장되며, .gitignore의 build/에 의해 자동 제외됩니다.

