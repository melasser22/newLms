# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jre-alpine AS runner

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY target/*.jar ./

RUN set -eux; \
    BOOT_JAR=$(find . -maxdepth 1 -name 'analytics-service-*.jar' ! -name '*-plain.jar' | head -n 1); \
    [ -n "$BOOT_JAR" ]; \
    mv "$BOOT_JAR" app.jar; \
    find . -maxdepth 1 -name 'analytics-service-*-plain.jar' -delete; \
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

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]
