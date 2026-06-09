package com.account_service.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.account_service.util.TraceIdUtil;

/**
 * Example Controller showing how to use distributed tracing
 * This demonstrates:
 * 1. Automatic trace ID in logs
 * 2. Propagating trace IDs to downstream services
 * 3. Using TraceIdUtil for manual trace logging
 */
@RestController
@RequestMapping("/api/examples")
public class TracingExampleController {

    private static final Logger logger = LoggerFactory.getLogger(TracingExampleController.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Example 1: Basic endpoint with automatic trace ID logging
     * 
     * Request:
     * curl -X GET http://localhost:8012/api/examples/basic
     * 
     * Response:
     * All logs automatically include trace ID from MDC
     */
    @GetMapping("/basic")
    public ResponseEntity<String> basicExample() {
        // Trace ID is automatically included in logs via MDC
        logger.info("Processing basic example request");
        
        try {
            Thread.sleep(100); // Simulate some work
            logger.debug("Basic example processing completed");
            
            return ResponseEntity.ok("Basic example completed successfully");
        } catch (InterruptedException e) {
            // Trace ID is automatically included even in error logs
            logger.error("Error in basic example", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Example 2: Using TraceIdUtil for manual trace logging
     * 
     * Request:
     * curl -X GET http://localhost:8012/api/examples/with-util
     * 
     * Shows how to manually access and log with trace IDs
     */
    @GetMapping("/with-util")
    public ResponseEntity<String> withTraceUtilExample() {
        // Get current trace ID
        String traceId = TraceIdUtil.getCurrentTraceId();
        logger.info("Current trace ID: {}", traceId);
        
        // Manual log with trace context
        TraceIdUtil.logWithTrace("Processing request with TraceIdUtil");
        
        try {
            // Simulate some business logic
            processBusinessLogic();
            
            TraceIdUtil.logWithTrace("Business logic completed successfully");
            
            return ResponseEntity.ok("Trace util example completed. Trace ID: " + traceId);
        } catch (Exception e) {
            // Log error with trace context
            TraceIdUtil.logErrorWithTrace("Error in TraceIdUtil example", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Example 3: Calling downstream service with automatic trace propagation
     * 
     * Request:
     * curl -X GET http://localhost:8012/api/examples/downstream
     * 
     * Shows how trace ID is automatically propagated to downstream services
     */
    @GetMapping("/downstream")
    public ResponseEntity<String> downstreamServiceExample() {
        logger.info("Starting downstream service call");
        
        try {
            // The trace ID will be automatically propagated by TraceIdClientInterceptor
            String traceId = TraceIdUtil.getCurrentTraceId();
            logger.info("Trace ID being propagated: {}", traceId);
            
            // Call downstream service using RestTemplate
            // TraceIdClientInterceptor will automatically add X-Trace-Id header
            String result = restTemplate.getForObject(
                "http://localhost:8012/api/examples/basic",
                String.class
            );
            
            logger.info("Received response from downstream service: {}", result);
            
            return ResponseEntity.ok("Downstream call completed. Same trace ID used throughout.");
        } catch (Exception e) {
            logger.error("Error calling downstream service", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Example 4: Multi-step process with trace context
     * 
     * Request:
     * curl -X POST http://localhost:8012/api/examples/multi-step \
     *   -H "Content-Type: application/json" \
     *   -d '{"name":"Test","value":100}'
     * 
     * Shows trace ID usage across multiple steps
     */
    @PostMapping("/multi-step")
    public ResponseEntity<String> multiStepProcessExample(@RequestBody ExampleRequest request) {
        String traceId = TraceIdUtil.getCurrentTraceId();
        logger.info("Starting multi-step process. Request: {}", request);
        
        try {
            // Step 1
            logger.info("Step 1: Validating input - name={}, value={}", request.getName(), request.getValue());
            validateInput(request);
            
            // Step 2
            logger.info("Step 2: Processing data");
            String processedData = processData(request);
            
            // Step 3
            logger.info("Step 3: Storing result");
            storeResult(processedData);
            
            // All logs automatically include the same trace ID
            logger.info("Multi-step process completed successfully. Trace ID: {}", traceId);
            
            return ResponseEntity.ok("Multi-step process completed. Result: " + processedData);
        } catch (Exception e) {
            TraceIdUtil.logErrorWithTrace("Multi-step process failed", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Example 5: Nested service calls with trace propagation
     * 
     * Request:
     * curl -X GET http://localhost:8012/api/examples/nested
     * 
     * Shows how trace ID flows through nested method calls
     */
    @GetMapping("/nested")
    public ResponseEntity<String> nestedCallsExample() {
        logger.info("Starting nested calls example");
        
        try {
            String result = outerMethod();
            return ResponseEntity.ok("Nested calls completed: " + result);
        } catch (Exception e) {
            logger.error("Error in nested calls", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Example 6: Performance monitoring with trace context
     * 
     * Request:
     * curl -X GET http://localhost:8012/api/examples/performance
     * 
     * Shows how to track performance metrics with trace IDs
     */
    @GetMapping("/performance")
    public ResponseEntity<String> performanceExample() {
        String traceId = TraceIdUtil.getCurrentTraceId();
        long startTime = System.currentTimeMillis();
        
        logger.info("Starting performance monitoring");
        
        try {
            // Simulate some processing
            simulateWork(500);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Performance monitoring completed. Duration: {}ms. Trace ID: {}", duration, traceId);
            
            return ResponseEntity.ok("Performance monitoring completed. Duration: " + duration + "ms");
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error in performance monitoring. Duration: {}ms", duration, e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ========== Helper Methods ==========

    private void processBusinessLogic() {
        logger.debug("Executing business logic");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void validateInput(ExampleRequest request) {
        if (request.getName() == null || request.getName().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (request.getValue() <= 0) {
            throw new IllegalArgumentException("Value must be positive");
        }
    }

    private String processData(ExampleRequest request) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Processed: " + request.getName() + " with value " + request.getValue();
    }

    private void storeResult(String result) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("Result stored: {}", result);
    }

    private String outerMethod() {
        logger.debug("In outerMethod");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return middleMethod();
    }

    private String middleMethod() {
        logger.debug("In middleMethod");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return innerMethod();
    }

    private String innerMethod() {
        logger.debug("In innerMethod");
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return "Inner method result";
    }

    private void simulateWork(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ========== Inner Classes ==========

    /**
     * Example request DTO
     */
    public static class ExampleRequest {
        private String name;
        private long value;

        public ExampleRequest() {
        }

        public ExampleRequest(String name, long value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ExampleRequest{" +
                    "name='" + name + '\'' +
                    ", value=" + value +
                    '}';
        }
    }
}

