# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **treblle-javax** SDK - a standalone JavaX implementation for Treblle API monitoring. It provides two filter implementations for intercepting HTTP requests/responses and sending telemetry data to Treblle:

1. **TreblleServletFilter** - For JavaX Servlet API 3.1.0 (traditional servlet containers)
2. **TreblleContainerFilter** - For JavaX RS API 2.1.1 (JAX-RS/Jersey applications)

**Key Dependencies:**
- `treblle-common` (1.0.5) - Shared DTOs, utilities, and base service logic
- `javax.servlet-api` (3.1.0, provided scope)
- `javax.ws.rs-api` (2.1.1)
- `httpclient5` (5.1) - Apache HTTP client for async telemetry transmission

## Build & Development Commands

```bash
# Build the project
mvn clean package

# Install to local Maven repository
mvn clean install

# Run tests
mvn test

# Skip tests during build
mvn clean package -DskipTests

# Generate sources JAR
mvn source:jar

# Generate javadoc JAR
mvn javadoc:jar

# Sign artifacts (for publishing)
mvn clean verify -Pci-cd

# Deploy to Maven Central
mvn clean deploy -Pci-cd
```

## Architecture

### Dual Filter Design Pattern

The SDK uses an **adapter pattern** to support both servlet and JAX-RS environments with a unified core:

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Application                        │
└───────┬─────────────────────────────────────────┬───────────┘
        │                                         │
        │ (Servlet Container)        (JAX-RS Container)
        │                                         │
┌───────▼──────────────┐              ┌──────────▼────────────┐
│ TreblleServletFilter │              │TreblleContainerFilter │
│  implements Filter   │              │ implements            │
│                      │              │ ContainerRequestFilter│
│ - init()             │              │ ContainerResponseFilter│
│ - doFilter()         │              │                       │
│ - destroy()          │              │ - filter(request)     │
│                      │              │ - filter(req, resp)   │
└───────┬──────────────┘              └──────────┬────────────┘
        │                                        │
        │ Uses                                Uses│
        │                                        │
┌───────▼────────────────────────────────────────▼───────────┐
│              TreblleServiceImpl                             │
│  extends AbstractTreblleService (from treblle-common)       │
│                                                             │
│  - createPayload()   (inherited)                            │
│  - maskAndSendPayload()   (inherited)                       │
│  - sendPayload()   (implemented: async HTTP POST)           │
└─────────────────────────────────────────────────────────────┘
```

### Request/Response Caching Strategy

Both filters cache the request/response bodies to enable:
1. **Treblle telemetry** - Send payload to Treblle API
2. **Passthrough** - Continue normal request processing

**Servlet Implementation:**
- `ContentCachingRequestWrapper` - Custom servlet wrapper that caches input stream
- `ContentCachingResponseWrapper` - Custom servlet wrapper that caches output stream
- Calls `cachingResponse.copyBodyToResponse()` in finally block to restore response

**JAX-RS Implementation:**
- Uses `ContainerRequestContext.setProperty()` to store request body
- Uses `CaptureOutputStream` (tee pattern) to duplicate response stream
- Wraps contexts with `ContainerRequestContextWrapper` and `ContainerResponseContextWrapper`

### Configuration Abstraction

Configuration is loaded differently per environment:

- **ServletFilterTreblleProperties** - Reads from `FilterConfig.getInitParameter()`
- **ContainerFilterTreblleProperties** - Reads from JAX-RS `Configuration`

Both implement `com.treblle.common.configuration.TreblleProperties`:
- `apiKey` - Treblle API key (required)
- `projectId` - Treblle project ID (required)
- `endpoint` - Custom Treblle endpoint (optional)
- `debug` - Enable debug logging (optional)
- `excludedPaths` - Comma-separated path patterns to exclude from monitoring with glob wildcards (optional, since 1.0.6)
- `urlPatterns` - DEPRECATED: Non-functional in earlier versions, replaced by excludedPaths (will be removed in 2.0.0)
- `maskedKeywords` - Comma-separated field names to mask (optional)

### Path Exclusion (New in 1.0.6)

The `excludedPaths` property enables excluding specific endpoints from monitoring:
- Configured via comma-separated glob patterns (e.g., `/health,/admin/*,*/internal`)
- Matching logic implemented in `PathMatcher.isExcluded()` utility
- Early return in filter execution if path matches (before telemetry processing)
- Default behavior: Monitor all endpoints (empty exclusion list)
- Supports wildcards: exact match (`/health`), prefix (`admin/*`), suffix (`*/internal`), middle (`/api/*/debug`)

Filter integration:
- **TreblleServletFilter**: Checks after wrapping, extracts path via `request.getRequestURI()` minus context path
- **TreblleContainerFilter**: Checks at request filter start, uses `context.getUriInfo().getPath()`, communicates state via request property

### Async Telemetry Pattern

`TreblleServiceImpl.sendPayload()` uses `CompletableFuture.runAsync()` to avoid blocking the request thread. The HTTP client is created per-request with:
- Custom connect/read timeouts from config
- Automatic retries disabled
- Optional request/response logging interceptors (debug mode)

## Important Implementation Details

### Filter Execution Flow

**Servlet Filter (`doFilter`):**
```java
1. Wrap request/response in caching wrappers
2. Start timing
3. filterChain.doFilter(cachingRequest, cachingResponse)
4. In finally block:
   a. Calculate response time
   b. Extract cached request/response bodies
   c. Copy response body back to original response (CRITICAL!)
   d. Create payload from wrappers
   e. Mask and send payload async
```

**JAX-RS Filter:**
```java
Request phase:
1. Read and cache request body from entity stream
2. Store in request property
3. Reset entity stream with cached bytes

Response phase:
1. Wrap entity stream with CaptureOutputStream
2. Extract cached request body from property
3. Calculate response time
4. Create payload from wrapper contexts
5. Mask and send payload async
```

### Critical Considerations

- **Response body restoration**: `cachingResponse.copyBodyToResponse()` must be called or client receives empty response
- **Stream consumption**: Both request/response streams can only be read once - caching is mandatory
- **Exception handling**: Exceptions during Treblle processing are logged but never propagate to client
- **Thread safety**: CompletableFuture ensures telemetry doesn't block request processing
- **SDK naming**: Each filter reports a different SDK name ("javax-servlet" vs "javax-container")

### Dependency on treblle-common

The `treblle-common` artifact (not in this repo) provides:
- `AbstractTreblleService` - Base class with payload creation, masking, and sending logic
- DTOs: `TrebllePayload`, `Request`, `Response`, `Server`, etc.
- `RequestWrapper`/`ResponseWrapper` interfaces
- Utility classes for HTTP, JSON masking, etc.

When modifying this SDK, changes to payload structure or masking logic likely belong in `treblle-common`.

## Version and Publishing

- Current version: **1.0.5**
- Java target: **1.8** (for broad compatibility)
- Published to: Maven Central
- Original repository: https://github.com/Treblle/treblle-java (this is an imported copy)

The `pom.xml` includes profiles for signing (`ci-cd`, `ci-cd-local`) and publishing to Sonatype OSSRH.
