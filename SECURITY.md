# Security Guide

## Overview

This document describes the security features and best practices for the Control Deck application.

## Recent Security Improvements (Phase 1)

### 1. CORS Configuration ✅

**Previous:** Accepted connections from any origin (`allow_origins=["*"]`)

**Now:** Configurable allowed origins via environment variable

```bash
# .env
DECK_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4455,https://yourdomain.com
```

**Impact:** Prevents Cross-Site Request Forgery (CSRF) attacks

**File:** [server/backend/app/main.py](server/backend/app/main.py)

---

### 2. WebSocket Rate Limiting ✅

**Feature:** Limits the number of requests per client per time window

**Configuration:**
```bash
# .env
DECK_RATE_LIMIT_REQUESTS=100  # Max requests
DECK_RATE_LIMIT_WINDOW=60     # Per 60 seconds
```

**Behavior:**
- Tracks requests per client IP or `X-Client-ID` header
- Returns error with `retry_after` when limit exceeded
- Automatically cleans old entries

**Response Example:**
```json
{
  "type": "error",
  "error": "rate_limit_exceeded",
  "retry_after": 12.5
}
```

**File:** [server/backend/app/websocket.py](server/backend/app/websocket.py#L55-L64)

---

### 3. Message Size Validation ✅

**Feature:** Prevents denial-of-service attacks via large payloads

**Configuration:**
```bash
# .env
DECK_MAX_MESSAGE_SIZE=102400  # 100KB (default)
```

**Behavior:**
- Validates message size before processing
- Closes connection with code 1009 (Message Too Big)
- Logs warning with client identifier

**File:** [server/backend/app/websocket.py](server/backend/app/websocket.py#L45-L49)

---

### 4. Script Execution Security ✅

**Previous:** Accepted any command sequence without validation

**Now:** Multi-layer security validation

#### Security Layers:

1. **Path Validation**
   - Resolves symlinks and relative paths
   - Verifies file exists and is readable

2. **Directory Whitelist**
   ```python
   ALLOWED_SCRIPT_DIRS = [
       settings.deck_data_dir / "scripts",
       Path.home() / ".config/control-deck/scripts"
   ]
   ```

3. **Extension Whitelist**
   ```python
   ALLOWED_EXTENSIONS = {".sh", ".bat", ".ps1", ".py"}
   ```

4. **Execution Safeguards**
   - `shell=False` (prevents shell injection)
   - 30-second timeout
   - Separate stdout/stderr capture

#### Example Error:
```json
{
  "status": "error",
  "error": "Script must be in allowed directories: ['/app/data/scripts', '/home/user/.config/control-deck/scripts']"
}
```

**File:** [server/backend/app/actions/scripts.py](server/backend/app/actions/scripts.py)

---

## Authentication

### Token-Based Authentication

The application uses token-based authentication for WebSocket connections:

1. **Handshake Secret** - Initial pairing uses `DECK_HANDSHAKE_SECRET`
2. **Access Token** - 24-hour token generated after pairing
3. **Token Validation** - Every WebSocket connection validates token

**Token can be provided via:**
- `Authorization: Bearer <token>` header
- `?token=<token>` query parameter

**Connection Rejection:**
- Invalid token: WebSocket closes with code `4001`

---

## Best Practices

### For Production Deployment

1. **Set Strong Secrets**
   ```bash
   DECK_TOKEN=$(openssl rand -hex 32)
   DECK_HANDSHAKE_SECRET=$(openssl rand -hex 32)
   ```

2. **Configure Allowed Origins**
   ```bash
   DECK_ALLOWED_ORIGINS=https://your-frontend.com,https://app.yourdomain.com
   ```

3. **Adjust Rate Limits** based on expected usage
   ```bash
   DECK_RATE_LIMIT_REQUESTS=200  # Higher for trusted networks
   DECK_RATE_LIMIT_WINDOW=60
   ```

4. **Enable HTTPS** with TLS certificates
   ```bash
   DECK_TLS_KEY_PATH=/path/to/key.pem
   DECK_TLS_CERT_PATH=/path/to/cert.pem
   ```

5. **Review Logs Regularly**
   ```bash
   DECK_LOG_LEVEL=info  # Use 'debug' for troubleshooting only
   ```

---

### For Script Actions

1. **Create Scripts Directory**
   ```bash
   mkdir -p ~/.config/control-deck/scripts
   chmod 700 ~/.config/control-deck/scripts
   ```

2. **Only Store Trusted Scripts**
   - Verify script source before adding
   - Review script contents
   - Use minimal permissions

3. **Test Scripts Manually First**
   ```bash
   cd ~/.config/control-deck/scripts
   chmod +x my-script.sh
   ./my-script.sh  # Test before using in Control Deck
   ```

---

## Security Checklist

Before deploying to production:

- [ ] Change default `DECK_TOKEN` and `DECK_HANDSHAKE_SECRET`
- [ ] Configure `DECK_ALLOWED_ORIGINS` with exact domains
- [ ] Enable TLS/HTTPS certificates
- [ ] Review and adjust rate limiting settings
- [ ] Test WebSocket authentication
- [ ] Verify script directory permissions (chmod 700)
- [ ] Set `DECK_LOG_LEVEL=info` (not debug)
- [ ] Document custom security configurations
- [ ] Set up log monitoring/alerting

---

## Reporting Security Issues

If you discover a security vulnerability, please:

1. **DO NOT** open a public GitHub issue
2. Email security details to: [your-security-email@example.com]
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

We aim to respond within 48 hours.

---

## Changelog

### Phase 1 (Current)
- ✅ CORS configuration with origin whitelist
- ✅ WebSocket rate limiting per client
- ✅ Message size validation
- ✅ Script execution path validation and sandboxing

### Planned (Phase 2+)
- [ ] Additional authentication methods (OAuth2, SAML)
- [ ] Audit logging for security events
- [ ] IP-based access control lists
- [ ] Certificate pinning for mobile clients
- [ ] Automated security scanning in CI/CD
