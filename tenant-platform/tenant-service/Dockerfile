FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/tenant-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
