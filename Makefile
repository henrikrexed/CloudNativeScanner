# Cloud Native Topic Scanner - Makefile
# Configurable build, test, and deployment automation

# Default configuration
DOCKER_BINARY ?= docker
DOCKER_COMPOSE_BINARY ?= docker-compose
PLATFORM ?= linux/amd64
REGISTRY ?= localhost:5000
VERSION ?= latest
NAMESPACE ?= topic-scanner

# Maven configuration
MAVEN_OPTS ?= -Xmx1024m
MAVEN_BINARY ?= mvn

# Test configuration
TEST_PROFILE ?= test
COVERAGE_THRESHOLD ?= 80

# Docker build configuration
DOCKER_BUILDX ?= false
DOCKER_PUSH ?= false
DOCKER_CACHE ?= true

# Service names
SERVICES := topic-scanner topic-analyzer webui
SHARED_MODULE := shared

# Colors for output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
BLUE := \033[0;34m
NC := \033[0m # No Color

.PHONY: help
help: ## Show this help message
	@echo "$(BLUE)Cloud Native Topic Scanner - Build System$(NC)"
	@echo ""
	@echo "$(YELLOW)Available targets:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(YELLOW)Configuration:$(NC)"
	@echo "  DOCKER_BINARY=$(DOCKER_BINARY)"
	@echo "  PLATFORM=$(PLATFORM)"
	@echo "  REGISTRY=$(REGISTRY)"
	@echo "  VERSION=$(VERSION)"
	@echo "  NAMESPACE=$(NAMESPACE)"
	@echo "  MAVEN_BINARY=$(MAVEN_BINARY)"
	@echo "  TEST_PROFILE=$(TEST_PROFILE)"
	@echo "  COVERAGE_THRESHOLD=$(COVERAGE_THRESHOLD)"

.PHONY: clean
clean: ## Clean all build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	$(MAVEN_BINARY) clean
	$(DOCKER_BINARY) system prune -f
	@echo "$(GREEN)Clean completed$(NC)"

.PHONY: compile
compile: ## Compile all modules
	@echo "$(BLUE)Compiling all modules...$(NC)"
	$(MAVEN_BINARY) compile -q
	@echo "$(GREEN)Compilation completed$(NC)"

.PHONY: test
test: ## Run all unit tests
	@echo "$(BLUE)Running unit tests...$(NC)"
	$(MAVEN_BINARY) test -P$(TEST_PROFILE)
	@echo "$(GREEN)Tests completed$(NC)"

.PHONY: test-coverage
test-coverage: ## Run tests with coverage report
	@echo "$(BLUE)Running tests with coverage...$(NC)"
	$(MAVEN_BINARY) test jacoco:report -P$(TEST_PROFILE)
	@echo "$(GREEN)Coverage report generated in target/site/jacoco/index.html$(NC)"

.PHONY: test-coverage-check
test-coverage-check: test-coverage ## Check if coverage meets threshold
	@echo "$(BLUE)Checking coverage threshold ($(COVERAGE_THRESHOLD)%)...$(NC)"
	@coverage=$$($(MAVEN_BINARY) jacoco:check -P$(TEST_PROFILE) -q | grep -o '[0-9]*\.[0-9]*%' | head -1 | sed 's/%//'); \
	if [ -z "$$coverage" ]; then \
		echo "$(RED)Could not determine coverage percentage$(NC)"; \
		exit 1; \
	fi; \
	if [ $$(echo "$$coverage < $(COVERAGE_THRESHOLD)" | bc -l) -eq 1 ]; then \
		echo "$(RED)Coverage $$coverage% is below threshold $(COVERAGE_THRESHOLD)%$(NC)"; \
		exit 1; \
	else \
		echo "$(GREEN)Coverage $$coverage% meets threshold $(COVERAGE_THRESHOLD)%$(NC)"; \
	fi

.PHONY: package
package: test ## Package all modules
	@echo "$(BLUE)Packaging all modules...$(NC)"
	$(MAVEN_BINARY) package -DskipTests
	@echo "$(GREEN)Packaging completed$(NC)"

.PHONY: install
install: test ## Install all modules to local repository
	@echo "$(BLUE)Installing modules to local repository...$(NC)"
	$(MAVEN_BINARY) install -DskipTests
	@echo "$(GREEN)Installation completed$(NC)"

.PHONY: docker-build
docker-build: package ## Build Docker images for all services
	@echo "$(BLUE)Building Docker images...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Building image for $$service...$(NC)"; \
		$(DOCKER_BINARY) build \
			--platform $(PLATFORM) \
			--build-arg VERSION=$(VERSION) \
			$(if $(filter true,$(DOCKER_CACHE)),,--no-cache) \
			-t $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION) \
			-t $(REGISTRY)/$(NAMESPACE)/$$service:latest \
			./$$service/; \
	done
	@echo "$(GREEN)Docker images built successfully$(NC)"

.PHONY: docker-buildx
docker-buildx: package ## Build multi-platform Docker images using buildx
	@echo "$(BLUE)Building multi-platform Docker images...$(NC)"
	@if [ "$(DOCKER_BUILDX)" = "true" ]; then \
		for service in $(SERVICES); do \
			echo "$(YELLOW)Building multi-platform image for $$service...$(NC)"; \
			$(DOCKER_BINARY) buildx build \
				--platform $(PLATFORM) \
				--build-arg VERSION=$(VERSION) \
				$(if $(filter true,$(DOCKER_CACHE)),,--no-cache) \
				$(if $(filter true,$(DOCKER_PUSH)),--push,) \
				-t $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION) \
				-t $(REGISTRY)/$(NAMESPACE)/$$service:latest \
				./$$service/; \
		done; \
	else \
		echo "$(RED)DOCKER_BUILDX is not enabled. Set DOCKER_BUILDX=true to use buildx$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)Multi-platform Docker images built successfully$(NC)"

