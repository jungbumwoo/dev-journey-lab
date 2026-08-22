FROM gradle:9.1.0-jdk21 AS builder
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY client-lab ./client-lab
RUN gradle --no-daemon :client-lab:installDist

FROM eclipse-temurin:21-jre
WORKDIR /opt/redis-lab
COPY --from=builder /workspace/client-lab/build/install/client-lab ./
ENTRYPOINT ["/opt/redis-lab/bin/client-lab"]
