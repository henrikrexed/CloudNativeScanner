# Scanners

TopicScanner discovers topics from six sources. Each scanner implements the `SourceScanner` interface.

## StackOverflow

- **Source type:** `stackoverflow`
- **API:** StackExchange v2.3 (`api.stackexchange.com/2.3`)
- **Auth:** None required (public API)
- **Rate limiting:** 2-second delay between page requests

Searches questions by keyword, fetches up to 3 pages of 25 results each. Returns questions sorted by relevance.

**Metadata:** `externalId`, `score`, `answerCount`, `viewCount`, `isAnswered`, `tags[]`

```yaml
# No special config needed — enabled by default
scanners:
  stackoverflow:
    enabled: true
```

## Reddit

- **Source type:** `reddit`
- **API:** Reddit JSON API (public, no OAuth needed for search)
- **Rate limiting:** 6-second delay between subreddit requests

Searches across 10 subreddits: `programming`, `devops`, `kubernetes`, `cloudnative`, `golang`, `java`, `python`, `rust`, `webdev`, `machinelearning`.

**Custom extraction:** Fetches full thread JSON (post + top comments) for richer content.

**Metadata:** `externalId`, `subreddit`, `score`, `numComments`, `upvoteRatio`

```yaml
scanners:
  reddit:
    enabled: true
    clientId: ""        # Optional — for authenticated API access
    clientSecret: ""
    userAgent: "TopicScanner/2.0"
```

## Medium

- **Source type:** `medium`
- **API:** Public RSS feeds (`medium.com/feed/tag/{tag}`)
- **Auth:** None required
- **Rate limiting:** 1-second delay between tag requests

Converts keywords to tags (lowercase, hyphens) and fetches RSS feeds. Parses XML with Jsoup.

**Metadata:** `externalId`, `author`, `tag`

```yaml
scanners:
  medium:
    enabled: true
```

## Dev.to

- **Source type:** `devto`
- **API:** Forem API (`dev.to/api`)
- **Auth:** None required
- **Rate limiting:** 1-second delay between tag requests

Converts keywords to tags (lowercase, alphanumeric) and searches rising articles.

**Metadata:** `externalId`, `author`, `readingTimeMinutes`, `positiveReactionsCount`, `commentsCount`, `tags[]`

```yaml
scanners:
  devto:
    enabled: true
```

## Hashnode

- **Source type:** `hashnode`
- **API:** GraphQL (`gql.hashnode.com`)
- **Auth:** Optional `HASHNODE_API_TOKEN` for authenticated requests
- **Rate limiting:** None (single GraphQL query per keyword)

Queries `tagPosts` or feed with `first: 20`. Returns posts with reaction and reply counts.

**Metadata:** `externalId`, `brief` (truncated to 200 chars), `reactionCount`, `replyCount`

```yaml
scanners:
  hashnode:
    enabled: true
```

## YouTube

- **Source type:** `youtube`
- **API:** Google Data API v3 (`googleapis.com/youtube/v3`)
- **Auth:** Required — YouTube Data API key
- **Quota:** Each search costs 100 units; daily limit is 10,000 units

Searches videos by keyword, then fetches statistics (views, likes, comments) and content details (duration).

**Metadata:** `externalId` (videoId), `channelTitle`, `channelId`, `viewCount`, `likeCount`, `commentCount`, `duration` (ISO 8601)

**Custom extraction:** Fetches video captions/transcripts via YouTube API.

```yaml
scanners:
  youtube:
    enabled: true
    apiKey: "your-youtube-api-key"
```

!!! warning
    YouTube API has strict quotas. A single scan with 5 keywords uses ~500 quota units. Monitor usage at [Google Cloud Console](https://console.cloud.google.com/apis/api/youtube.googleapis.com/quotas).

## Scanner Interface

All scanners implement:

```java
public interface SourceScanner {
    String getSourceType();                        // unique ID
    String getDisplayName();                       // UI label
    List<ScanResult> scan(ScanRequest request);    // discover topics
    default Optional<String> extractContent(String url) {
        return Optional.empty();  // defer to generic extraction
    }
}
```

**ScanRequest:**

```java
record ScanRequest(
    List<String> keywords,
    List<String> negativeKeywords,
    Map<String, Object> scannerConfig,
    int maxResults  // default 25
)
```

**ScanResult:**

```java
record ScanResult(
    String title,           // required
    String url,             // required
    String sourceType,      // required
    Map<String, Object> metadata,
    LocalDateTime sourceDate
)
```
