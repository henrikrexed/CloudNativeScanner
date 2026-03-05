# Deployment

## Helm Chart

The v2 Helm chart is at `helm/cloud-native-scanner-v2/`. It deploys:

- **PostgreSQL** (Bitnami subchart) with pgvector extension
- **pipeline-service** (Spring Boot)
- **webui** (Next.js)

### Prerequisites

- Kubernetes 1.24+
- Helm 3.x
- (Optional) Ollama running in-cluster or accessible externally

### Quick Install

```bash
# Build chart dependencies (downloads PostgreSQL subchart)
helm dependency build helm/cloud-native-scanner-v2/

# Install with defaults (Ollama provider)
helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=my-secure-password \
  --set llm.ollama.url=http://ollama:11434
```

### Install with OpenAI

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=my-secure-password \
  --set llm.provider=openai \
  --set llm.model=gpt-4o-mini \
  --set llm.apiKey=sk-your-key
```

### Install with Claude

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=my-secure-password \
  --set llm.provider=claude \
  --set llm.model=claude-sonnet-4-6 \
  --set llm.apiKey=sk-ant-your-key \
  --set llm.fallback.enabled=true \
  --set llm.fallback.provider=ollama
```

!!! note
    Claude does not support embeddings. Enable a fallback provider (Ollama or OpenAI) for embedding tasks.

### Enable Ingress

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set ingress.enabled=true \
  --set ingress.className=nginx \
  --set ingress.hosts[0].host=scanner.example.com \
  --set ingress.hosts[0].paths[0].path=/ \
  --set ingress.hosts[0].paths[0].pathType=Prefix
```

The ingress routes `/api/*` to pipeline-service and everything else to the webui.

### Enable Scanners

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set scanners.reddit.enabled=true \
  --set scanners.reddit.clientId=your-client-id \
  --set scanners.reddit.clientSecret=your-secret \
  --set scanners.youtube.enabled=true \
  --set scanners.youtube.apiKey=your-youtube-key
```

## Full Values Reference

### Global

| Value | Description | Default |
|-------|-------------|---------|
| `global.imageRegistry` | Override image registry | `""` |
| `global.imagePullSecrets` | Image pull secrets | `[]` |

### PostgreSQL

| Value | Description | Default |
|-------|-------------|---------|
| `postgresql.enabled` | Deploy PostgreSQL subchart | `true` |
| `postgresql.auth.database` | Database name | `topicscanner` |
| `postgresql.auth.username` | Database user | `topicscanner` |
| `postgresql.auth.password` | Database password | `""` |
| `postgresql.auth.existingSecret` | Use existing secret | `""` |
| `postgresql.primary.persistence.size` | PVC size | `10Gi` |

### External Database

When `postgresql.enabled=false`:

| Value | Description | Default |
|-------|-------------|---------|
| `externalDatabase.host` | Database host | `""` |
| `externalDatabase.port` | Database port | `5432` |
| `externalDatabase.database` | Database name | `topicscanner` |
| `externalDatabase.username` | Username | `topicscanner` |
| `externalDatabase.password` | Password | `""` |

### Pipeline Service

| Value | Description | Default |
|-------|-------------|---------|
| `pipelineService.replicaCount` | Replicas | `1` |
| `pipelineService.image.repository` | Image | `ghcr.io/henrikrexed/pipeline-service` |
| `pipelineService.image.tag` | Tag (defaults to appVersion) | `""` |
| `pipelineService.resources.requests.cpu` | CPU request | `500m` |
| `pipelineService.resources.requests.memory` | Memory request | `512Mi` |
| `pipelineService.resources.limits.cpu` | CPU limit | `1` |
| `pipelineService.resources.limits.memory` | Memory limit | `1Gi` |
| `pipelineService.extraEnv` | Extra environment variables | `[]` |

### WebUI

| Value | Description | Default |
|-------|-------------|---------|
| `webui.replicaCount` | Replicas | `1` |
| `webui.image.repository` | Image | `ghcr.io/henrikrexed/webui-nodejs` |
| `webui.image.tag` | Tag (defaults to appVersion) | `""` |
| `webui.resources.requests.cpu` | CPU request | `100m` |
| `webui.resources.requests.memory` | Memory request | `128Mi` |

### LLM

| Value | Description | Default |
|-------|-------------|---------|
| `llm.provider` | Provider: `ollama`, `openai`, `anthropic` | `ollama` |
| `llm.model` | Model name | `llama3` |
| `llm.apiKey` | API key | `""` |
| `llm.existingSecret` | Existing secret for API key | `""` |
| `llm.ollama.url` | Ollama URL | `http://ollama:11434` |
| `llm.fallback.enabled` | Enable fallback | `false` |
| `llm.fallback.provider` | Fallback provider | `openai` |
| `llm.fallback.model` | Fallback model | `gpt-4o-mini` |
| `llm.fallback.apiKey` | Fallback API key | `""` |

### Scanners

| Value | Description | Default |
|-------|-------------|---------|
| `scanners.reddit.enabled` | Enable Reddit | `false` |
| `scanners.reddit.clientId` | Reddit client ID | `""` |
| `scanners.reddit.clientSecret` | Reddit client secret | `""` |
| `scanners.stackoverflow.enabled` | Enable StackOverflow | `true` |
| `scanners.medium.enabled` | Enable Medium | `true` |
| `scanners.devto.enabled` | Enable Dev.to | `true` |
| `scanners.hashnode.enabled` | Enable Hashnode | `true` |
| `scanners.youtube.enabled` | Enable YouTube | `false` |
| `scanners.youtube.apiKey` | YouTube Data API key | `""` |

### pgvector

| Value | Description | Default |
|-------|-------------|---------|
| `pgvector.enabled` | Enable pgvector | `true` |
| `pgvector.dimensions` | Vector dimensions | `1536` |

### Ingress

| Value | Description | Default |
|-------|-------------|---------|
| `ingress.enabled` | Enable ingress | `false` |
| `ingress.className` | Ingress class | `nginx` |
| `ingress.annotations` | Ingress annotations | `{}` |
| `ingress.hosts` | Host rules | `[{host: scanner.example.com}]` |
| `ingress.tls` | TLS config | `[]` |

## pgvector Setup

The Helm chart handles pgvector automatically:

1. **Bitnami initdb script** creates the extension on first install
2. **Post-install hook job** ensures the extension exists after upgrades

For an external PostgreSQL, manually enable pgvector:

```sql
-- Requires the pgvector extension to be installed on the server
CREATE EXTENSION IF NOT EXISTS vector;
```

!!! warning
    pgvector must be installed as a PostgreSQL server extension. Cloud providers (RDS, Cloud SQL, Azure) may need explicit enabling. See your provider's documentation.

## Ollama Connectivity

### In-Cluster Ollama

Deploy Ollama as a Kubernetes deployment:

```yaml
# ollama-deployment.yaml
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
              nvidia.com/gpu: 1  # if GPU available
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
```

Then set `llm.ollama.url=http://ollama:11434`.

### External Ollama

Point to your Ollama server:

```bash
helm install scanner helm/cloud-native-scanner-v2/ \
  --set llm.ollama.url=http://192.168.1.100:11434
```

## Docker Images

Build images locally:

```bash
# Pipeline service (from project root — needs parent POM)
docker build -f pipeline-service/Dockerfile -t pipeline-service:latest .

# WebUI
docker build -t webui-nodejs:latest webui-nodejs/
```
