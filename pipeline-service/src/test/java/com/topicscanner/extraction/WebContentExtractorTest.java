package com.topicscanner.extraction;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WebContentExtractorTest {

    private WebContentExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new WebContentExtractor();
    }

    @Test
    void extractFromDocument_findsArticleContent() {
        String html = """
                <html>
                <body>
                  <nav><a href="/">Home</a></nav>
                  <article>
                    <h1>Kubernetes Best Practices</h1>
                    <p>Kubernetes is a container orchestration platform that has become the standard
                    for deploying cloud-native applications. This guide covers essential best practices
                    for running production workloads.</p>
                    <h2>Resource Limits</h2>
                    <p>Always set resource requests and limits for your containers. This ensures proper
                    scheduling and prevents noisy neighbor issues in shared clusters.</p>
                    <pre>resources:
                      requests:
                        cpu: 100m
                        memory: 128Mi</pre>
                    <ul>
                      <li>Set CPU requests based on actual usage</li>
                      <li>Set memory limits to prevent OOM kills</li>
                    </ul>
                  </article>
                  <footer>Copyright 2024</footer>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        String text = content.get();
        assertTrue(text.contains("Kubernetes Best Practices"));
        assertTrue(text.contains("container orchestration platform"));
        assertTrue(text.contains("Resource Limits"));
        assertTrue(text.contains("cpu: 100m"));
        assertFalse(text.contains("Home"));  // nav removed
        assertFalse(text.contains("Copyright"));  // footer removed
    }

    @Test
    void extractFromDocument_handlesDevToLayout() {
        String html = """
                <html>
                <body>
                  <header class="header">Site Header</header>
                  <div class="crayons-article__body">
                    <h1>Getting Started with Go</h1>
                    <p>Go is a statically typed, compiled programming language designed at Google.
                    It is syntactically similar to C but with memory safety, garbage collection,
                    and structural typing.</p>
                    <p>Go makes it easy to build simple, reliable, and efficient software. Let's
                    explore the key features that make Go great for backend development.</p>
                  </div>
                  <div class="sidebar">Related Posts</div>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("Getting Started with Go"));
        assertTrue(content.get().contains("statically typed"));
        assertFalse(content.get().contains("Related Posts"));
    }

    @Test
    void extractFromDocument_returnsEmptyForShortContent() {
        String html = """
                <html><body><article><p>Too short.</p></article></body></html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isEmpty());
    }

    @Test
    void extractFromDocument_removesBoilerplateElements() {
        String html = """
                <html>
                <body>
                  <nav>Navigation links</nav>
                  <div class="advertisement">Buy now!</div>
                  <div class="social-share">Share on Twitter</div>
                  <article>
                    <p>This is the actual article content that should be extracted by the readability
                    parser. It contains enough text to meet the minimum content length requirement
                    for extraction.</p>
                  </article>
                  <div class="related-posts">You might also like...</div>
                  <footer>Footer content</footer>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        assertFalse(content.get().contains("Navigation"));
        assertFalse(content.get().contains("Buy now"));
        assertFalse(content.get().contains("Share on Twitter"));
        assertFalse(content.get().contains("You might also like"));
        assertFalse(content.get().contains("Footer content"));
        assertTrue(content.get().contains("actual article content"));
    }

    @Test
    void extractFromDocument_preservesCodeBlocks() {
        String html = """
                <html>
                <body>
                  <article>
                    <h2>Code Example</h2>
                    <p>Here is how to create a simple HTTP server in Go. This is a common pattern
                    used in many production applications for building REST APIs.</p>
                    <pre><code>package main

                func main() {
                    http.ListenAndServe(":8080", nil)
                }</code></pre>
                    <p>The above code starts a server on port 8080 and listens for incoming requests.</p>
                  </article>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("```"));
        assertTrue(content.get().contains("ListenAndServe"));
    }

    @Test
    void extractFromDocument_fallsBackToLargestBlock() {
        String html = """
                <html>
                <body>
                  <div class="post-wrapper">
                    <div class="small-div"><p>Small content</p></div>
                    <div class="main-content">
                      <p>This is the main content of the page. It contains substantial text
                      that makes it the largest content block on the page. The extractor should
                      identify this div as the main content area even without semantic HTML tags.</p>
                      <p>Additional paragraph with more content to ensure this block is clearly
                      the largest one on the page.</p>
                    </div>
                  </div>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        assertTrue(content.get().contains("main content of the page"));
    }

    @Test
    void extractFromDocument_handlesBlockquotes() {
        String html = """
                <html>
                <body>
                  <article>
                    <p>As the documentation states about Kubernetes resource management and
                    container orchestration for cloud-native applications:</p>
                    <blockquote>Kubernetes provides a framework to run distributed systems
                    resiliently. It takes care of scaling and failover for your application.</blockquote>
                    <p>This is a key insight for anyone deploying applications at scale in
                    production environments.</p>
                  </article>
                </body>
                </html>
                """;
        Document doc = Jsoup.parse(html);
        Optional<String> content = extractor.extractFromDocument(doc);

        assertTrue(content.isPresent());
        assertTrue(content.get().contains(">"));  // blockquote marker
    }
}
