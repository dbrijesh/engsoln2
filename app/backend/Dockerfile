# Multi-stage Dockerfile for Spring Boot backend
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy POM file
COPY pom.xml .

# Download dependencies (cached layer) - non-fatal if Maven Central is unavailable
RUN mvn dependency:go-offline -B || echo "Dependency pre-download failed, will download during build"

# Copy source code
COPY src ./src

# Build the application (skip tests and quality checks - they run in CI)
RUN mvn clean package -DskipTests -Dcheckstyle.skip=true -Dspotless.apply.skip=true

# Stage 2: Create runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]
