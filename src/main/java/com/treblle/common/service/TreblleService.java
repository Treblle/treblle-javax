package com.treblle.common.service;

import com.treblle.common.configuration.TreblleProperties;
import com.treblle.common.dto.TrebllePayload;
import com.treblle.common.infrastructure.RequestWrapper;
import com.treblle.common.infrastructure.ResponseWrapper;

public interface TreblleService {

    TrebllePayload createPayload(RequestWrapper request, ResponseWrapper response, Exception exception, long responseTimeInMillis);

    void maskAndSendPayload(TrebllePayload payload, byte[] requestBody, byte[] responseBody, Exception chainException);

    int getMaxBodySizeInBytes();

    /**
     * Get the configuration properties for this Treblle service.
     *
     * @return TreblleProperties instance
     * @since 1.0.6
     */
    TreblleProperties getProperties();

}