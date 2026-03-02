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


# Java Internals (`java-internal` branch)

Bytecode analysis of Abstract Classes and Generics (Type Erasure, Bridge Methods).

## Project Structure

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

## Run

```bash
# 1. 컴파일
./scripts/compile.sh

# 2. 전체 데모 실행
./scripts/run.sh all

# 3. 바이트코드 확인
./scripts/inspect.sh help
```

---

## Learnings

### 1. Abstract Class Internals

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

## JVM Instructions Summary

| 인스트럭션 | 의미 |
|------------|------|
| `invokevirtual` | 런타임 타입 기반 메서드 호출 (다형성) |
| `invokespecial` | 컴파일 타임 결정: `super()`, `private`, constructor |
| `invokeinterface` | 인터페이스 메서드 호출 |
| `invokestatic` | static 메서드 호출 |
| `checkcast` | 런타임 타입 캐스트 검사 (type erasure로 컴파일러 삽입) |
| `instanceof` | 타입 검사 |

---
### Compile
./gradlew compileJava

### Run Individual Demos
./gradlew runAbstract
./gradlew runErasure
./gradlew runBridge
./gradlew runBounded

### Run All (in sequence)
./gradlew runAll

### Inspect Bytecode
./gradlew inspect -PclassName=Shape
./gradlew inspect -PclassName=Shape           -Pflags="-verbose"
./gradlew inspect -PclassName=Box             -Pflags="-verbose"
./gradlew inspect -PclassName=UpperCaseTransformer  -Pflags="-p"
./gradlew inspect -PclassName=TypeErasureDemo -Pflags="-c"

### Check Available Classes
./gradlew inspect

build/classes/ 에 컴파일 결과가 저장되며, .gitignore의 build/에 의해 자동 제외됩니다.