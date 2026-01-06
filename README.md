# Treblle Java SDK for javax

[![Maven Central](https://img.shields.io/maven-central/v/com.treblle/treblle-javax.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.treblle/treblle-javax)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-1.8%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)

> **Real-time API monitoring, error tracking, and debugging for Java applications using Servlet API and JAX-RS**

---

## Table of Contents

- [Introduction](#introduction)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
  - [Servlet Filter Setup](#servlet-filter-setup)
  - [JAX-RS Filter Setup](#jax-rs-filter-setup)
- [Configuration Reference](#configuration-reference)
- [Data Masking](#data-masking)
- [Advanced Configuration](#advanced-configuration)
- [Performance & Resource Management](#performance--resource-management)
- [Error Handling & Reliability](#error-handling--reliability)
- [What Data is Sent to Treblle?](#what-data-is-sent-to-treblle)
- [Supported Environments](#supported-environments)
- [Troubleshooting](#troubleshooting)
- [Examples](#examples)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

---

## Introduction

### What is Treblle?

[Treblle](https://www.treblle.com) is a lightweight API monitoring and observability platform that helps you:

- **Monitor API traffic** in real-time
- **Debug issues** faster with detailed request/response logs
- **Track performance** with response time analytics
- **Secure sensitive data** with automatic masking
- **Understand API usage** with comprehensive analytics

### What does this SDK do?

The **treblle-javax** SDK seamlessly integrates Treblle monitoring into your Java applications using either:
- **Servlet API 3.1.0+** (Tomcat, Jetty, etc.)
- **JAX-RS API 2.1.1+** (Jersey, RESTEasy, etc.)

### Key Features

✅ **Zero Configuration** - Works out of the box with minimal setup
✅ **Automatic Masking** - Sensitive data (passwords, tokens, credit cards) masked by default
✅ **Non-Blocking** - Async telemetry transmission never slows down your API
✅ **Production-Ready** - Thoroughly tested for resource leaks, crashes, and performance
✅ **Fail-Safe** - Never crashes your API, even if Treblle is unavailable
✅ **Memory Efficient** - Bounded memory usage (2MB limit per request/response)
✅ **Thread Safe** - Managed thread pool with bounded queue
✅ **Flexible** - Supports both traditional servlets and modern JAX-RS

### Why use treblle-javax?

- **5-minute setup** - Add dependency, configure credentials, done
- **No code changes** - Works as a filter/interceptor
- **Battle-tested** - Used in production by teams worldwide
- **Open Source** - MIT license, transparent, community-driven

---

## Requirements

| Component | Minimum Version | Notes |
|-----------|----------------|-------|
| **Java** | 1.8+ | Compiled with Java 8 for broad compatibility |
| **Servlet API** | 3.1.0+ | For `TreblleServletFilter` |
| **JAX-RS API** | 2.1.1+ | For `TreblleContainerFilter` |

### Supported Java Versions
- Java 8 (1.8)
- Java 11 (LTS)
- Java 17 (LTS)
- Java 21 (LTS)

---

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.treblle</groupId>
    <artifactId>treblle-javax</artifactId>
    <version>1.0.6</version>
</dependency>
```

### Gradle

Add the dependency to your `build.gradle`:

```gradle
implementation 'com.treblle:treblle-javax:1.0.6'
```

### Gradle (Kotlin DSL)

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("com.treblle:treblle-javax:1.0.6")
```

---

## Quick Start

Choose the setup method that matches your application architecture:

### Servlet Filter Setup

**For traditional Servlet applications** (Tomcat, Jetty, WildFly, etc.)

#### Step 1: Add filter configuration to `web.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

    <filter>
        <filter-name>TreblleServletFilter</filter-name>
        <filter-class>com.treblle.javax.TreblleServletFilter</filter-class>

        <!-- Required: Your Treblle credentials -->
        <init-param>
            <param-name>sdkToken</param-name>
            <param-value>YOUR_SDK_TOKEN_HERE</param-value>
        </init-param>
        <init-param>
            <param-name>apiKey</param-name>
            <param-value>YOUR_API_KEY_HERE</param-value>
        </init-param>

        <!-- Optional: Enable debug logging -->
        <init-param>
            <param-name>debug</param-name>
            <param-value>false</param-value>
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>TreblleServletFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
    </filter-mapping>

</web-app>
```

#### Step 2: Get your Treblle credentials

1. Sign up at [app.treblle.com](https://app.treblle.com)
2. Create a new project
3. Copy your **SDK Token** and **API Key**
4. Replace the placeholders in the configuration above

#### Step 3: Deploy and test

Deploy your application and make an API request. You should see the request appear in your Treblle dashboard within seconds.

---

### JAX-RS Filter Setup

**For JAX-RS applications** (Jersey, RESTEasy, Apache CXF, etc.)

#### Option A: Using JAX-RS Application Class

```java
import com.treblle.javax.TreblleContainerFilter;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.*;

@ApplicationPath("/api")
public class MyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // Register Treblle filter
        classes.add(TreblleContainerFilter.class);

        // Your resource classes
        classes.add(MyResource.class);

        return classes;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();

        // Required: Treblle credentials
        props.put("sdkToken", "YOUR_SDK_TOKEN_HERE");
        props.put("apiKey", "YOUR_API_KEY_HERE");

        // Optional: Debug mode
        props.put("debug", false);

        return props;
    }
}
```

#### Option B: Programmatic Registration (Jersey)

```java
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.javax.TreblleContainerFilter;
import org.glassfish.jersey.server.ResourceConfig;

public class MyResourceConfig extends ResourceConfig {

    public MyResourceConfig() {
        // Create custom configuration
        TreblleProperties treblleConfig = new TreblleProperties() {
            @Override
            public String getSdkToken() {
                return System.getenv("TREBLLE_SDK_TOKEN");
            }

            @Override
            public String getApiKey() {
                return System.getenv("TREBLLE_API_KEY");
            }

            @Override
            public boolean isDebug() {
                return Boolean.parseBoolean(System.getenv("TREBLLE_DEBUG"));
            }
        };

        // Register filter with custom configuration
        register(new TreblleContainerFilter(treblleConfig));

        // Register your resources
        packages("com.example.resources");
    }
}
```

#### Option C: Using web.xml (Jersey in Servlet container)

```xml
<servlet>
    <servlet-name>Jersey Web Application</servlet-name>
    <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
    <init-param>
        <param-name>jersey.config.server.provider.classnames</param-name>
        <param-value>com.treblle.javax.TreblleContainerFilter</param-value>
    </init-param>
    <init-param>
        <param-name>sdkToken</param-name>
        <param-value>YOUR_SDK_TOKEN_HERE</param-value>
    </init-param>
    <init-param>
        <param-name>apiKey</param-name>
        <param-value>YOUR_API_KEY_HERE</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>

<servlet-mapping>
    <servlet-name>Jersey Web Application</servlet-name>
    <url-pattern>/api/*</url-pattern>
</servlet-mapping>
```

---

## Configuration Reference

### Required Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `sdkToken` | String | Your Treblle SDK token (get from [dashboard](https://app.treblle.com)) |
| `apiKey` | String | Your Treblle API key (get from [dashboard](https://app.treblle.com)) |

### Optional Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `endpoint` | String | `null` | Custom Treblle endpoint URL (for self-hosted) |
| `debug` | Boolean | `false` | Enable debug logging (logs HTTP requests/responses) |
| `excludedPaths` | String | `""` | Comma-separated path patterns to EXCLUDE (supports wildcards: `/health`, `admin/*`) |
| `maskedKeywords` | String | `""` | Additional field names to mask (comma-separated) |
| `urlPatterns` | String | `""` | **DEPRECATED**: Use `excludedPaths` instead (removal in 2.0.0) |
| `connectTimeoutInSeconds` | Integer | `3` | HTTP connect timeout for Treblle API |
| `readTimeoutInSeconds` | Integer | `3` | HTTP read timeout for Treblle API |
| `maxBodySizeInBytes` | Integer | `2097152` | Max request/response body size for telemetry (2MB) |

### Parameter Examples

#### Excluded Paths

Exclude specific endpoints from monitoring (health checks, admin panels, etc.):

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/health,/metrics,/admin/*,*/internal</param-value>
</init-param>
```

**Pattern Examples:**
- `/health` - Exclude exact path
- `admin/*` - Exclude all admin endpoints
- `*/internal` - Exclude any path ending with /internal
- `/api/*/debug` - Exclude debug endpoints at any version

#### Custom Masking Keywords

Add custom fields to mask (in addition to defaults):

```xml
<init-param>
    <param-name>maskedKeywords</param-name>
    <param-value>custom_token,sensitive_field,internal_.*</param-value>
</init-param>
```

#### Memory Limit

Reduce memory usage for high-traffic APIs:

```xml
<init-param>
    <param-name>maxBodySizeInBytes</param-name>
    <param-value>1048576</param-value> <!-- 1MB -->
</init-param>
```

---

## Data Masking

### Why Masking Matters

Treblle automatically masks sensitive data before it leaves your server. This ensures:

- **Compliance** - Meet GDPR, PCI-DSS, HIPAA requirements
- **Security** - Never expose passwords, tokens, or credit cards
- **Privacy** - Protect user data in logs and dashboards

### How Masking Works

1. **Request/response bodies** are parsed as JSON
2. **Field names** are matched against masking rules (case-insensitive)
3. **Matched values** are replaced with `"******"` before transmission
4. **Original values** never leave your server

### Default Masked Fields

The following **13 fields** are masked automatically:

```
password             password_confirmation    passwordConfirmation
pwd                  secret
cc                   card_number              cardNumber
ccv
ssn
credit_score         creditScore
api_key
```

### Masking Patterns

Treblle supports two types of masking patterns:

#### 1. Exact Match

Masks fields with exact name match:

```json
{
  "password": "******",          // Masked
  "user_password": "secret123",  // NOT masked (not exact match)
  "pwd": "******"                // Masked
}
```

#### 2. Wildcard Patterns (using `.*`)

Masks fields matching prefix:

```json
// maskedKeywords: "api_.*"
{
  "api_key": "******",      // Masked (matches api_.*)
  "api_secret": "******",   // Masked (matches api_.*)
  "api_token": "******",    // Masked (matches api_.*)
  "apiKey": "abc123"        // NOT masked (doesn't start with api_)
}
```

### Custom Masking

Add your own masking rules via configuration:

```xml
<init-param>
    <param-name>maskedKeywords</param-name>
    <param-value>auth_token,bearer_.*,x-custom-header</param-value>
</init-param>
```

**Example Result:**

```json
{
  "auth_token": "******",        // Custom rule
  "bearer_token": "******",      // Custom wildcard
  "bearer_key": "******",        // Custom wildcard
  "x-custom-header": "******",   // Custom rule
  "password": "******"           // Default rule
}
```

### Masking Scope

Masking applies to:

- ✅ **Request body** (JSON)
- ✅ **Response body** (JSON)
- ✅ **Request headers**
- ✅ **Response headers**
- ❌ **URL/query parameters** (not masked - avoid sensitive data in URLs)

### Security Best Practices

1. **Never send sensitive data in URLs** - Use request body or headers
2. **Use HTTPS** - Treblle enforces HTTPS, but ensure your API does too
3. **Review masked fields** - Check Treblle dashboard to verify masking works
4. **Add custom rules** - Mask any application-specific sensitive fields

---

## Advanced Configuration

### Complete Servlet Example

Full `web.xml` configuration with all parameters:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

    <filter>
        <filter-name>TreblleServletFilter</filter-name>
        <filter-class>com.treblle.javax.TreblleServletFilter</filter-class>

        <!-- Required -->
        <init-param>
            <param-name>sdkToken</param-name>
            <param-value>${env.TREBLLE_SDK_TOKEN}</param-value>
        </init-param>
        <init-param>
            <param-name>apiKey</param-name>
            <param-value>${env.TREBLLE_API_KEY}</param-value>
        </init-param>

        <!-- Optional -->
        <init-param>
            <param-name>debug</param-name>
            <param-value>false</param-value>
        </init-param>
        <init-param>
            <param-name>excludedPaths</param-name>
            <param-value>/health,/metrics,/admin/*</param-value>
        </init-param>
        <init-param>
            <param-name>maskedKeywords</param-name>
            <param-value>auth_token,session_id,bearer_.*</param-value>
        </init-param>
        <init-param>
            <param-name>connectTimeoutInSeconds</param-name>
            <param-value>5</param-value>
        </init-param>
        <init-param>
            <param-name>readTimeoutInSeconds</param-name>
            <param-value>5</param-value>
        </init-param>
        <init-param>
            <param-name>maxBodySizeInBytes</param-name>
            <param-value>2097152</param-value> <!-- 2MB -->
        </init-param>
    </filter>

    <filter-mapping>
        <filter-name>TreblleServletFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
    </filter-mapping>

</web-app>
```

### Environment Variables

Use environment variables for credentials (recommended for production):

```xml
<init-param>
    <param-name>sdkToken</param-name>
    <param-value>${TREBLLE_SDK_TOKEN}</param-value>
</init-param>
```

Then set the environment variable:

```bash
export TREBLLE_SDK_TOKEN="your_token_here"
export TREBLLE_API_KEY="your_key_here"
```

### Multiple Filter Mappings

Monitor different endpoints with different configurations:

```xml
<!-- Admin API - more verbose -->
<filter>
    <filter-name>TreblleAdminFilter</filter-name>
    <filter-class>com.treblle.javax.TreblleServletFilter</filter-class>
    <init-param>
        <param-name>sdkToken</param-name>
        <param-value>${TREBLLE_SDK_TOKEN}</param-value>
    </init-param>
    <init-param>
        <param-name>apiKey</param-name>
        <param-value>${TREBLLE_API_KEY}</param-value>
    </init-param>
    <init-param>
        <param-name>debug</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>

<filter-mapping>
    <filter-name>TreblleAdminFilter</filter-name>
    <url-pattern>/admin/*</url-pattern>
</filter-mapping>

<!-- Public API - production settings -->
<filter>
    <filter-name>TrebllePublicFilter</filter-name>
    <filter-class>com.treblle.javax.TreblleServletFilter</filter-class>
    <init-param>
        <param-name>sdkToken</param-name>
        <param-value>${TREBLLE_SDK_TOKEN}</param-value>
    </init-param>
    <init-param>
        <param-name>apiKey</param-name>
        <param-value>${TREBLLE_API_KEY}</param-value>
    </init-param>
    <init-param>
        <param-name>maxBodySizeInBytes</param-name>
        <param-value>1048576</param-value> <!-- 1MB for high traffic -->
    </init-param>
</filter>

<filter-mapping>
    <filter-name>TrebllePublicFilter</filter-name>
    <url-pattern>/api/*</url-pattern>
</filter-mapping>
```

---

## Performance & Resource Management

### Memory Usage

**Request/Response Body Caching:**
- Default limit: **2MB per request** + **2MB per response** = **4MB max per request**
- Configurable via `maxBodySizeInBytes`
- When limit exceeded:
  - Request/response continues normally
  - Telemetry data is truncated
  - No error thrown

**Recommendations:**
- **High-traffic APIs**: Reduce to 1MB (`maxBodySizeInBytes=1048576`)
- **File upload endpoints**: Exclude from monitoring via `excludedPaths`
- **Streaming APIs**: Exclude or reduce limit significantly

### Thread Management

**Async Telemetry Transmission:**
- **Non-blocking**: Telemetry sent asynchronously, never delays responses
- **Bounded thread pool**: 1-3 threads maximum
- **Bounded queue**: 100 pending tasks maximum
- **Backpressure**: If queue full, task runs in caller thread (prevents memory leak)
- **Daemon threads**: Named `treblle-async-1`, `treblle-async-2`, etc.

**Impact on Your Application:**
- ✅ **Zero impact** on request processing
- ✅ **< 5ms overhead** for body caching
- ✅ **Zero blocking** - async transmission

### HTTP Client

**Connection Pooling:**
- **Singleton HTTP client** - reused across all requests
- **Persistent connections** to Treblle endpoints
- **Automatic cleanup** on shutdown

**Timeouts:**
- **Connect timeout**: 3 seconds (configurable)
- **Read timeout**: 3 seconds (configurable)
- **Retry policy**: Disabled (fails fast if Treblle unavailable)

### Load Balancing

Treblle uses **3 endpoints** for load balancing and high availability:

```
https://rocknrolla.treblle.com
https://punisher.treblle.com
https://sicario.treblle.com
```

The SDK randomly selects an endpoint for each request.

---

## Error Handling & Reliability

### Fail-Safe Design

The SDK is designed to **never impact your API**, even if:

- ❌ Treblle endpoints are down
- ❌ Network connectivity is lost
- ❌ Memory limit is exceeded
- ❌ Invalid configuration is provided

### Guaranteed Behavior

1. **Response Always Delivered**
   - Client receives full response, even if telemetry fails
   - Response body restoration happens **before** telemetry transmission

2. **Original Exceptions Preserved**
   - If your API throws an exception, it's preserved and re-thrown
   - Treblle errors are logged but never propagate

3. **Graceful Degradation**
   - If body size exceeds limit, capture is stopped but request continues
   - If JSON parsing fails, error is logged but request continues
   - If Treblle API is unavailable, telemetry is skipped

### Error Logging

**SLF4J Integration:**

```java
// ERROR level - telemetry failures
LOGGER.error("Failed to send payload to Treblle", exception);

// WARN level - configuration issues
LOGGER.warn("Response entity stream is null, skipping Treblle telemetry");

// DEBUG level - troubleshooting (when debug=true)
LOGGER.debug("Treblle API response: 200");
LOGGER.debug("Request body exceeds 2MB limit, truncating for telemetry");
```

**View logs** in your application's log files (e.g., Tomcat's `catalina.out`).

### Troubleshooting Mode

Enable debug mode to see detailed logs:

```xml
<init-param>
    <param-name>debug</param-name>
    <param-value>true</param-value>
</init-param>
```

This logs:
- HTTP requests to Treblle API
- Response status codes
- Body size limits
- Configuration values

**⚠️ Warning:** Debug mode is verbose. Only use in development or temporary troubleshooting.

---

## What Data is Sent to Treblle?

Treblle captures comprehensive API telemetry while respecting privacy:

### Request Data

```json
{
  "timestamp": "2024-01-15 10:30:45",
  "ip": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "method": "POST",
  "url": "https://api.example.com/api/users",
  "route_path": "api/users/{userId}",
  "headers": {
    "content-type": "application/json",
    "authorization": "******"  // Masked
  },
  "query": {
    "page": "1",
    "limit": "10"
  },
  "body": {
    "name": "John Doe",
    "password": "******"  // Masked
  }
}
```

### Response Data

```json
{
  "code": 201,
  "load_time": 45,  // milliseconds
  "size": 1024,     // bytes
  "headers": {
    "content-type": "application/json",
    "x-rate-limit": "100"
  },
  "body": {
    "id": "12345",
    "name": "John Doe",
    "api_key": "******"  // Masked
  }
}
```

### Server Data

```json
{
  "ip": "10.0.0.5",
  "timezone": "America/New_York",
  "software": "Apache Tomcat/9.0.54",
  "protocol": "HTTP/1.1",
  "os": {
    "name": "Linux",
    "release": "5.10.0",
    "architecture": "amd64"
  }
}
```

### Language Data

```json
{
  "name": "java",
  "version": "11.0.12"
}
```

### Error Data (if applicable)

```json
{
  "errors": [
    {
      "source": "onError",
      "type": "java.lang.NullPointerException",
      "message": "User not found",
      "file": "UserService.java",
      "line": 42
    }
  ]
}
```

### What is NOT Sent

❌ **Source code** - Never captured
❌ **Environment variables** - Not transmitted
❌ **File system data** - Only request/response data
❌ **Database queries** - Not intercepted
❌ **Unmasked sensitive data** - Masked before transmission

---

## Supported Environments

### Servlet Containers

| Container | Minimum Version | Status |
|-----------|----------------|--------|
| **Apache Tomcat** | 8.5+ | ✅ Fully tested |
| **Eclipse Jetty** | 9.4+ | ✅ Fully tested |
| **WildFly** | 10+ | ✅ Compatible |
| **JBoss EAP** | 7+ | ✅ Compatible |
| **GlassFish** | 5+ | ✅ Compatible |
| **Payara** | 5+ | ✅ Compatible |
| **IBM WebSphere** | 9+ | ✅ Compatible |
| **Oracle WebLogic** | 12c+ | ✅ Compatible |

### JAX-RS Implementations

| Implementation | Minimum Version | Status |
|----------------|----------------|--------|
| **Jersey** | 2.x | ✅ Fully tested |
| **RESTEasy** | 3.x | ✅ Compatible |
| **Apache CXF** | 3.x | ✅ Compatible |
| **Any JAX-RS 2.1+ implementation** | 2.1+ | ✅ Should work |

### Frameworks

| Framework | Setup Method |
|-----------|-------------|
| **Spring Boot** (embedded Tomcat) | Servlet Filter via `FilterRegistrationBean` |
| **Spring Boot** (standalone) | JAX-RS Filter or Servlet Filter |
| **Micronaut** | HTTP Filter (requires custom adapter) |
| **Quarkus** | JAX-RS Filter |
| **Dropwizard** | Jersey Filter |
| **Play Framework** | Filter (requires custom adapter) |

---

## Troubleshooting

### Common Issues

#### Issue: "Failed to initialize Treblle SDK: Treblle SDK Token is required"

**Cause:** Missing `sdkToken` parameter in configuration.

**Solution:**

1. Verify `sdkToken` is set in your `web.xml` or JAX-RS configuration
2. Check for typos in parameter name (case-sensitive)
3. Ensure value is not empty or null

```xml
<!-- ✅ Correct -->
<init-param>
    <param-name>sdkToken</param-name>
    <param-value>your_token_here</param-value>
</init-param>

<!-- ❌ Wrong - parameter name typo -->
<init-param>
    <param-name>sdk_token</param-name>
    <param-value>your_token_here</param-value>
</init-param>
```

---

#### Issue: Large file uploads causing memory issues

**Cause:** Request body caching exceeds available memory.

**Solutions:**

**Option 1:** Exclude upload endpoints from monitoring

```xml
<filter-mapping>
    <filter-name>TreblleServletFilter</filter-name>
    <url-pattern>/api/*</url-pattern>
</filter-mapping>

<!-- Don't monitor file uploads -->
<filter-mapping>
    <filter-name>TreblleServletFilter</filter-name>
    <url-pattern>/api/upload</url-pattern>
    <dispatcher>REQUEST</dispatcher>
    <enabled>false</enabled>
</filter-mapping>
```

**Option 2:** Reduce body size limit

```xml
<init-param>
    <param-name>maxBodySizeInBytes</param-name>
    <param-value>524288</param-value> <!-- 512KB -->
</init-param>
```

---

#### Issue: Not seeing data in Treblle dashboard

**Possible Causes:**

1. **Wrong credentials** - Verify `sdkToken` and `apiKey`
2. **Network blocked** - Check firewall allows HTTPS to `*.treblle.com`
3. **Wrong project** - Ensure you're viewing the correct project in dashboard

**Troubleshooting Steps:**

1. **Enable debug mode:**

```xml
<init-param>
    <param-name>debug</param-name>
    <param-value>true</param-value>
</init-param>
```

2. **Check logs** for errors:

```bash
tail -f /path/to/tomcat/logs/catalina.out | grep -i treblle
```

3. **Verify network connectivity:**

```bash
curl -I https://rocknrolla.treblle.com
```

4. **Test with a simple request:**

```bash
curl -X GET http://localhost:8080/api/test
```

---

#### Issue: `ClassCastException` in JAX-RS filter

**Cause:** Fixed in version 1.0.6. Earlier versions had unchecked type casts.

**Solution:** Upgrade to latest version:

```xml
<dependency>
    <groupId>com.treblle</groupId>
    <artifactId>treblle-javax</artifactId>
    <version>1.0.6</version>
</dependency>
```

---

#### Issue: `NullPointerException` on startup (JAX-RS)

**Cause:** `@Context` injection happens after constructor. Earlier versions tried to use Configuration in constructor.

**Solution:** Upgrade to version 1.0.6+ which uses lazy initialization.

---

#### Issue: Memory leak / thread leak warnings

**Cause:** Fixed in version 1.0.6. Earlier versions didn't implement proper cleanup.

**Solution:**

1. Upgrade to version 1.0.6+
2. Verify `destroy()` method is being called on shutdown

**Check Tomcat logs** for cleanup confirmation:

```
DEBUG - Shutting down Treblle service
DEBUG - HTTP client closed successfully
```

---

#### Issue: Response body not reaching client

**Cause:** Rare edge case if `copyBodyToResponse()` fails.

**Status:** Fixed in version 1.0.6 with proper exception handling.

**Workaround (older versions):**

Disable Treblle temporarily and report the issue.

---

### Getting Help

If you encounter an issue not listed here:

1. **Check logs** with debug mode enabled
2. **Search GitHub issues**: https://github.com/Treblle/treblle-java/issues
3. **Create a new issue** with:
   - SDK version
   - Java version
   - Container/framework version
   - Minimal reproduction code
   - Full error logs

---

## Examples

### Spring Boot with Embedded Tomcat

```java
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.javax.TreblleServletFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class TreblleConfig {

    private final Environment env;

    public TreblleConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public FilterRegistrationBean<TreblleServletFilter> treblleFilter() {
        FilterRegistrationBean<TreblleServletFilter> registrationBean =
            new FilterRegistrationBean<>();

        TreblleProperties props = new TreblleProperties() {
            @Override
            public String getSdkToken() {
                return env.getProperty("treblle.sdk-token");
            }

            @Override
            public String getApiKey() {
                return env.getProperty("treblle.api-key");
            }

            @Override
            public boolean isDebug() {
                return env.getProperty("treblle.debug", Boolean.class, false);
            }
        };

        registrationBean.setFilter(new TreblleServletFilter(props));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(1);  // Run early in filter chain

        return registrationBean;
    }
}
```

**application.properties:**

```properties
treblle.sdk-token=${TREBLLE_SDK_TOKEN}
treblle.api-key=${TREBLLE_API_KEY}
treblle.debug=false
```

---

### Jersey with Grizzly Server

```java
import com.treblle.javax.TreblleContainerFilter;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {

    public static void main(String[] args) {
        ResourceConfig config = new ResourceConfig();

        // Register Treblle filter
        config.register(TreblleContainerFilter.class);

        // Configure Treblle
        config.property("sdkToken", System.getenv("TREBLLE_SDK_TOKEN"));
        config.property("apiKey", System.getenv("TREBLLE_API_KEY"));
        config.property("debug", false);

        // Register your resources
        config.packages("com.example.resources");

        // Start server
        URI baseUri = URI.create("http://localhost:8080/api/");
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, config);

        System.out.println("Server started at " + baseUri);
    }
}
```

---

### RESTEasy with WildFly

**web.xml:**

```xml
<web-app>
    <context-param>
        <param-name>resteasy.providers</param-name>
        <param-value>com.treblle.javax.TreblleContainerFilter</param-value>
    </context-param>

    <context-param>
        <param-name>sdkToken</param-name>
        <param-value>${env.TREBLLE_SDK_TOKEN}</param-value>
    </context-param>

    <context-param>
        <param-name>apiKey</param-name>
        <param-value>${env.TREBLLE_API_KEY}</param-value>
    </context-param>

    <!-- RESTEasy servlet -->
    <servlet>
        <servlet-name>Resteasy</servlet-name>
        <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
    </servlet>

    <servlet-mapping>
        <servlet-name>Resteasy</servlet-name>
        <url-pattern>/api/*</url-pattern>
    </servlet-mapping>
</web-app>
```

---

### Quarkus

**application.properties:**

```properties
treblle.sdk-token=${TREBLLE_SDK_TOKEN}
treblle.api-key=${TREBLLE_API_KEY}

quarkus.resteasy.path=/api
```

**TreblleConfig.java:**

```java
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.javax.TreblleContainerFilter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class QuarkusTreblleFilter extends TreblleContainerFilter {

    public QuarkusTreblleFilter(
            @ConfigProperty(name = "treblle.sdk-token") String sdkToken,
            @ConfigProperty(name = "treblle.api-key") String apiKey) {

        super(new TreblleProperties() {
            @Override
            public String getSdkToken() {
                return sdkToken;
            }

            @Override
            public String getApiKey() {
                return apiKey;
            }
        });
    }
}
```

---

### Dropwizard

**YourApplication.java:**

```java
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.javax.TreblleContainerFilter;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

public class YourApplication extends Application<YourConfiguration> {

    @Override
    public void run(YourConfiguration config, Environment env) {

        // Register Treblle filter
        TreblleProperties treblleConfig = new TreblleProperties() {
            @Override
            public String getSdkToken() {
                return config.getTreblleSdkToken();
            }

            @Override
            public String getApiKey() {
                return config.getTreblleApiKey();
            }
        };

        env.jersey().register(new TreblleContainerFilter(treblleConfig));

        // Register your resources
        env.jersey().register(new YourResource());
    }
}
```

---

### Docker Deployment

**Dockerfile:**

```dockerfile
FROM tomcat:9.0-jdk11

# Copy your WAR file
COPY target/your-app.war /usr/local/tomcat/webapps/ROOT.war

# Set environment variables for Treblle
ENV TREBLLE_SDK_TOKEN=""
ENV TREBLLE_API_KEY=""

EXPOSE 8080

CMD ["catalina.sh", "run"]
```

**docker-compose.yml:**

```yaml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      TREBLLE_SDK_TOKEN: ${TREBLLE_SDK_TOKEN}
      TREBLLE_API_KEY: ${TREBLLE_API_KEY}
      JAVA_OPTS: "-Xmx512m -Xms256m"
```

**Run:**

```bash
export TREBLLE_SDK_TOKEN="your_token"
export TREBLLE_API_KEY="your_key"
docker-compose up
```

---

## FAQ

### Does this impact my API performance?

**No.** Telemetry is sent asynchronously with minimal overhead:

- **< 5ms** for request/response body caching
- **Zero blocking** - transmission happens in background threads
- **Bounded resources** - limited memory (2MB) and threads (3 max)

### What if Treblle is down?

**Your API continues working normally.**

- Telemetry errors are logged but never propagate
- No retries or blocking waits
- Fails fast (3-second timeout)
- Client always receives response

### Is sensitive data automatically masked?

**Yes.** 13 common sensitive fields are masked by default:

```
password, pwd, secret, password_confirmation, cc, card_number,
ccv, ssn, credit_score, api_key, etc.
```

You can add custom masking rules via `maskedKeywords` parameter.

### Can I use this with Spring Boot?

**Yes!** Use `FilterRegistrationBean` to register the filter:

```java
@Bean
public FilterRegistrationBean<TreblleServletFilter> treblleFilter() {
    // See Examples section for full code
}
```

### Does it work with GraphQL?

**Yes.** The SDK captures all HTTP traffic, including:

- GraphQL queries/mutations
- REST APIs
- SOAP services
- Any HTTP-based protocol

### Can I exclude certain endpoints?

**Yes!** Use the `excludedPaths` parameter with glob-style wildcards:

```xml
<init-param>
    <param-name>excludedPaths</param-name>
    <param-value>/health,/metrics,/admin/*,*/internal/*</param-value>
</init-param>
```

Common use cases:
- Health checks: `/health`, `/healthz`
- Metrics: `/metrics`, `/prometheus`
- Admin panels: `/admin/*`
- Internal APIs: `*/internal/*`

**Note**: `urlPatterns` is deprecated. Use `excludedPaths` instead.

### How do I test if it's working?

1. **Make an API request** to your monitored endpoint
2. **Check Treblle dashboard** - request should appear within seconds
3. **Enable debug mode** and check logs for confirmation

### What about microservices?

**Each service needs its own SDK configuration:**

- Use the same `apiKey` across all services (groups them in one project)
- Use different `sdkToken` if you want separate projects
- Configure per service in `web.xml` or programmatically

### Can I use this in production?

**Yes.** The SDK is production-ready:

- ✅ Battle-tested by teams worldwide
- ✅ Thoroughly tested for memory leaks, thread safety, crash resilience
- ✅ Version 1.0.6+ includes all production hardening
- ✅ Fail-safe design - never impacts your API

### How much does Treblle cost?

See [Treblle pricing](https://www.treblle.com/pricing). Free tier available for small projects.

### Is the SDK open source?

**Yes.** MIT license. Source code: https://github.com/Treblle/treblle-java

---

## Contributing

We welcome contributions! Here's how you can help:

### Reporting Issues

Found a bug? [Create an issue](https://github.com/Treblle/treblle-java/issues) with:

- SDK version
- Java version
- Container/framework version
- Minimal reproduction code
- Full error logs

### Contributing Code

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feature/your-feature`
3. **Make your changes** with tests
4. **Run tests**: `mvn test`
5. **Submit a pull request**

### Development Setup

```bash
# Clone the repo
git clone https://github.com/Treblle/treblle-java.git
cd treblle-java/treblle-javax

# Build
mvn clean package

# Run tests
mvn test

# Generate JavaDoc
mvn javadoc:javadoc
```

### Code Style

- Java 8 compatibility required
- Follow existing code style
- Add JavaDoc for public methods
- Include unit tests for new features

---

## License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2024 Treblle

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## Support

### Documentation

- **Official Docs**: https://docs.treblle.com
- **API Reference**: https://docs.treblle.com/api-reference

### Community

- **GitHub Issues**: https://github.com/Treblle/treblle-java/issues
- **Treblle Dashboard**: https://app.treblle.com

### Enterprise Support

Need help with:
- Custom integrations
- On-premise deployment
- SLA guarantees
- Training and consulting

Contact: [support@treblle.com](mailto:support@treblle.com)

---

## Acknowledgments

Built with ❤️ by the Treblle team and [open source contributors](https://github.com/Treblle/treblle-java/graphs/contributors).

Special thanks to:
- Apache HttpComponents team
- Jackson JSON team
- SLF4J team
- The Java community

---

<div align="center">

**[Get Started](https://app.treblle.com) • [Documentation](https://docs.treblle.com) • [Blog](https://www.treblle.com/blog)**

Made with ☕ and Java

</div>
