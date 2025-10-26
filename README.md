# Cloud Native Topic Scanner

A comprehensive solution for scanning internet sources (StackOverflow, Reddit, etc.) to discover and classify cloud-native topics, built as microservices for Kubernetes deployment.

## Architecture

The solution consists of four main components:

1. **Topic Scanner (CronJob)** - Scans configured sources daily and sends topics to Kafka
2. **Kafka** - Message broker for asynchronous processing
3. **Topic Analyzer** - Processes topics from Kafka and classifies them by themes
4. **Web UI** - Displays topics organized by themes with discussion links

## Components

### Topic Scanner Service
- **Purpose**: Scans internet sources for new topics
- **Deployment**: Kubernetes CronJob (runs daily at 2 AM)
- **Sources**: StackOverflow, Reddit (extensible to other sources)
- **Output**: Sends topic data to Kafka

### Topic Analyzer Service
- **Purpose**: Processes topics from Kafka and classifies them by themes
- **Deployment**: Kubernetes Deployment (2 replicas)
- **Features**: AI-powered content classification, duplicate detection
- **Storage**: Saves classified topics to PostgreSQL

### Web UI Service
- **Purpose**: Provides web interface for browsing topics by themes and system administration
- **Deployment**: Kubernetes Deployment (2 replicas)
- **Features**: Theme-based browsing, topic details, external links, admin configuration panel

### Infrastructure
- **PostgreSQL**: Database for storing topics, themes, and metadata
- **Kafka**: Message broker for asynchronous processing
- **Zookeeper**: Kafka coordination service

## Features

- **Configurable Sources**: Easy to add new sources (StackOverflow, Reddit, etc.)
- **AI-Powered Classification**: Advanced AI automatically classifies topics into themes
- **Semantic Understanding**: AI analyzes topic content for better understanding
- **Intelligent Duplicate Detection**: AI-powered semantic similarity detection
- **Content Summarization**: AI generates summaries and extracts key insights
- **Relevance Scoring**: AI determines topic relevance and quality
- **Real-time Processing**: Asynchronous processing via Kafka
- **Web Interface**: User-friendly interface for browsing topics
- **Admin Panel**: Complete system administration and configuration interface
- **AI Configuration**: Manage AI models, thresholds, and analysis parameters
- **System Monitoring**: Real-time monitoring of scans, performance, and system health
- **Kubernetes Native**: Designed for cloud-native deployment
- **Scalable**: Horizontal scaling of processing components

## Quick Start

### Prerequisites
- Kubernetes cluster
- kubectl configured
- Docker (for building images)
- OpenAI API key (for AI features)

### Build and Deploy

1. **Build all services**:
   ```bash
   ./build.sh
   ```

2. **Deploy to Kubernetes**:
   ```bash
   ./deploy.sh
   ```

3. **Access the Web UI**:
   ```bash
   kubectl port-forward -n topic-scanner service/webui 8080:80
   ```
   Then open http://localhost:8080 in your browser

4. **Access the Admin Panel**:
   Navigate to http://localhost:8080/admin to access the administration interface

5. **Configure AI Settings**:
   - Set your OpenAI API key in the Kubernetes secret
   - Configure AI parameters in the admin panel at http://localhost:8080/admin/ai-config

### Manual Deployment

If you prefer to deploy manually:

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Deploy infrastructure
kubectl apply -f k8s/postgresql.yaml
kubectl apply -f k8s/kafka.yaml

# Wait for infrastructure to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n topic-scanner --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka -n topic-scanner --timeout=300s

