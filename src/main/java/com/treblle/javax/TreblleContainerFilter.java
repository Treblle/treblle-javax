package com.treblle.javax;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.TreblleService;
import com.treblle.common.utils.PathMatcher;
import com.treblle.javax.configuration.ContainerFilterTreblleProperties;
import com.treblle.javax.infrastructure.ContainerRequestContextWrapper;
import com.treblle.javax.infrastructure.ContainerResponseContextWrapper;
import com.treblle.javax.service.TreblleServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import java.io.*;
import java.util.List;

/**
 * JAX-RS container filter that monitors HTTP requests and responses and sends telemetry data to Treblle.
 * <p>
 * This filter intercepts all JAX-RS HTTP traffic, caches request and response bodies, and asynchronously
 * sends monitoring data to the Treblle API. It supports path exclusion patterns and configurable
 * masking of sensitive data.
 * <p>
 * <b>Configuration:</b>
 * Configure this filter by setting properties in your JAX-RS Application or ResourceConfig:
 * <ul>
 *   <li>{@code sdkToken} - Your Treblle SDK token (required)</li>
 *   <li>{@code apiKey} - Your Treblle API key (required)</li>
 *   <li>{@code customTreblleEndpoint} - Custom endpoint URL (optional)</li>
 *   <li>{@code debugMode} - Enable debug logging (optional, default: false)</li>
 *   <li>{@code excludedPaths} - Comma-separated path patterns to exclude (optional)</li>
 *   <li>{@code maskedKeywords} - Additional fields to mask (optional)</li>
 * </ul>
 *
 * @see TreblleServletFilter
 * @since 1.0.0
 */
