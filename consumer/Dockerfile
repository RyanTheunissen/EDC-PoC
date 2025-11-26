# Build the consumer jar with Gradle
FROM gradle:8.10-jdk17-alpine AS build

WORKDIR /workspace

COPY . .

RUN gradle :consumer:build -x test --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/consumer/build/libs/consumer-all.jar app.jar

COPY consumer/config.properties config.properties

EXPOSE 29191 29192 29193 29194 29291

CMD ["java", "-Dedc.fs.config=/app/config.properties", "-jar", "app.jar"]
