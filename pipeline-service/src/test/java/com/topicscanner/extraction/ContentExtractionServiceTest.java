package com.topicscanner.extraction;

import com.topicscanner.scanner.ScannerRegistry;
import com.topicscanner.scanner.SourceScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentExtractionServiceTest {

    @Mock
    private ScannerRegistry scannerRegistry;
    @Mock
    private WebContentExtractor webExtractor;
    @Mock
    private StackOverflowExtractor stackOverflowExtractor;
    @Mock
    private YouTubeTranscriptExtractor youtubeExtractor;

    private ContentExtractionService service;

    @BeforeEach
    void setUp() {
        service = new ContentExtractionService(
                scannerRegistry, webExtractor, stackOverflowExtractor, youtubeExtractor);
    }

    @Test
    void extract_usesScannersCustomExtraction() {
        SourceScanner scanner = mock(SourceScanner.class);
        when(scannerRegistry.getScanner("reddit")).thenReturn(Optional.of(scanner));
        when(scanner.extractContent("https://reddit.com/r/test/1")).thenReturn(Optional.of("Reddit content"));

        Optional<String> result = service.extract("https://reddit.com/r/test/1", "reddit");

        assertTrue(result.isPresent());
        assertEquals("Reddit content", result.get());
        verifyNoInteractions(webExtractor);
    }

    @Test
    void extract_fallsBackToWebExtractorForBlogSources() {
        when(scannerRegistry.getScanner("medium")).thenReturn(Optional.empty());
        when(webExtractor.extract("https://medium.com/article")).thenReturn(Optional.of("Blog content"));

        Optional<String> result = service.extract("https://medium.com/article", "medium");

        assertTrue(result.isPresent());
        assertEquals("Blog content", result.get());
    }

    @Test
    void extract_usesYouTubeExtractorForYouTube() {
        when(scannerRegistry.getScanner("youtube")).thenReturn(Optional.empty());
        when(youtubeExtractor.extract("https://youtube.com/watch?v=abc")).thenReturn(Optional.of("Transcript"));

        Optional<String> result = service.extract("https://youtube.com/watch?v=abc", "youtube");

        assertTrue(result.isPresent());
        assertEquals("Transcript", result.get());
        verifyNoInteractions(webExtractor);
    }

    @Test
    void extract_usesStackOverflowExtractor() {
        when(scannerRegistry.getScanner("stackoverflow")).thenReturn(Optional.empty());
        when(stackOverflowExtractor.extract("https://stackoverflow.com/questions/123"))
                .thenReturn(Optional.of("Q&A content"));

        Optional<String> result = service.extract("https://stackoverflow.com/questions/123", "stackoverflow");

        assertTrue(result.isPresent());
        assertEquals("Q&A content", result.get());
    }

    @Test
    void extract_stackOverflowFallsBackToWebExtractor() {
        when(scannerRegistry.getScanner("stackoverflow")).thenReturn(Optional.empty());
        when(stackOverflowExtractor.extract("https://stackoverflow.com/questions/123"))
                .thenReturn(Optional.empty());
        when(webExtractor.extract("https://stackoverflow.com/questions/123"))
                .thenReturn(Optional.of("Web scraped content"));

        Optional<String> result = service.extract("https://stackoverflow.com/questions/123", "stackoverflow");

        assertTrue(result.isPresent());
        assertEquals("Web scraped content", result.get());
    }

    @Test
    void extract_returnsEmptyWhenAllMethodsFail() {
        when(scannerRegistry.getScanner("devto")).thenReturn(Optional.empty());
        when(webExtractor.extract("https://dev.to/article")).thenReturn(Optional.empty());

        Optional<String> result = service.extract("https://dev.to/article", "devto");

        assertTrue(result.isEmpty());
    }

    @Test
    void extract_scannerCustomExtractionTakesPriority() {
        SourceScanner scanner = mock(SourceScanner.class);
        when(scannerRegistry.getScanner("stackoverflow")).thenReturn(Optional.of(scanner));
        when(scanner.extractContent("https://stackoverflow.com/questions/123"))
                .thenReturn(Optional.of("Scanner extracted"));

        Optional<String> result = service.extract("https://stackoverflow.com/questions/123", "stackoverflow");

        assertTrue(result.isPresent());
        assertEquals("Scanner extracted", result.get());
        verifyNoInteractions(stackOverflowExtractor);
        verifyNoInteractions(webExtractor);
    }

    @Test
    void extract_scannerReturnsEmpty_fallsToSourceSpecific() {
        SourceScanner scanner = mock(SourceScanner.class);
        when(scannerRegistry.getScanner("youtube")).thenReturn(Optional.of(scanner));
        when(scanner.extractContent("https://youtube.com/watch?v=abc")).thenReturn(Optional.empty());
        when(youtubeExtractor.extract("https://youtube.com/watch?v=abc")).thenReturn(Optional.of("Transcript"));

        Optional<String> result = service.extract("https://youtube.com/watch?v=abc", "youtube");

        assertTrue(result.isPresent());
        assertEquals("Transcript", result.get());
    }
}
