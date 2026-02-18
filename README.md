# nio-server 🧪

프로젝트 설명: [NIO non-blocking server (NIO_SERVER_REVIEW.md)](https://github.com/jungbumwoo/dev-journey-lab/blob/nio-server/NIO_SERVER_REVIEW.md)

https://github.com/jjenkov/java-nio-server/tree/master 에 일부 개선 및 테스트를 추가하였음.

## Branches

[**main branch**](https://github.com/jungbumwoo/dev-journey-lab)

| Branch                                                                                      | Topic                   | Details                                                |
|:--------------------------------------------------------------------------------------------|:------------------------|:-------------------------------------------------------|
| [**nio-server**](https://github.com/jungbumwoo/dev-journey-lab/tree/nio-server)             | **Non-Blocking Server** | jenkov.com NIO server 예제 코드 최신화, minor 개선 및 테스트 추가     |
| [**coroutine**](https://github.com/jungbumwoo/dev-journey-lab/tree/coroutine)               | **Coroutine**           | coroutine programming 방식 확인                            |
| [**spring/dive**](https://github.com/jungbumwoo/dev-journey-lab/tree/spring/dive)           | **Spring Aop**          | 『토비의 스프링』 기반 AOP, Proxy, FactoryBean 원리 확인 및 예제 코드 최신화 |
| [**feat/json_parser**](https://github.com/jungbumwoo/dev-journey-lab/tree/feat/json_parser) | **JSON Parser**         | 라이브러리 없이 직접 구현하며 json parser 확인                        |


## Build and Run

This project uses Gradle.

### Prerequisites

*   Java Development Kit (JDK) 11 or higher.

### Build the Project

To build the project, navigate to the root directory of the project and run the following command:

```bash
./gradlew build
```

### Run test
```bash
./gradlew clean test
```

### Run the NIO Server Example

To run the `Main.java` example, which demonstrates a basic HTTP server, use the following command:

```bash
./gradlew :app:run
```

Once the server is running (it will listen on port `9999` by default), you can test it using a web browser or `curl`:

*   **Default response:**
    ```bash
    curl http://localhost:9999/
    ```
    (Expected output: `Hello World!`)

*   **Response with a name parameter:**
    ```bash
    curl http://localhost:9999/?name=YourName
    ```
    (Expected output: `Hello YourName!`)

To stop the server, press `Ctrl+C` in the terminal where it is running.