.PHONY: docker-push
docker-push: docker-build ## Push Docker images to registry
	@echo "$(BLUE)Pushing Docker images to registry...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Pushing image for $$service...$(NC)"; \
		$(DOCKER_BINARY) push $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION); \
		$(DOCKER_BINARY) push $(REGISTRY)/$(NAMESPACE)/$$service:latest; \
	done
	@echo "$(GREEN)Docker images pushed successfully$(NC)"

.PHONY: docker-pull
docker-pull: ## Pull Docker images from registry
	@echo "$(BLUE)Pulling Docker images from registry...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Pulling image for $$service...$(NC)"; \
		$(DOCKER_BINARY) pull $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION); \
	done
	@echo "$(GREEN)Docker images pulled successfully$(NC)"

.PHONY: docker-tag
docker-tag: ## Tag Docker images with new version
	@echo "$(BLUE)Tagging Docker images...$(NC)"
	@read -p "Enter new version tag: " new_version; \
	for service in $(SERVICES); do \
		echo "$(YELLOW)Tagging $$service with version $$new_version...$(NC)"; \
		$(DOCKER_BINARY) tag $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION) $(REGISTRY)/$(NAMESPACE)/$$service:$$new_version; \
	done
	@echo "$(GREEN)Docker images tagged successfully$(NC)"

.PHONY: docker-rmi
docker-rmi: ## Remove Docker images
	@echo "$(BLUE)Removing Docker images...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Removing image for $$service...$(NC)"; \
		$(DOCKER_BINARY) rmi $(REGISTRY)/$(NAMESPACE)/$$service:$(VERSION) || true; \
		$(DOCKER_BINARY) rmi $(REGISTRY)/$(NAMESPACE)/$$service:latest || true; \
	done
	@echo "$(GREEN)Docker images removed successfully$(NC)"

.PHONY: k8s-deploy
k8s-deploy: ## Deploy to Kubernetes
	@echo "$(BLUE)Deploying to Kubernetes...$(NC)"
	kubectl apply -f k8s/
	@echo "$(GREEN)Kubernetes deployment completed$(NC)"

