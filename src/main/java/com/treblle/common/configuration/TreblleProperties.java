package com.treblle.common.configuration;

import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Treblle SDK.
 * <p>
 * This interface defines all configuration options for monitoring HTTP requests and responses
 * with Treblle. Implementations can read these values from various sources such as
 * servlet filter configuration, JAX-RS configuration, or custom property providers.
 *
 * @since 1.0.0
 */
public interface TreblleProperties {

    /**
     * Returns the custom Treblle API endpoint URL.
     * <p>
     * Use this for self-hosted Treblle instances or custom routing.
     * If not specified, the default Treblle cloud endpoint will be used.
     *
     * @return custom endpoint URL, or {@code null} to use default
     * @since 2.0.0
     */
    default String getCustomTreblleEndpoint() {
        return null;
    }

    /**
     * Returns the Treblle SDK token for authentication.
     * <p>
     * This is a required configuration value. Get your SDK token from
     * the Treblle dashboard at https://app.treblle.com
     *
     * @return the SDK token
     * @since 1.0.0
     */
    String getSdkToken();

    /**
     * Returns the Treblle API key for authentication.
     * <p>
     * This is a required configuration value. Get your API key from
     * the Treblle dashboard at https://app.treblle.com
     *
     * @return the API key
     * @since 1.0.0
     */
    String getApiKey();

    /**
     * Returns whether debug mode is enabled.
     * <p>
     * When enabled, the SDK logs HTTP request and response details to help
     * with troubleshooting. This should be disabled in production environments.
     *
     * @return {@code true} if debug mode is enabled, {@code false} otherwise
     * @since 2.0.0
     */
    default boolean isDebugMode() {
        return false;
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
        return Collections.emptyList();
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
        return Collections.emptyList();
    }

    /**
     * Returns the connection timeout for HTTP requests to Treblle API.
     * <p>
     * This timeout controls how long to wait when establishing a connection
     * to the Treblle API endpoint.
     *
     * @return connection timeout in seconds, default is 3 seconds
     * @since 1.0.0
     */
    default int getConnectTimeoutInSeconds() {
        return 3;
    }

    /**
     * Returns the read timeout for HTTP requests to Treblle API.
     * <p>
     * This timeout controls how long to wait for data to be received
     * from the Treblle API endpoint after connection is established.
     *
     * @return read timeout in seconds, default is 3 seconds
     * @since 1.0.0
     */
    default int getReadTimeoutInSeconds() {
        return 3;
    }

    /**
     * Returns the maximum body size to capture for telemetry.
     * <p>
     * Request and response bodies larger than this size will be truncated
     * to prevent excessive memory usage. This helps protect against
     * memory issues when processing large payloads.
     *
     * @return maximum body size in bytes, default is 2MB (2097152 bytes)
     * @since 1.0.0
     */
    default int getMaxBodySizeInBytes() {
        return 2 * 1024 * 1024;  // 2MB default
    }

    /**
     * Returns the core pool size for the async telemetry thread pool.
     * <p>
     * This is the minimum number of worker threads kept alive in the pool,
     * even when idle. Increase for high-throughput environments.
     *
     * @return core pool size, default is 1
     * @since 2.0.3
     */
    default int getThreadPoolCoreSize() {
        return 1;
    }

    /**
     * Returns the maximum pool size for the async telemetry thread pool.
     * <p>
     * This is the maximum number of worker threads allowed in the pool.
     * When the queue is full, new threads are created up to this limit.
     *
     * @return maximum pool size, default is 3
     * @since 2.0.3
     */
    default int getThreadPoolMaxSize() {
        return 3;
    }

    /**
     * Returns the queue size for the async telemetry thread pool.
     * <p>
     * This is the maximum number of tasks that can be queued for execution.
     * When both the queue and max threads are saturated, the CallerRunsPolicy
     * kicks in and the calling thread executes the task.
     *
     * @return queue size, default is 100
     * @since 2.0.3
     */
    default int getThreadPoolQueueSize() {
        return 100;
    }

}
