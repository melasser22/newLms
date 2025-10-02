# syntax=docker/dockerfile:1.7

FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy shared libraries and module sources
COPY shared-lib ./shared-lib
COPY tenant-platform/pom.xml ./tenant-platform/pom.xml
COPY tenant-platform/catalog-service/pom.xml ./tenant-platform/catalog-service/pom.xml
COPY tenant-platform/catalog-service/src ./tenant-platform/catalog-service/src

# Build shared libraries and package the catalog-service module
RUN mvn -f shared-lib/pom.xml -B -DskipTests install \
    && mvn -f tenant-platform/catalog-service/pom.xml -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup \
    && adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the packaged application JAR from the builder stage
COPY --from=builder /workspace/tenant-platform/catalog-service/target/catalog-service-*.jar ./

# Select the Spring Boot fat JAR (exclude the "-plain" classifier) and rename it
RUN set -eux; \
    BOOT_JAR=$(ls catalog-service-*.jar | grep -v -- '-plain\\.jar'); \
    mv "$BOOT_JAR" app.jar; \
    rm -f catalog-service-*-plain.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the correct port
EXPOSE 8080

# Add JVM optimizations
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
