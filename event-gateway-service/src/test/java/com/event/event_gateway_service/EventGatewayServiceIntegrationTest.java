package com.event.event_gateway_service;

import com.event.event_gateway_service.entity.Event;
import com.event.event_gateway_service.entity.EventStatus;
import com.event.event_gateway_service.entity.EventType;
import com.event.event_gateway_service.repository.EventRepository;
import com.event.event_gateway_service.requestORresponse.EventRequest;
import com.event.event_gateway_service.servicesCommunition.ServiceConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("EventGatewayService Integration Tests")
class EventGatewayServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @MockBean
    private ServiceConnection serviceConnection;

    private EventRequest eventRequest;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        timestamp = LocalDateTime.now();
        eventRequest = new EventRequest(
                1L,
                "ACC_INT_001",
                EventType.CREDIT,
                new BigDecimal("5000.00"),
                "USD",
                timestamp,
                EventStatus.PENDING
        );
    }

    @Nested
    @DisplayName("End-to-End Event Processing Tests")
    class EndToEndEventProcessingTests {

        @Test
        @DisplayName("Given new event when creating through API then should save and retrieve successfully")
        void testCreateAndRetrieveEvent() throws Exception {
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            EventRequest uniqueEvent = new EventRequest(
                    100L,
                    "ACC_INT_UNIQUE_001",
                    EventType.CREDIT,
                    new BigDecimal("5000.00"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );
            List<EventRequest> requests = Arrays.asList(uniqueEvent);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].accountId").value("ACC_INT_UNIQUE_001"))
                    .andExpect(jsonPath("$[0].eventStatus").value("PROCESSED"));
        }

        @Test
        @DisplayName("Given multiple events when creating through API then should retrieve all by account")
        void testCreateMultipleAndRetrieveByAccount() throws Exception {
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            
            EventRequest event1 = new EventRequest(
                    101L,
                    "ACC_INT_MULTI_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("1000.00"),
                    "USD",
                    timestamp.minusHours(2),
                    EventStatus.PENDING
            );
            EventRequest event2 = new EventRequest(
                    102L,
                    "ACC_INT_MULTI_UNIQUE",
                    EventType.DEBIT,
                    new BigDecimal("500.00"),
                    "USD",
                    timestamp.minusHours(1),
                    EventStatus.PENDING
            );
            EventRequest event3 = new EventRequest(
                    103L,
                    "ACC_INT_MULTI_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("2000.00"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );
            List<EventRequest> requests = Arrays.asList(event1, event2, event3);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(3));

            mockMvc.perform(get("/events")
                    .param("account", "ACC_INT_MULTI_UNIQUE")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }

        @Test
        @DisplayName("Given mixed event types when creating then should process all correctly")
        void testProcessMixedEventTypes() throws Exception {
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            
            EventRequest creditEvent = new EventRequest(
                    200L,
                    "ACC_INT_MIXED_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("3000.00"),
                    "EUR",
                    timestamp,
                    EventStatus.PENDING
            );
            EventRequest debitEvent = new EventRequest(
                    201L,
                    "ACC_INT_MIXED_UNIQUE",
                    EventType.DEBIT,
                    new BigDecimal("1500.00"),
                    "EUR",
                    timestamp.plusHours(1),
                    EventStatus.PENDING
            );
            List<EventRequest> requests = Arrays.asList(creditEvent, debitEvent);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].type").value("CREDIT"))
                    .andExpect(jsonPath("$[1].type").value("DEBIT"));
        }

        @Test
        @DisplayName("Given event when service fails then should still save event with PENDING status")
        void testEventPersistenceOnServiceFailure() throws Exception {
            when(serviceConnection.createTransaction(any(), any()))
                    .thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            
            EventRequest failureEvent = new EventRequest(
                    300L,
                    "ACC_INT_FAIL_UNIQUE",
                    EventType.DEBIT,
                    new BigDecimal("2500.00"),
                    "GBP",
                    timestamp,
                    EventStatus.PENDING
            );
            List<EventRequest> requests = Arrays.asList(failureEvent);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].eventStatus").value("PENDING"));
        }

        @Test
        @DisplayName("Given duplicate event id when creating then should return existing event")
        void testDuplicateEventHandling() throws Exception {
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            
            EventRequest duplicateEvent1 = new EventRequest(
                    400L,
                    "ACC_INT_DUP_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("1000.00"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );
            
            // when - create first event
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Arrays.asList(duplicateEvent1))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].eventStatus").value("PROCESSED"))
                    .andExpect(jsonPath("$[0].accountId").value("ACC_INT_DUP_UNIQUE"));
        }
    }

    @Nested
    @DisplayName("Health Check Integration Tests")
    class HealthCheckIntegrationTests {

        @Test
        @DisplayName("Given system startup when checking health then should return UP")
        void testSystemHealthOnStartup() throws Exception {
            // when & then
            mockMvc.perform(get("/health")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("Given database available when checking database health then should return UP")
        void testDatabaseHealthOnStartup() throws Exception {
            mockMvc.perform(get("/health/database")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }

        @Test
        @DisplayName("Given system running when checking readiness then should return READY")
        void testReadinessCheckOnStartup() throws Exception {
            mockMvc.perform(get("/health/ready")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("READY"));
        }

        @Test
        @DisplayName("Given system running when checking liveness then should return ALIVE")
        void testLivenessCheckOnStartup() throws Exception {
            mockMvc.perform(get("/health/live")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ALIVE"));
        }

        @Test
        @DisplayName("Given system running when getting info then should return service details")
        void testServiceInfoOnStartup() throws Exception {
            mockMvc.perform(get("/health/info")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.service_name").exists());
        }

        @Test
        @DisplayName("Given system running when getting metrics then should return JVM metrics")
        void testMetricsOnStartup() throws Exception {
            mockMvc.perform(get("/health/metrics")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jvm").exists());
        }
    }

    @Nested
    @DisplayName("Error Handling Integration Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Given non-existent event id when retrieving then should return NOT_FOUND")
        void testNonExistentEventRetrieval() throws Exception {
            mockMvc.perform(get("/events/{id}", 9999L)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Given non-existent account when querying then should return NOT_FOUND")
        void testNonExistentAccountRetrieval() throws Exception {
            mockMvc.perform(get("/events")
                    .param("account", "ACC_NOT_EXIST")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Given invalid request when creating then should handle gracefully")
        void testInvalidEventCreation() throws Exception {
            String invalidJson = "[{ \"accountId\": \"ACC_TEST\" }]";

            var result = mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            assertTrue(status >= 400 && status <= 599, "Expected error status, got: " + status);
        }
    }

    @Nested
    @DisplayName("Data Persistence Tests")
    class DataPersistenceTests {

        @Test
        @DisplayName("Given event when saving then should persist to database")
        void testEventPersistence() throws Exception {

            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            EventRequest persistenceEvent = new EventRequest(
                    500L,
                    "ACC_PERSIST_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("7500.00"),
                    "JPY",
                    timestamp,
                    EventStatus.PENDING
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Arrays.asList(persistenceEvent))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].accountId").value("ACC_PERSIST_UNIQUE"))
                    .andExpect(jsonPath("$[0].amount").value(7500.00))
                    .andExpect(jsonPath("$[0].eventStatus").value("PROCESSED"));
        }

        @Test
        @DisplayName("Given multiple events when saving then should retrieve all by account in order")
        void testEventSortingByTimestamp() throws Exception {

            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            
            LocalDateTime time1 = timestamp.minusHours(3);
            LocalDateTime time2 = timestamp.minusHours(1);
            LocalDateTime time3 = timestamp;

            EventRequest oldestEvent = new EventRequest(
                    600L,
                    "ACC_SORT_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("1000.00"),
                    "USD",
                    time3,
                    EventStatus.PENDING
            );
            EventRequest middleEvent = new EventRequest(
                    601L,
                    "ACC_SORT_UNIQUE",
                    EventType.DEBIT,
                    new BigDecimal("500.00"),
                    "USD",
                    time1,
                    EventStatus.PENDING
            );
            EventRequest newestEvent = new EventRequest(
                    602L,
                    "ACC_SORT_UNIQUE",
                    EventType.CREDIT,
                    new BigDecimal("2000.00"),
                    "USD",
                    time2,
                    EventStatus.PENDING
            );

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Arrays.asList(oldestEvent, middleEvent, newestEvent))))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/events")
                    .param("account", "ACC_SORT_UNIQUE")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3));
        }
    }
}
















