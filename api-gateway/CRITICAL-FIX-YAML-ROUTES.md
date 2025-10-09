# CRITICAL FIX: YAML Route Configuration

## Problem Found

**CRITICAL YAML INDENTATION ERROR** in `application.yaml`

The routes for `tenant`, `catalog`, `subscription`, `billing`, and `policy` services were **incorrectly nested under the `jasypt.encryptor.property` section** instead of `gateway.routes`.

This caused only 1 route (`setup-service`) to be registered, resulting in ALL other requests returning 404 errors with "Error starting response" messages from Reactor Netty.

## What Was Wrong

### Before (BROKEN):
```yaml
gateway:
  routes:
    setup:
      id: setup-service
      # ... config ...

jasypt:
  encryptor:
    property:
      prefix: ENC(
      suffix: )
      resilience:              # <-- THIS WAS WRONG!
        enabled: true
        circuit-breaker-name: setup-service
      tenant:                  # <-- ALL ROUTES WERE HERE!
        id: tenant-service
      catalog:
        id: catalog-service
      # ... etc
```

### After (FIXED):
```yaml
gateway:
  routes:
    setup:
      id: setup-service
      # ... config ...
      resilience:              # <-- MOVED HERE
        enabled: true
        circuit-breaker-name: setup-service
    tenant:                    # <-- ALL ROUTES MOVED HERE
      id: tenant-service
      # ... config ...
    catalog:
      id: catalog-service
      # ... config ...
    # ... all other routes ...

app:
  # ... app config ...

jasypt:
  encryptor:
    property:
      prefix: ENC(
      suffix: )              # <-- FIXED
```

## Files Changed

1. **application.yaml** - Fixed YAML structure
   - Moved `resilience` config under `gateway.routes.setup`
   - Moved all route definitions under `gateway.routes`
   - Restored `app` and `jasypt` sections in correct positions

## Impact

**Before Fix:**
- Only 1 route registered: `setup-service`
- All other requests: 404 NOT FOUND
- Error: "Error starting response. Replying error status"
- Routes count: 0 (excluding setup)

**After Fix:**
- 6+ routes will be registered:
  - `setup-service`
  - `tenant-service` (primary + canary)
  - `catalog-service`
  - `subscription-service`
  - `billing-service`
  - `policy-service`
- Proper routing for all configured paths
- Clear JSON error responses for actual 404s

## How to Verify

After redeploying, check logs for:

```
Registered route tenant-service -> lb://tenant-service
Registered route catalog-service -> lb://catalog-service
Registered route subscription-service -> lb://subscription-service
Registered route billing-service -> lb://billing-service
Registered route policy-service -> lb://policy-service
New routes count: 6 (or more)
```

## Root Cause

The YAML indentation error likely occurred during:
1. Copy/paste operations
2. Merge conflicts
3. Manual editing without YAML validation

## Prevention

1. **Use YAML validators** before committing:
   ```bash
   yamllint application.yaml
   ```

2. **Use IDE YAML plugins** with syntax checking

3. **Add pre-commit hooks** to validate YAML structure

4. **Review route counts** in startup logs:
   ```
   New routes count: X  # Should be > 1
   ```

5. **Add startup validation** to fail fast if no routes are loaded

## Testing

Test each route after deployment:

```bash
# Tenant service
curl http://localhost:8000/api/v1/tenants

# Catalog service
curl http://localhost:8000/api/v1/catalog/plans

# Subscription service
curl http://localhost:8000/api/v1/subscriptions

# Billing service
curl http://localhost:8000/api/v1/billing/usage

# Policy service
curl http://localhost:8000/api/v1/policies
```

All should return proper JSON responses (not "Error starting response").

## Related Issues

This fix resolves:
- ✅ "Error starting response. Replying error status" errors
- ✅ No routes being loaded from YAML
- ✅ All requests returning generic errors
- ✅ "New routes count: 0" in logs
