# Migration Guide - Security Updates (Phase 1)

## Overview

This guide helps you migrate to the new security-enhanced version of Control Deck.

## Breaking Changes

### 1. CORS Configuration (ACTION REQUIRED)

**What changed:**
- CORS now requires explicit origin configuration
- Default: `http://localhost:3000,http://localhost:4455`

**Who is affected:**
- Users accessing the web UI from custom domains
- Users accessing from non-localhost addresses

**How to fix:**

```bash
# Add to .env file
DECK_ALLOWED_ORIGINS=http://192.168.1.100:4455,http://my-server.local:4455
```

**Testing:**
```bash
# Start server
cd server/backend
python -m uvicorn app.main:app --host 0.0.0.0 --port 4455

# Test CORS (from browser console on allowed origin)
fetch('http://your-server:4455/health')
  .then(r => r.json())
  .then(console.log)
```

---

### 2. Script Actions Security (ACTION REQUIRED)

**What changed:**
- Scripts must be in whitelisted directories
- Only specific file extensions allowed (`.sh`, `.bat`, `.ps1`, `.py`)
- Path traversal attacks prevented

**Who is affected:**
- Users with custom script actions
- Users running scripts from arbitrary locations

**How to migrate:**

1. **Create scripts directory:**
   ```bash
   mkdir -p ~/.config/control-deck/scripts
   # Or use project directory
   mkdir -p /path/to/deck/data/scripts
   ```

2. **Move existing scripts:**
   ```bash
   # Move your scripts to allowed directory
   mv /old/location/my-script.sh ~/.config/control-deck/scripts/
   chmod +x ~/.config/control-deck/scripts/my-script.sh
   ```

3. **Update profiles:**
   ```json
   {
     "action": {
       "type": "scripts",
       "payload": [
         "~/.config/control-deck/scripts/my-script.sh",
         "arg1",
         "arg2"
       ]
     }
   }
   ```

**Before (insecure):**
```json
{
  "payload": ["/etc/../home/user/dangerous-script.sh"]
}
```

**After (secure):**
```json
{
  "payload": ["~/.config/control-deck/scripts/safe-script.sh"]
}
```

---

### 3. Rate Limiting (NEW FEATURE)

**What changed:**
- WebSocket connections now limited to 100 requests/minute by default
- Prevents DoS attacks

**Who is affected:**
- High-frequency automation users
- Multiple clients from same IP

**How to adjust:**

```bash
# Increase limits for trusted networks
DECK_RATE_LIMIT_REQUESTS=200
DECK_RATE_LIMIT_WINDOW=60
```

**Error handling in client code:**

```typescript
// Frontend (TypeScript)
ws.onmessage = (event) => {
  const data = JSON.parse(event.data);

  if (data.error === 'rate_limit_exceeded') {
    console.warn(`Rate limited. Retry after ${data.retry_after}s`);
    setTimeout(() => retryAction(), data.retry_after * 1000);
  }
};
```

```kotlin
// Android (Kotlin)
when (response.error) {
    "rate_limit_exceeded" -> {
        val retryAfter = response.retryAfter ?: 5.0
        delay((retryAfter * 1000).toLong())
        retryAction()
    }
}
```

---

### 4. Message Size Validation (NEW FEATURE)

**What changed:**
- Messages larger than 100KB rejected by default
- Prevents memory exhaustion attacks

**Who is affected:**
- Users sending large payloads (rare)
- Users with complex profile structures

**How to adjust:**

```bash
# Increase if needed (bytes)
DECK_MAX_MESSAGE_SIZE=204800  # 200KB
```

**Error:**
```
WebSocket closed with code 1009 (Message Too Big)
```

---

## Non-Breaking Improvements

### Enhanced Logging

Security events now logged with client information:

```
2025-01-15 10:23:45 | WARNING | Rate limit exceeded for 192.168.1.105
2025-01-15 10:24:12 | WARNING | Message too large from 192.168.1.110: 150000 bytes
```

**Configure log level:**
```bash
DECK_LOG_LEVEL=info  # info, debug, warn, error
```

---

## Migration Checklist

### Pre-Migration

- [ ] Backup current `.env` file
- [ ] Document current script locations
- [ ] Export all profiles
- [ ] Note any custom configurations

### Migration Steps

1. **Update code:**
   ```bash
   git pull origin main
   cd server/backend
   pip install -r requirements.txt
   ```

2. **Create `.env` from example:**
   ```bash
   cp .env.example .env
   # Edit .env with your settings
   ```

3. **Configure CORS:**
   ```bash
   # Add to .env
   DECK_ALLOWED_ORIGINS=http://localhost:3000,http://YOUR_SERVER_IP:4455
   ```

