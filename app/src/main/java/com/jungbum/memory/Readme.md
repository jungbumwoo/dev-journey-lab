# JVM Memory Structure Lab

이 프로젝트는 Java Virtual Machine(JVM)의 핵심 메모리 구조와 각 영역의 동작 원리를 코드로 직접 실행해 보며 탐구하기 위한 실습 저장소입니다.
https://github.com/WegraLee/JVM 을 참고하였습니다.


## 프로젝트 구조

각 패키지는 JVM의 주요 메모리 영역별로 구성되어 있으며, 영역별 특징을 증명하고 확인할 수 있는 예제 코드를 포함합니다.

```text
com.jungbum.memory/
├── overview/                                                                                                                                                                                                                                                                                                           
│   └── JVMMemoryOverview.java          # 전체 메모리 구조 개요 + MXBean으로 실제 사용량 확인                                                                                                                                                                                                                           
├── heap/                                                                                                                                                                                                                                                                                                               
│   ├── HeapMemoryExample.java          # 객체 할당, GC, 메모리 사용량 확인                                                                                                                                                                                                                                           
│   └── YoungOldGenerationExample.java  # Young/Old Generation, Minor GC, 객체 승격                                                                                                                                                                                                                                     
├── stack/                                                                                                                                                                                                                                                                                                              
│   ├── VMStackExample.java             # 스택 프레임, 지역 변수, 오퍼랜드 스택, 메서드 호출 체인                                                                                                                                                                                                                       
│   └── StackFrameDetailExample.java    # 슬롯 재사용과 GC, this 참조                                                                                                                                                                                                                                                   
├── methodarea/                                                                                                                                                                                                                                                                                                         
│   └── MethodAreaExample.java          # 클래스 메타데이터, static 변수, 클래스 로딩 과정                                                                                                                                                                                                                              
├── pc/                                                                                                                                                                                                                                                                                                                 
│   └── ProgramCounterExample.java      # 컨텍스트 스위칭, 바이트코드 주소 개념                                                                                                                                                                                                                                       
├── constantpool/                                                                                                                                                                                                                                                                                                       
│   └── RuntimeConstantPoolExample.java # String Pool, intern(), 컴파일 타임 상수, Integer Cache                                                                                                                                                                                                                      
├── directmemory/                                                                                                                                                                                                                                                                                                       
│   └── DirectMemoryExample.java        # Heap Buffer vs Direct Buffer, 성능 비교                                                                                                                                                                                                                                     
└── nativestack/                                                                                                                                                                                                                                                                                                        
    └── NativeMethodStackExample.java   # native 메서드 확인, 호출 흐름
```

## 영역별 핵심 학습 포인트

| 메모리 영역 | 핵심 내용 |
| :--- | :--- |
| **Heap** | 객체/배열 할당, GC(Garbage Collection) 동작 원리, Young/Old Generation, 객체 승격(Promotion) 현상 확인 |
| **VM Stack** | 스택 프레임 구조(지역 변수 테이블, 오퍼랜드 스택), 로컬 변수 슬롯 재사용, 스레드별 독립적인 메모리 할당 |
| **Method Area** | 클래스 메타데이터 저장소, `static` 변수, 클래스 로딩 생명주기(Loading → Linking → Initialization) |
| **PC Register** | 멀티스레드 환경에서 컨텍스트 스위칭 시의 역할, 실행 중인 바이트코드 주소(Address) 개념 |
| **Runtime Constant Pool** | Java `String Pool` 메커니즘과 `intern()`, Constant Folding 최적화, Integer Cache 동작 |
| **Direct Memory** | NIO `DirectByteBuffer` 활용, Zero-copy 개념, 기존 Heap Buffer와의 성능(I/O) 비교 |
| **Native Method Stack** | `native` 키워드 메서드 식별, JNI 호출 시 VM Stack과의 컨텍스트 전환 및 관계 |

## 실행 가이드 (Getting Started)

1. **시작하기 (Overview)**
    - 가장 먼저 `overview/JVMMemoryOverview.java`를 실행해 보세요. JVM 전체 구조와 현재 메모리 사용량을 한눈에 파악할 수 있는 훌륭한 출발점입니다.

2. **VM 옵션(매개변수) 설정 필수**
    - 각 실습 파일이 의도한 대로 동작하기 위해서는(예: OutOfMemoryError 유발 등) 특정한 JVM 설정이 필요
    - 각 파일 상단의 `Javadoc` 주석에 **실행 방법**과 **필수 VM 매개변수(VM Options)**가 명시되어 있으니, IDE에서 실행 구성을 세팅할 때 반드시 참고해 주세요.
                                                                                  