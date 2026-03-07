# LLM Models Guide

TopicScanner uses different LLM models for different pipeline tasks. This guide covers which models are needed, their hardware requirements, and how to set them up.

## Pipeline Tasks & Model Requirements

TopicScanner uses **5 specialized tasks**, each with its own model configuration:

| Task | Purpose | Default (Ollama) | Min VRAM | Fallback (Cloud) |
|------|---------|-------------------|----------|-------------------|
| **Relevance** | Score topic relevance to categories (0.0–1.0) | `qwen3:14b` | ~10 GB | `gpt-4o-mini` |
| **Classification** | Categorize topics into themes | `qwen3:14b` | ~10 GB | `gpt-4o-mini` |
| **Summarization** | Summarize extracted content | `qwen3:32b` | ~20 GB | `gpt-4o` |
| **Embedding** | Generate vector embeddings for dedup | `nomic-embed-text` | ~0.5 GB | `text-embedding-3-small` |
| **Generation** | Generate blog posts, scripts, newsletters | `qwen3:32b` | ~20 GB | `gpt-4o` |

## Ollama Setup (Primary Provider)

### 1. Install Ollama

```bash
# Linux/macOS
curl -fsSL https://ollama.ai/install.sh | sh

# Verify installation
ollama --version
```

### 2. Pull Required Models

Pull all 3 models (qwen3:14b, qwen3:32b, nomic-embed-text):

```bash
# Embedding model (small, fast — pull first)
ollama pull nomic-embed-text

# Relevance + Classification model (14B parameters)
ollama pull qwen3:14b

# Summarization + Generation model (32B parameters)
ollama pull qwen3:32b
```

### 3. Verify Models

```bash
ollama list
# Should show:
# NAME                ID          SIZE    MODIFIED
# qwen3:14b         ...         9.0 GB  ...
# qwen3:32b         ...         19 GB   ...
# nomic-embed-text    ...         274 MB  ...
```

### 4. Test Models

```bash
# Test chat model
ollama run qwen3:14b "What is Kubernetes?"

# Test embedding model
curl http://localhost:11434/api/embed -d '{"model":"nomic-embed-text","input":"test"}'
```

## Hardware Requirements

### Minimum (relevance + classification + embedding only)

- **GPU:** 12 GB VRAM (e.g., RTX 3060 12GB, RTX 4070)
- **RAM:** 16 GB system RAM
- **Models:** `qwen3:14b` + `nomic-embed-text`
- **Note:** Summarization and generation will fall back to cloud provider

### Recommended (all tasks local)

- **GPU:** 24 GB VRAM (e.g., RTX 3090, RTX 4090, A5000)
- **RAM:** 32 GB system RAM
- **Models:** All 3 models
- **Note:** 32B model requires ~20 GB VRAM; Ollama swaps to RAM if VRAM insufficient (slower)

### Apple Silicon

- **M1/M2 Pro (16 GB):** Can run 14B + embedding. 32B will be slow (memory pressure).
- **M1/M2 Max (32 GB):** All models run well. Unified memory is shared with GPU.
- **M1/M2 Ultra (64+ GB):** Ideal. Can run multiple models concurrently.
- **M3/M4 variants:** Same guidelines based on unified memory.

### CPU-Only (No GPU)

Ollama can run on CPU but will be significantly slower:

- `nomic-embed-text`: Fast on CPU (~100ms per embedding)
- `qwen3:14b`: Usable (~10-30s per response)
- `qwen3:32b`: Very slow (~60-120s per response) — recommend cloud fallback

## Kubernetes Deployment

When running Ollama in Kubernetes, you have two options:

### Option A: External Ollama (Recommended)

Run Ollama on a GPU machine accessible from the cluster:

```yaml
# Helm values
llm:
  provider: ollama
  ollama:
    url: http://ollama-host.internal:11434
```

### Option B: Ollama in Kubernetes

Deploy Ollama as a pod with GPU access:

```yaml
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
              nvidia.com/gpu: 1  # Requires NVIDIA device plugin
          volumeMounts:
            - name: models
              mountPath: /root/.ollama
      volumes:
        - name: models
          persistentVolumeClaim:
            claimName: ollama-models
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
      targetPort: 11434
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ollama-models
spec:
  accessModes: [ReadWriteOnce]
  resources:
    requests:
      storage: 50Gi  # Enough for all 3 models
```

