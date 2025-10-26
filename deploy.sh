#!/bin/bash

# Deployment script for Cloud Native Scanner

set -e

echo "Deploying Cloud Native Scanner to Kubernetes..."

# Apply all Kubernetes manifests
echo "Creating namespace..."
kubectl apply -f k8s/namespace.yaml

echo "Deploying PostgreSQL..."
kubectl apply -f k8s/postgresql.yaml

echo "Deploying Kafka..."
kubectl apply -f k8s/kafka.yaml

echo "Waiting for infrastructure to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n topic-scanner --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n topic-scanner --timeout=300s

echo "Deploying topic-analyzer..."
kubectl apply -f k8s/topic-analyzer.yaml

echo "Deploying webui..."
kubectl apply -f k8s/webui.yaml

echo "Deploying topic-scanner CronJob..."
kubectl apply -f k8s/topic-scanner-cronjob.yaml

echo "Waiting for services to be ready..."
kubectl wait --for=condition=ready pod -l app=topic-analyzer -n topic-scanner --timeout=300s
kubectl wait --for=condition=ready pod -l app=webui -n topic-scanner --timeout=300s

echo "Deployment completed successfully!"
echo ""
echo "Services deployed:"
echo "- PostgreSQL database"
echo "- Kafka message broker"
echo "- Topic Analyzer (2 replicas)"
echo "- Web UI (2 replicas)"
echo "- Topic Scanner (CronJob - runs daily at 2 AM)"
echo ""
echo "To access the Web UI:"
echo "kubectl port-forward -n topic-scanner service/webui 8080:80"
echo "Then open http://localhost:8080 in your browser"
echo ""
echo "To check the status:"
echo "kubectl get pods -n topic-scanner"
echo "kubectl get cronjobs -n topic-scanner"


