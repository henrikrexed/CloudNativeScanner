package com.topicscanner.extraction;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Extracts clean text content from blog post URLs using readability-style parsing.
 * Strips navigation, ads, sidebars, footers, and boilerplate.
 * Preserves headings, paragraphs, code blocks, and lists.
 */
@Component
public class WebContentExtractor {

    private static final Logger logger = LoggerFactory.getLogger(WebContentExtractor.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int MIN_CONTENT_LENGTH = 100;

    /** Tags that typically contain boilerplate, not article content. */
    private static final Set<String> BOILERPLATE_TAGS = Set.of(
            "nav", "header", "footer", "aside", "sidebar",
            "menu", "toolbar", "banner"
    );

    /** CSS selectors for elements to remove before extraction. */
    private static final String[] REMOVE_SELECTORS = {
            "nav", "header", "footer", "aside",
            ".sidebar", ".menu", ".navigation", ".nav",
            ".footer", ".header", ".advertisement", ".ad",
            ".social-share", ".share-buttons", ".related-posts",
            ".comments", ".comment-section", "#comments",
            ".cookie-banner", ".popup", ".modal",
            "script", "style", "noscript", "iframe",
            "[role=navigation]", "[role=banner]", "[role=contentinfo]",
            ".subscribe", ".newsletter", ".cta"
    };

    /** Selectors to try for finding the main article content, in priority order. */
    private static final String[] CONTENT_SELECTORS = {
            "article",
            "[role=main]",
            "main",
            ".post-content",
            ".article-content",
            ".entry-content",
            ".post-body",
            ".article-body",
            "#content",
            ".content",
            ".markdown-body",  // GitHub/Dev.to
            ".crayons-article__body",  // Dev.to
            "#article-body"  // Hashnode
    };

    /**
     * Extract clean text content from a URL.
     *
     * @param url the URL to extract content from
     * @return extracted text, or empty if content is inaccessible or too short
     */
    public Optional<String> extract(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("TopicScanner/2.0 (DevRel Intelligence)")
                    .timeout(CONNECT_TIMEOUT_MS)
                    .maxBodySize(5_000_000)
                    .followRedirects(true)
                    .get();

            return extractFromDocument(doc);
        } catch (Exception e) {
            logger.warn("Failed to fetch URL {}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract content from an already-parsed JSoup Document.
     * Useful for testing and for content already fetched by other means.
     */
    public Optional<String> extractFromDocument(Document doc) {
        // Clone to avoid mutating the caller's document
        doc = doc.clone();

        // Remove boilerplate elements
        for (String selector : REMOVE_SELECTORS) {
            doc.select(selector).remove();
        }

        // Try to find the main content area
        Element contentElement = findContentElement(doc);
        if (contentElement == null) {
            logger.debug("No content element found in document: {}", doc.title());
            return Optional.empty();
        }

        String text = formatContent(contentElement);

        if (text.length() < MIN_CONTENT_LENGTH) {
            logger.debug("Extracted content too short ({} chars) for: {}", text.length(), doc.title());
            return Optional.empty();
        }

        return Optional.of(text);
    }

    private Element findContentElement(Document doc) {
        // Try known content selectors in priority order
        for (String selector : CONTENT_SELECTORS) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                // Pick the one with the most text content
                Element best = null;
                int bestLength = 0;
                for (Element el : elements) {
                    int len = el.text().length();
                    if (len > bestLength) {
                        bestLength = len;
                        best = el;
                    }
                }
                if (best != null && bestLength >= MIN_CONTENT_LENGTH) {
                    return best;
                }
            }
        }

        // Fallback: find the largest text block in the body
        Element body = doc.body();
        if (body == null) {
            return null;
        }

        return findLargestContentBlock(body);
    }

    private Element findLargestContentBlock(Element root) {
        Element largest = null;
        int largestLength = 0;

        for (Element el : root.select("div, section")) {
            // Skip elements that are likely boilerplate
            String tagName = el.tagName().toLowerCase();
            if (BOILERPLATE_TAGS.contains(tagName)) {
                continue;
            }

            String className = el.className().toLowerCase();
            String id = el.id().toLowerCase();
            if (isBoilerplateByAttribute(className) || isBoilerplateByAttribute(id)) {
                continue;
            }

            // Count direct paragraph text
            int pTextLength = 0;
            for (Element p : el.select("> p, > h1, > h2, > h3, > h4, > pre, > ul, > ol")) {
                pTextLength += p.text().length();
            }

            if (pTextLength > largestLength) {
                largestLength = pTextLength;
                largest = el;
            }
        }

        return largestLength >= MIN_CONTENT_LENGTH ? largest : null;
    }

    private boolean isBoilerplateByAttribute(String attr) {
        return attr.contains("nav") || attr.contains("sidebar")
                || attr.contains("footer") || attr.contains("header")
                || attr.contains("menu") || attr.contains("comment")
                || attr.contains("ad-") || attr.contains("advertisement");
    }

    private String formatContent(Element contentElement) {
        StringBuilder sb = new StringBuilder();

        for (Element child : contentElement.select("h1, h2, h3, h4, h5, h6, p, pre, code, ul, ol, blockquote, li")) {
            String tag = child.tagName();

            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    sb.append("\n\n## ").append(child.text()).append("\n\n");
                }
                case "pre" -> {
                    sb.append("\n```\n").append(child.text()).append("\n```\n");
                }
                case "blockquote" -> {
                    sb.append("\n> ").append(child.text()).append("\n");
                }
                case "li" -> {
                    // Only add if not nested deeply
                    if (child.parents().select("li").isEmpty()) {
                        sb.append("- ").append(child.text()).append("\n");
                    }
                }
                case "p" -> {
                    String text = child.text().trim();
                    if (!text.isEmpty()) {
                        sb.append(text).append("\n\n");
                    }
                }
                default -> {
                    // Skip code tags inside pre (already handled)
                    // Skip ul/ol (handled via li)
                }
            }
        }

        // Clean up excessive whitespace
        return sb.toString()
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
