# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /usr/src/app
COPY pom.xml .
COPY src ./src
# Cache dependencies
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B && \
    mvn -B -Dmaven.test.skip package && \
    rm -rf /root/.m2

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="T4M Notification Service" \
      org.opencontainers.image.description="Notification management service for T4M alerts" \
      org.opencontainers.image.version="v0.5" \
      org.opencontainers.image.vendor="Athens Technology Center (ATC)"

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /usr/src/app/target/*.jar app.jar

# JVM tuning
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=100", \
            "-XX:+ParallelRefProcEnabled", \
            "-XX:+HeapDumpOnOutOfMemoryError", \
            "-XX:+DisableExplicitGC", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "/app/app.jar"]