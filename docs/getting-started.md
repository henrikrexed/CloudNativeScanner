# Getting Started

This guide walks you through deploying TopicScanner on Kubernetes from scratch.

## Prerequisites

### Kubernetes Cluster

You need a Kubernetes 1.28+ cluster. Any distribution works — EKS, GKE, AKS, k3s, kind, etc.

```bash
kubectl version --short
# Client Version: v1.30.x
# Server Version: v1.28.x+
```

### Gateway API CRDs

TopicScanner uses the Kubernetes Gateway API for routing. Install the CRDs:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml
```

!!! note
    Gateway API CRDs are cluster-scoped resources. You only need to install them once per cluster.

### GatewayClass Provider

You need a Gateway API implementation. Install one of:

=== "Istio"

    ```bash
    istioctl install --set profile=minimal
    ```

=== "Cilium"

    ```bash
    cilium install --set gatewayAPI.enabled=true
    ```

=== "Traefik"

    ```bash
    helm install traefik traefik/traefik --set providers.kubernetesGateway.enabled=true
    ```

=== "Envoy Gateway"

    ```bash
    helm install eg oci://docker.io/envoyproxy/gateway-helm --version v1.2.0 -n envoy-gateway-system --create-namespace
    ```

### Helm 3.x

```bash
helm version
# version.BuildInfo{Version:"v3.x.x", ...}
```

### Ollama

TopicScanner requires Ollama for LLM tasks. Install it locally or deploy in-cluster.

Pull the required models:

```bash
ollama pull qwen2.5:14b       # relevance scoring + classification
ollama pull qwen2.5:32b       # summarization + content generation
ollama pull nomic-embed-text   # embeddings for semantic dedup
```

!!! tip
    See [LLM Models](llm-models.md) for the full model configuration and task assignments.

## Step 1: Deploy Ollama (if in-cluster)

If running Ollama inside Kubernetes:

```bash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ollama
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ollama
  template:
    metadata:
      labels:
        app: ollama
    spec:
      containers:
        - name: ollama
          image: ollama/ollama:latest
          ports:
            - containerPort: 11434
          resources:
            limits:
              nvidia.com/gpu: 1
---
apiVersion: v1
kind: Service
metadata:
  name: ollama
spec:
  selector:
    app: ollama
  ports:
    - port: 11434
EOF
```

Then pull models inside the pod:

```bash
kubectl exec -it deploy/ollama -- ollama pull qwen2.5:14b
kubectl exec -it deploy/ollama -- ollama pull qwen2.5:32b
kubectl exec -it deploy/ollama -- ollama pull nomic-embed-text
```

## Step 2: Install TopicScanner

```bash
# Build chart dependencies
helm dependency build helm/cloud-native-scanner-v2/

# Install
helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=changeme \
  --set llm.ollama.url=http://ollama:11434 \
  --set gateway.enabled=true \
  --set gateway.className=istio \
  --set gateway.hostname=scanner.example.com
```

!!! warning
    Always set `postgresql.auth.password` — the default is empty.

### Using an existing Gateway

If your cluster already has a shared Gateway:

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=changeme \
  --set llm.ollama.url=http://ollama:11434 \
  --set gateway.enabled=true \
  --set gateway.create=false \
  --set gateway.gatewayRef=shared-gateway \
  --set gateway.gatewayRefNamespace=gateway-system \
  --set gateway.hostname=scanner.example.com
```

## Step 3: Verify

```bash
# Check pods
kubectl get pods -l app.kubernetes.io/instance=scanner

# Check HTTPRoutes
kubectl get httproutes

# Check Gateway (if created)
kubectl get gateways

# Test the API
curl http://scanner.example.com/api/health
```

You should see all pods running and the API returning a health response.

## Local Development Setup

For local development without Kubernetes:

### 1. Start PostgreSQL

```bash
docker compose up -d
```

### 2. Run the Backend

```bash
mvn spring-boot:run -pl pipeline-service -Dspring-boot.run.profiles=local
```

### 3. Run the Frontend

```bash
cd webui-nodejs
npm install
npm run dev
```

The WebUI will be available at `http://localhost:3000` and the API at `http://localhost:8080`.

## Your First Scan

1. Open the WebUI at your configured hostname (or `http://localhost:3000` for local dev)
2. Navigate to **Topics** and configure your areas of interest
3. Go to **Scanners** and enable the sources you want (StackOverflow, Medium, Dev.to, etc.)
4. Trigger a scan manually or wait for the scheduled Quartz job
5. View discovered content in the **Dashboard**

!!! tip
    Start with the default scanners (StackOverflow, Medium, Dev.to, Hashnode) — they don't require API keys. Reddit and YouTube need credentials. See [Scanners](scanners.md) for details.

## Next Steps

- [Configuration](configuration.md) — tune pipeline parameters and LLM settings
- [Content Studio](content-studio.md) — upload your writing style and generate content
- [Deployment](deployment.md) — full Helm values reference
- [Architecture](architecture.md) — understand the pipeline internals
