# syntax=docker/dockerfile:1.7

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY shared-lib ./shared-lib
COPY tenant-platform ./tenant-platform

RUN mvn -f shared-lib/pom.xml -B -DskipTests install \
    && mvn -f tenant-platform/pom.xml -pl tenant-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine AS runner

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /workspace/tenant-platform/tenant-service/target/*.jar ./

RUN set -eux; \
    BOOT_JAR=$(find . -maxdepth 1 -name 'tenant-service-*.jar' ! -name '*-plain.jar' | head -n 1); \
    [ -n "$BOOT_JAR" ]; \
    mv "$BOOT_JAR" app.jar; \
    find . -maxdepth 1 -name 'tenant-service-*-plain.jar' -delete; \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
