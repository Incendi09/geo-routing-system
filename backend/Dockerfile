# Multi-stage Dockerfile for the Java backend
# Builds a fat JAR and runs it on a minimal JRE image

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy build files first for better layer caching
COPY build.gradle settings.gradle ./

# Pre-download dependencies (cached if build files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src/ src/

# Build the fat JAR
RUN gradle fatJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

# Install curl for health check
RUN apk add --no-cache curl

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=builder /app/build/libs/*-all.jar app.jar

# Data files are mounted via docker-compose volumes
# No need to copy them here

# Default port (can be overridden via SERVER_PORT env var)
EXPOSE 8080

# Health check using curl
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/evac/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
