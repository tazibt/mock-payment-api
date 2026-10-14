# Multi-stage build for Spring Boot application
# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy source code and build files
COPY . .

# Build the application using Maven wrapper
RUN chmod +x mvnw && ./mvnw -q clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -g 1000 appuser && adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder --chown=appuser:appuser /build/target/*.jar app.jar
# Copy external application config into the runtime image
COPY --from=builder --chown=appuser:appuser /build/config ./config

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/payment/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
