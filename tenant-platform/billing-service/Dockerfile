# syntax=docker/dockerfile:1.7

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY shared-lib ./shared-lib
COPY tenant-platform/pom.xml ./tenant-platform/pom.xml
COPY tenant-platform/billing-service/pom.xml ./tenant-platform/billing-service/pom.xml
COPY tenant-platform/billing-service/src ./tenant-platform/billing-service/src

RUN mvn -f shared-lib/pom.xml -B -DskipTests install \
    && mvn -f tenant-platform/billing-service/pom.xml -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /workspace/tenant-platform/billing-service/target/billing-service-*.jar ./

RUN set -eux; \
    BOOT_JAR=$(ls billing-service-*.jar | grep -v -- '-plain\\.jar'); \
    mv "$BOOT_JAR" app.jar; \
    rm -f billing-service-*-plain.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
