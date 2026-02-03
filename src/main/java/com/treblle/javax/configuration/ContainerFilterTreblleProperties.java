package com.treblle.javax.configuration;

import com.treblle.common.configuration.TreblleProperties;

import javax.ws.rs.core.Configuration;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link TreblleProperties} that reads configuration from JAX-RS configuration properties.
 * <p>
 * This class is used by {@link com.treblle.javax.TreblleContainerFilter} to load configuration
 * from JAX-RS application properties or ResourceConfig.
 *
 * @since 1.0.0
 */
public class ContainerFilterTreblleProperties implements TreblleProperties {

    private static final String ENDPOINT = "customTreblleEndpoint";
    private static final String SDK_TOKEN = "sdkToken";
    private static final String API_KEY = "apiKey";
    private static final String DEBUG = "debugMode";
    private static final String MASKED_KEYWORDS = "maskedKeywords";
    private static final String EXCLUDED_PATHS = "excludedPaths";
    private static final String THREAD_POOL_CORE_SIZE = "threadPoolCoreSize";
    private static final String THREAD_POOL_MAX_SIZE = "threadPoolMaxSize";
    private static final String THREAD_POOL_QUEUE_SIZE = "threadPoolQueueSize";

    private final Configuration filterConfig;

    /**
     * Creates a new instance that reads configuration from the given JAX-RS configuration.
     *
     * @param filterConfig the JAX-RS configuration containing properties
     */
    public ContainerFilterTreblleProperties(Configuration filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public String getCustomTreblleEndpoint() {
        Object value = filterConfig.getProperty(ENDPOINT);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    @Override
    public String getSdkToken() {
        Object value = filterConfig.getProperty(SDK_TOKEN);
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("Treblle SDK Token is required and must be a String");
    }

    @Override
    public String getApiKey() {
        Object value = filterConfig.getProperty(API_KEY);
        if (value instanceof String) {
            return (String) value;
        }
        throw new IllegalStateException("Treblle API Key is required and must be a String");
    }

    @Override
    public boolean isDebugMode() {
        Object value = filterConfig.getProperty(DEBUG);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    @Override
    public List<String> getMaskedKeywords() {
        Object value = filterConfig.getProperty(MASKED_KEYWORDS);
        if (value instanceof String) {
            return java.util.Arrays.stream(((String) value).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getExcludedPaths() {
        Object value = filterConfig.getProperty(EXCLUDED_PATHS);
        if (value instanceof String) {
            return java.util.Arrays.stream(((String) value).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public int getThreadPoolCoreSize() {
        Object value = filterConfig.getProperty(THREAD_POOL_CORE_SIZE);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 1;
    }

    @Override
    public int getThreadPoolMaxSize() {
        Object value = filterConfig.getProperty(THREAD_POOL_MAX_SIZE);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 3;
    }

    @Override
    public int getThreadPoolQueueSize() {
        Object value = filterConfig.getProperty(THREAD_POOL_QUEUE_SIZE);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 100;
    }

}
