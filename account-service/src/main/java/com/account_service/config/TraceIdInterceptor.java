package com.account_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Interceptor to manage trace IDs across requests
 * Generates a trace ID for incoming requests and adds it to MDC (Mapped Diagnostic Context)
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdInterceptor.class);
    
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        
        // Get trace ID from incoming request header or generate a new one
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }
        
        // Add trace ID to MDC for logging
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        
        // Add trace ID to response header
        response.setHeader(TRACE_ID_HEADER, traceId);
        
        logger.info("Incoming request - Method: {}, URI: {}", request.getMethod(), request.getRequestURI());
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) throws Exception {
        
        String traceId = MDC.get(TRACE_ID_MDC_KEY);
        
        if (ex != null) {
            logger.error("Request failed with trace ID: {} - Exception: {}", traceId, ex.getMessage(), ex);
        } else {
            logger.info("Request completed successfully with trace ID: {} - Status: {}", traceId, response.getStatus());
        }
        
        // Clear MDC
        MDC.remove(TRACE_ID_MDC_KEY);
    }
}

