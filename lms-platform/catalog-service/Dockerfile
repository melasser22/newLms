FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/catalog-service-*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
