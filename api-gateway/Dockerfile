# syntax=docker/dockerfile:1.7

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY shared-lib ./shared-lib
COPY api-gateway/pom.xml ./api-gateway/pom.xml
COPY api-gateway/src ./api-gateway/src

RUN mvn -pl api-gateway -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup \
    && apk add --no-cache curl

WORKDIR /app

COPY --from=builder /workspace/api-gateway/target/api-gateway-*.jar ./app.jar

RUN chown -R appuser:appgroup /app

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication" \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