public class TreblleContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleContainerFilter.class);

    private static final String SDK_NAME = "javax-container";

    private static final String TREBLLE_EXCLUDED_PROPERTY = "treblle.excluded";

    // ThreadLocal storage for request context data
    private static final ThreadLocal<TreblleRequestData> REQUEST_DATA = new ThreadLocal<>();

    @Context
    private Configuration configuration;

    @Context
    private ResourceInfo resourceInfo;

    private volatile TreblleService treblleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Container for data collected during request processing
     */
    private static class TreblleRequestData {
        byte[] requestBody;
        Object responseEntity;
        long startTime;
        ContainerRequestContextWrapper requestWrapper;
        ContainerResponseContextWrapper responseWrapper;
        boolean excluded;
    }

    /**
     * Default constructor for JAX-RS framework instantiation.
     * <p>
     * Configuration will be loaded lazily from injected {@link Context} fields.
     */
    public TreblleContainerFilter() {
        // Constructor body is empty - initialization happens lazily
    }

    /**
     * Constructor for programmatic configuration.
     * <p>
     * Use this constructor when registering the filter programmatically
     * with custom configuration properties.
     *
     * @param treblleProperties the configuration properties
     */
    public TreblleContainerFilter(TreblleProperties treblleProperties) {
        this.treblleService = new TreblleServiceImpl(
                SDK_NAME,
                treblleProperties,
                new ObjectMapper()
        );
    }

    /**
     * Lazy initialization of TreblleService to ensure @Context fields are injected
     */
    private TreblleService getTreblleService() {
        if (treblleService == null) {
            synchronized (this) {
                if (treblleService == null) {
                    try {
                        treblleService = new TreblleServiceImpl(
                                SDK_NAME,
                                new ContainerFilterTreblleProperties(configuration),
                                new ObjectMapper()
                        );
                    } catch (IllegalStateException e) {
                        LOGGER.error("CRITICAL: Failed to initialize Treblle SDK: {}", e.getMessage(), e);
                        throw e;
                    }
                }
            }
        }
        return treblleService;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        TreblleRequestData data = new TreblleRequestData();
        REQUEST_DATA.set(data);

        try {
            // Start timing
            data.startTime = System.currentTimeMillis();

            // Check if this path should be excluded from monitoring
            String requestPath = extractRequestPath(containerRequestContext);
            List<String> excludedPaths = getTreblleService().getProperties().getExcludedPaths();

            if (PathMatcher.isExcluded(requestPath, excludedPaths)) {
                // Mark as excluded
                data.excluded = true;
                containerRequestContext.setProperty(TREBLLE_EXCLUDED_PROPERTY, Boolean.TRUE);
                return; // Skip request body caching
            }

            // Capture request body if present
            if (containerRequestContext.hasEntity()) {
                InputStream inputStream = containerRequestContext.getEntityStream();
                int maxSize = getTreblleService().getMaxBodySizeInBytes();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] chunk = new byte[1024];
                int bytesRead;
                int totalRead = 0;

                while ((bytesRead = inputStream.read(chunk)) != -1) {
                    if (totalRead + bytesRead > maxSize) {
                        // Truncate at limit
                        int remaining = maxSize - totalRead;
                        if (remaining > 0) {
                            buffer.write(chunk, 0, remaining);
                        }
                        LOGGER.debug("Request body exceeds limit, truncating at {} bytes", maxSize);
                        break;
                    }
                    buffer.write(chunk, 0, bytesRead);
                    totalRead += bytesRead;
                }

                data.requestBody = buffer.toByteArray();

                // Reset entity stream for resource method to consume
                containerRequestContext.setEntityStream(new ByteArrayInputStream(data.requestBody));
            } else {
                data.requestBody = new byte[0];
            }

            // Store request wrapper for later use
            data.requestWrapper = new ContainerRequestContextWrapper(containerRequestContext, resourceInfo);

        } catch (Exception e) {
            // Log but don't fail the request
            LOGGER.error("Error in Treblle request filter", e);
            data.requestBody = new byte[0];
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        try {
            TreblleRequestData data = REQUEST_DATA.get();
            if (data == null || data.excluded) {
                REQUEST_DATA.remove();
                return; // Skip
            }

            // Capture response entity and wrapper
            data.responseEntity = containerResponseContext.getEntity();
            data.responseWrapper = new ContainerResponseContextWrapper(containerResponseContext);

            // Calculate response time
            long responseTimeInMillis = System.currentTimeMillis() - data.startTime;

            // Serialize response entity to bytes
            byte[] responseBody = new byte[0];
            if (data.responseEntity != null) {
                try {
                    String json = objectMapper.writeValueAsString(data.responseEntity);
                    responseBody = json.getBytes("UTF-8");
                } catch (Exception e) {
                    LOGGER.debug("Could not serialize response entity", e);
                }
            }

            // Create payload while still in request scope
            TrebllePayload payload = getTreblleService().createPayload(
                    data.requestWrapper,
                    data.responseWrapper,
                    null,
                    responseTimeInMillis
            );

            // Send asynchronously (payload already contains all extracted data)
            sendToTreblle(payload, data.requestBody, responseBody);

        } catch (Exception exception) {
            // NEVER let Treblle errors crash the response
            LOGGER.error("Error in Treblle response filter", exception);
        } finally {
            // Always clean up ThreadLocal
            REQUEST_DATA.remove();
        }
    }

    private void sendToTreblle(final TrebllePayload payload, final byte[] requestBody, final byte[] responseBody) {
        // Send asynchronously to avoid blocking the response
        new Thread(() -> {
            try {
                getTreblleService().maskAndSendPayload(
                        payload,
                        requestBody != null ? requestBody : new byte[0],
                        responseBody,
                        null
                );
            } catch (Exception e) {
                LOGGER.error("Error sending data to Treblle", e);
            }
        }, "treblle-async").start();
    }

    /**
     * Cleanup method called when filter is being destroyed
     */
    public void destroy() {
        if (treblleService instanceof TreblleServiceImpl) {
            ((TreblleServiceImpl) treblleService).shutdown();
        }
    }

    /**
     * Extract request path from JAX-RS context.
     * Uses the path without query string.
     *
     * @param context The container request context
     * @return The request path (e.g., "/api/users")
     */
    private String extractRequestPath(ContainerRequestContext context) {
        // Get path without query string - already relative to application
        return context.getUriInfo().getPath();
    }

}
