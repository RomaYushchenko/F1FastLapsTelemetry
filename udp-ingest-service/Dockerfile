# Stage 1: Maven Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml files for dependency caching
COPY pom.xml ./
COPY telemetry-api-contracts/pom.xml ./telemetry-api-contracts/
COPY f1-telemetry-udp-core/pom.xml ./f1-telemetry-udp-core/
COPY f1-telemetry-udp-spring/pom.xml ./f1-telemetry-udp-spring/
COPY f1-telemetry-udp-starter/pom.xml ./f1-telemetry-udp-starter/
COPY udp-ingest-service/pom.xml ./udp-ingest-service/
COPY telemetry-processing-api-service/pom.xml ./telemetry-processing-api-service/
COPY infra/pom.xml ./infra/

# Download dependencies (this layer will be cached if pom.xml files don't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY telemetry-api-contracts/ ./telemetry-api-contracts/
COPY f1-telemetry-udp-core/ ./f1-telemetry-udp-core/
COPY f1-telemetry-udp-spring/ ./f1-telemetry-udp-spring/
COPY f1-telemetry-udp-starter/ ./f1-telemetry-udp-starter/
COPY udp-ingest-service/ ./udp-ingest-service/

# Build the project
RUN mvn clean package -pl udp-ingest-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-privileged user
RUN groupadd -r f1app && useradd -r -g f1app f1app

# Copy JAR from builder stage
COPY --from=builder /build/udp-ingest-service/target/*.jar app.jar

# Change ownership
RUN chown -R f1app:f1app /app

# Switch to non-privileged user
USER f1app

# Expose UDP port for F1 25 telemetry
EXPOSE 20777/udp

# Health check (simple check that process is running)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD pgrep -f "java.*app.jar" || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
