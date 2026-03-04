package com.topicscanner.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry of all available source scanners.
 * <p>
 * Discovers scanners from two paths:
 * <ol>
 *   <li><b>Built-in:</b> Spring {@code @Component} scanners injected via constructor</li>
 *   <li><b>Plugins:</b> External JARs in the plugins directory loaded via {@link ServiceLoader}</li>
 * </ol>
 */
@Component
public class ScannerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ScannerRegistry.class);

    private final Map<String, SourceScanner> scanners = new ConcurrentHashMap<>();
    private final String pluginsDir;
    private URLClassLoader pluginClassLoader;

    /**
     * Spring injects all {@code @Component} beans that implement {@link SourceScanner}.
     * The list may be empty if no built-in scanners are on the classpath yet.
     */
    public ScannerRegistry(
            List<SourceScanner> builtInScanners,
            @Value("${topicscanner.scanner.plugins-dir:plugins}") String pluginsDir) {
        this.pluginsDir = pluginsDir;
        for (SourceScanner scanner : builtInScanners) {
            register(scanner);
        }
    }

    @PostConstruct
    void discoverPlugins() {
        loadPluginScanners();
        logger.info("ScannerRegistry initialized with {} scanner(s): {}",
                scanners.size(), scanners.keySet());
    }

    @PreDestroy
    void cleanup() {
        if (pluginClassLoader != null) {
            try {
                pluginClassLoader.close();
            } catch (Exception e) {
                logger.warn("Failed to close plugin classloader", e);
            }
        }
    }

    /**
     * Register a scanner. Logs a warning and skips if a scanner with the same
     * source type is already registered.
     */
    public void register(SourceScanner scanner) {
        String type = scanner.getSourceType();
        SourceScanner existing = scanners.putIfAbsent(type, scanner);
        if (existing != null) {
            logger.warn("Duplicate scanner for source type '{}': keeping {}, ignoring {}",
                    type, existing.getClass().getName(), scanner.getClass().getName());
        } else {
            logger.info("Registered scanner: {} ({})", scanner.getDisplayName(), type);
        }
    }

    /**
     * Get a scanner by source type.
     */
    public Optional<SourceScanner> getScanner(String sourceType) {
        return Optional.ofNullable(scanners.get(sourceType));
    }

    /**
     * Get all registered scanners.
     */
    public Collection<SourceScanner> getAllScanners() {
        return Collections.unmodifiableCollection(scanners.values());
    }

    /**
     * Get all registered source types.
     */
    public Set<String> getRegisteredTypes() {
        return Collections.unmodifiableSet(scanners.keySet());
    }

    /**
     * Load scanner plugins from the plugins directory using ServiceLoader.
     */
    private void loadPluginScanners() {
        File dir = new File(pluginsDir);
        if (!dir.isDirectory()) {
            logger.debug("No plugins directory found at '{}', skipping plugin discovery", dir.getAbsolutePath());
            return;
        }

        File[] jars = dir.listFiles((d, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            logger.debug("No JAR files found in plugins directory");
            return;
        }

        logger.info("Loading scanner plugins from {} ({} JAR files)", dir.getAbsolutePath(), jars.length);

        try {
            URL[] urls = new URL[jars.length];
            for (int i = 0; i < jars.length; i++) {
                urls[i] = jars[i].toURI().toURL();
                logger.debug("Plugin JAR: {}", jars[i].getName());
            }

            pluginClassLoader = new URLClassLoader(urls, getClass().getClassLoader());
            ServiceLoader<SourceScanner> loader = ServiceLoader.load(SourceScanner.class, pluginClassLoader);

            for (SourceScanner scanner : loader) {
                register(scanner);
                logger.info("Loaded plugin scanner: {} ({})", scanner.getDisplayName(), scanner.getSourceType());
            }
        } catch (Exception e) {
            logger.error("Failed to load scanner plugins", e);
        }
    }
}