.PHONY: k8s-delete
k8s-delete: ## Delete Kubernetes resources
	@echo "$(BLUE)Deleting Kubernetes resources...$(NC)"
	kubectl delete -f k8s/ --ignore-not-found=true
	@echo "$(GREEN)Kubernetes resources deleted$(NC)"

.PHONY: k8s-status
k8s-status: ## Check Kubernetes deployment status
	@echo "$(BLUE)Checking Kubernetes deployment status...$(NC)"
	kubectl get pods -n $(NAMESPACE)
	kubectl get services -n $(NAMESPACE)
	kubectl get cronjobs -n $(NAMESPACE)

.PHONY: k8s-logs
k8s-logs: ## Show logs from Kubernetes pods
	@echo "$(BLUE)Showing Kubernetes logs...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Logs for $$service:$(NC)"; \
		kubectl logs -n $(NAMESPACE) -l app=$$service --tail=50; \
		echo ""; \
	done

.PHONY: lint
lint: ## Run code quality checks
	@echo "$(BLUE)Running code quality checks...$(NC)"
	$(MAVEN_BINARY) checkstyle:check
	$(MAVEN_BINARY) spotbugs:check
	@echo "$(GREEN)Code quality checks completed$(NC)"

.PHONY: format
format: ## Format code
	@echo "$(BLUE)Formatting code...$(NC)"
	$(MAVEN_BINARY) spotless:apply
	@echo "$(GREEN)Code formatting completed$(NC)"

.PHONY: security-scan
security-scan: ## Run security vulnerability scan
	@echo "$(BLUE)Running security scan...$(NC)"
	$(MAVEN_BINARY) org.owasp:dependency-check-maven:check
	@echo "$(GREEN)Security scan completed$(NC)"

.PHONY: dependency-update
dependency-update: ## Update dependencies
	@echo "$(BLUE)Updating dependencies...$(NC)"
	$(MAVEN_BINARY) versions:display-dependency-updates
	$(MAVEN_BINARY) versions:display-plugin-updates
	@echo "$(GREEN)Dependency update check completed$(NC)"

.PHONY: dependency-tree
dependency-tree: ## Show dependency tree
	@echo "$(BLUE)Showing dependency tree...$(NC)"
	$(MAVEN_BINARY) dependency:tree

.PHONY: generate-dockerfiles
generate-dockerfiles: ## Generate Dockerfiles for all services
	@echo "$(BLUE)Generating Dockerfiles...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(YELLOW)Generating Dockerfile for $$service...$(NC)"; \
		./scripts/generate-dockerfile.sh $$service $(PLATFORM) $(VERSION); \
	done
	@echo "$(GREEN)Dockerfiles generated successfully$(NC)"

.PHONY: validate
validate: lint test-coverage-check security-scan ## Run all validation checks
	@echo "$(GREEN)All validation checks passed$(NC)"

.PHONY: build
build: clean compile test package ## Full build pipeline
	@echo "$(GREEN)Full build pipeline completed$(NC)"

.PHONY: ci
ci: validate build docker-build ## CI pipeline
	@echo "$(GREEN)CI pipeline completed$(NC)"

.PHONY: dev
dev: ## Start development environment
	@echo "$(BLUE)Starting development environment...$(NC)"
	$(DOCKER_COMPOSE_BINARY) -f docker-compose.dev.yml up -d
	@echo "$(GREEN)Development environment started$(NC)"

.PHONY: dev-stop
dev-stop: ## Stop development environment
	@echo "$(BLUE)Stopping development environment...$(NC)"
	$(DOCKER_COMPOSE_BINARY) -f docker-compose.dev.yml down
	@echo "$(GREEN)Development environment stopped$(NC)"

.PHONY: dev-logs
dev-logs: ## Show development environment logs
	@echo "$(BLUE)Showing development environment logs...$(NC)"
	$(DOCKER_COMPOSE_BINARY) -f docker-compose.dev.yml logs -f

