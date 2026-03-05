package com.topicscanner.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts transcripts from YouTube videos.
 * <p>
 * Primary: fetches captions via YouTube's timedtext API.
 * Fallback: uses yt-dlp to download auto-generated subtitles.
 * Optional: if Whisper service is configured, downloads audio and transcribes.
 */
@Component
public class YouTubeTranscriptExtractor {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeTranscriptExtractor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:v=|youtu\\.be/)([a-zA-Z0-9_-]{11})");

    private final WebClient webClient;
    private final int maxDurationMinutes;
    private final String whisperUrl;

    public YouTubeTranscriptExtractor(
            WebClient.Builder webClientBuilder,
            @Value("${topicscanner.extraction.youtube.max-duration-minutes:90}") int maxDurationMinutes,
            @Value("${topicscanner.extraction.whisper.url:}") String whisperUrl) {
        this.webClient = webClientBuilder.clone().build();
        this.maxDurationMinutes = maxDurationMinutes;
        this.whisperUrl = whisperUrl;
    }

    /**
     * Extract transcript from a YouTube video URL.
     *
     * @param url the YouTube video URL
     * @return transcript text, or empty if extraction fails
     */
    public Optional<String> extract(String url) {
        String videoId = extractVideoId(url);
        if (videoId == null) {
            logger.debug("Could not extract video ID from URL: {}", url);
            return Optional.empty();
        }

        // Try yt-dlp subtitle extraction first (most reliable)
        Optional<String> transcript = extractViaYtDlp(videoId);
        if (transcript.isPresent()) {
            return transcript;
        }

        // Fallback: try Whisper if configured
        if (whisperUrl != null && !whisperUrl.isBlank()) {
            transcript = extractViaWhisper(videoId);
            if (transcript.isPresent()) {
                return transcript;
            }
        }

        logger.info("All transcript extraction methods failed for video {}", videoId);
        return Optional.empty();
    }

    /**
     * Extract subtitles using yt-dlp command-line tool.
     * Downloads auto-generated or manual subtitles in SRT format.
     */
    private Optional<String> extractViaYtDlp(String videoId) {
        try {
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

            ProcessBuilder textPb = new ProcessBuilder(
                    "yt-dlp",
                    "--skip-download",
                    "--write-auto-sub",
                    "--write-sub",
                    "--sub-lang", "en",
                    "--sub-format", "vtt/srt/best",
                    "--print", "%(subtitles.en.-1.data)s",
                    "--no-warnings",
                    "--quiet",
                    videoUrl
            );
            textPb.redirectErrorStream(true);

            Process process = textPb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("yt-dlp timed out for video {}", videoId);
                return Optional.empty();
            }

            if (process.exitValue() == 0 && !output.isEmpty()) {
                String transcript = cleanSubtitleText(output.toString());
                if (!transcript.isBlank() && !"NA".equals(transcript.trim())) {
                    logger.info("Extracted transcript via yt-dlp for video {}", videoId);
                    return Optional.of(transcript);
                }
            }

            logger.debug("yt-dlp returned no subtitles for video {}", videoId);
            return Optional.empty();
        } catch (Exception e) {
            logger.debug("yt-dlp extraction failed for video {}: {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extract transcript via Whisper REST service.
     * Downloads audio with yt-dlp, sends to Whisper for transcription.
     */
    private Optional<String> extractViaWhisper(String videoId) {
        try {
            // Check Whisper service health
            String health = webClient.get()
                    .uri(whisperUrl + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (health == null) {
                logger.debug("Whisper service not available");
                return Optional.empty();
            }

            // Download audio via yt-dlp
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            ProcessBuilder pb = new ProcessBuilder(
                    "yt-dlp",
                    "-f", "bestaudio",
                    "--max-filesize", "200M",
                    "-o", "-",
                    "--no-warnings",
                    "--quiet",
                    videoUrl
            );

            Process process = pb.start();
            byte[] audioBytes;
            try {
                audioBytes = process.getInputStream().readAllBytes();
            } finally {
                boolean finished = process.waitFor(300, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                }
            }
            if (audioBytes.length == 0) {
                logger.warn("Audio download failed for video {}", videoId);
                return Optional.empty();
            }

            // Send to Whisper service
            String response = webClient.post()
                    .uri(whisperUrl + "/transcribe")
                    .bodyValue(audioBytes)
                    .header("Content-Type", "application/octet-stream")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(10))
                    .block();

            if (response != null) {
                JsonNode result = MAPPER.readTree(response);
                String text = result.path("text").asText("");
                if (!text.isBlank()) {
                    logger.info("Extracted transcript via Whisper for video {}", videoId);
                    return Optional.of(text);
                }
            }

            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Whisper extraction failed for video {}: {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Clean subtitle text by removing timestamps, formatting tags, and duplicate lines.
     */
    String cleanSubtitleText(String raw) {
        if (raw == null) return "";

        return raw
                // Remove VTT header
                .replaceAll("(?m)^WEBVTT.*$", "")
                .replaceAll("(?m)^Kind:.*$", "")
                .replaceAll("(?m)^Language:.*$", "")
                // Remove timestamp lines (00:00:01.000 --> 00:00:05.000)
                .replaceAll("(?m)^[\\d:.]+\\s*-->\\s*[\\d:.]+.*$", "")
                // Remove SRT sequence numbers
                .replaceAll("(?m)^\\d+$", "")
                // Remove VTT positioning tags
                .replaceAll("<[^>]+>", "")
                // Remove alignment/position tags
                .replaceAll("(?m)^align:.*$", "")
                .replaceAll("(?m)^position:.*$", "")
                // Clean up whitespace
                .replaceAll("\n{2,}", "\n")
                .trim();
    }

    String extractVideoId(String url) {
        if (url == null) return null;
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
