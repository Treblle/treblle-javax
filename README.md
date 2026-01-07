# Treblle - API Intelligence Platform

[![Treblle API Intelligence](https://github.com/user-attachments/assets/b268ae9e-7c8a-4ade-95da-b4ac6fce6eea)](https://treblle.com)

[Website](http://treblle.com/) • [Documentation](https://docs.treblle.com/) • [Pricing](https://treblle.com/pricing)

Treblle is an API intelligence platfom that helps developers, teams and organizations understand their APIs from a single integration point.

---

## Treblle Javax SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.treblle/treblle-javax.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.treblle/treblle-javax)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-1.8%2B-blue.svg)](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)

### Requirements

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

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.treblle</groupId>
    <artifactId>treblle-javax</artifactId>
    <version>2.0.2</version>
</dependency>
```

### Gradle

Add the dependency to your `build.gradle`:

```gradle
implementation 'com.treblle:treblle-javax:2.0.2'
```

### Gradle (Kotlin DSL)

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("com.treblle:treblle-javax:2.0.2")
```

---

## Quick Start

1. Sign up at [treblle.com](https://treblle.com)
2. Create a new API
3. Copy your **SDK Token** and **API Key**
4. Replace the placeholders in the configuration below

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
            <param-name>debugMode</param-name>
            <param-value>false</param-value>
        </init-param>
    </filter>

</web-app>
```


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
        props.put("debugMode", false);

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
            public boolean isDebugMode() {
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
| `customTreblleEndpoint` | String | `null` | Custom Treblle endpoint URL (for self-hosted) |
| `debugMode` | Boolean | `false` | Enable debug logging (logs HTTP requests/responses) |
| `excludedPaths` | String | `""` | Comma-separated path patterns to EXCLUDE (supports wildcards: `/health`, `admin/*`) |
| `maskedKeywords` | String | `""` | Additional field names to mask (comma-separated) |
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

## Integration Examples

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
            public boolean isDebugMode() {
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
        config.property("debugMode", false);

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

## Getting Help

If you continue to experience issues:

1. Enable `debugMode: true` and check console output
2. Verify your SDK token and API key are correct in Treblle dashboard
3. Test with a simple endpoint first
4. Check [Treblle documentation](https://docs.treblle.com) for the latest updates
5. Contact support at <https://treblle.com> or email support@treblle.com

## Support

If you have problems of any kind feel free to reach out via <https://treblle.com> or email support@treblle.com and we'll do our best to help you out.

## License

Copyright 2025, Treblle Inc. Licensed under the MIT license:
http://www.opensource.org/licenses/mit-license.php
