#!/bin/bash

# Topic Scanner Debug Mode Runner
# This script runs the topic-scanner with debug logging enabled

echo "🔧 Starting Topic Scanner in DEBUG mode..."
echo "📋 Debug features enabled:"
echo "   - Detailed scan logging"
echo "   - Request tracing"
echo "   - Performance monitoring"
echo "   - Algorithm flow tracing"
echo ""

# Set debug environment variables
export DEBUG_MODE=true
export TRACE_REQUESTS=true
export TRACE_PERFORMANCE=true
export DETAILED_SCAN_LOGGING=true
export LOG_LEVEL=DEBUG

# Set Spring profile to debug
export SPRING_PROFILES_ACTIVE=debug

# Create logs directory if it doesn't exist
mkdir -p logs

echo "🚀 Running topic-scanner with debug configuration..."
echo "📁 Logs will be written to:"
echo "   - Console: Structured JSON logs"
echo "   - logs/topic-scanner.log: Main application logs"
echo "   - logs/topic-scanner-debug.log: Detailed debug logs"
echo "   - logs/topic-scanner-metrics.log: Performance metrics"
echo ""

# Run the application
java -jar target/topic-scanner-1.0.0.jar

echo ""
echo "✅ Topic Scanner execution completed"
echo "📊 Check the log files for detailed trace information"