# Deploy services
kubectl apply -f k8s/topic-analyzer.yaml
kubectl apply -f k8s/webui.yaml
kubectl apply -f k8s/topic-scanner-cronjob.yaml
```

## Admin Panel

The Web UI includes a comprehensive admin panel accessible at `/admin` with the following features:

### Source Management
- **Add/Edit/Delete Sources**: Configure new data sources (StackOverflow, Reddit, etc.)
- **Source Status Control**: Enable/disable sources for scanning
- **Manual Scanning**: Trigger immediate scans for specific sources
- **Scan Frequency**: Configure how often each source is scanned

### Search Topics Management
- **Configurable Search Topics**: Define specific topics to search for on each source
- **Keyword Management**: Set primary keywords and custom search queries
- **Priority Control**: Set search priority (High/Medium/Low) for different topics
- **Result Limits**: Configure maximum results per search topic
- **Search Frequency**: Set individual search frequency for each topic
- **Web Bot Capabilities**: Scanners act as intelligent web bots that search for specific topics

### Theme Management
- **Create/Edit Themes**: Define classification categories
- **Theme Organization**: Organize topics by custom themes
- **Default Themes**: Pre-configured themes for cloud-native topics

### System Configuration
- **Cron Schedule**: Configure when automatic scanning occurs
- **Kafka Settings**: Manage message broker configuration
- **Classification Settings**: Adjust AI classification parameters
- **System Limits**: Set maximum topics per scan, etc.

### System Monitoring
- **Real-time Dashboard**: Monitor system health and performance
- **Scan History**: View detailed scan logs and statistics
- **Success Rates**: Track scanning success and failure rates
- **Topic Statistics**: Monitor topics found and processed

### AI Configuration
- **AI Model Selection**: Choose between GPT-3.5, GPT-4, or GPT-4 Turbo
- **Similarity Thresholds**: Configure duplicate detection sensitivity
- **Confidence Thresholds**: Set minimum AI confidence scores
- **Feature Toggles**: Enable/disable specific AI features
- **Performance Monitoring**: Track AI analysis performance and accuracy

### Default Configuration
The system comes pre-configured with:
- **StackOverflow**: Scans cloud-native questions and answers
- **Reddit**: Monitors cloud-native subreddits
- **Default Search Topics**: Kubernetes, Docker, Microservices, DevOps, etc.
- **Default Themes**: Kubernetes, DevOps, Security, Monitoring, etc.
- **Daily Scanning**: Automatic scanning at 2 AM daily
- **Web Bot Scanners**: Intelligent web bots that search for specific topics

## Build System

The project includes a comprehensive Makefile for building, testing, and deploying the application.

### Available Make Targets

#### Build and Test
```bash
make build          # Full build pipeline (clean, compile, test, package)
make compile        # Compile all modules
make test           # Run all unit tests
make test-coverage  # Run tests with coverage report
make package        # Package all modules
```

#### Docker Operations
```bash
make docker-build   # Build Docker images for all services
make docker-push    # Push Docker images to registry
make docker-pull    # Pull Docker images from registry
make docker-rmi     # Remove Docker images
```

#### Kubernetes Operations
```bash
make k8s-deploy     # Deploy to Kubernetes
make k8s-delete     # Delete Kubernetes resources
make k8s-status     # Check deployment status
make k8s-logs       # Show logs from pods
```

#### Code Quality
```bash
make lint           # Run code quality checks
make format         # Format code
make security-scan  # Run security vulnerability scan
make validate       # Run all validation checks
```

#### Development
```bash
make dev            # Start development environment
make dev-stop       # Stop development environment
make dev-logs       # Show development logs
```

### Configuration

The build system is highly configurable through environment variables:

```bash
# Docker Configuration
DOCKER_BINARY=docker                    # Docker binary to use
PLATFORM=linux/amd64                   # Target platform (default: x86_64)
REGISTRY=localhost:5000                # Docker registry
VERSION=latest                         # Image version
NAMESPACE=topic-scanner                # Kubernetes namespace

# Maven Configuration
MAVEN_BINARY=mvn                       # Maven binary to use
MAVEN_OPTS=-Xmx1024m                   # Maven JVM options

# Test Configuration
TEST_PROFILE=test                      # Maven test profile
COVERAGE_THRESHOLD=80                  # Minimum coverage percentage

