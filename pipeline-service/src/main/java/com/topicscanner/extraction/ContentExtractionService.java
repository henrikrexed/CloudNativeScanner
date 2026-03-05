package com.topicscanner.extraction;

import com.topicscanner.scanner.ScannerRegistry;
import com.topicscanner.scanner.SourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Orchestrates content extraction across all source types.
 * <p>
 * Strategy:
 * 1. Check if the scanner for this source type has a custom extractContent() method
 * 2. For YouTube, use YouTubeTranscriptExtractor
 * 3. For StackOverflow, use StackOverflowExtractor
 * 4. For all other URLs, fall back to WebContentExtractor (readability-style)
 */
@Service
public class ContentExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(ContentExtractionService.class);

    private final ScannerRegistry scannerRegistry;
    private final WebContentExtractor webExtractor;
    private final StackOverflowExtractor stackOverflowExtractor;
    private final YouTubeTranscriptExtractor youtubeExtractor;

    public ContentExtractionService(ScannerRegistry scannerRegistry,
                                    WebContentExtractor webExtractor,
                                    StackOverflowExtractor stackOverflowExtractor,
                                    YouTubeTranscriptExtractor youtubeExtractor) {
        this.scannerRegistry = scannerRegistry;
        this.webExtractor = webExtractor;
        this.stackOverflowExtractor = stackOverflowExtractor;
        this.youtubeExtractor = youtubeExtractor;
    }

    /**
     * Extract content from a URL, choosing the appropriate extraction strategy.
     *
     * @param url the URL to extract content from
     * @param sourceType the source type (e.g., "reddit", "stackoverflow", "youtube")
     * @return extracted text content, or empty if extraction fails
     */
    public Optional<String> extract(String url, String sourceType) {
        logger.debug("Extracting content from {} (source: {})", url, sourceType);

        // 1. Try scanner's custom extraction first
        Optional<SourceScanner> scanner = scannerRegistry.getScanner(sourceType);
        if (scanner.isPresent()) {
            Optional<String> content = scanner.get().extractContent(url);
            if (content.isPresent()) {
                logger.debug("Content extracted via scanner for {}", url);
                return content;
            }
        }

        // 2. Source-specific extractors
        return switch (sourceType) {
            case "youtube" -> {
                Optional<String> transcript = youtubeExtractor.extract(url);
                if (transcript.isEmpty()) {
                    logger.info("YouTube transcript extraction failed for {}", url);
                }
                yield transcript;
            }
            case "stackoverflow" -> {
                Optional<String> qa = stackOverflowExtractor.extract(url);
                if (qa.isEmpty()) {
                    logger.debug("SO API extraction failed, falling back to web for {}", url);
                    yield webExtractor.extract(url);
                }
                yield qa;
            }
            default -> {
                // 3. Generic web extraction for blogs (medium, devto, hashnode, etc.)
                yield webExtractor.extract(url);
            }
        };
    }
}