4. **Migrate scripts:**
   ```bash
   mkdir -p ~/.config/control-deck/scripts
   # Move scripts to allowed directory
   ```

5. **Test connection:**
   ```bash
   # Start server
   python -m uvicorn app.main:app --reload

   # Test WebSocket
   wscat -c ws://localhost:4455/ws?token=YOUR_TOKEN
   ```

6. **Update profiles** (if using script actions)

### Post-Migration

- [ ] Test web UI access from all required origins
- [ ] Verify script actions execute correctly
- [ ] Monitor logs for security events
- [ ] Test mobile app reconnection
- [ ] Verify rate limiting doesn't impact normal usage

---

## Rollback Plan

If you encounter issues:

1. **Temporary CORS fix** (development only):
   ```bash
   # In .env - DO NOT USE IN PRODUCTION
   DECK_ALLOWED_ORIGINS=*
   ```

2. **Disable rate limiting** (troubleshooting):
   ```bash
   DECK_RATE_LIMIT_REQUESTS=999999
   ```

3. **Revert to previous version:**
   ```bash
   git checkout <previous-commit>
   pip install -r requirements.txt
   ```

---

## Testing Your Migration

### 1. Test CORS
```bash
# From browser console on your frontend
fetch('http://your-server:4455/health')
  .then(r => r.json())
  .then(d => console.log('✅ CORS working:', d))
  .catch(e => console.error('❌ CORS issue:', e))
```

### 2. Test WebSocket
```bash
# Using wscat (npm install -g wscat)
wscat -c "ws://localhost:4455/ws?token=YOUR_TOKEN"
> {"action": "processes", "messageId": "test123"}
< {"type": "ack", "status": "ok", ...}
```

### 3. Test Script Action
```bash
# Create test script
echo '#!/bin/bash\necho "Hello from Control Deck"' > ~/.config/control-deck/scripts/test.sh
chmod +x ~/.config/control-deck/scripts/test.sh

# Via WebSocket
> {"action": "scripts", "payload": ["~/.config/control-deck/scripts/test.sh"], "messageId": "script-test"}
```

### 4. Test Rate Limiting
```bash
# Send 101 requests rapidly (should see rate limit error)
for i in {1..101}; do
  echo '{"action":"processes","messageId":"test'$i'"}' | wscat -c ws://localhost:4455/ws?token=TOKEN --wait 0
done
```

---

## Troubleshooting

### CORS Errors

**Symptom:**
```
Access to fetch at 'http://server:4455/api' from origin 'http://localhost:3000'
has been blocked by CORS policy
```

**Solution:**
```bash
# Add origin to .env
DECK_ALLOWED_ORIGINS=http://localhost:3000,http://server:4455
```

---

### Script Validation Errors

**Symptom:**
```json
{
  "status": "error",
  "error": "Script must be in allowed directories: [...]"
}
```

**Solution:**
1. Move script to `~/.config/control-deck/scripts/`
2. Or add to `ALLOWED_SCRIPT_DIRS` in [scripts.py](../server/backend/app/actions/scripts.py#L13-L16)

---

### Rate Limit Issues

**Symptom:**
```json
{
  "type": "error",
  "error": "rate_limit_exceeded",
  "retry_after": 15.3
}
```

**Solutions:**

1. **Increase limits** (if legitimate usage):
   ```bash
   DECK_RATE_LIMIT_REQUESTS=500
   ```

2. **Use separate client IDs**:
   ```typescript
   // Send unique client ID
   ws = new WebSocket('ws://server/ws', {
     headers: { 'X-Client-ID': 'unique-client-123' }
   });
   ```

3. **Implement exponential backoff**:
   ```typescript
   async function sendWithRetry(payload, retries = 3) {
     try {
       await sendControl(payload);
     } catch (err) {
       if (err.error === 'rate_limit_exceeded' && retries > 0) {
         await sleep(err.retry_after * 1000);
         return sendWithRetry(payload, retries - 1);
       }
       throw err;
     }
   }
   ```

---

## Support

If you encounter issues during migration:

1. Check [SECURITY.md](../SECURITY.md) for detailed configuration
2. Review server logs: `tail -f server/backend/logs/app.log`
3. Open GitHub issue with:
   - Migration step where issue occurred
   - Error messages from logs
   - Your sanitized `.env` configuration
   - Browser/client version

---

## Next Steps

After successful migration:

1. Review [SECURITY.md](../SECURITY.md) for best practices
2. Consider implementing Phase 2 improvements (tests)
3. Set up monitoring/alerting for security events
4. Document any custom security configurations
5. Train team members on new security features
