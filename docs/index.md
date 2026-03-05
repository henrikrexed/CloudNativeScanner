# TopicScanner

AI-powered DevRel intelligence platform. Automatically discovers trending topics from developer communities, extracts and analyzes content with LLMs, and generates publication-ready material.

## What It Does

1. **Scans** developer communities (StackOverflow, Reddit, Medium, Dev.to, Hashnode, YouTube) for trending topics
2. **Extracts** full content from discovered URLs
3. **Analyzes** content through a 7-stage filter pipeline — dedup, language detection, quality scoring, relevance scoring, and semantic dedup via embeddings
4. **Generates** publication-ready content (blog posts, YouTube scripts, LinkedIn posts, newsletters) using your writing style

## Key Features

- **Pluggable scanner system** — built-in scanners via Spring `@Component`, external plugins via ServiceLoader
- **LLM abstraction** — task-specific models across Ollama, OpenAI, and Anthropic (Claude)
- **Content Studio** — upload your writing, analyze your style, generate new content that matches your voice
- **PostgreSQL job queue** — no Kafka dependency, uses `SELECT ... FOR UPDATE SKIP LOCKED`
- **pgvector embeddings** — semantic dedup and RAG-enhanced content generation

## Quick Start

### Docker Compose (local dev)

```bash
# Start PostgreSQL
docker compose up -d

# Run pipeline-service
mvn spring-boot:run -pl pipeline-service -Dspring-boot.run.profiles=local

# Run webui
cd webui-nodejs && npm install && npm run dev
```

### Kubernetes (Helm)

```bash
helm dependency build helm/cloud-native-scanner-v2/

helm install scanner helm/cloud-native-scanner-v2/ \
  --set postgresql.auth.password=changeme \
  --set llm.ollama.url=http://ollama:11434
```

See [Deployment](deployment.md) for full Helm configuration.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17, JdbcTemplate, Quartz |
| Frontend | Next.js 14, React, Tailwind CSS, TypeScript |
| Database | PostgreSQL 15 + pgvector |
| LLM | Ollama / OpenAI / Anthropic (Claude) |
| Build | Maven (Java), npm (frontend) |
| CI/CD | GitHub Actions, GHCR |
| Deploy | Helm 3 on Kubernetes |
