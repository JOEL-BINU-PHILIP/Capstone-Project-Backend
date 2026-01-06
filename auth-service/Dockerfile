FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/auth-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]

