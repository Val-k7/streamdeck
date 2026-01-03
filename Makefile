# Control Deck - Development Makefile

.PHONY: help install install-dev test test-cov lint format clean run docker-build docker-run

# Default target
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Control Deck - Development Commands"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

## Setup & Installation

install: ## Install production dependencies
	cd server/backend && pip install -r requirements.txt
	cd server/frontend && npm install

install-dev: ## Install development dependencies
	cd server/backend && pip install -e ".[dev]"
	cd server/frontend && npm install
	pre-commit install

setup-precommit: ## Setup pre-commit hooks
	pip install pre-commit
	pre-commit install
	@echo "✅ Pre-commit hooks installed"

## Testing

test: ## Run all tests
	cd server/backend && pytest

test-cov: ## Run tests with coverage report
	cd server/backend && pytest --cov=app --cov-report=html --cov-report=term-missing
	@echo "📊 Coverage report: server/backend/htmlcov/index.html"

test-watch: ## Run tests in watch mode
	cd server/backend && pytest-watch

test-security: ## Run security tests only
	cd server/backend && pytest -m security -v

test-unit: ## Run unit tests only
	cd server/backend && pytest -m unit -v

## Code Quality

lint: ## Run all linters
	@echo "🔍 Running Python linters..."
	cd server/backend && pylint app/
	cd server/backend && mypy app/
	cd server/backend && bandit -r app/
	@echo "🔍 Running TypeScript linters..."
	cd server/frontend && npm run lint

format: ## Format code with Black and Prettier
	@echo "✨ Formatting Python code..."
	cd server/backend && black app/ tests/
	cd server/backend && isort app/ tests/
	@echo "✨ Formatting TypeScript code..."
	cd server/frontend && npm run format

format-check: ## Check code formatting without making changes
	cd server/backend && black --check app/ tests/
	cd server/backend && isort --check app/ tests/

pre-commit-run: ## Run pre-commit hooks on all files
	pre-commit run --all-files

## Development

run-backend: ## Run backend development server
	cd server/backend && python -m uvicorn app.main:app --reload --host 0.0.0.0 --port 4455

run-frontend: ## Run frontend development server
	cd server/frontend && npm run dev

run: ## Run both backend and frontend (requires tmux or separate terminals)
	@echo "⚠️  Run 'make run-backend' and 'make run-frontend' in separate terminals"

dev: install-dev ## Setup development environment
	@echo "✅ Development environment ready!"
	@echo "📝 Run 'make run-backend' and 'make run-frontend' to start"

## Docker

docker-build: ## Build Docker image
	docker build -t control-deck:latest .

docker-run: ## Run Docker container
	docker-compose up -d

docker-stop: ## Stop Docker container
	docker-compose down

docker-logs: ## View Docker logs
	docker-compose logs -f

## Cleanup

clean: ## Clean build artifacts and cache
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".mypy_cache" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "htmlcov" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name ".coverage" -delete 2>/dev/null || true
	find . -type f -name "*.pyc" -delete 2>/dev/null || true
	cd server/frontend && rm -rf node_modules/.cache 2>/dev/null || true
	@echo "🧹 Cleaned build artifacts"

clean-all: clean ## Clean everything including dependencies
	cd server/frontend && rm -rf node_modules 2>/dev/null || true
	@echo "🧹 Cleaned all artifacts and dependencies"

## Documentation

docs: ## Generate API documentation
	@echo "📚 Documentation generation not yet implemented"

## Release

check: test-cov lint ## Run all checks before commit
	@echo "✅ All checks passed!"

release-patch: check ## Create a patch release
	@echo "🚀 Creating patch release..."
	@# TODO: Implement versioning

release-minor: check ## Create a minor release
	@echo "🚀 Creating minor release..."
	@# TODO: Implement versioning

## CI/CD

ci: install-dev test-cov lint ## Run CI checks locally
	@echo "✅ CI checks complete"
