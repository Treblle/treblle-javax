package com.treblle.javax.configuration;

import com.treblle.common.configuration.TreblleProperties;

import javax.ws.rs.core.Configuration;
import java.util.List;
import java.util.Optional;

public class ContainerFilterTreblleProperties implements TreblleProperties {

    private static final String ENDPOINT = "endpoint";
    private static final String SDK_TOKEN = "sdkToken";
    private static final String API_KEY = "apiKey";
    private static final String DEBUG = "debug";
    private static final String URL_PATTERNS = "urlPatterns";
    private static final String MASKING_KEYWORDS = "maskedKeywords";
    private static final String EXCLUDED_PATHS = "excludedPaths";

    private final Configuration filterConfig;

    public ContainerFilterTreblleProperties(Configuration filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public String getEndpoint() {
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
    public boolean isDebug() {
        Object value = filterConfig.getProperty(DEBUG);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    @Override
    public List<String> getUrlPatterns() {
        Object value = filterConfig.getProperty(URL_PATTERNS);
        if (value instanceof String) {
            return List.of(((String) value).split(","));
        }
        return List.of();
    }

    @Override
    public List<String> getMaskingKeywords() {
        Object value = filterConfig.getProperty(MASKING_KEYWORDS);
        if (value instanceof String) {
            return java.util.Arrays.stream(((String) value).split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    @Override
    public List<String> getMaskedKeywords() {
        return getMaskingKeywords();
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
        return List.of();
    }

}
