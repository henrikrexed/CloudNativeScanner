package com.topicscanner.scanner;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ScannerRegistryTest {

    static class RedditScanner implements SourceScanner {
        @Override
        public String getSourceType() {
            return "reddit";
        }

        @Override
        public String getDisplayName() {
            return "Reddit";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return List.of(new ScanResult("Reddit Post", "https://reddit.com/r/test/1",
                    "reddit", Map.of(), LocalDateTime.now()));
        }
    }

    static class StackOverflowScanner implements SourceScanner {
        @Override
        public String getSourceType() {
            return "stackoverflow";
        }

        @Override
        public String getDisplayName() {
            return "Stack Overflow";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return List.of();
        }
    }

    static class DuplicateRedditScanner implements SourceScanner {
        @Override
        public String getSourceType() {
            return "reddit"; // same type as RedditScanner
        }

        @Override
        public String getDisplayName() {
            return "Reddit v2";
        }

        @Override
        public List<ScanResult> scan(ScanRequest request) {
            return List.of();
        }
    }

    private ScannerRegistry createRegistry(List<SourceScanner> scanners) {
        return new ScannerRegistry(scanners, "nonexistent-plugins-dir");
    }

    @Test
    void builtInScanners_registeredAtConstruction() {
        var registry = createRegistry(
                List.of(new RedditScanner(), new StackOverflowScanner()));

        assertEquals(2, registry.getAllScanners().size());
        assertTrue(registry.getRegisteredTypes().contains("reddit"));
        assertTrue(registry.getRegisteredTypes().contains("stackoverflow"));
    }

    @Test
    void getScanner_existingType_returnsScanner() {
        var registry = createRegistry(List.of(new RedditScanner()));

        Optional<SourceScanner> scanner = registry.getScanner("reddit");

        assertTrue(scanner.isPresent());
        assertEquals("Reddit", scanner.get().getDisplayName());
    }

    @Test
    void getScanner_unknownType_returnsEmpty() {
        var registry = createRegistry(List.of());

        Optional<SourceScanner> scanner = registry.getScanner("mastodon");

        assertTrue(scanner.isEmpty());
    }

    @Test
    void duplicateSourceType_keepsFirst() {
        var registry = createRegistry(
                List.of(new RedditScanner(), new DuplicateRedditScanner()));

        assertEquals(1, registry.getAllScanners().size());
        assertEquals("Reddit", registry.getScanner("reddit").get().getDisplayName());
    }

    @Test
    void register_afterConstruction_addsScanner() {
        var registry = createRegistry(List.of());
        assertEquals(0, registry.getAllScanners().size());

        registry.register(new StackOverflowScanner());

        assertEquals(1, registry.getAllScanners().size());
        assertTrue(registry.getScanner("stackoverflow").isPresent());
    }

    @Test
    void emptyRegistry_returnsEmptyCollections() {
        var registry = createRegistry(List.of());

        assertTrue(registry.getAllScanners().isEmpty());
        assertTrue(registry.getRegisteredTypes().isEmpty());
    }

    @Test
    void getRegisteredTypes_returnsUnmodifiableSet() {
        var registry = createRegistry(List.of(new RedditScanner()));

        assertThrows(UnsupportedOperationException.class,
                () -> registry.getRegisteredTypes().add("fake"));
    }

    @Test
    void getAllScanners_returnsUnmodifiableCollection() {
        var registry = createRegistry(List.of(new RedditScanner()));

        assertThrows(UnsupportedOperationException.class,
                () -> registry.getAllScanners().clear());
    }

    @Test
    void discoverPlugins_nonExistentDir_noErrors() {
        var registry = createRegistry(List.of(new RedditScanner()));

        // Should not throw — just logs debug and continues
        registry.discoverPlugins();

        assertEquals(1, registry.getAllScanners().size());
    }
}
