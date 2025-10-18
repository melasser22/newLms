# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jre-alpine AS runner

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup \
    && apk add --no-cache wget

WORKDIR /app

COPY target/*.jar ./

RUN set -eux; \
    BOOT_JAR=$(find . -maxdepth 1 -name 'tenant-service-*.jar' ! -name '*-plain.jar' | head -n 1); \
    [ -n "$BOOT_JAR" ]; \
    mv "$BOOT_JAR" app.jar; \
    find . -maxdepth 1 -name 'tenant-service-*-plain.jar' -delete

COPY entrypoint.sh ./entrypoint.sh

RUN sed -i 's/\r$//' /app/entrypoint.sh \
    && chmod +x /app/entrypoint.sh \
    && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
