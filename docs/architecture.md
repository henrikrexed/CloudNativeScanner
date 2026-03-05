# Architecture

## System Overview

```mermaid
graph LR
  subgraph Sources
    SO[StackOverflow]
    RD[Reddit]
    MD[Medium]
    DT[Dev.to]
    HN[Hashnode]
    YT[YouTube]
  end

  subgraph Pipeline Service
    direction TB
    SCAN[Scan Stage]
    EXTRACT[Extract Stage]
    ANALYZE[Analyze Stage]
    GEN[Generate Stage]
    API[REST API]
  end

  subgraph Storage
    PG[(PostgreSQL + pgvector)]
  end

  subgraph UI
    NEXT[Next.js WebUI]
  end

  SO & RD & MD & DT & HN & YT --> SCAN
  SCAN --> PG
  PG --> EXTRACT --> ANALYZE --> GEN --> PG
  PG --> API --> NEXT
```

## Pipeline Stages

Topics flow through four stages, each managed by the PostgreSQL job queue:

```mermaid
stateDiagram-v2
    [*] --> scanned: Scan discovers topic
    scanned --> extracted: Extract full content
    extracted --> analyzed: Pass 7-stage filter
    analyzed --> [*]: Available in UI
    extracted --> rejected: Failed filter
    rejected --> [*]
```

### Stage 1: Scan

The `ScanOrchestrator` runs on a cron schedule (default: 6 AM daily, configurable via `topicscanner.pipeline.scan-cron`). For each enabled category:

1. Get category keywords and configured source types
2. For each source, get the `SourceScanner` from `ScannerRegistry`
3. Call `scanner.scan(ScanRequest)` — returns list of `ScanResult`
4. Persist new topics via `INSERT ... ON CONFLICT DO NOTHING` (URL + source dedup)
5. Enqueue `EXTRACT` job for each new topic

### Stage 2: Extract

The `ExtractStageHandler` fetches full content from each discovered URL:

1. Try the scanner's custom `extractContent(url)` method first
2. Fall back to source-specific extractors (YouTube transcripts, SO Q&A)
3. Fall back to generic web extraction (Jsoup readability-style)
4. Store extracted content, enqueue `ANALYZE` job

### Stage 3: Analyze

The `AnalyzeStageHandler` runs the 7-stage filter pipeline:

1. **URL dedup** (order 10) — reject if URL already analyzed
2. **Negative keywords** (order 20) — reject if matches exclusion terms
3. **Language** (order 30) — reject non-English content (ASCII ratio + stop words)
4. **Content length** (order 40) — reject if < 200 chars
5. **Quality** (order 45) — score based on structure (headings, code blocks, paragraphs)
6. **Relevance** (order 50) — LLM scores relevance to category keywords (0.0–1.0)
7. **Embedding dedup** (order 70) — pgvector cosine similarity rejects near-duplicates

Filters are ordered cheapest-first. The pipeline short-circuits on the first failure.

### Stage 4: Generate

Content generation is triggered on-demand via the Content Studio UI or API. The `ContentGenerationService`:

1. Loads the topic and up to 5 related topics from the same group
2. Retrieves the user's composite writing style profile
3. Finds similar user-uploaded content via pgvector (RAG context)
4. Builds format-specific prompts with style + RAG context
5. Calls LLM with `GENERATION` task type
6. Stores result in `generated_content` table

## Scanner System

```mermaid
classDiagram
    class SourceScanner {
        <<interface>>
        +getSourceType() String
        +getDisplayName() String
        +scan(ScanRequest) List~ScanResult~
        +extractContent(url) Optional~String~
    }

    class ScannerRegistry {
        -scanners Map
        +register(SourceScanner)
        +getScanner(type) Optional
        +getAllScanners() Collection
    }

    SourceScanner <|.. RedditScanner
    SourceScanner <|.. StackOverflowScanner
    SourceScanner <|.. MediumScanner
    SourceScanner <|.. DevToScanner
    SourceScanner <|.. HashnodeScanner
    SourceScanner <|.. YouTubeScanner
    ScannerRegistry o-- SourceScanner
```

Built-in scanners are Spring `@Component` beans auto-discovered via classpath scanning. External plugins are JARs in the `plugins/` directory loaded via `ServiceLoader`.

## LLM Abstraction

```mermaid
classDiagram
    class LLMService {
        <<interface>>
        +getProvider() String
        +complete(taskType, system, user) LLMResponse
        +embed(texts) List~float[]~
        +isAvailable() boolean
    }

    class LLMTaskType {
        <<enum>>
        RELEVANCE
        CLASSIFICATION
        SUMMARIZATION
        EMBEDDING
        GENERATION
    }

    LLMService <|.. OllamaLLMService
    LLMService <|.. OpenAILLMService
    LLMService <|.. ClaudeLLMService
    LLMService <|.. FallbackLLMService
    FallbackLLMService o-- LLMService : primary
    FallbackLLMService o-- LLMService : fallback
```

Each provider supports **task-specific models** — you can use a small model for relevance scoring and a large model for content generation. The `FallbackLLMService` wraps primary + fallback, trying primary first and falling back on failure.

## Job Queue

The pipeline uses PostgreSQL as a job queue instead of Kafka:

```sql
-- Claim next job (atomic, skip locked)
UPDATE pipeline_jobs
SET status = 'PROCESSING', started_at = NOW(), updated_at = NOW()
WHERE id = (
    SELECT id FROM pipeline_jobs
    WHERE stage = ? AND status = 'PENDING'
    ORDER BY created_at ASC
    LIMIT 1
    FOR UPDATE SKIP LOCKED
)
RETURNING *;
```

The `PipelineOrchestrator` polls each stage on a configurable interval (default: 30 seconds). Stale jobs (stuck in `PROCESSING` for > 10 minutes) are automatically reset to `PENDING`.

## Content Generation Flow

```mermaid
sequenceDiagram
    participant User
    participant API
    participant Gen as ContentGenerationService
    participant Style as StyleAnalysisService
    participant RAG as UserContentService
    participant LLM

    User->>API: POST /api/generate {topicId, outputFormat}
    API->>Gen: generate(topicId, format)
    Gen->>Gen: Load topic + related topics
    Gen->>Style: getCompositeStyleProfile()
    Style-->>Gen: style JSON
    Gen->>RAG: findSimilarContent(topic text, 3)
    RAG-->>Gen: similar user content
    Gen->>LLM: complete(GENERATION, systemPrompt, userPrompt)
    LLM-->>Gen: generated text
    Gen->>Gen: Store in generated_content
    Gen-->>API: content ID
    API-->>User: {id, generated_text, ...}
```
