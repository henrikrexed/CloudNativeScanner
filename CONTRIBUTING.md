# Contributing to TopicScanner

## How to Add a New Scanner

Scanners discover topics from external sources. Built-in scanners are Spring `@Component` beans auto-discovered via classpath scanning. External plugins can be loaded from JARs in the `plugins/` directory via ServiceLoader.

### 1. Implement `SourceScanner`

Create a new class in `pipeline-service/src/main/java/com/topicscanner/scanner/`:

```java
package com.topicscanner.scanner;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Component
public class MySourceScanner implements SourceScanner {

    @Override
    public String getSourceType() {
        return "my-source";  // unique identifier, used as FK in sources table
    }

    @Override
    public String getDisplayName() {
        return "My Source";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        // request.keywords()          — search terms
        // request.negativeKeywords()   — exclusion terms
        // request.scannerConfig()      — scanner-specific settings
        // request.maxResults()         — max topics to return (default 25)

        // Fetch from your source API, return discovered topics:
        return List.of(
            new ScanResult(
                "Topic Title",                    // title (required)
                "https://example.com/topic",      // url (required)
                getSourceType(),                  // sourceType (required)
                Map.of("author", "Jane Doe"),     // metadata (optional extras)
                LocalDateTime.now()               // sourceDate
            )
        );
    }
}
```

The `@Component` annotation registers it automatically. The `ScannerRegistry` will discover it on startup.

### 2. External plugin (alternative to `@Component`)

For scanners distributed as separate JARs, use ServiceLoader instead. Create:
```
META-INF/services/com.topicscanner.scanner.SourceScanner
```
containing your fully-qualified class name. Place the JAR in the `plugins/` directory.

### 3. Add configuration (optional)

Scanner-specific config can be passed via `ScanRequest.scannerConfig()`. Add environment variables to `pipeline-service/src/main/resources/application.yml` under `topicscanner:`:

```yaml
topicscanner:
  scanner:
    my-source:
      api-key: ${MY_SOURCE_API_KEY:}
```

### 4. Write tests

Create `pipeline-service/src/test/java/com/topicscanner/scanner/MySourceScannerTest.java` with at least:
- Test that `getSourceType()` returns the expected string
- Test `scan()` with a mocked HTTP response (use `MockWebServer`)
- Test that `scan()` handles errors gracefully (returns empty list, doesn't throw)

---

## How to Add a New Content Format

Content formats define how the Generate stage transforms analyzed topics into publication-ready content. Current formats: `blog_post` (default), `youtube_script`, `linkedin_post`, `newsletter`.

### 1. Add a case to `ContentGenerationService.buildSystemPrompt()`

In `pipeline-service/src/main/java/com/topicscanner/generator/ContentGenerationService.java`, add a new case to the switch statement (~line 283):

```java
case "my_format" -> sb.append(
    "Write a [your format description]. " +
    "Include [specific instructions for this format].");
```

### 2. Add UI option

In `webui-nodejs/app/topics/[id]/page.tsx`, add the option to the format select:

```tsx
<option value="my_format">My Format</option>
```

### 3. Write tests

- Unit test that `buildSystemPrompt` returns the expected instructions for your format
- Integration test that the full generate pipeline produces non-empty output

---

## PR Guidelines

### Branch naming
```
feature/short-description
fix/issue-description
```

### Commit messages
- Use imperative mood: "Add Reddit scanner" not "Added Reddit scanner"
- Keep the first line under 72 characters
- Add a blank line then details if needed

### Before submitting
1. **Java**: `mvn verify -pl shared,pipeline-service -am` passes
2. **Frontend**: `npm run lint && npx tsc --noEmit && npm run build` passes (in `webui-nodejs/`)
3. **No secrets** committed (API keys, credentials)
4. **Tests added** for new functionality
5. **Helm**: `helm lint helm/cloud-native-scanner-v2/` passes if chart was modified

### Review checklist
- [ ] Code follows existing patterns (JdbcTemplate for queries, `LLMService` for LLM calls)
- [ ] New scanners implement `SourceScanner` with `@Component` (or ServiceLoader for plugins)
- [ ] New REST endpoints follow the `/api/` prefix convention
- [ ] Frontend types are defined in `lib/v2api.ts`
- [ ] No `any` types in TypeScript
- [ ] Accessibility: `aria-label` on icon buttons, `htmlFor`/`id` on form fields
