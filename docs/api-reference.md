# API Reference

All endpoints are served by the pipeline-service at `http://localhost:8080`.

## Dashboard

### GET /api/topics

Paginated topic feed (only analyzed, non-rejected topics).

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | `0` | Page number (zero-based) |
| `size` | int | `20` | Page size |
| `categoryId` | int | — | Filter by category |
| `sourceType` | string | — | Filter by source (e.g., `reddit`, `stackoverflow`) |
| `dateFrom` | string | — | ISO date (e.g., `2024-01-01`) |
| `dateTo` | string | — | ISO date |
| `minQuality` | float | — | Minimum quality score (0.0–1.0) |

**Response:**

```json
{
  "topics": [
    {
      "id": 42,
      "title": "Kubernetes HPA with Custom Metrics",
      "url": "https://stackoverflow.com/questions/...",
      "source_type": "stackoverflow",
      "source_date": "2024-03-01T12:00:00",
      "collected_at": "2024-03-02T06:00:00",
      "pipeline_stage": "analyzed",
      "quality_score": 0.78,
      "relevance_score": 0.92,
      "summary": "Discussion about...",
      "tags": "kubernetes,autoscaling,hpa",
      "category_id": 1
    }
  ],
  "total": 156,
  "page": 0,
  "size": 20
}
```

### GET /api/stats

Dashboard statistics.

**Response:**

```json
{
  "totalTopics": 1250,
  "topicsThisWeek": 47,
  "pendingJobs": 12,
  "failedJobs": 3,
  "byCategory": [
    { "name": "Cloud Native", "id": 1, "count": 450 }
  ],
  "bySource": [
    { "name": "stackoverflow", "count": 380 }
  ]
}
```

## Topics

### GET /api/topics/{id}

Full topic detail with related topics.

**Response:**

```json
{
  "id": 42,
  "title": "Kubernetes HPA with Custom Metrics",
  "url": "https://stackoverflow.com/questions/...",
  "source_type": "stackoverflow",
  "pipeline_stage": "analyzed",
  "quality_score": 0.78,
  "relevance_score": 0.92,
  "summary": "Comprehensive discussion about...",
  "extracted_content": "Full extracted text...",
  "tags": "kubernetes,autoscaling",
  "group_id": "abc-123",
  "source_date": "2024-03-01T12:00:00",
  "relatedTopics": [
    { "id": 43, "title": "Related topic...", "source_type": "medium" }
  ]
}
```

## Categories

### GET /api/categories

List all categories with topic counts.

**Response:**

```json
[
  {
    "id": 1,
    "name": "Cloud Native",
    "description": "Kubernetes, containers, service mesh",
    "keywords": "kubernetes,docker,istio",
    "negative_keywords": "spam,ads",
    "sources": "stackoverflow,reddit,medium",
    "scan_frequency": 60,
    "enabled": true,
    "topic_count": 450,
    "created_at": "2024-01-15T10:00:00"
  }
]
```

### POST /api/categories

Create a new category.

**Request:**

```json
{
  "name": "Cloud Native",
  "description": "Kubernetes, containers, service mesh topics",
  "keywords": "kubernetes,docker,istio,envoy",
  "negativeKeywords": "spam,marketing",
  "sources": "stackoverflow,reddit,medium",
  "scanFrequency": 60,
  "enabled": true
}
```

**Response:** `{ "id": 1, "name": "Cloud Native" }`

### PUT /api/categories/{id}

Update a category. Partial updates supported (COALESCE with existing values).

### DELETE /api/categories/{id}

Delete a category. Topics are not deleted, only unlinked.

### GET /api/scanners

List available scanner plugins.

**Response:**

```json
[
  { "type": "stackoverflow", "displayName": "Stack Overflow" },
  { "type": "reddit", "displayName": "Reddit" },
  { "type": "medium", "displayName": "Medium" },
  { "type": "devto", "displayName": "Dev.to" },
  { "type": "hashnode", "displayName": "Hashnode" },
  { "type": "youtube", "displayName": "YouTube" }
]
```

## Content Studio

### POST /api/content

Upload content for style analysis.

**Request:**

```json
{
  "title": "My Blog Post",
  "content": "Full text of the blog post...",
  "contentType": "blog_post"
}
```

### POST /api/content/import

Import content from a URL.

**Request:** `{ "url": "https://dev.to/mypost" }`

### POST /api/content/{id}/analyze

Trigger writing style analysis. Returns JSON style metadata.

### GET /api/content/style-profile

Get composite style profile merged from all analyzed uploads.

**Response:** `{ "profile": "{...json...}", "hasProfile": true }`

### POST /api/generate

Generate content from a topic.

**Request:**

```json
{
  "topicId": 42,
  "outputFormat": "blog_post"
}
```

Or generate from a topic group:

```json
{
  "groupId": "abc-123",
  "outputFormat": "youtube_script"
}
```

### POST /api/generate/{id}/regenerate

Regenerate with feedback.

**Request:** `{ "feedback": "Add more code examples, make it shorter" }`

### GET /api/generate/{id}/export

Download generated content as Markdown file.

## Pipeline Monitor

### GET /api/pipeline/status

Job queue status overview.

**Response:**

```json
{
  "jobCounts": [
    { "stage": "EXTRACT", "status": "PENDING", "count": 5 },
    { "stage": "ANALYZE", "status": "PROCESSING", "count": 1 }
  ],
  "summary": {
    "pending": 12,
    "processing": 2,
    "completed": 850,
    "failed": 15
  }
}
```

### GET /api/pipeline/errors

Recent failed jobs (default limit 20, max 100).

### GET /api/pipeline/scan-history

Recent scan runs with topic counts.

### POST /api/pipeline/scan

Trigger an immediate scan across all enabled categories.

**Response:** `{ "triggered": true, "message": "Scan started" }`

### GET /api/pipeline/topics-by-stage

Topic distribution across pipeline stages.

**Response:**

```json
[
  { "pipeline_stage": "scanned", "count": 45 },
  { "pipeline_stage": "extracted", "count": 12 },
  { "pipeline_stage": "analyzed", "count": 850 },
  { "pipeline_stage": "rejected", "count": 200 }
]
```
