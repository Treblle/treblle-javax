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
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TreblleContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleContainerFilter.class);

    private static final String SDK_NAME = "javax-container";

    private static final String REQUEST_BODY_PROPERTY = "requestBody";
    private static final String START_TIME_PROPERTY = "startTime";
    private static final String TREBLLE_EXCLUDED_PROPERTY = "treblle.excluded";

    @Context
    private Configuration configuration;

    @Context
    private ResourceInfo resourceInfo;

    private volatile TreblleService treblleService;

    public TreblleContainerFilter() {
        // Constructor body is empty - initialization happens lazily
    }

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
        try {
            // Check if this path should be excluded from monitoring
            String requestPath = extractRequestPath(containerRequestContext);
            List<String> excludedPaths = getTreblleService().getProperties().getExcludedPaths();

            if (PathMatcher.isExcluded(requestPath, excludedPaths)) {
                // Mark as excluded so response filter can skip it
                containerRequestContext.setProperty(TREBLLE_EXCLUDED_PROPERTY, Boolean.TRUE);
                return; // Skip request body caching
            }

            InputStream inputStream = containerRequestContext.getEntityStream();

            // Get max size from configuration
            int maxSize = getTreblleService().getMaxBodySizeInBytes();
            LimitedByteArrayOutputStream byteArrayOutputStream =
                    new LimitedByteArrayOutputStream(maxSize);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                try {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                } catch (LimitExceededException e) {
                    // Body exceeds limit - stop reading but don't fail request
                    LOGGER.debug("Request body exceeds {}MB limit, truncating for telemetry",
                            maxSize / (1024 * 1024));
                    break;
                }
            }

            String requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            containerRequestContext.setProperty(REQUEST_BODY_PROPERTY, requestBody);
            containerRequestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());

            // Reset entity stream
            containerRequestContext.setEntityStream(
                    new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8))
            );

        } catch (Exception e) {
            // Log but don't fail the request
            LOGGER.error("Error reading request body for Treblle telemetry", e);
            // Set empty body so request continues
            containerRequestContext.setProperty(REQUEST_BODY_PROPERTY, "");
            containerRequestContext.setProperty(START_TIME_PROPERTY, System.currentTimeMillis());
            // Re-throw IOException so container knows
            if (e instanceof IOException) {
                throw (IOException) e;
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        try {
            // Check if request was excluded
            Boolean excluded = (Boolean) containerRequestContext.getProperty(TREBLLE_EXCLUDED_PROPERTY);
            if (Boolean.TRUE.equals(excluded)) {
                return; // Skip telemetry
            }

            int maxSize = getTreblleService().getMaxBodySizeInBytes();
            LimitedByteArrayOutputStream byteArrayOutputStream =
                    new LimitedByteArrayOutputStream(maxSize);

            OutputStream originalOutputStream = containerResponseContext.getEntityStream();

            // Null check
            if (originalOutputStream == null) {
                LOGGER.warn("Response entity stream is null, skipping Treblle telemetry");
                return;
            }

            // Wrap with capturing stream
            CaptureOutputStream captureStream = new CaptureOutputStream(
                    byteArrayOutputStream,
                    originalOutputStream
            );
            containerResponseContext.setEntityStream(captureStream);

            // Extract cached data
            byte[] responseBody = byteArrayOutputStream.toByteArray();
            String requestBodyString = (String) containerRequestContext.getProperty(REQUEST_BODY_PROPERTY);
            byte[] requestBody = requestBodyString != null ?
                    requestBodyString.getBytes(StandardCharsets.UTF_8) : new byte[0];

            Long startTimeObj = (Long) containerRequestContext.getProperty(START_TIME_PROPERTY);
            long startTime = startTimeObj != null ? startTimeObj : System.currentTimeMillis();
            long responseTimeInMillis = System.currentTimeMillis() - startTime;

            // Send telemetry asynchronously
            TrebllePayload payload = getTreblleService().createPayload(
                    new ContainerRequestContextWrapper(containerRequestContext, resourceInfo),
                    new ContainerResponseContextWrapper(containerResponseContext),
                    null,
                    responseTimeInMillis
            );
            getTreblleService().maskAndSendPayload(payload, requestBody, responseBody, null);

        } catch (Exception exception) {
            // NEVER let Treblle errors crash the response
            LOGGER.error("An error occurred while processing Treblle telemetry", exception);
        }
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
     * Limited ByteArrayOutputStream that throws exception when size limit exceeded
     */
    private static class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
        private final int maxSize;

        public LimitedByteArrayOutputStream(int maxSize) {
            super(Math.min(maxSize, 1024));
            this.maxSize = maxSize;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            if (count + len > maxSize) {
                throw new LimitExceededException("Body size exceeds " + maxSize + " bytes");
            }
            super.write(b, off, len);
        }

        @Override
        public synchronized void write(int b) {
            if (count >= maxSize) {
                throw new LimitExceededException("Body size exceeds " + maxSize + " bytes");
            }
            super.write(b);
        }
    }

    /**
     * Exception thrown when body size limit is exceeded
     */
    private static class LimitExceededException extends RuntimeException {
        public LimitExceededException(String message) {
            super(message);
        }
    }

    /**
     * Improved CaptureOutputStream with size limiting
     */
    private static class CaptureOutputStream extends OutputStream {
        private final LimitedByteArrayOutputStream captureBuffer;
        private final OutputStream originalOutputStream;
        private boolean limitExceeded = false;

        public CaptureOutputStream(LimitedByteArrayOutputStream captureBuffer,
                                   OutputStream originalOutputStream) {
            this.captureBuffer = captureBuffer;
            this.originalOutputStream = originalOutputStream;
        }

        @Override
        public void write(int b) throws IOException {
            // Always write to original
            originalOutputStream.write(b);

            // Try to capture if not exceeded
            if (!limitExceeded) {
                try {
                    captureBuffer.write(b);
                } catch (LimitExceededException e) {
                    limitExceeded = true;
                    // Continue without capturing
                }
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // Always write to original
            originalOutputStream.write(b, off, len);

            // Try to capture if not exceeded
            if (!limitExceeded) {
                try {
                    captureBuffer.write(b, off, len);
                } catch (LimitExceededException e) {
                    limitExceeded = true;
                    // Continue without capturing
                }
            }
        }

        @Override
        public void flush() throws IOException {
            originalOutputStream.flush();
        }

        @Override
        public void close() throws IOException {
            originalOutputStream.close();
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
