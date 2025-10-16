#!/bin/sh

# Older /bin/sh implementations included in some lightweight container images
# (notably BusyBox variants) do not recognise any options passed to `set`.
# Invoking `set -e` in those environments causes the interpreter to abort with
# an "illegal option" error before our logic runs, which was surfacing as the
# repeated `/app/entrypoint.sh: set: line 2: illegal option -` messages during
# container start-up.  The commands in this script are deliberately simple and
# either idempotent or followed by an explicit `exec`, so we can safely avoid
# relying on `set -e` here.

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
