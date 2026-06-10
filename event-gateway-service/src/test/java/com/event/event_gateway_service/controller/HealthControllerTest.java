package com.event.event_gateway_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@DisplayName("HealthController Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private Connection connection;

    @BeforeEach
    void setUp() {
        reset(dataSource, connection);
    }

    @Nested
    @DisplayName("GET /health Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Given health check endpoint when called then should  UP status")
        void testHealthCheckSuccess() throws Exception {

            mockMvc.perform(get("/health")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.message").value("Event Gateway Service is running"))
                    .andExpect(jsonPath("$.service").value("event-gateway-service"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Given  check endpoint when called then should return valid JSON response")
        void testHealthCheckResponseFormat() throws Exception {

            mockMvc.perform(get("/health")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.service").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/database Tests")
    class DatabaseHealthTests {

        @Test
        @DisplayName("Given database s connected when checking then should return UP status")
        void testDatabaseHealthSuccess() throws Exception {

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);

            mockMvc.perform(get("/health/database")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.message").value("Database connection is active"))
                    .andExpect(jsonPath("$.database").value("H2 (In-Memory)"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(dataSource, times(1)).getConnection();
            verify(connection, times(1)).isValid(2);
            verify(connection, times(1)).close();
        }

        @Test
        @DisplayName("Given database is disconnected when checking then should return DOWN status")
        void testDatabaseHealthFailure() throws Exception {

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(false);

            mockMvc.perform(get("/health/database")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.message").value("Database connection validation failed"));

            verify(dataSource, times(1)).getConnection();
            verify(connection, times(1)).isValid(2);
            verify(connection, times(1)).close();
        }

        @Test
        @DisplayName("Given database connection throws exception when checking then should return DOWN status")
        void testDatabaseHealthException() throws Exception {

            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            mockMvc.perform(get("/health/database")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("DOWN"))
                    .andExpect(jsonPath("$.message").value("Database connection error"))
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(dataSource, times(1)).getConnection();
        }
    }

    @Nested
    @DisplayName("GET /health/ready Tests")
    class ReadinessCheckTests {

        @Test
        @DisplayName("Given database is connected when checking readiness then should return READY status")
        void testReadinessCheckReady() throws Exception {

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);

            mockMvc.perform(get("/health/ready")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY"))
                    .andExpect(jsonPath("$.database").value("CONNECTED"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(dataSource, times(1)).getConnection();
            verify(connection, times(1)).isValid(2);
            verify(connection, times(1)).close();
        }

        @Test
        @DisplayName("Given database is disconnected when checking readiness then should return NOT_READY status")
        void testReadinessCheckNotReady() throws Exception {

            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(false);

            mockMvc.perform(get("/health/ready")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value("NOT_READY"))
                    .andExpect(jsonPath("$.database").value("DISCONNECTED"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(dataSource, times(1)).getConnection();
            verify(connection, times(1)).isValid(2);
            verify(connection, times(1)).close();
        }

        @Test
        @DisplayName("Given database connection throws exception when checking readiness then should return error response")
        void testReadinessCheckException() throws Exception {

            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            mockMvc.perform(get("/health/ready")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("ERROR"))
                    .andExpect(jsonPath("$.message").value("Readiness check failed"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(dataSource, times(1)).getConnection();
        }
    }

    @Nested
    @DisplayName("GET /health/live Tests")
    class LivenessCheckTests {

        @Test
        @DisplayName("Given liveness check endpoint when called then should return ALIVE status")
        void testLivenessCheckSuccess() throws Exception {

            mockMvc.perform(get("/health/live")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ALIVE"))
                    .andExpect(jsonPath("$.message").value("Service is alive and responsive"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Given liveness check endpoint when called then should return valid response format")
        void testLivenessCheckResponseFormat() throws Exception {

            mockMvc.perform(get("/health/live")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/info Tests")
    class ServiceInfoTests {

        @Test
        @DisplayName("Given info endpoint when called then should return service information")
        void testServiceInfoSuccess() throws Exception {

            mockMvc.perform(get("/health/info")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.service_name").value("Event Gateway Service"))
                    .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
                    .andExpect(jsonPath("$.java_version").exists())
                    .andExpect(jsonPath("$.os_name").exists())
                    .andExpect(jsonPath("$.os_version").exists())
                    .andExpect(jsonPath("$.spring_boot_version").value("3.3.0"))
                    .andExpect(jsonPath("$.database").value("H2 In-Memory Database"))
                    .andExpect(jsonPath("$.api_base_path").value("/api/v1"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Given info endpoint when called then should return complete response")
        void testServiceInfoCompleteness() throws Exception {

            mockMvc.perform(get("/health/info")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.service_name").exists())
                    .andExpect(jsonPath("$.version").exists())
                    .andExpect(jsonPath("$.spring_boot_version").exists());
        }
    }

    @Nested
    @DisplayName("GET /health/metrics Tests")
    class ServiceMetricsTests {

        @Test
        @DisplayName("Given metrics endpoint when called then should return JVM metrics")
        void testServiceMetricsSuccess() throws Exception {

            mockMvc.perform(get("/health/metrics")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.jvm").exists())
                    .andExpect(jsonPath("$.jvm.total_memory").exists())
                    .andExpect(jsonPath("$.jvm.free_memory").exists())
                    .andExpect(jsonPath("$.jvm.max_memory").exists())
                    .andExpect(jsonPath("$.jvm.used_memory").exists())
                    .andExpect(jsonPath("$.jvm.available_processors").exists())
                    .andExpect(jsonPath("$.uptime_ms").exists());
        }

        @Test
        @DisplayName("Given metrics endpoint when called then should return positive memory values")
        void testServiceMetricsValidValues() throws Exception {

            mockMvc.perform(get("/health/metrics")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jvm.total_memory").isNumber())
                    .andExpect(jsonPath("$.jvm.free_memory").isNumber())
                    .andExpect(jsonPath("$.jvm.max_memory").isNumber())
                    .andExpect(jsonPath("$.jvm.available_processors").isNumber());
        }

        @Test
        @DisplayName("Given  endpoint when called then should return proper response format")
        void testServiceMetricsResponseFormat() throws Exception {

            mockMvc.perform(get("/health/metrics")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.timestamp").isString())
                    .andExpect(jsonPath("$.jvm").isMap());
        }
    }
}