# Build Configuration
DOCKER_BUILDX=false                    # Use Docker Buildx for multi-platform
DOCKER_PUSH=false                      # Push images after building
DOCKER_CACHE=true                      # Use Docker build cache
```

### Platform Support

The build system supports multiple platforms:
- `linux/amd64` (default)
- `linux/arm64`
- `linux/arm/v7`
- `linux/arm/v6`

### Docker Binary Configuration

You can use different Docker binaries:
- `docker` (default)
- `podman`
- `nerdctl`
- Custom Docker-compatible binary

Example:
```bash
DOCKER_BINARY=podman make docker-build
```

### Testing

The project includes comprehensive unit tests for all components:

#### Test Structure
- **Shared Module Tests**: Test shared services and models
- **Topic Scanner Tests**: Test web bot scanners and search functionality
- **Topic Analyzer Tests**: Test AI analysis and classification
- **Web UI Tests**: Test controllers and admin functionality

#### Test Coverage
- Minimum coverage threshold: 80%
- JaCoCo integration for coverage reporting
- TestContainers for integration testing
- Mockito for unit testing

#### Running Tests
```bash
make test                    # Run all tests
make test-coverage          # Run with coverage
make test-coverage-check    # Check coverage threshold
make test-topic-scanner     # Test specific service
make test-topic-analyzer    # Test specific service
make test-webui            # Test specific service
make test-shared           # Test shared module
```

### Code Quality

The project enforces high code quality standards:

#### Quality Tools
- **Checkstyle**: Code style enforcement
- **SpotBugs**: Static analysis for bug detection
- **Spotless**: Code formatting
- **OWASP Dependency Check**: Security vulnerability scanning

#### Quality Checks
```bash
make lint           # Run all quality checks
make format         # Format code
make security-scan  # Security vulnerability scan
make validate       # Run all validation checks
```

### Development Environment

The project includes a complete development environment:

#### Development Services
- PostgreSQL database
- Kafka message broker
- Zookeeper coordination
- Kafka UI for message monitoring
- pgAdmin for database management

#### Starting Development Environment
```bash
make dev            # Start all development services
make dev-logs       # View logs
make dev-stop       # Stop services
```

#### Development URLs
- Web UI: http://localhost:8082
- Admin Panel: http://localhost:8082/admin
- Kafka UI: http://localhost:8083
- pgAdmin: http://localhost:8084

## Configuration

### Environment Variables

Each service can be configured via environment variables:

#### Topic Scanner
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `KAFKA_TOPIC_NAME`: Kafka topic name for topics

#### Topic Analyzer
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers
- `SPRING_KAFKA_CONSUMER_GROUP_ID`: Kafka consumer group ID

#### Web UI
- `SPRING_DATASOURCE_URL`: Database connection URL

### Adding New Sources

To add a new source:

1. Implement the `SourceScanner` interface
2. Add the scanner as a Spring component
3. Configure the source in the database
4. The system will automatically pick up the new scanner

Example:
```java
@Component
public class GitHubScanner implements SourceScanner {
    @Override
    public String getSourceType() {
        return "GitHub";
    }
    
    @Override
    public boolean canHandle(Source source) {
        return "GitHub".equalsIgnoreCase(source.getName());
    }
    
    @Override
    public List<ScanResult> scan(Source source, LocalDateTime lastScanTime) {
        // Implementation
    }
}
```

## Database Schema

The system uses the following main entities:

- **Sources**: Configuration for external sources to scan
- **Topics**: Individual topics/discussions found
- **Themes**: Classification categories for topics
- **TopicThemes**: Many-to-many relationship between topics and themes
- **ScanHistory**: Tracking of scanning activities

## Monitoring

### Health Checks
All services include health check endpoints:
- `/actuator/health` - Liveness probe
- `/actuator/health/readiness` - Readiness probe

### Logs
View logs for any service:
```bash
kubectl logs -n topic-scanner deployment/topic-analyzer
kubectl logs -n topic-scanner deployment/webui
```

### CronJob Status
Check CronJob execution:
```bash
kubectl get cronjobs -n topic-scanner
kubectl get jobs -n topic-scanner
```

## Scaling

### Horizontal Scaling
- **Topic Analyzer**: Increase replicas in deployment
- **Web UI**: Increase replicas in deployment
- **Kafka**: Add more partitions and consumers

### Vertical Scaling
Adjust resource requests/limits in the Kubernetes manifests.

## Troubleshooting

### Common Issues

1. **Database Connection Issues**
   - Check PostgreSQL pod status: `kubectl get pods -n topic-scanner -l app=postgres`
   - Verify database credentials in secrets

2. **Kafka Connection Issues**
   - Check Kafka pod status: `kubectl get pods -n topic-scanner -l app=kafka`
   - Verify Kafka configuration

3. **CronJob Not Running**
   - Check CronJob status: `kubectl get cronjobs -n topic-scanner`
   - Check job history: `kubectl get jobs -n topic-scanner`

4. **Web UI Not Accessible**
   - Check service status: `kubectl get svc -n topic-scanner`
   - Verify port forwarding: `kubectl port-forward -n topic-scanner service/webui 8080:80`

### Debug Commands

```bash
# Check all pods
kubectl get pods -n topic-scanner

# Check services
kubectl get svc -n topic-scanner

# Check configmaps and secrets
kubectl get configmaps -n topic-scanner
kubectl get secrets -n topic-scanner

# Describe a pod for detailed information
kubectl describe pod -n topic-scanner <pod-name>
```

## Development

### Local Development

1. **Start infrastructure**:
   ```bash
   docker-compose up -d postgres kafka
   ```

2. **Run services locally**:
   ```bash
   cd topic-scanner && mvn spring-boot:run
   cd topic-analyzer && mvn spring-boot:run
   cd webui && mvn spring-boot:run
   ```

### Testing

Run tests for each service:
```bash
cd topic-scanner && mvn test
cd topic-analyzer && mvn test
cd webui && mvn test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the logs for error details
