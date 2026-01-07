package com.treblle.javax.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.service.AbstractTreblleService;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.treblle.common.utils.HttpUtils.APPLICATION_JSON_VALUE;

/**
 * Implementation of Treblle telemetry service for JavaX applications.
 * <p>
 * This service handles asynchronous transmission of monitoring data to the Treblle API
 * using Apache HTTP Client 5. It manages HTTP connections, request/response logging in
 * debug mode, and graceful shutdown of resources.
 *
 * @since 1.0.0
 */
public class TreblleServiceImpl extends AbstractTreblleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreblleServiceImpl.class);

    private final CloseableHttpClient httpClient;
    private final ExecutorService executorService;

    /**
     * Creates a new Treblle service instance.
     *
     * @param sdkName the SDK identifier (e.g., "javax-servlet" or "javax-container")
     * @param treblleProperties configuration properties for the service
     * @param objectMapper JSON object mapper for serialization
     */
    public TreblleServiceImpl(String sdkName, TreblleProperties treblleProperties, ObjectMapper objectMapper) {
        super(sdkName, treblleProperties, objectMapper);

        // Create singleton HTTP client
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(treblleProperties.getConnectTimeoutInSeconds()))
                .setResponseTimeout(Timeout.ofSeconds(treblleProperties.getReadTimeoutInSeconds()))
                .build();

        HttpClientBuilder builder = HttpClients.custom()
                .disableAutomaticRetries()
                .setDefaultRequestConfig(requestConfig);

        if (treblleProperties.isDebugMode()) {
            builder.addRequestInterceptorFirst(new RequestLogger())
                    .addResponseInterceptorFirst(new ResponseLogger());
        }

        this.httpClient = builder.build();

        // Create bounded thread pool for async tasks
        this.executorService = new ThreadPoolExecutor(
                1,  // core pool size
                3,  // max pool size
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),  // bounded queue
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "treblle-async-" + threadNumber.getAndIncrement());
                        t.setDaemon(true);  // Don't block JVM shutdown
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // If queue full, run in caller thread
        );
    }

    @Override
    protected void sendPayload(TrebllePayload payload) {
        CompletableFuture.runAsync(() -> {
            final HttpPost httpPost = new HttpPost(
                    Optional.ofNullable(treblleProperties.getCustomTreblleEndpoint())
                            .orElse(getRandomAPIEndpoint())
            );
            httpPost.setHeader("Content-Type", APPLICATION_JSON_VALUE);
            httpPost.setHeader(TREBLLE_API_KEY_HEADER, treblleProperties.getSdkToken());

            try {
                StringEntity entity = new StringEntity(objectMapper.writeValueAsString(payload));
                httpPost.setEntity(entity);

                // Use singleton client
                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    if (treblleProperties.isDebugMode()) {
                        if (response.getCode() != 200) {
                            LOGGER.error("An error occurred while sending network request to Treblle. Status Code: {}", response.getCode());
                        } else {
                            LOGGER.debug("Treblle API response: {}", response.getCode());
                        }
                    }
                }
            } catch (IOException exception) {
                LOGGER.error("Failed to send payload to Treblle", exception);
            }
        }, executorService);  // Use managed executor
    }

    /**
     * Shuts down the telemetry service and releases resources.
     * <p>
     * This method gracefully stops the executor service and waits for pending
     * tasks to complete. It should be called when the filter is destroyed.
     */
    public void shutdown() {
        LOGGER.debug("Shutting down Treblle service");

        // Shutdown executor service
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("Executor service did not terminate in time, forcing shutdown");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.error("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for executor service shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close HTTP client
        try {
            httpClient.close();
            LOGGER.debug("HTTP client closed successfully");
        } catch (IOException e) {
            LOGGER.error("Error closing HTTP client", e);
        }
    }

    public static class RequestLogger implements HttpRequestInterceptor {

        @Override
        public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
            LOGGER.info("Request Method: {}", httpRequest.getMethod());
            LOGGER.info("Request Authority: {}", httpRequest.getAuthority().toString());
        }

    }

    public static class ResponseLogger implements HttpResponseInterceptor {

        @Override
        public void process(HttpResponse httpResponse, EntityDetails entityDetails, HttpContext httpContext) throws HttpException, IOException {
            LOGGER.info("Response Status Code: {}", httpResponse.getCode());
            LOGGER.info("Response Status Reason: {}", httpResponse.getReasonPhrase());
        }

    }

}
