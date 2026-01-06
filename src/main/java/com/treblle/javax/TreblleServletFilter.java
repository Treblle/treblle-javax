package com.treblle.javax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.common.utils.PathMatcher;
import com.treblle.javax.configuration.ServletFilterTreblleProperties;
import com.treblle.javax.infrastructure.ContentCachingRequestWrapper;
import com.treblle.javax.infrastructure.ContentCachingResponseWrapper;
import com.treblle.javax.service.TreblleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class TreblleServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleServletFilter.class);

    private static final String SDK_NAME = "javax-servlet";

    private TreblleService treblleService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            this.treblleService = new TreblleServiceImpl(
                    SDK_NAME,
                    new ServletFilterTreblleProperties(filterConfig),
                    new ObjectMapper()
            );
        } catch (IllegalStateException e) {
            // Re-throw as ServletException so container knows filter failed to initialize
            throw new ServletException("Failed to initialize Treblle SDK: " + e.getMessage(), e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        int maxBodySize = treblleService.getMaxBodySizeInBytes();

        final ContentCachingRequestWrapper cachingRequest =
                new ContentCachingRequestWrapper((HttpServletRequest) request, maxBodySize);
        final ContentCachingResponseWrapper cachingResponse =
                new ContentCachingResponseWrapper((HttpServletResponse) response, maxBodySize);

        // Check if this path should be excluded from monitoring
        String requestPath = extractRequestPath(cachingRequest);
        List<String> excludedPaths = treblleService.getProperties().getExcludedPaths();

        if (PathMatcher.isExcluded(requestPath, excludedPaths)) {
            // Skip Treblle monitoring - just pass through to next filter
            filterChain.doFilter(cachingRequest, cachingResponse);
            return;
        }

        final long start = System.currentTimeMillis();
        Exception potentialException = null;

        try {
            filterChain.doFilter(cachingRequest, cachingResponse);
        } catch (Exception exception) {
            potentialException = exception;
            // Don't re-throw yet - need to restore response first
        } finally {
            final long responseTimeInMillis = System.currentTimeMillis() - start;
            final byte[] requestBody = cachingRequest.getContentAsByteArray();
            final byte[] responseBody = cachingResponse.getContentAsByteArray();

            // CRITICAL: Restore response body FIRST before any other operations
            // Wrap in try-catch to prevent masking original exception
            boolean responseRestored = false;
            try {
                cachingResponse.copyBodyToResponse();
                responseRestored = true;
            } catch (IOException copyException) {
                // Log but don't override original exception
                LOGGER.error("CRITICAL: Failed to restore response body to client", copyException);

                // If there was no original exception, this IS the problem
                if (potentialException == null) {
                    potentialException = copyException;
                } else {
                    // Add as suppressed exception to preserve both
                    potentialException.addSuppressed(copyException);
                }
            }

            // Only send telemetry if response was successfully restored
            if (responseRestored) {
                try {
                    TrebllePayload payload = treblleService.createPayload(
                            cachingRequest,
                            cachingResponse,
                            potentialException,
                            responseTimeInMillis
                    );
                    treblleService.maskAndSendPayload(payload, requestBody, responseBody, potentialException);
                } catch (Exception telemetryException) {
                    // Never let telemetry errors crash the request
                    LOGGER.error("An error occurred while sending data to Treblle", telemetryException);
                }
            }
        }

        // Now re-throw original exception if there was one
        if (potentialException != null) {
            if (potentialException instanceof IOException) {
                throw (IOException) potentialException;
            } else if (potentialException instanceof ServletException) {
                throw (ServletException) potentialException;
            } else if (potentialException instanceof RuntimeException) {
                throw (RuntimeException) potentialException;
            } else {
                throw new ServletException(potentialException);
            }
        }
    }

    @Override
    public void destroy() {
        if (treblleService instanceof TreblleServiceImpl) {
            ((TreblleServiceImpl) treblleService).shutdown();
        }
    }

    /**
     * Extract the request path for pattern matching.
     * Uses servlet request URI without context path.
     *
     * @param request The HTTP servlet request
     * @return The request path (e.g., "/api/users")
     */
    private String extractRequestPath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Remove context path if present
        if (contextPath != null && !contextPath.isEmpty() && requestURI.startsWith(contextPath)) {
            return requestURI.substring(contextPath.length());
        }

        return requestURI;
    }

}
