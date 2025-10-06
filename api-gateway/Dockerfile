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
    find . -maxdepth 1 -type f \( -name 'api-gateway-*-plain.jar' -o -name 'api-gateway-*-stubs.jar' -o -name 'api-gateway-*.jar.original' \) -delete; \
    cat <<'EOF' > /entrypoint.sh
#!/bin/sh
set -e

if [ -z "${SPRING_PROFILES_ACTIVE:-}" ]; then
  export SPRING_PROFILES_ACTIVE=dev
  echo "SPRING_PROFILES_ACTIVE not set. Defaulting to 'dev'."
else
  echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
fi

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  export SPRING_DATASOURCE_URL="${DATABASE_URL}"
  echo "Mapped DATABASE_URL -> SPRING_DATASOURCE_URL"
fi

if [ -n "${KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
  if [ -z "${SPRING_KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
    export SPRING_KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}"
  fi
  if [ -z "${SHARED_KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
    export SHARED_KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}"
  fi
  echo "Using Kafka bootstrap servers from environment"
fi

exec java $JAVA_OPTS -jar /app/app.jar
EOF

RUN chmod +x /entrypoint.sh
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

ENTRYPOINT ["/entrypoint.sh"]
