# Configuration

## Application Configuration

The pipeline-service reads configuration from `application.yml`. All values can be overridden via environment variables.

### Pipeline Settings

```yaml
topicscanner:
  pipeline:
    scan-cron: "0 0 6 * * *"          # When to run daily scans
    poll-interval-seconds: 30           # Job queue polling interval
    max-results-per-scan: 25            # Max topics per source per scan
    stale-job-minutes: 10               # Reset stuck jobs after this
    relevance-threshold: 0.7            # LLM relevance score cutoff (0.0–1.0)
    quality-threshold: 0.5              # Quality score cutoff (0.0–1.0)
    min-content-length: 200             # Minimum extracted content chars
    dedup-similarity-threshold: 0.95    # Embedding cosine similarity cutoff
    max-retries: 3                      # Max retry attempts per job
```

| Env Variable | Config Key | Default |
|-------------|-----------|---------|
| `PIPELINE_SCAN_CRON` | `topicscanner.pipeline.scan-cron` | `0 0 6 * * *` |
| `PIPELINE_POLL_INTERVAL` | `topicscanner.pipeline.poll-interval-seconds` | `30` |
| `PIPELINE_MAX_RESULTS_PER_SCAN` | `topicscanner.pipeline.max-results-per-scan` | `25` |
| `PIPELINE_STALE_JOB_MINUTES` | `topicscanner.pipeline.stale-job-minutes` | `10` |
| `PIPELINE_RELEVANCE_THRESHOLD` | `topicscanner.pipeline.relevance-threshold` | `0.7` |
| `PIPELINE_QUALITY_THRESHOLD` | `topicscanner.pipeline.quality-threshold` | `0.5` |
| `PIPELINE_MIN_CONTENT_LENGTH` | `topicscanner.pipeline.min-content-length` | `200` |
| `PIPELINE_DEDUP_THRESHOLD` | `topicscanner.pipeline.dedup-similarity-threshold` | `0.95` |

### LLM Settings

Each LLM provider supports **task-specific models** — use a cheap model for scoring and a powerful model for generation.

```yaml
topicscanner:
  llm:
    primary: ollama                     # Primary provider: ollama | openai | claude
    cloud-fallback: openai              # Fallback when primary fails

    ollama:
      url: http://localhost:11434
      timeout-seconds: 120
      models:
        relevance: qwen2.5:14b
        classification: qwen2.5:14b
        summarization: qwen2.5:32b
        embedding: nomic-embed-text
        generation: qwen2.5:32b

    openai:
      api-key: ${OPENAI_API_KEY:}
      timeout-seconds: 60
      models:
        relevance: gpt-4o-mini
        classification: gpt-4o-mini
        summarization: gpt-4o
        embedding: text-embedding-3-small
        generation: gpt-4o

    claude:
      api-key: ${ANTHROPIC_API_KEY:}
      timeout-seconds: 60
      models:
        relevance: claude-haiku-4-5-20251001
        classification: claude-haiku-4-5-20251001
        summarization: claude-sonnet-4-6
        generation: claude-sonnet-4-6
```

| Env Variable | Description |
|-------------|------------|
| `LLM_PRIMARY` | Primary LLM provider |
| `LLM_CLOUD_FALLBACK` | Fallback provider |
| `LLM_OLLAMA_URL` | Ollama server URL |
| `OPENAI_API_KEY` | OpenAI API key |
| `ANTHROPIC_API_KEY` | Anthropic Claude API key |

!!! note
    Claude does not support embeddings. When using Claude as primary, configure Ollama or OpenAI as fallback for embedding tasks.

### Database Settings

```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/topicscanner}
    username: ${DATABASE_USERNAME:topicscanner}
    password: ${DATABASE_PASSWORD:topicscanner}
```

### Scanner Plugin Directory

```yaml
topicscanner:
  scanner:
    plugins-dir: ${SCANNER_PLUGINS_DIR:plugins}
```

## Helm Values

See [Deployment](deployment.md) for the full Helm values reference.

Key values:

| Value | Description | Default |
|-------|-------------|---------|
| `llm.provider` | LLM provider | `ollama` |
| `llm.model` | Model name | `llama3` |
| `llm.apiKey` | API key | `""` |
| `llm.ollama.url` | Ollama URL | `http://ollama:11434` |
| `llm.fallback.enabled` | Enable fallback LLM | `false` |
| `pgvector.enabled` | Enable embeddings | `true` |
| `pgvector.dimensions` | Vector dimensions | `1536` |
| `scanners.reddit.enabled` | Enable Reddit | `false` |
| `scanners.stackoverflow.enabled` | Enable StackOverflow | `true` |
| `scanners.medium.enabled` | Enable Medium | `true` |
| `scanners.devto.enabled` | Enable Dev.to | `true` |
| `scanners.hashnode.enabled` | Enable Hashnode | `true` |
| `scanners.youtube.enabled` | Enable YouTube | `false` |
