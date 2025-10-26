#!/bin/bash

# Build script for Cloud Native Scanner microservices

set -e

echo "Building Cloud Native Scanner microservices..."

# Build topic-scanner
echo "Building topic-scanner..."
cd topic-scanner
mvn clean package -DskipTests
docker build -t topic-scanner:1.0.0 .
cd ..

# Build topic-analyzer
echo "Building topic-analyzer..."
cd topic-analyzer
mvn clean package -DskipTests
docker build -t topic-analyzer:1.0.0 .
cd ..

# Build webui
echo "Building webui..."
cd webui
mvn clean package -DskipTests
docker build -t webui:1.0.0 .
cd ..

echo "All microservices built successfully!"
echo ""
echo "Docker images created:"
echo "- topic-scanner:1.0.0"
echo "- topic-analyzer:1.0.0"
echo "- webui:1.0.0"
echo ""
echo "To deploy to Kubernetes, run:"
echo "kubectl apply -f k8s/"


