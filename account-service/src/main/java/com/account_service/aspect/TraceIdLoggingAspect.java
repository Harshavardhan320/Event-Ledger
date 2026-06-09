package com.account_service.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.account_service.util.TraceIdUtil;

/**
 * AOP Aspect for automatic trace ID logging in service methods
 */
@Aspect
@Component
public class TraceIdLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(TraceIdLoggingAspect.class);

    /**
     * Log all service method calls with trace ID
     */
    @Around("execution(* com.account_service.service.*.*(..))")
    public Object logServiceMethodWithTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String traceId = TraceIdUtil.getCurrentTraceId();
        
        logger.debug("Entering method: {}.{} [TraceId: {}]", className, methodName, traceId);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.debug("Exiting method: {}.{} - Duration: {}ms [TraceId: {}]", 
                    className, methodName, duration, traceId);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            logger.error("Exception in method: {}.{} - Duration: {}ms [TraceId: {}] - Error: {}", 
                    className, methodName, duration, traceId, e.getMessage(), e);
            
            throw e;
        }
    }

    /**
     * Log all controller method calls with trace ID
     */
    @Around("execution(* com.account_service.controller.*.*(..))")
    public Object logControllerMethodWithTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String traceId = TraceIdUtil.getCurrentTraceId();
        
        logger.info("Entering controller method: {}.{} [TraceId: {}]", className, methodName, traceId);
        
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Exiting controller method: {}.{} - Duration: {}ms [TraceId: {}]", 
                    className, methodName, duration, traceId);
            
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            logger.error("Exception in controller method: {}.{} - Duration: {}ms [TraceId: {}] - Error: {}", 
                    className, methodName, duration, traceId, e.getMessage(), e);
            
            throw e;
        }
    }
}

