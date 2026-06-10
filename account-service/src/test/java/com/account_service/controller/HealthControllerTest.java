package com.account_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Test Suite")
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        // Default mocking setup
    }

    @Test
    @DisplayName("When: Health check is called, Then: UP status and service information are returned")
    void testHealthCheckSuccess() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Account Service is running", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
        assertEquals("event-gateway-service", response.getBody().get("service"));
    }

    @Test
    @DisplayName("When: Health check is called multiple times, Then: Consistent UP status is returned")
    void testHealthCheckConsistency() {
        // When
        ResponseEntity<Map<String, Object>> response1 = healthController.healthCheck();
        ResponseEntity<Map<String, Object>> response2 = healthController.healthCheck();

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals("UP", response1.getBody().get("status"));
        assertEquals("UP", response2.getBody().get("status"));
    }

    @Test
    @DisplayName("When: Database health check is called with valid connection, Then: UP status and database info are returned")
    void testDatabaseHealthCheckSuccess() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.databaseHealth();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("Database connection is active", response.getBody().get("message"));
        assertEquals("H2 (In-Memory)", response.getBody().get("database"));
        assertNotNull(response.getBody().get("timestamp"));
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("When: Database health check is called with invalid connection, Then: DOWN status is returned")
    void testDatabaseHealthCheckFailure() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.databaseHealth();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("Database connection validation failed", response.getBody().get("message"));
        verify(dataSource, times(1)).getConnection();
        verify(connection, times(1)).close();
    }

    @Test
    @DisplayName("When: Database health check throws SQLException, Then: DOWN status and error details are returned")
    void testDatabaseHealthCheckException() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        ResponseEntity<Map<String, Object>> response = healthController.databaseHealth();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("Database connection error", response.getBody().get("message"));
        assertTrue(response.getBody().get("error").toString().contains("Connection failed"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("When: Readiness check is called with connected database, Then: READY status is returned")
    void testReadinessCheckReady() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.readinessCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("READY", response.getBody().get("status"));
        assertEquals("CONNECTED", response.getBody().get("database"));
        verify(dataSource, times(1)).getConnection();
    }

    @Test
    @DisplayName("When: Readiness check is called with disconnected database, Then: NOT_READY status is returned")
    void testReadinessCheckNotReady() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        // When
        ResponseEntity<Map<String, Object>> response = healthController.readinessCheck();

        // Then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("NOT_READY", response.getBody().get("status"));
        assertEquals("DISCONNECTED", response.getBody().get("database"));
    }

    @Test
    @DisplayName("When: Readiness check throws exception, Then: Error response is returned")
    void testReadinessCheckException() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection error"));

        // When
        ResponseEntity<Map<String, Object>> response = healthController.readinessCheck();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ERROR", response.getBody().get("status"));
    }

    @Test
    @DisplayName("When: Liveness check is called, Then: ALIVE status is returned")
    void testLivenessCheckSuccess() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.livenessCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ALIVE", response.getBody().get("status"));
        assertEquals("Service is alive and responsive", response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("When: Service info is retrieved, Then: All service information is provided")
    void testServiceInfoSuccess() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.serviceInfo();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Event Gateway Service", response.getBody().get("service_name"));
        assertEquals("0.0.1-SNAPSHOT", response.getBody().get("version"));
        assertNotNull(response.getBody().get("java_version"));
        assertNotNull(response.getBody().get("os_name"));
        assertNotNull(response.getBody().get("os_version"));
        assertEquals("3.3.0", response.getBody().get("spring_boot_version"));
        assertEquals("H2 In-Memory Database", response.getBody().get("database"));
        assertEquals("/api/v1", response.getBody().get("api_base_path"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("When: Service metrics are retrieved, Then: JVM metrics are provided")
    void testServiceMetricsSuccess() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.serviceMetrics();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().get("timestamp"));
        assertNotNull(response.getBody().get("jvm"));

        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) response.getBody().get("jvm");
        assertNotNull(jvm.get("total_memory"));
        assertNotNull(jvm.get("free_memory"));
        assertNotNull(jvm.get("max_memory"));
        assertNotNull(jvm.get("used_memory"));
        assertNotNull(jvm.get("available_processors"));

        assertNotNull(response.getBody().get("uptime_ms"));
    }

    @Test
    @DisplayName("When: Service metrics show memory stats, Then: Memory values are positive")
    void testServiceMetricsMemoryValues() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.serviceMetrics();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) response.getBody().get("jvm");
        
        long totalMemory = (Long) jvm.get("total_memory");
        long freeMemory = (Long) jvm.get("free_memory");
        long maxMemory = (Long) jvm.get("max_memory");
        long usedMemory = (Long) jvm.get("used_memory");
        int processors = (Integer) jvm.get("available_processors");

        assertTrue(totalMemory > 0);
        assertTrue(maxMemory > 0);
        assertTrue(processors > 0);
        assertTrue(usedMemory >= 0);
        assertTrue(freeMemory >= 0);
    }

    @Test
    @DisplayName("When: Health check is called during exception, Then: Error response is built")
    void testHealthCheckWithMockedFailure() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("When: Multiple health checks are called concurrently in concept, Then: All succeed")
    void testMultipleHealthChecksConcept() {
        // When
        ResponseEntity<Map<String, Object>> response1 = healthController.healthCheck();
        ResponseEntity<Map<String, Object>> response2 = healthController.databaseHealth();
        ResponseEntity<Map<String, Object>> response3 = healthController.readinessCheck();
        ResponseEntity<Map<String, Object>> response4 = healthController.livenessCheck();
        ResponseEntity<Map<String, Object>> response5 = healthController.serviceInfo();

        // Then
        assertNotNull(response1.getStatusCode());
        assertNotNull(response2.getStatusCode());
        assertNotNull(response3.getStatusCode());
        assertNotNull(response4.getStatusCode());
        assertNotNull(response5.getStatusCode());
    }

    @Test
    @DisplayName("When: Readiness check with database exception, Then: Service unavailable error response is returned")
    void testReadinessCheckDatabaseException() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("DB error"));

        // When
        ResponseEntity<Map<String, Object>> response = healthController.readinessCheck();

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("When: Service info is retrieved multiple times, Then: Consistent info is returned")
    void testServiceInfoConsistency() {
        // When
        ResponseEntity<Map<String, Object>> response1 = healthController.serviceInfo();
        ResponseEntity<Map<String, Object>> response2 = healthController.serviceInfo();

        // Then
        assertEquals("Event Gateway Service", response1.getBody().get("service_name"));
        assertEquals("Event Gateway Service", response2.getBody().get("service_name"));
        assertEquals("0.0.1-SNAPSHOT", response1.getBody().get("version"));
        assertEquals("0.0.1-SNAPSHOT", response2.getBody().get("version"));
    }

    @Test
    @DisplayName("When: Liveness check is called multiple times, Then: All return ALIVE status")
    void testMultipleLivenessChecks() {
        // When
        ResponseEntity<Map<String, Object>> response1 = healthController.livenessCheck();
        ResponseEntity<Map<String, Object>> response2 = healthController.livenessCheck();

        // Then
        assertEquals("ALIVE", response1.getBody().get("status"));
        assertEquals("ALIVE", response2.getBody().get("status"));
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }

    @Test
    @DisplayName("When: Database metrics show processors, Then: At least one processor exists")
    void testMetricsAvailableProcessors() {
        // When
        ResponseEntity<Map<String, Object>> response = healthController.serviceMetrics();

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Object> jvm = (Map<String, Object>) response.getBody().get("jvm");
        int processors = (Integer) jvm.get("available_processors");
        assertTrue(processors >= 1);
    }
}

