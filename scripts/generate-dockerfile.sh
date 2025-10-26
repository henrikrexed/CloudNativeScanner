#!/bin/bash

# Generate Dockerfile for a specific service
# Usage: ./scripts/generate-dockerfile.sh <service-name> <platform> <version>

set -e

SERVICE_NAME=$1
PLATFORM=$2
VERSION=${3:-latest}

if [ -z "$SERVICE_NAME" ]; then
    echo "Error: Service name is required"
    echo "Usage: $0 <service-name> <platform> <version>"
    exit 1
fi

if [ -z "$PLATFORM" ]; then
    echo "Error: Platform is required"
    echo "Usage: $0 <service-name> <platform> <version>"
    exit 1
fi

# Validate service name
case $SERVICE_NAME in
    topic-scanner|topic-analyzer|webui)
        ;;
    *)
        echo "Error: Invalid service name '$SERVICE_NAME'. Must be one of: topic-scanner, topic-analyzer, webui"
        exit 1
        ;;
esac

# Validate platform
case $PLATFORM in
    linux/amd64|linux/arm64|linux/arm/v7|linux/arm/v6)
        ;;
    *)
        echo "Error: Invalid platform '$PLATFORM'. Must be one of: linux/amd64, linux/arm64, linux/arm/v7, linux/arm/v6"
        exit 1
        ;;
esac

SERVICE_DIR="./$SERVICE_NAME"
DOCKERFILE_PATH="$SERVICE_DIR/Dockerfile"

if [ ! -d "$SERVICE_DIR" ]; then
    echo "Error: Service directory '$SERVICE_DIR' does not exist"
    exit 1
fi

echo "Generating Dockerfile for $SERVICE_NAME (platform: $PLATFORM, version: $VERSION)"

# Determine base image based on platform
case $PLATFORM in
    linux/amd64)
        BASE_IMAGE="eclipse-temurin:17-jre-alpine"
        ARCH="amd64"
        ;;
    linux/arm64)
        BASE_IMAGE="eclipse-temurin:17-jre-alpine"
        ARCH="arm64"
        ;;
    linux/arm/v7)
        BASE_IMAGE="eclipse-temurin:17-jre-alpine"
        ARCH="armv7"
        ;;
    linux/arm/v6)
        BASE_IMAGE="eclipse-temurin:17-jre-alpine"
        ARCH="armv6"
        ;;
esac

# Generate Dockerfile content
cat > "$DOCKERFILE_PATH" << EOF
# Multi-stage build for $SERVICE_NAME
# Platform: $PLATFORM
# Version: $VERSION
# Generated: $(date)

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set build arguments
ARG VERSION=$VERSION
ARG PLATFORM=$PLATFORM
ARG ARCH=$ARCH

# Install build dependencies
RUN apk add --no-cache \\
    maven \\
    git \\
    && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml ./
COPY shared/pom.xml ./shared/
COPY $SERVICE_NAME/pom.xml ./$SERVICE_NAME/

# Download dependencies
RUN mvn dependency:go-offline -B -pl $SERVICE_NAME

# Copy source code
COPY shared/src ./shared/src
COPY $SERVICE_NAME/src ./$SERVICE_NAME/src

# Build application
RUN mvn clean package -DskipTests -B -pl $SERVICE_NAME \\
    && cp $SERVICE_NAME/target/*.jar app.jar

# Runtime stage
FROM $BASE_IMAGE

# Set build arguments
ARG VERSION=$VERSION
ARG PLATFORM=$PLATFORM
ARG ARCH=$ARCH

# Install runtime dependencies
RUN apk add --no-cache \\
    curl \\
    tzdata \\
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \\
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy application from builder stage
COPY --from=builder /app/app.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port (service-specific)
EOF

# Add service-specific port exposure
case $SERVICE_NAME in
    topic-scanner)
        echo "EXPOSE 8080" >> "$DOCKERFILE_PATH"
        echo "" >> "$DOCKERFILE_PATH"
        echo "# Health check" >> "$DOCKERFILE_PATH"
        echo "HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\\\" >> "$DOCKERFILE_PATH"
        echo "  CMD curl -f http://localhost:8080/actuator/health || exit 1" >> "$DOCKERFILE_PATH"
        ;;
    topic-analyzer)
        echo "EXPOSE 8081" >> "$DOCKERFILE_PATH"
        echo "" >> "$DOCKERFILE_PATH"
        echo "# Health check" >> "$DOCKERFILE_PATH"
        echo "HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\\\" >> "$DOCKERFILE_PATH"
        echo "  CMD curl -f http://localhost:8081/actuator/health || exit 1" >> "$DOCKERFILE_PATH"
        ;;
    webui)
        echo "EXPOSE 8082" >> "$DOCKERFILE_PATH"
        echo "" >> "$DOCKERFILE_PATH"
        echo "# Health check" >> "$DOCKERFILE_PATH"
        echo "HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\\\" >> "$DOCKERFILE_PATH"
        echo "  CMD curl -f http://localhost:8082/actuator/health || exit 1" >> "$DOCKERFILE_PATH"
        ;;
esac

# Add final instructions
cat >> "$DOCKERFILE_PATH" << EOF

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Add labels
LABEL org.opencontainers.image.title="$SERVICE_NAME"
LABEL org.opencontainers.image.description="Cloud Native Topic Scanner - $SERVICE_NAME Service"
LABEL org.opencontainers.image.version="$VERSION"
LABEL org.opencontainers.image.platform="$PLATFORM"
LABEL org.opencontainers.image.architecture="$ARCH"
LABEL org.opencontainers.image.created="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
LABEL org.opencontainers.image.source="https://github.com/your-org/cloud-native-scanner"

# Start application
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar app.jar"]
EOF

echo "Dockerfile generated successfully at $DOCKERFILE_PATH"
echo "Platform: $PLATFORM"
echo "Version: $VERSION"
echo "Base Image: $BASE_IMAGE"


