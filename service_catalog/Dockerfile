FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/service-catalog-1.0-SNAPSHOT.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
