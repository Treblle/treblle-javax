package com.treblle.common.utils;

import java.util.List;

/**
 * Utility class for matching request paths against glob-style exclusion patterns.
 * <p>
 * Supports the following pattern types:
 * <ul>
 *   <li>Exact match: {@code /health} matches only {@code /health}</li>
 *   <li>Prefix wildcard: {@code admin/*} matches {@code /admin/users}, {@code /admin/settings}, etc.</li>
 *   <li>Suffix wildcard: {@code * /internal} matches {@code /api/internal}, {@code /v1/internal}, etc.</li>
 *   <li>Middle wildcard: {@code /api/* /debug} matches {@code /api/v1/debug}, {@code /api/v2/debug}, etc.</li>
 *   <li>Match all: {@code *} matches any path</li>
 * </ul>
 *
 * @since 1.0.6
 */
public class PathMatcher {

    private PathMatcher() {
        // Prevent instantiation
    }

    /**
     * Check if a request path should be excluded from monitoring based on exclusion patterns.
     *
     * @param requestPath     The request path to check (e.g., "/api/users/123")
     * @param excludePatterns List of glob-style exclusion patterns
     * @return {@code true} if the path matches any exclusion pattern and should be excluded, {@code false} otherwise
     */
    public static boolean isExcluded(String requestPath, List<String> excludePatterns) {
        // Null or empty patterns mean nothing is excluded
        if (excludePatterns == null || excludePatterns.isEmpty()) {
            return false;
        }

        // Null or empty path should not be excluded
        if (requestPath == null || requestPath.isEmpty()) {
            return false;
        }

        // Normalize the request path
        String normalizedPath = normalizePath(requestPath);

        // Check if path matches any exclusion pattern
        for (String pattern : excludePatterns) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }

            String normalizedPattern = normalizePath(pattern);
            if (matchesPattern(normalizedPath, normalizedPattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a path matches a single glob-style pattern.
     * <p>
     * Pattern matching rules:
     * <ul>
     *   <li>Exact match: Pattern without wildcards must match exactly</li>
     *   <li>{@code *} wildcard matches zero or more characters (including {@code /})</li>
     *   <li>Multiple wildcards are supported</li>
     * </ul>
     *
     * @param path    The normalized path to test
     * @param pattern The normalized glob pattern
     * @return {@code true} if the path matches the pattern, {@code false} otherwise
     */
    private static boolean matchesPattern(String path, String pattern) {
        // Handle exact match (no wildcards)
        if (!pattern.contains("*")) {
            return path.equals(pattern);
        }

        // Handle wildcard-only pattern
        if (pattern.equals("*")) {
            return true;
        }

        // Split pattern by wildcard to get segments
        String[] segments = pattern.split("\\*", -1);

        int pathIndex = 0;

        // Check first segment (before first wildcard)
        if (!segments[0].isEmpty()) {
            if (!path.startsWith(segments[0])) {
                return false;
            }
            pathIndex = segments[0].length();
        }

        // Check middle segments
        for (int i = 1; i < segments.length - 1; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                continue; // Multiple consecutive wildcards
            }

            int index = path.indexOf(segment, pathIndex);
            if (index == -1) {
                return false; // Segment not found
            }
            pathIndex = index + segment.length();
        }

        // Check last segment (after last wildcard)
        String lastSegment = segments[segments.length - 1];
        if (!lastSegment.isEmpty()) {
            if (!path.endsWith(lastSegment)) {
                return false;
            }
            // Ensure the last segment appears after the previous segments
            int expectedIndex = path.length() - lastSegment.length();
            if (expectedIndex < pathIndex) {
                return false;
            }
        }

        return true;
    }

    /**
     * Normalize a path by ensuring it starts with a forward slash and removing trailing slashes.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code api/users} → {@code /api/users}</li>
     *   <li>{@code /api/users/} → {@code /api/users}</li>
     *   <li>{@code /} → {@code /}</li>
     * </ul>
     *
     * @param path The path to normalize
     * @return The normalized path
     */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }

        // Ensure leading slash
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        // Remove trailing slash (except for root "/")
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
