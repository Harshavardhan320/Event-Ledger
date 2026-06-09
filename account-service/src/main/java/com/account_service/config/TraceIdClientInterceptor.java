package com.account_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP Client Interceptor to propagate trace IDs to downstream services
 */
@Component
public class TraceIdClientInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdClientInterceptor.class);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
        
        // Get trace ID from MDC
        String traceId = MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
        
        // Add trace ID to outgoing request header
        if (traceId != null && !traceId.isEmpty()) {
            request.getHeaders().set(TraceIdInterceptor.TRACE_ID_HEADER, traceId);
            logger.debug("Propagating trace ID {} to downstream service - URL: {}", traceId, request.getURI());
        }
        
        // Execute the request
        ClientHttpResponse response = execution.execute(request, body);
        
        logger.debug("Downstream service response received with status: {}", response.getStatusCode());
        
        return response;
    }
}

