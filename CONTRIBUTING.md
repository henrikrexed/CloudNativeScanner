# Contributing to TopicScanner

## How to Add a New Scanner

Scanners discover topics from external sources. The system uses Java ServiceLoader SPI, so adding a new scanner requires no changes to existing code.

### 1. Implement `SourceScanner`

Create a new class in `pipeline-service/src/main/java/com/topicscanner/scanner/`:

```java
package com.topicscanner.scanner;

import java.util.List;

public class MySourceScanner implements SourceScanner {

    @Override
    public String getType() {
        return "my-source";  // unique identifier
    }

    @Override
    public String getDisplayName() {
        return "My Source";
    }

    @Override
    public boolean isEnabled() {
        // Read from config: scanners.my-source.enabled
        return true;
    }

    @Override
    public List<ScanResult> scan(String keyword, int maxResults) {
        // Fetch from your source API
        // Return list of ScanResult(title, url, snippet, sourceDate)
        return List.of();
    }
}
```

### 2. Register via ServiceLoader

Create or append to:
```
pipeline-service/src/main/resources/META-INF/services/com.topicscanner.scanner.SourceScanner
```

Add one line:
```
com.topicscanner.scanner.MySourceScanner
```

### 3. Add configuration

In `pipeline-service/src/main/resources/application.yaml`:
```yaml
scanners:
  my-source:
    enabled: true
    api-key: ${MY_SOURCE_API_KEY:}
```

Update `helm/cloud-native-scanner-v2/values.yaml`:
```yaml
scanners:
  mySource:
    enabled: false
    apiKey: ""
```

### 4. Write tests

Create `pipeline-service/src/test/java/com/topicscanner/scanner/MySourceScannerTest.java` with at least:
- Test that `getType()` returns the expected string
- Test `scan()` with a mocked HTTP response (use `MockWebServer`)
- Test that `scan()` handles errors gracefully (returns empty list, doesn't throw)

---

## How to Add a New Content Format

Content formats define how the Generate stage transforms analyzed topics into publication-ready content (blog post, YouTube script, LinkedIn post, etc.).

### 1. Add the format constant

In `pipeline-service/src/main/java/com/topicscanner/pipeline/generate/`:

```java
// Add to OutputFormat enum or constants class
public static final String MY_FORMAT = "my_format";
```

### 2. Add a prompt template

In `pipeline-service/src/main/resources/prompts/` or via the `PromptManagementService`, add a system prompt for your format:

```
You are a technical writer creating a {format} about {topic}.
Given the following analyzed content:
{content}

Write a {format} that is engaging, technically accurate, and ...
```

### 3. Register in ContentGenerationService

The `ContentGenerationService` maps format strings to prompt templates. Add your format to the switch/map:

```java
case "my_format" -> buildPrompt("my-format-system-prompt", topic, content);
```

### 4. Add UI option

In `webui-nodejs/app/topics/[id]/page.tsx`, add the option to the format select:

```tsx
<option value="my_format">My Format</option>
```

### 5. Write tests

- Unit test that the prompt is correctly assembled for your format
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
- [ ] New scanners implement `SourceScanner` and are registered via ServiceLoader
- [ ] New REST endpoints follow the `/api/` prefix convention
- [ ] Frontend types are defined in `lib/v2api.ts`
- [ ] No `any` types in TypeScript
- [ ] Accessibility: `aria-label` on icon buttons, `htmlFor`/`id` on form fields
