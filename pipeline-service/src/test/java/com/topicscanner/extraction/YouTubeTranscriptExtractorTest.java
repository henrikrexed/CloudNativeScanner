package com.topicscanner.extraction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeTranscriptExtractorTest {

    private YouTubeTranscriptExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new YouTubeTranscriptExtractor(WebClient.builder(), 90, "");
    }

    @Test
    void extractVideoId_fromStandardUrl() {
        assertEquals("dQw4w9WgXcQ",
                extractor.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
    }

    @Test
    void extractVideoId_fromShortUrl() {
        assertEquals("dQw4w9WgXcQ",
                extractor.extractVideoId("https://youtu.be/dQw4w9WgXcQ"));
    }

    @Test
    void extractVideoId_fromUrlWithParams() {
        assertEquals("dQw4w9WgXcQ",
                extractor.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=120"));
    }

    @Test
    void extractVideoId_returnsNullForInvalidUrl() {
        assertNull(extractor.extractVideoId("https://www.youtube.com/channel/UC123"));
    }

    @Test
    void extractVideoId_returnsNullForNull() {
        assertNull(extractor.extractVideoId(null));
    }

    @Test
    void cleanSubtitleText_removesTimestamps() {
        String raw = """
                WEBVTT
                Kind: captions
                Language: en

                00:00:01.000 --> 00:00:05.000
                Hello and welcome to this tutorial

                00:00:05.500 --> 00:00:10.000
                Today we'll be talking about Kubernetes
                """;

        String cleaned = extractor.cleanSubtitleText(raw);

        assertFalse(cleaned.contains("WEBVTT"));
        assertFalse(cleaned.contains("-->"));
        assertFalse(cleaned.contains("Kind:"));
        assertTrue(cleaned.contains("Hello and welcome to this tutorial"));
        assertTrue(cleaned.contains("Today we'll be talking about Kubernetes"));
    }

    @Test
    void cleanSubtitleText_removesSrtSequenceNumbers() {
        String raw = """
                1
                00:00:01,000 --> 00:00:05,000
                First line of text

                2
                00:00:05,500 --> 00:00:10,000
                Second line of text
                """;

        String cleaned = extractor.cleanSubtitleText(raw);

        assertTrue(cleaned.contains("First line"));
        assertTrue(cleaned.contains("Second line"));
        assertFalse(cleaned.contains("-->"));
    }

    @Test
    void cleanSubtitleText_removesHtmlTags() {
        String raw = """
                00:00:01.000 --> 00:00:05.000
                <c.colorCCCCCC>Hello</c> and <b>welcome</b>
                """;

        String cleaned = extractor.cleanSubtitleText(raw);

        assertTrue(cleaned.contains("Hello"));
        assertTrue(cleaned.contains("welcome"));
        assertFalse(cleaned.contains("<c."));
        assertFalse(cleaned.contains("<b>"));
    }

    @Test
    void cleanSubtitleText_handlesNull() {
        assertEquals("", extractor.cleanSubtitleText(null));
    }

    @Test
    void cleanSubtitleText_handlesEmpty() {
        assertEquals("", extractor.cleanSubtitleText(""));
    }
}
