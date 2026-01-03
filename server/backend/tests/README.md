# Control Deck Backend Tests

Comprehensive test suite for the Control Deck backend application.

## Test Structure

```
tests/
├── __init__.py                   # Test package marker
├── conftest.py                   # Shared fixtures and configuration
├── test_profile_manager.py       # ProfileManager unit tests
├── test_actions.py               # Action handlers (keyboard, audio, system)
├── test_actions_scripts.py       # Script execution security tests
├── test_security.py              # Security tests (rate limiting, validation)
├── test_api.py                   # REST API endpoint tests (existing)
└── test_websocket.py             # WebSocket integration tests (existing)
```

## Running Tests

### Install Dependencies

```bash
cd server/backend

# Install dev dependencies
pip install -e ".[dev]"

# Or install from requirements-dev.txt
pip install -r requirements-dev.txt
```

### Run All Tests

```bash
# Run all tests with coverage
pytest

# Run with verbose output
pytest -v

# Run with coverage report
pytest --cov=app --cov-report=html
```

### Run Specific Test Files

```bash
# Run profile manager tests only
pytest tests/test_profile_manager.py

# Run security tests only
pytest tests/test_security.py -v

# Run script security tests
pytest tests/test_actions_scripts.py
```

### Run Tests by Marker

```bash
# Run only unit tests (fast)
pytest -m unit

# Run integration tests
pytest -m integration

# Run security-focused tests
pytest -m security

# Exclude slow tests
pytest -m "not slow"
```

### Run Specific Test Functions

```bash
# Run a specific test
pytest tests/test_profile_manager.py::TestProfileManager::test_save_profile_success

# Run all tests in a class
pytest tests/test_security.py::TestRateLimiter
```

## Coverage Reports

### Generate HTML Coverage Report

```bash
pytest --cov=app --cov-report=html

# Open report
# Windows:
start htmlcov/index.html

# Linux/Mac:
open htmlcov/index.html
```

### Terminal Coverage Report

```bash
# Show missing lines
pytest --cov=app --cov-report=term-missing

# Just show percentage
pytest --cov=app --cov-report=term
```

### Coverage Thresholds

The project requires **80% minimum coverage** (configured in `pytest.ini`).

```bash
# This will fail if coverage < 80%
pytest --cov=app --cov-fail-under=80
```

## Test Categories

### Unit Tests (fast, isolated)

Tests that don't require external dependencies:

- `test_profile_manager.py` - ProfileManager logic
- `test_actions.py` - Action handler logic
- `test_security.py` - Security utilities (rate limiter, validation)

```bash
pytest -m unit
```

### Integration Tests (may require services)

Tests that interact with external systems:

- `test_api.py` - REST API endpoints
- `test_websocket.py` - WebSocket connections

```bash
pytest -m integration
```

### Security Tests

Tests focused on security constraints:

- `test_actions_scripts.py` - Script execution security
- `test_security.py` - Rate limiting, validation, token auth

```bash
pytest -m security
```

## Writing Tests

### Test File Naming

- Test files: `test_*.py`
- Test classes: `Test*`
- Test functions: `test_*`

### Using Fixtures

```python
def test_with_settings(test_settings):
    """Use test settings fixture."""
    assert test_settings.deck_token == "test-token-12345"

def test_with_profile(sample_profile):
    """Use sample profile fixture."""
    assert sample_profile["id"] == "test-profile"
```

### Mocking Examples

```python
def test_with_mock(monkeypatch):
    """Mock external dependencies."""
    def mock_function():
        return "mocked"

    monkeypatch.setattr(module, "function", mock_function)
    assert module.function() == "mocked"
```

### Async Tests

```python
@pytest.mark.asyncio
async def test_async_function():
    """Test async code."""
    result = await some_async_function()
    assert result == expected
```

## Common Fixtures

Defined in `conftest.py`:

- `temp_data_dir` - Temporary directory for test data
- `test_settings` - Settings instance with test configuration
- `client` - FastAPI TestClient
- `rate_limiter` - Fresh RateLimiter instance
- `token_manager` - TokenManager for testing
- `sample_profile` - Sample profile data
- `sample_script` - Sample script file
- `mock_websocket_client` - Mock WebSocket client

## Continuous Integration

Tests run automatically on:

- Pull requests to `main` or `develop`
- Pushes to `main` or `develop`

See `.github/workflows/ci.yml` for CI configuration.

## Troubleshooting

### Import Errors

If you get import errors, ensure you're in the correct directory:

```bash
cd server/backend
pytest
```

### Windows-Specific Tests

Some tests are Windows-specific (system actions, audio). They will be skipped on other platforms:

```bash
# These tests will skip on Linux/Mac
pytest tests/test_actions.py::TestSystemActions
```

### Slow Tests

Mark slow tests with `@pytest.mark.slow`:

```python
@pytest.mark.slow
def test_long_running():
    # Long-running test
    pass
```

Skip them with:

```bash
pytest -m "not slow"
```

### Debugging Tests

```bash
# Drop into pdb on failure
pytest --pdb

# Show local variables in tracebacks
pytest -l

# Show print statements
pytest -s
```

## Code Quality Tools

### Run Linters

```bash
# Black (code formatting)
black app/ tests/

# isort (import sorting)
isort app/ tests/

# Pylint (static analysis)
pylint app/

# Mypy (type checking)
mypy app/

# Bandit (security linting)
bandit -r app/
```

### Pre-commit Hooks

Install pre-commit hooks to run checks automatically:

```bash
pip install pre-commit
pre-commit install

# Run manually
pre-commit run --all-files
```

## Test Coverage Goals

| Module | Current | Target |
|--------|---------|--------|
| `app/utils/profile_manager.py` | - | 95% |
| `app/actions/scripts.py` | - | 100% |
| `app/actions/keyboard.py` | - | 90% |
| `app/actions/audio.py` | - | 85% |
| `app/websocket.py` | - | 90% |
| `app/utils/rate_limiter.py` | - | 95% |
| **Overall** | - | **80%+** |

## Contributing

When adding new features:

1. **Write tests first** (TDD approach recommended)
2. Ensure **all tests pass**: `pytest`
3. Maintain **coverage above 80%**: `pytest --cov=app --cov-fail-under=80`
4. Run **code quality tools**: `black`, `pylint`, `mypy`
5. Add **security tests** for security-sensitive code

## Resources

- [Pytest Documentation](https://docs.pytest.org/)
- [Coverage.py Documentation](https://coverage.readthedocs.io/)
- [FastAPI Testing](https://fastapi.tiangolo.com/tutorial/testing/)
- [Pytest Fixtures](https://docs.pytest.org/en/stable/fixture.html)
