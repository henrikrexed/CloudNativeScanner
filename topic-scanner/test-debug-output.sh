#!/bin/bash

# Test script to verify debug logging is working
echo "🧪 Testing Debug Logging Output"
echo "==============================="
echo ""

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "🔧 Environment Variables:"
echo "   DEBUG_MODE: ${DEBUG_MODE:-not set}"
echo "   TRACE_REQUESTS: ${TRACE_REQUESTS:-not set}"
echo "   TRACE_PERFORMANCE: ${TRACE_PERFORMANCE:-not set}"
echo "   DETAILED_SCAN_LOGGING: ${DETAILED_SCAN_LOGGING:-not set}"
echo "   LOG_LEVEL: ${LOG_LEVEL:-not set}"
echo "   SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-not set}"
echo ""

echo "🚀 Running topic scanner with H2 database and debug logging..."
echo "   This should show:"
echo "   - Database initialization logs"
echo "   - Spring Boot startup logs"
echo "   - Debug configuration logs"
echo "   - Source and search topic discovery"
echo "   - HTTP requests to external APIs"
echo ""

# Set debug environment variables
export DEBUG_MODE=true
export TRACE_REQUESTS=true
export TRACE_PERFORMANCE=true
export DETAILED_SCAN_LOGGING=true
export LOG_LEVEL=DEBUG
export SPRING_PROFILES_ACTIVE=h2

# Run with H2 configuration and capture output
echo "📋 Starting application..."
timeout 30s mvn spring-boot:run -Dspring-boot.run.profiles=h2 2>&1 | head -50

echo ""
echo "✅ Test completed!"
echo ""
echo "💡 If you didn't see debug logs above, the issue might be:"
echo "   1. Application is failing to start due to missing dependencies"
echo "   2. Logging configuration is not being applied correctly"
echo "   3. Application is running but not finding any sources to scan"
echo ""
echo "🔍 To troubleshoot further:"
echo "   1. Check if PostgreSQL is running: brew services list | grep postgres"
echo "   2. Try running with PostgreSQL: ./run-debug.sh"
echo "   3. Check application logs: tail -f logs/topic-scanner.log"
echo "   4. Verify database setup: psql -h localhost -U scanner -d cloud_native_scanner -c 'SELECT * FROM sources;'"

