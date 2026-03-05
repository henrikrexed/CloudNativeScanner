# Development

## Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- Node.js 18+
- PostgreSQL 15+ (or Docker)

## Project Structure

```
.
├── shared/                  # JPA entities, repositories, shared services
├── pipeline-service/        # Spring Boot — scan, extract, analyze, generate
│   ├── src/main/java/com/topicscanner/
│   │   ├── api/             # REST controllers
│   │   ├── scanner/         # SourceScanner SPI + implementations
│   │   ├── extraction/      # Content extraction
│   │   ├── analyzer/        # Classification and analysis
│   │   ├── generator/       # Content generation + style analysis
│   │   ├── queue/           # PostgreSQL job queue
│   │   ├── llm/             # LLM providers (Ollama, OpenAI, Claude)
│   │   ├── filter/          # 7-stage filter chain
│   │   └── config/          # Spring configuration
│   └── Dockerfile
├── webui-nodejs/            # Next.js 14 frontend
│   ├── app/                 # Pages (App Router)
│   ├── components/          # UI components
│   ├── lib/                 # API client, utilities
│   └── Dockerfile
├── helm/cloud-native-scanner-v2/  # Helm chart
├── .github/workflows/ci.yaml     # CI pipeline
├── docs/                          # This documentation
└── pom.xml                        # Maven parent POM
```

## Building

### Java (Maven)

```bash
# Build all modules
mvn clean install -DskipTests

# Build only shared + pipeline-service
mvn package -pl shared,pipeline-service -am -DskipTests

# Run tests
mvn verify -pl shared,pipeline-service -am

# Run pipeline-service locally
mvn spring-boot:run -pl pipeline-service
```

### Frontend (Next.js)

```bash
cd webui-nodejs

npm install           # Install dependencies
npm run dev           # Dev server at http://localhost:3000
npm run build         # Production build
npm run lint          # ESLint
npx tsc --noEmit      # Type check
npm test              # Jest tests
```

## Running Locally

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Or use a local PostgreSQL instance. Create a database:

```sql
CREATE DATABASE topicscanner;
CREATE USER topicscanner WITH PASSWORD 'topicscanner';
GRANT ALL PRIVILEGES ON DATABASE topicscanner TO topicscanner;
```

Enable pgvector (optional, for embedding features):

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Start pipeline-service

```bash
mvn spring-boot:run -pl pipeline-service
```

Flyway will create all tables on first run. The API is at `http://localhost:8080`.

### 3. Start the frontend

```bash
cd webui-nodejs
npm install
npm run dev
```

The UI is at `http://localhost:3000`. It proxies API calls to `http://localhost:8080`.

### 4. (Optional) Start Ollama

```bash
ollama serve
ollama pull qwen2.5:14b
ollama pull nomic-embed-text
```

## Adding a New Scanner

See [CONTRIBUTING.md](https://github.com/cloud-native-scanner/CloudNativeScanner/blob/main/CONTRIBUTING.md) for the step-by-step guide.

In brief:

1. Create a class implementing `SourceScanner` with `@Component`
2. Implement `getSourceType()`, `getDisplayName()`, and `scan(ScanRequest)`
3. Optionally override `extractContent(url)` for custom extraction
4. Write tests with `MockWebServer`

## CI Pipeline

The GitHub Actions CI (`ci.yaml`) runs:

1. **Java** — `mvn verify -pl shared,pipeline-service -am`
2. **Next.js** — lint, type check, build
3. **Docker** — build + push both images to GHCR (main branch only)
4. **Helm** — lint the v2 chart
