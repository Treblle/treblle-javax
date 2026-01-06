package com.treblle.common.configuration;

import java.util.List;

public interface TreblleProperties {

    default String getEndpoint() {
        return null;
    }

    String getSdkToken();

    String getApiKey();

    default boolean isDebug() {
        return false;
    }

    /**
     * @deprecated Use {@link #getExcludedPaths()} instead. This method will be removed in version 2.0.0.
     *             Note: This property was never functional in earlier versions.
     */
    @Deprecated
    default List<String> getUrlPatterns() {
        return List.of();
    }

    /**
     * Returns list of path patterns to exclude from Treblle monitoring.
     * <p>
     * Supports glob-style wildcards:
     * <ul>
     *   <li>{@code /health} - Exact match</li>
     *   <li>{@code admin/*} - Prefix wildcard (all paths starting with {@code admin/})</li>
     *   <li>{@code * /internal} - Suffix wildcard (all paths ending with {@code /internal})</li>
     *   <li>{@code /api/* /debug} - Middle wildcard</li>
     *   <li>{@code *} - Match all paths (excludes everything)</li>
     * </ul>
     *
     * @return List of exclusion patterns (empty list = monitor all paths)
     * @since 1.0.6
     */
    default List<String> getExcludedPaths() {
        return List.of();
    }

    /**
     * @deprecated Use {@code maskedKeywords} configuration property instead. This method name will be updated in 2.0.0.
     */
    @Deprecated
    default List<String> getMaskingKeywords() {
        return List.of();
    }

    /**
     * Returns list of field names to mask in request/response bodies.
     * <p>
     * Supports exact matches and wildcard patterns (e.g., {@code api_.*}).
     *
     * @return List of field names/patterns to mask
     * @since 1.0.6
     */
    default List<String> getMaskedKeywords() {
        return getMaskingKeywords(); // Delegate to deprecated method for backwards compatibility
    }

    default int getConnectTimeoutInSeconds() {
        return 3;
    }

    default int getReadTimeoutInSeconds() {
        return 3;
    }

    default int getMaxBodySizeInBytes() {
        return 2 * 1024 * 1024;  // 2MB default
    }

}
