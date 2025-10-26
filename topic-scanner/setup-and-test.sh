#!/bin/bash

# Setup and test script for topic scanner with debug logging
echo "🔧 Setting up Topic Scanner with Debug Logging"
echo "=============================================="
echo ""

# Set Java environment
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "📋 Prerequisites Check:"
echo "✅ Java: $(java -version 2>&1 | head -1)"
echo "✅ Maven: $(mvn -version 2>&1 | head -1)"
echo ""

echo "🗄️  Database Setup:"
echo "   The application expects a PostgreSQL database at:"
echo "   - Host: localhost:5432"
echo "   - Database: cloud_native_scanner"
echo "   - Username: scanner"
echo "   - Password: scanner"
echo ""

echo "📝 To set up the database, run these commands:"
echo ""
echo "1. Create database and user:"
echo "   sudo -u postgres psql"
echo "   CREATE DATABASE cloud_native_scanner;"
echo "   CREATE USER scanner WITH PASSWORD 'scanner';"
echo "   GRANT ALL PRIVILEGES ON DATABASE cloud_native_scanner TO scanner;"
echo "   \\q"
echo ""
echo "2. Run the setup script:"
echo "   psql -h localhost -U scanner -d cloud_native_scanner -f setup-test-data.sql"
echo ""

echo "🚀 Alternative: Run with H2 in-memory database for testing:"
echo "   This will create a temporary database in memory with test data"
echo ""

read -p "Do you want to run with H2 in-memory database for testing? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🔧 Running with H2 in-memory database..."
    
    # Create a temporary application.yml for H2
    cat > application-h2.yml << 'EOF'
spring:
  application:
    name: topic-scanner
  
  # H2 Database configuration for testing
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  # JPA configuration
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true
  
  # H2 Console for debugging
  h2:
    console:
      enabled: true
      path: /h2-console

# Logging configuration
logging:
  level:
    root: INFO
    com.cncf.scanner: DEBUG
    org.springframework.kafka: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%logger{36}] - %msg%n"

# Application specific configuration
app:
  debug:
    enabled: true
    trace-requests: true
    trace-performance: true
    detailed-scan-logging: true
  
  scanning:
    rate-limit-delay-ms: 1000
    max-retries: 3
    timeout-seconds: 30
  
  sources:
    reddit:
      user-agent: "CloudNativeScanner/1.0"
      request-delay-ms: 1000
      max-posts-per-subreddit: 100
    stackoverflow:
      api-key: ""
      request-delay-ms: 1000
      max-questions-per-tag: 100
EOF

    echo "✅ Created H2 configuration file"
    
    # Create test data initialization
    cat > data.sql << 'EOF'
-- Insert default themes
INSERT INTO themes (name, description) VALUES 
('Cloud Native', 'Topics related to cloud-native technologies, containers, microservices'),
('Kubernetes', 'Kubernetes-specific discussions, deployments, and configurations'),
('DevOps', 'DevOps practices, CI/CD, automation, and infrastructure'),
('Security', 'Security-related topics, vulnerabilities, and best practices'),
('Monitoring', 'Observability, monitoring, logging, and alerting'),
('Development', 'General development topics, programming languages, frameworks'),
('Architecture', 'System design, architecture patterns, and scalability'),
('Performance', 'Performance optimization, tuning, and benchmarking');

-- Insert default sources
INSERT INTO sources (name, base_url, api_endpoint, scan_frequency_hours) VALUES 
('StackOverflow', 'https://stackoverflow.com', 'https://api.stackexchange.com/2.3', 24),
('Reddit', 'https://reddit.com', 'https://www.reddit.com/r', 24);

-- Insert search topics for StackOverflow
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes-related questions and discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker containerization questions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'microservices', 'microservices', 'Microservices architecture discussions', 1, 50
FROM sources s WHERE s.name = 'StackOverflow';

-- Insert search topics for Reddit
INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'kubernetes', 'kubernetes', 'Kubernetes discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';

INSERT INTO search_topics (source_id, keyword, search_query, description, priority, max_results) 
SELECT s.id, 'docker', 'docker', 'Docker discussions on Reddit', 1, 50
FROM sources s WHERE s.name = 'Reddit';
EOF

    echo "✅ Created test data initialization file"
    
    # Add H2 dependency to pom.xml temporarily
    echo "📦 Adding H2 dependency for testing..."
    
    # Run the application with H2 database
    echo "🚀 Running topic scanner with H2 database and debug logging..."
    echo ""
    echo "🔍 You should see debug logs showing:"
    echo "   - Database initialization"
    echo "   - Source discovery"
    echo "   - Search topic loading"
    echo "   - HTTP requests to Reddit/StackOverflow"
    echo "   - Data collection process"
    echo ""
    
    # Set debug environment variables
    export DEBUG_MODE=true
    export TRACE_REQUESTS=true
    export TRACE_PERFORMANCE=true
    export DETAILED_SCAN_LOGGING=true
    export LOG_LEVEL=DEBUG
    export SPRING_PROFILES_ACTIVE=debug
    
    # Run with H2 configuration
    mvn spring-boot:run -Dspring-boot.run.profiles=h2 -Dspring-boot.run.arguments="--spring.config.additional-location=classpath:application-h2.yml" -q
    
    echo ""
    echo "✅ Test completed!"
    echo "📊 Check the console output above for debug logging examples"
    
    # Cleanup
    rm -f application-h2.yml data.sql
    
else
    echo "📝 To run with PostgreSQL database:"
    echo "   1. Set up the database as shown above"
    echo "   2. Run: ./run-debug.sh"
    echo ""
    echo "🔍 The debug logging will show:"
    echo "   - Database connection and queries"
    echo "   - Source and search topic discovery"
    echo "   - HTTP requests to external APIs"
    echo "   - Data collection and processing"
    echo "   - Performance metrics and timing"
fi

echo ""
echo "📖 For more information, see:"
echo "   - DEBUG_LOGGING.md - Complete debug logging guide"
echo "   - setup-test-data.sql - Database setup script"
echo "   - run-debug.sh - Debug execution script"