Then pull models inside the pod:

```bash
kubectl exec -it deploy/ollama -- ollama pull nomic-embed-text
kubectl exec -it deploy/ollama -- ollama pull qwen3:14b
kubectl exec -it deploy/ollama -- ollama pull qwen3:32b
```

## Alternative Models

You can swap models by overriding environment variables:

| Task | Env Variable | Alternatives |
|------|-------------|-------------|
| Relevance | `LLM_OLLAMA_MODEL_RELEVANCE` | `llama3.1:8b`, `mistral:7b`, `gemma2:9b` |
| Classification | `LLM_OLLAMA_MODEL_CLASSIFICATION` | `llama3.1:8b`, `mistral:7b`, `gemma2:9b` |
| Summarization | `LLM_OLLAMA_MODEL_SUMMARIZATION` | `llama3.1:70b`, `qwen3:72b`, `mixtral:8x7b` |
| Embedding | `LLM_OLLAMA_MODEL_EMBEDDING` | `mxbai-embed-large`, `all-minilm` |
| Generation | `LLM_OLLAMA_MODEL_GENERATION` | `llama3.1:70b`, `qwen3:72b`, `mixtral:8x7b` |

### Model Selection Tips

- **Smaller models** (7-8B): Faster, less VRAM, slightly lower quality. Good for relevance/classification.
- **Larger models** (32-72B): Better quality for summarization and content generation. Need more VRAM.
- **Embedding models**: `nomic-embed-text` is the best balance of quality/speed. `mxbai-embed-large` is slightly better but larger.
- **Quantization**: Ollama models are quantized (Q4_K_M by default). For better quality at the cost of more VRAM, use `qwen3:14b-instruct-fp16`.

## Cloud Fallback Configuration

When Ollama is unavailable or a task fails, TopicScanner falls back to a cloud provider:

### OpenAI

```yaml
# Helm values
llm:
  fallback:
    enabled: true
    provider: openai
    apiKey: "sk-..."
```

Or via environment:

```bash
LLM_CLOUD_FALLBACK=openai
OPENAI_API_KEY=sk-...
```

### Anthropic (Claude)

```bash
LLM_CLOUD_FALLBACK=claude
ANTHROPIC_API_KEY=sk-ant-...
```

### Cost Estimates (Cloud Fallback)

Assuming ~1000 topics scanned per day:

| Task | Model | Est. Tokens/Day | Est. Cost/Day |
|------|-------|-----------------|---------------|
| Relevance | gpt-4o-mini | ~500K | ~$0.08 |
| Classification | gpt-4o-mini | ~300K | ~$0.05 |
| Summarization | gpt-4o | ~2M | ~$10.00 |
| Embedding | text-embedding-3-small | ~1M | ~$0.02 |
| Generation | gpt-4o | ~3M | ~$15.00 |
| **Total** | | | **~$25/day** |

> **Note:** Running locally with Ollama costs $0/day after hardware investment. Cloud fallback is for reliability, not primary use.

## Troubleshooting

### Ollama Not Responding

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Restart Ollama
systemctl restart ollama  # Linux
brew services restart ollama  # macOS
```

### Model Too Slow

- Check GPU utilization: `nvidia-smi` (NVIDIA) or `sudo powermetrics --samplers gpu_power` (macOS)
- If running on CPU, consider using smaller models or enabling cloud fallback
- Reduce `max-results-per-scan` to process fewer topics per cycle

### Out of Memory

- Use smaller models: replace `qwen3:32b` with `qwen3:14b` for summarization/generation
- Enable cloud fallback for the heavy tasks (summarization, generation) and run only relevance/classification/embedding locally
- Set `OLLAMA_NUM_PARALLEL=1` to prevent concurrent model loading

### Embedding Dimension Mismatch

If you switch embedding models, the vector dimensions must match the database:

```yaml
# Helm values — must match your embedding model's output dimensions
pgvector:
  dimensions: 768  # nomic-embed-text = 768, text-embedding-3-small = 1536
```

If you change models, you need to re-embed all existing topics or reset the embeddings table.
