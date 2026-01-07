package com.treblle.javax.configuration;

import com.treblle.common.configuration.TreblleProperties;

import javax.servlet.FilterConfig;
import java.util.*;

/**
 * Implementation of {@link TreblleProperties} that reads configuration from servlet filter init parameters.
 * <p>
 * This class is used by {@link com.treblle.javax.TreblleServletFilter} to load configuration
 * from web.xml or programmatic filter registration.
 *
 * @since 1.0.0
 */
public class ServletFilterTreblleProperties implements TreblleProperties {

    private static final String ENDPOINT = "customTreblleEndpoint";
    private static final String SDK_TOKEN = "sdkToken";
    private static final String API_KEY = "apiKey";
    private static final String DEBUG = "debugMode";
    private static final String MASKED_KEYWORDS = "maskedKeywords";
    private static final String EXCLUDED_PATHS = "excludedPaths";

    private final FilterConfig filterConfig;

    /**
     * Creates a new instance that reads configuration from the given filter config.
     *
     * @param filterConfig the servlet filter configuration containing init parameters
     */
    public ServletFilterTreblleProperties(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public String getCustomTreblleEndpoint() {
        return filterConfig.getInitParameter(ENDPOINT);
    }

    @Override
    public String getSdkToken() {
        return filterConfig.getInitParameter(SDK_TOKEN);
    }

    @Override
    public String getApiKey() {
        return filterConfig.getInitParameter(API_KEY);
    }

    @Override
    public boolean isDebugMode() {
        return Optional.ofNullable(filterConfig.getInitParameter(DEBUG)).map(Boolean::parseBoolean).orElse(false);
    }

    @Override
    public List<String> getMaskedKeywords() {
        return Optional.ofNullable(filterConfig.getInitParameter(MASKED_KEYWORDS))
                .map(keywords -> Arrays.asList(keywords.split(",")))
                .map(list -> list.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<String> getExcludedPaths() {
        return Optional.ofNullable(filterConfig.getInitParameter(EXCLUDED_PATHS))
                .map(patterns -> Arrays.asList(patterns.split(",")))
                .map(list -> list.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(java.util.stream.Collectors.toList()))
                .orElse(Collections.emptyList());
    }

}
