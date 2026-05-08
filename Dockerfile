# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN <<EOF
chmod +x gradlew
./gradlew dependencies --no-daemon
EOF

COPY src ./src

RUN ./gradlew bootjar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]