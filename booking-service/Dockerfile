FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY target/booking-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]
