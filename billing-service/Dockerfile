FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/billing-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java","-jar","/app/app.jar"]