.PHONY: setup
setup: ## Initial setup
	@echo "$(BLUE)Setting up development environment...$(NC)"
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(YELLOW)Created .env file from .env.example$(NC)"; \
	fi
	@if [ ! -d .git ]; then \
		git init; \
		echo "$(YELLOW)Initialized git repository$(NC)"; \
	fi
	@echo "$(GREEN)Setup completed$(NC)"

.PHONY: info
info: ## Show build information
	@echo "$(BLUE)Build Information:$(NC)"
	@echo "  Docker Binary: $(DOCKER_BINARY)"
	@echo "  Platform: $(PLATFORM)"
	@echo "  Registry: $(REGISTRY)"
	@echo "  Version: $(VERSION)"
	@echo "  Namespace: $(NAMESPACE)"
	@echo "  Maven Binary: $(MAVEN_BINARY)"
	@echo "  Services: $(SERVICES)"
	@echo "  Shared Module: $(SHARED_MODULE)"

# Service-specific targets
.PHONY: test-topic-scanner
test-topic-scanner: ## Run tests for topic-scanner service
	@echo "$(BLUE)Running tests for topic-scanner...$(NC)"
	$(MAVEN_BINARY) test -pl topic-scanner -P$(TEST_PROFILE)

.PHONY: test-topic-analyzer
test-topic-analyzer: ## Run tests for topic-analyzer service
	@echo "$(BLUE)Running tests for topic-analyzer...$(NC)"
	$(MAVEN_BINARY) test -pl topic-analyzer -P$(TEST_PROFILE)

.PHONY: test-webui
test-webui: ## Run tests for webui service
	@echo "$(BLUE)Running tests for webui...$(NC)"
	$(MAVEN_BINARY) test -pl webui -P$(TEST_PROFILE)

.PHONY: test-shared
test-shared: ## Run tests for shared module
	@echo "$(BLUE)Running tests for shared module...$(NC)"
	$(MAVEN_BINARY) test -pl shared -P$(TEST_PROFILE)

# Docker service-specific targets
.PHONY: docker-build-topic-scanner
docker-build-topic-scanner: package ## Build Docker image for topic-scanner
	@echo "$(BLUE)Building Docker image for topic-scanner...$(NC)"
	$(DOCKER_BINARY) build \
		--platform $(PLATFORM) \
		--build-arg VERSION=$(VERSION) \
		$(if $(filter true,$(DOCKER_CACHE)),,--no-cache) \
		-t $(REGISTRY)/$(NAMESPACE)/topic-scanner:$(VERSION) \
		-t $(REGISTRY)/$(NAMESPACE)/topic-scanner:latest \
		./topic-scanner/

.PHONY: docker-build-topic-analyzer
docker-build-topic-analyzer: package ## Build Docker image for topic-analyzer
	@echo "$(BLUE)Building Docker image for topic-analyzer...$(NC)"
	$(DOCKER_BINARY) build \
		--platform $(PLATFORM) \
		--build-arg VERSION=$(VERSION) \
		$(if $(filter true,$(DOCKER_CACHE)),,--no-cache) \
		-t $(REGISTRY)/$(NAMESPACE)/topic-analyzer:$(VERSION) \
		-t $(REGISTRY)/$(NAMESPACE)/topic-analyzer:latest \
		./topic-analyzer/

.PHONY: docker-build-webui
docker-build-webui: package ## Build Docker image for webui
	@echo "$(BLUE)Building Docker image for webui...$(NC)"
	$(DOCKER_BINARY) build \
		--platform $(PLATFORM) \
		--build-arg VERSION=$(VERSION) \
		$(if $(filter true,$(DOCKER_CACHE)),,--no-cache) \
		-t $(REGISTRY)/$(NAMESPACE)/webui:$(VERSION) \
		-t $(REGISTRY)/$(NAMESPACE)/webui:latest \
		./webui/

# Default target
.DEFAULT_GOAL := help


