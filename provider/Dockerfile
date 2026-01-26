FROM eclipse-temurin:17-jre

WORKDIR /app

COPY provider/docker/provider-all.jar app.jar

COPY provider/config.properties config.properties

EXPOSE 19191 19192 19193 19194 19291

CMD ["java", "-Dedc.fs.config=/app/config.properties", "-jar", "app.jar"]
