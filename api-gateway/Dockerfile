# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jre-alpine AS runner

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup \
    && apk add --no-cache curl

WORKDIR /app

COPY target/*.jar ./

RUN set -eux; \
    BOOT_JAR=$(find . -maxdepth 1 -type f -name 'api-gateway-*.jar' ! -name '*-plain.jar' ! -name '*-stubs.jar' ! -name '*.original' | sort | head -n 1); \
    [ -n "$BOOT_JAR" ]; \
    mv "$BOOT_JAR" app.jar; \
    find . -maxdepth 1 -type f \( -name 'api-gateway-*-plain.jar' -o -name 'api-gateway-*-stubs.jar' -o -name 'api-gateway-*.jar.original' \) -delete

COPY entrypoint.sh ./entrypoint.sh

RUN sed -i 's/\r$//' /app/entrypoint.sh \
    && chmod +x /app/entrypoint.sh \
    && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

ENTRYPOINT ["/bin/sh", "/app/entrypoint.sh"]
