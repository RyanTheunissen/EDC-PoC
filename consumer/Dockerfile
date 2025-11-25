FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/consumer-all.jar app.jar
COPY config.properties config.properties

EXPOSE 29191 29192 29193 29194 29291

CMD ["java", "-Dedc.fs.config=/app/config.properties", "-jar", "app.jar"]
