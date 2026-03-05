package com.topicscanner.scanner.impl;

import com.topicscanner.scanner.ScanRequest;
import com.topicscanner.scanner.ScanResult;
import com.topicscanner.scanner.ScannerUtils;
import com.topicscanner.scanner.SourceScanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans Medium for topics using the public RSS feed.
 * No API key required. Searches by tag via RSS.
 */
@Component
public class MediumScanner implements SourceScanner {

    private static final Logger logger = LoggerFactory.getLogger(MediumScanner.class);
    private static final String RSS_BASE_URL = "https://medium.com/feed/tag";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final long REQUEST_DELAY_MS = 1000;

    private final WebClient webClient;

    public MediumScanner(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.clone()
                .defaultHeader("User-Agent", "TopicScanner/2.0 (DevRel Intelligence)")
                .build();
    }

    @Override
    public String getSourceType() {
        return "medium";
    }

    @Override
    public String getDisplayName() {
        return "Medium";
    }

    @Override
    public List<ScanResult> scan(ScanRequest request) {
        List<ScanResult> results = new ArrayList<>();

        if (request.keywords().isEmpty()) {
            logger.warn("Medium scan skipped: no keywords provided");
            return results;
        }

        for (String keyword : request.keywords()) {
            if (results.size() >= request.maxResults()) break;

            try {
                String tag = keyword.toLowerCase().replaceAll("[^a-z0-9-]", "-");
                if (tag.isBlank()) continue;

                List<ScanResult> batch = fetchRssFeed(tag, request);
                results.addAll(batch);

                if (request.keywords().size() > 1) {
                    Thread.sleep(REQUEST_DELAY_MS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("Medium scan failed for keyword '{}': {}", keyword, e.getMessage());
            }
        }

        return results.stream().limit(request.maxResults()).toList();
    }

    private List<ScanResult> fetchRssFeed(String tag, ScanRequest request) {
        String feedUrl = RSS_BASE_URL + "/" + tag;

        String body = webClient.get()
                .uri(feedUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        if (body == null) return List.of();

        List<ScanResult> results = new ArrayList<>();
        try {
            Document doc = Jsoup.parse(body, "", org.jsoup.parser.Parser.xmlParser());
            Elements items = doc.select("item");

            for (Element item : items) {
                String title = item.select("title").text();
                String link = item.select("link").text();
                if (link.isBlank()) link = item.select("guid").text();

                String pubDate = item.select("pubDate").text();
                String author = item.select("dc|creator").text();
                if (author.isBlank()) author = item.select("creator").text();

                if (title.isBlank() || link.isBlank()) continue;

                // Strip query parameters
                if (link.contains("?")) link = link.substring(0, link.indexOf("?"));
                if (ScannerUtils.matchesNegativeKeywords(title, request.negativeKeywords())) continue;

                LocalDateTime sourceDate = parseRssDate(pubDate);
                String externalId = extractMediumId(link);

                results.add(new ScanResult(title, link, getSourceType(),
                        Map.of("externalId", externalId, "author", author, "tag", tag),
                        sourceDate));
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Medium RSS feed for tag '{}': {}", tag, e.getMessage());
        }
        return results;
    }

    private String extractMediumId(String url) {
        int lastDash = url.lastIndexOf('-');
        if (lastDash > 0 && lastDash < url.length() - 1) {
            return url.substring(lastDash + 1);
        }
        return url;
    }

    private LocalDateTime parseRssDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME);
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
