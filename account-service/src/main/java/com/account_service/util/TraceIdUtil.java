package com.account_service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import com.account_service.config.TraceIdInterceptor;

/**
 * Utility class for managing trace IDs in the application
 */
public class TraceIdUtil {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdUtil.class);

    /**
     * Get the current trace ID from MDC
     */
    public static String getCurrentTraceId() {
        return MDC.get(TraceIdInterceptor.TRACE_ID_MDC_KEY);
    }

    /**
     * Set trace ID in MDC
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.isEmpty()) {
            MDC.put(TraceIdInterceptor.TRACE_ID_MDC_KEY, traceId);
        }
    }

    /**
     * Clear trace ID from MDC
     */
    public static void clearTraceId() {
        MDC.remove(TraceIdInterceptor.TRACE_ID_MDC_KEY);
    }

    /**
     * Log message with current trace ID
     */
    public static void logWithTrace(String message) {
        String traceId = getCurrentTraceId();
        if (traceId != null) {
            logger.info("TraceId: {} - {}", traceId, message);
        } else {
            logger.info(message);
        }
    }

    /**
     * Log error with current trace ID
     */
    public static void logErrorWithTrace(String message, Throwable throwable) {
        String traceId = getCurrentTraceId();
        if (traceId != null) {
            logger.error("TraceId: {} - {}", traceId, message, throwable);
        } else {
            logger.error(message, throwable);
        }
    }
}

