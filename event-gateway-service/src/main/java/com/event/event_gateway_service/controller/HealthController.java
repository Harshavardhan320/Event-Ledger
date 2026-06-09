package com.event.event_gateway_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     * Returns: Simple OK status with timestamp
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> healthResponse = new HashMap<>();
            healthResponse.put("status", "UP");
            healthResponse.put("message", "Event Gateway Service is running");
            healthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            healthResponse.put("service", "event-gateway-service");
            
            log.info("Health check successful");
            return ResponseEntity.ok(healthResponse);
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return buildErrorResponse("Health check failed");
        }
    }

    /**
     * Database health check
     * Verifies database connectivity
     */
    @GetMapping(value = "/database", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        try {
            Connection connection = dataSource.getConnection();
            boolean isValid = connection.isValid(2); // 2 second timeout
            connection.close();

            Map<String, Object> dbHealthResponse = new HashMap<>();
            if (isValid) {
                dbHealthResponse.put("status", "UP");
                dbHealthResponse.put("message", "Database connection is active");
                dbHealthResponse.put("database", "H2 (In-Memory)");
                dbHealthResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                log.info("Database health check successful");
                return ResponseEntity.ok(dbHealthResponse);
            } else {
                dbHealthResponse.put("status", "DOWN");
                dbHealthResponse.put("message", "Database connection validation failed");
                log.warn("Database connection invalid");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(dbHealthResponse);
            }
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "DOWN");
            errorResponse.put("message", "Database connection error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    /**
     * Application ready check
     * Returns detailed application status
     */
    @GetMapping(value = "/ready", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> readinessCheck() {
        try {
            Map<String, Object> readinessResponse = new HashMap<>();

            // Check database
            Connection connection = dataSource.getConnection();
            boolean dbConnected = connection.isValid(2);
            connection.close();

            readinessResponse.put("status", dbConnected ? "READY" : "NOT_READY");
            readinessResponse.put("database", dbConnected ? "CONNECTED" : "DISCONNECTED");
            readinessResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            HttpStatus statusCode = dbConnected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            log.info("Readiness check completed with status: {}", dbConnected ? "READY" : "NOT_READY");

            return ResponseEntity.status(statusCode).body(readinessResponse);
        } catch (Exception e) {
            log.error("Readiness check failed: {}", e.getMessage());
            return buildErrorResponse("Readiness check failed");
        }
    }

    /**
     * Liveness check (is the service alive?)
     * Returns basic UP status
     */
    @GetMapping(value = "/live", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> livenessCheck() {
        try {
            Map<String, Object> livenessResponse = new HashMap<>();
            livenessResponse.put("status", "ALIVE");
            livenessResponse.put("message", "Service is alive and responsive");
            livenessResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            log.info("Liveness check successful");
            return ResponseEntity.ok(livenessResponse);
        } catch (Exception e) {
            log.error("Liveness check failed: {}", e.getMessage());
            return buildErrorResponse("Liveness check failed");
        }
    }

    /**
     * Info endpoint
     * Returns service information and versions
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> serviceInfo() {
        try {
            Map<String, Object> infoResponse = new HashMap<>();
            infoResponse.put("service_name", "Event Gateway Service");
            infoResponse.put("version", "0.0.1-SNAPSHOT");
            infoResponse.put("java_version", System.getProperty("java.version"));
            infoResponse.put("os_name", System.getProperty("os.name"));
            infoResponse.put("os_version", System.getProperty("os.version"));
            infoResponse.put("spring_boot_version", "3.3.0");
            infoResponse.put("database", "H2 In-Memory Database");
            infoResponse.put("api_base_path", "/api/v1");
            infoResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            log.info("Service info retrieved");
            return ResponseEntity.ok(infoResponse);
        } catch (Exception e) {
            log.error("Service info retrieval failed: {}", e.getMessage());
            return buildErrorResponse("Service info retrieval failed");
        }
    }

    /**
     * Metrics endpoint (basic)
     * Returns service metrics
     */
    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> serviceMetrics() {
        try {
            Map<String, Object> metricsResponse = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();

            metricsResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            metricsResponse.put("jvm", new HashMap<String, Object>() {{
                put("total_memory", runtime.totalMemory());
                put("free_memory", runtime.freeMemory());
                put("max_memory", runtime.maxMemory());
                put("used_memory", runtime.totalMemory() - runtime.freeMemory());
                put("available_processors", runtime.availableProcessors());
            }});
            metricsResponse.put("uptime_ms", System.currentTimeMillis());

            log.info("Metrics retrieved");
            return ResponseEntity.ok(metricsResponse);
        } catch (Exception e) {
            log.error("Metrics retrieval failed: {}", e.getMessage());
            return buildErrorResponse("Metrics retrieval failed");
        }
    }

    /**
     * Helper method to build error response
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "ERROR");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}


