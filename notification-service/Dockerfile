FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/notification-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
