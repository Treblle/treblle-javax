# Migration Guide: urlPatterns → excludedPaths

## Breaking Change in Version 1.0.6

The `urlPatterns` configuration property has been **deprecated** and replaced with `excludedPaths`.

---

## Key Differences

| Aspect | urlPatterns (OLD) | excludedPaths (NEW) |
|--------|------------------|-------------------|
| **Logic** | Inclusion (monitor ONLY these) | Exclusion (monitor EXCEPT these) |
| **Status** | Non-functional | Fully implemented |
| **Default** | Monitor all (not enforced) | Monitor all |
| **Wildcards** | Not supported | Supported (`admin/*`, `*/internal`) |
| **Behavior** | Never actually worked | Works as expected |

---

## Important Note

**The `urlPatterns` property was never functional in earlier versions.**
All endpoints were monitored regardless of the `urlPatterns` setting. This migration primarily affects documentation and configuration - your actual monitoring behavior will not change unless you configure `excludedPaths`.

---

## Migration Steps

### Before (1.0.5 and earlier)

```xml
<init-param>
    <param-name>urlPatterns</param-name>
    <param-value>/api/users,/api/orders</param-value>
</init-param>
```

**Note**: This configuration had **NO EFFECT**. All endpoints were monitored.

---

### After (1.0.6+)

#### Option 1: Monitor Everything (Recommended Starting Point)

Simply **remove** the `urlPatterns` parameter entirely. All endpoints will be monitored by default.

```xml
<!-- No urlPatterns or excludedPaths parameter needed -->
<!-- All endpoints monitored by default -->
```

---

#### Option 2: Exclude Specific Endpoints

Use the new `excludedPaths` parameter to exclude health checks, admin panels, etc.:

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/health,/metrics,/admin/*,*/internal</param-value>
</init-param>
```

**Pattern Examples:**
- `/health` - Exact match
- `admin/*` - Prefix wildcard (all paths starting with `admin/`)
- `*/internal` - Suffix wildcard (all paths ending with `/internal`)
- `/api/*/debug` - Middle wildcard
- `*` - Match all paths (excludes everything - use with caution!)

---

## Common Exclusion Patterns

### Health Check Endpoints

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/health,/healthz,/live,/ready</param-value>
</init-param>
```

### Admin and Internal APIs

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/admin/*,/internal/*,*/private/*</param-value>
</init-param>
```

### Metrics and Monitoring

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/metrics,/prometheus,/actuator/*</param-value>
</init-param>
```

### File Uploads (Performance Optimization)

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/upload,/api/*/upload,/files/*</param-value>
</init-param>
```

### Combined Example

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/health,/metrics,/admin/*,/internal/*,/upload</param-value>
</init-param>
```

---

## Pattern Matching Behavior

### Exact Match
```
Pattern: /health
Matches: /health
Does NOT match: /healthz, /api/health
```

### Prefix Wildcard
```
Pattern: admin/*
Matches: /admin/users, /admin/settings, /admin/users/123
Does NOT match: /api/admin, /users/admin
```

### Suffix Wildcard
```
Pattern: */internal
Matches: /api/internal, /v1/internal, /users/internal
Does NOT match: /internal/api, /internalapi
```

### Middle Wildcard
```
Pattern: /api/*/debug
Matches: /api/v1/debug, /api/v2/debug, /api/users/debug
Does NOT match: /api/debug, /v1/api/debug
```

### Match All (Use with Caution!)
```
Pattern: *
Matches: Every path (excludes all monitoring)
⚠️ WARNING: This will disable Treblle monitoring entirely
```

---

## JAX-RS Configuration

The same migration applies to JAX-RS applications:

### Before (1.0.5 and earlier)

```java
@Override
public Map<String, Object> getProperties() {
    Map<String, Object> props = new HashMap<>();
    props.put("sdkToken", "YOUR_SDK_TOKEN");
    props.put("apiKey", "YOUR_API_KEY");
    props.put("urlPatterns", "/api/users,/api/orders"); // Never worked
    return props;
}
```

### After (1.0.6+)

```java
@Override
public Map<String, Object> getProperties() {
    Map<String, Object> props = new HashMap<>();
    props.put("sdkToken", "YOUR_SDK_TOKEN");
    props.put("apiKey", "YOUR_API_KEY");
    props.put("excludedPaths", "/health,/admin/*,*/internal"); // Now functional!
    return props;
}
```

---

## Deprecation Timeline

| Version | Status |
|---------|--------|
| **1.0.6** | `excludedPaths` introduced, `urlPatterns` deprecated with warning |
| **2.0.0** | `urlPatterns` removed entirely |

---

## Testing Your Configuration

After migrating to `excludedPaths`:

1. **Deploy your application** with the new configuration
2. **Make requests** to both monitored and excluded endpoints:
   ```bash
   # This should appear in Treblle dashboard
   curl http://localhost:8080/api/users

   # This should NOT appear in Treblle dashboard
   curl http://localhost:8080/health
   curl http://localhost:8080/admin/settings
   ```
3. **Check Treblle dashboard** - verify excluded endpoints don't appear
4. **Enable debug mode** (optional) to see exclusion logic in logs:
   ```xml
   <init-param>
       <param-name>debug</param-name>
       <param-value>true</param-value>
   </init-param>
   ```

---

## Troubleshooting

### Issue: Endpoints still being monitored despite exclusion

**Possible Causes:**
1. Pattern syntax error (missing leading `/`, incorrect wildcard placement)
2. Whitespace in configuration (should be trimmed automatically, but verify)
3. Path doesn't match pattern exactly

**Solution:**
- Test patterns individually: `excludedPaths=/health` (one pattern at a time)
- Check logs with `debug=true` for path matching details
- Verify request path format (use browser dev tools or logs)

### Issue: All endpoints excluded accidentally

**Cause:** Used `*` wildcard pattern

**Solution:**
```xml
<!-- WRONG - excludes everything -->
<param-value>*</param-value>

<!-- CORRECT - excludes specific paths -->
<param-value>/health,/metrics,/admin/*</param-value>
```

### Issue: Deprecation warnings in logs

**Cause:** Still using `urlPatterns` parameter

**Solution:** Remove `urlPatterns` and use `excludedPaths` instead

---

## Questions?

- **Documentation**: See [README.md](README.md) for complete usage guide
- **Issues**: Report bugs at [GitHub Issues](https://github.com/Treblle/treblle-java/issues)
- **Support**: Contact support@treblle.com for enterprise help

---

## Quick Reference Card

```xml
<!-- Monitor everything (default) -->
<!-- No configuration needed -->

<!-- Exclude health checks -->
<param-name>excludedPaths</param-name>
<param-value>/health,/healthz</param-value>

<!-- Exclude admin panel -->
<param-name>excludedPaths</param-name>
<param-value>/admin/*</param-value>

<!-- Exclude multiple patterns -->
<param-name>excludedPaths</param-name>
<param-value>/health,/metrics,/admin/*,*/internal</param-value>

<!-- ⚠️ AVOID: Exclude everything -->
<param-name>excludedPaths</param-name>
<param-value>*</param-value>
```

---

**Made with ☕ by the Treblle team**
