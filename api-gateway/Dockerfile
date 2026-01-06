FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/api-gateway-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
