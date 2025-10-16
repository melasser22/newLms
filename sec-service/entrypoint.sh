#!/bin/sh
set -e

if [ -z "${SPRING_PROFILES_ACTIVE:-}" ]; then
  SPRING_PROFILES_ACTIVE=dev
  export SPRING_PROFILES_ACTIVE
  echo "SPRING_PROFILES_ACTIVE not set. Defaulting to 'dev'."
else
  echo "SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}"
fi

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  SPRING_DATASOURCE_URL="${DATABASE_URL}"
  export SPRING_DATASOURCE_URL
  echo "Mapped DATABASE_URL -> SPRING_DATASOURCE_URL"
fi

if [ -n "${KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
  if [ -z "${SPRING_KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
    SPRING_KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}"
    export SPRING_KAFKA_BOOTSTRAP_SERVERS
  fi
  if [ -z "${SHARED_KAFKA_BOOTSTRAP_SERVERS:-}" ]; then
    SHARED_KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BOOTSTRAP_SERVERS}"
    export SHARED_KAFKA_BOOTSTRAP_SERVERS
  fi
  echo "Using Kafka bootstrap servers from environment"
fi

exec java $JAVA_OPTS -jar /app/app.jar
