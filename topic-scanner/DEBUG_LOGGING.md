# Topic Scanner Debug Logging Guide

This document explains how to use the enhanced debug logging and tracing features in the topic-scanner.

## Overview

The topic-scanner now includes comprehensive debug logging and tracing capabilities to help you monitor:
- Source browsing and data collection algorithms
- HTTP requests and responses
- Performance metrics
- Algorithm flow and decision points
- Error handling and recovery

## Debug Features

### 1. Trace-Based Logging
- **Trace IDs**: Each scan operation gets a unique trace ID for correlation
- **Operation Tracking**: Track the flow through different operations (DAILY_SCAN, SOURCE_SCAN, REDDIT_SCAN, etc.)
- **Step-by-Step Logging**: Detailed logging of each step in the scanning process
- **Performance Metrics**: Timing information for each operation

### 2. Structured Logging
- **JSON Format**: All logs are structured in JSON format for easy parsing
- **MDC Context**: Trace information is automatically included in log context
- **Multiple Outputs**: Console, file, and specialized metric logs

### 3. Debug Configuration
The debug features can be controlled via environment variables or application.yml:

```yaml
app:
  debug:
    enabled: true                    # Enable/disable debug mode
    trace-requests: true            # Log HTTP requests/responses
    trace-performance: true         # Log performance metrics
    detailed-scan-logging: true     # Log detailed scan results
```

## Running in Debug Mode

### Option 1: Using the Debug Script
```bash
./run-debug.sh
```

### Option 2: Manual Configuration
```bash
export DEBUG_MODE=true
export TRACE_REQUESTS=true
export TRACE_PERFORMANCE=true
export DETAILED_SCAN_LOGGING=true
export LOG_LEVEL=DEBUG
export SPRING_PROFILES_ACTIVE=debug

java -jar target/topic-scanner-1.0.0.jar
```

### Option 3: Environment Variables
```bash
DEBUG_MODE=true TRACE_REQUESTS=true TRACE_PERFORMANCE=true DETAILED_SCAN_LOGGING=true LOG_LEVEL=DEBUG SPRING_PROFILES_ACTIVE=debug java -jar target/topic-scanner-1.0.0.jar
```

## Log Files

When running in debug mode, the following log files are created:

### 1. `logs/topic-scanner.log`
- Main application logs
- Structured JSON format
- Includes all log levels (INFO, WARN, ERROR, DEBUG)

### 2. `logs/topic-scanner-debug.log`
- Detailed debug information
- Only DEBUG and TRACE level logs
- Includes method names and thread information

### 3. `logs/topic-scanner-metrics.log`
- Performance metrics only
- Timing information for operations
- Success/failure statistics

## Understanding the Logs

### Trace Structure
Each log entry includes:
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "DEBUG",
  "logger": "com.cncf.scanner.service.ScanningService",
  "message": "🔍 Starting scan for source: Reddit (ID: 1)",
  "traceId": "SOURCE_SCAN-Reddit-12345",
  "operation": "SOURCE_SCAN",
  "source": "Reddit",
  "step": "SCAN_HISTORY_CREATED"
}
```

### Key Log Messages

#### Scan Process Flow
- `🚀 Starting topic scanning process...` - Process begins
- `🔍 Starting scan for source: {source}` - Individual source scan starts
- `📋 Found {count} search topics to process` - Topics discovered
- `✅ Found {count} results for search topic: {topic}` - Results found
- `🎯 {source} scan completed: {topics} topics processed, {results} results found in {time}ms` - Scan summary

#### Algorithm Flow
- `🔄 Algorithm Flow: Source={source} Step={step} Input={input} Output={output}` - Algorithm decisions
- `📥 Data Collection: Source={source} Topic={topic} Items={count} - {details}` - Data collection
- `🌐 HTTP Request: {method} {url} - Headers: {headers} - Body: {body}` - HTTP requests
- `📡 HTTP Response: {url} - Status: {status} - Time: {time}ms - Body: {body}` - HTTP responses

#### Performance Metrics
- `📊 Metrics: {key}={value} {key}={value} ...` - Performance data
- `✅ Completed trace: {operation} for source: {source} in {time}ms - {result}` - Operation timing

#### Error Handling
- `❌ Error in {operation} for source {source}: {error} - {exception}` - Error details
- `💥 {operation} failed after {time}ms: {error}` - Failure summary

## Monitoring Algorithm Behavior

### 1. Source Browsing
Look for these log patterns to understand how the algorithm browses sources:
```
🔍 Scanning {count} search topics for {source} source: {name}
📋 Found {count} search topics to process
🔄 Algorithm Flow: Source={source} Step=GETTING_SEARCH_TOPICS Input={input} Output={output}
```

### 2. Data Collection
Monitor data collection with:
```
📥 Data Collection: Source={source} Topic={topic} Items={count} - {details}
✅ Found {count} results for search topic: {topic}
📋 Topic '{topic}' results: {results}
```

### 3. HTTP Requests
Track external API calls:
```
🌐 HTTP Request: GET https://api.reddit.com/r/{subreddit}/search.json - Headers: {headers}
📡 HTTP Response: https://api.reddit.com/r/{subreddit}/search.json - Status: 200 - Time: 150ms
```

### 4. Performance Analysis
Use metrics logs to analyze performance:
```
📊 Metrics: totalTopics=5 successfulTopics=4 failedTopics=1 totalResults=23 scanDurationMs=2500 avgResultsPerTopic=4
```

## Troubleshooting

### Common Issues

1. **No topics found**: Check if search topics are configured
   ```
   📭 No search topics configured for source: {source}
   ```

2. **HTTP errors**: Look for request/response logs
   ```
   📡 HTTP Response: {url} - Status: 429 - Time: {time}ms
   ```

3. **Rate limiting**: Check for rate limiting logs
   ```
   📋 Step: RATE_LIMITING - Sleeping for 1000ms between requests
   ```

### Debug Tips

1. **Filter logs by trace ID**: Use the traceId to follow a specific operation
2. **Monitor metrics file**: Check `topic-scanner-metrics.log` for performance issues
3. **Use structured queries**: Parse JSON logs with tools like `jq` for analysis
4. **Check debug file**: Use `topic-scanner-debug.log` for detailed method-level tracing

## Example Analysis Commands

### Count successful vs failed scans
```bash
grep "scan completed" logs/topic-scanner.log | jq -r '.message' | grep -c "Success"
grep "scan completed" logs/topic-scanner.log | jq -r '.message' | grep -c "Failed"
```

### Find slow operations
```bash
grep "📊 Metrics" logs/topic-scanner-metrics.log | jq -r '.message' | grep "scanDurationMs"
```

### Track a specific trace
```bash
grep "SOURCE_SCAN-Reddit-12345" logs/topic-scanner-debug.log
```

## Configuration Reference

| Environment Variable | Description | Default |
|---------------------|-------------|---------|
| `DEBUG_MODE` | Enable debug mode | `true` |
| `TRACE_REQUESTS` | Log HTTP requests/responses | `true` |
| `TRACE_PERFORMANCE` | Log performance metrics | `true` |
| `DETAILED_SCAN_LOGGING` | Log detailed scan results | `true` |
| `LOG_LEVEL` | Logging level | `DEBUG` |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `debug` |

