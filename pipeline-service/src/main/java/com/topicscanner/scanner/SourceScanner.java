package com.topicscanner.scanner;

import java.util.List;
import java.util.Optional;

/**
 * Contract for all topic source scanners.
 * <p>
 * Built-in scanners are Spring {@code @Component}s discovered via classpath scanning.
 * External plugins are JARs in the {@code plugins/} directory loaded via
 * {@link java.util.ServiceLoader}.
 */
public interface SourceScanner {

    /**
     * Unique identifier for this source (e.g., "reddit", "stackoverflow", "mastodon").
     * Must be stable — used as FK to the sources table and in configuration keys.
     */
    String getSourceType();

    /**
     * Human-readable name shown in the UI (e.g., "Reddit", "Stack Overflow").
     */
    String getDisplayName();

    /**
     * Scan the source and return discovered topics.
     *
     * @param request keywords, negative keywords, scanner config, and max results
     * @return list of discovered scan results (may be empty, never null)
     */
    List<ScanResult> scan(ScanRequest request);

    /**
     * Extract full text content from a discovered URL.
     * Override this if the source needs custom extraction logic
     * (e.g., YouTube transcript, Reddit thread expansion).
     * <p>
     * If not overridden, the pipeline's generic extraction step handles it.
     *
     * @param url the URL to extract content from
     * @return extracted text content, or empty if this scanner defers to generic extraction
     */
    default Optional<String> extractContent(String url) {
        return Optional.empty();
    }
}
