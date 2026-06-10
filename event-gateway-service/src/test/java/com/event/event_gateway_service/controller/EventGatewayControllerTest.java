package com.event.event_gateway_service.controller;

import com.event.event_gateway_service.entity.Event;
import com.event.event_gateway_service.entity.EventStatus;
import com.event.event_gateway_service.entity.EventType;
import com.event.event_gateway_service.requestORresponse.EventRequest;
import com.event.event_gateway_service.services.EventGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventGatewayController.class)
@DisplayName("EventGatewayController Tests")
class EventGatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventGatewayService eventGatewayService;

    @Autowired
    private ObjectMapper objectMapper;

    private EventRequest eventRequest;
    private Event event;
    private LocalDateTime timestamp;

    @BeforeEach
    void setUp() {
        timestamp = LocalDateTime.now();
        eventRequest = new EventRequest(
                1L,
                "ACC123456",
                EventType.CREDIT,
                new BigDecimal("1000.00"),
                "USD",
                timestamp,
                EventStatus.PENDING
        );

        event = Event.builder()
                .eventId(1L)
                .accountId("ACC123456")
                .type(EventType.CREDIT)
                .amount(new BigDecimal("1000.00"))
                .currency("USD")
                .eventTimestamp(timestamp)
                .eventStatus(EventStatus.PROCESSED)
                .build();
    }

    @Nested
    @DisplayName("POST /events Tests")
    class CreateEventTests {

        @Test
        @DisplayName("Given valid event requests when creating then should return CREATED status with events")
        void testTransactionEventSuccess() throws Exception {
           
            List<EventRequest> requests = Arrays.asList(eventRequest);
            List<Event> savedEvents = Arrays.asList(event);
            
            when(eventGatewayService.processAllEvent(requests)).thenReturn(savedEvents);
            
            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].eventId").value(1L))
                    .andExpect(jsonPath("$[0].accountId").value("ACC123456"))
                    .andExpect(jsonPath("$[0].eventStatus").value("PROCESSED"));

            verify(eventGatewayService, times(1)).processAllEvent(requests);
        }

        @Test
        @DisplayName("Given empty event list when creating then should return CREATED status with empty list")
        void testTransactionEventWithEmptyList() throws Exception {
       
            List<EventRequest> requests = List.of();
            List<Event> savedEvents = List.of();
            
            when(eventGatewayService.processAllEvent(requests)).thenReturn(savedEvents);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));

            verify(eventGatewayService, times(1)).processAllEvent(requests);
        }

        @Test
        @DisplayName("Given service throws exception when creating then should return INTERNAL_SERVER_ERROR")
        void testTransactionEventWithServiceException() throws Exception {

            List<EventRequest> requests = Collections.singletonList(eventRequest);
            
            when(eventGatewayService.processAllEvent(any())).thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isInternalServerError());

            verify(eventGatewayService, times(1)).processAllEvent(requests);
        }

        @Test
        @DisplayName("Given multiple events when creating the should return all created events")
        void testTransactionEventWithMultipleEvents() throws Exception {

            EventRequest event2 = new EventRequest(
                    2L,
                    "ACC789",
                    EventType.DEBIT,
                    new BigDecimal("500.00"),
                    "EUR",
                    timestamp,
                    EventStatus.PENDING
            );
            List<EventRequest> requests = Arrays.asList(eventRequest, event2);
            
            Event savedEvent2 = Event.builder()
                    .eventId(2L)
                    .accountId("ACC789")
                    .type(EventType.DEBIT)
                    .amount(new BigDecimal("500.00"))
                    .currency("EUR")
                    .eventTimestamp(timestamp)
                    .eventStatus(EventStatus.PROCESSED)
                    .build();
            
            List<Event> savedEvents = Arrays.asList(event, savedEvent2);
            when(eventGatewayService.processAllEvent(requests)).thenReturn(savedEvents);

            mockMvc.perform(post("/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].eventId").value(1L))
                    .andExpect(jsonPath("$[1].eventId").value(2L));

            verify(eventGatewayService, times(1)).processAllEvent(requests);
        }
    }

    @Nested
    @DisplayName("GET /events/{id} Tests")
    class GetEventByIdTests {

        @Test
        @DisplayName("Given valid evet id when getting then should return OK status with event")
        void testGetByEventIdSuccess() throws Exception {

            when(eventGatewayService.getEventById(1L)).thenReturn(event);

            mockMvc.perform(get("/events/{id}", 1L)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(1L))
                    .andExpect(jsonPath("$.accountId").value("ACC123456"))
                    .andExpect(jsonPath("$.type").value("CREDIT"))
                    .andExpect(jsonPath("$.eventStatus").value("PROCESSED"));

            verify(eventGatewayService, times(1)).getEventById(1L);
        }

        @Test
        @DisplayName("Given non-existing eent id when getting then should return NOT_FOUND status")
        void testGetByEventIdNotFound() throws Exception {

            when(eventGatewayService.getEventById(999L)).thenReturn(null);

            mockMvc.perform(get("/events/{id}", 999L)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(eventGatewayService, times(1)).getEventById(999L);
        }

        @Test
        @DisplayName("Gien service throws exception when getting by id then should return INTERNAL_SERVER_ERROR")
        void testGetByEventIdWithServiceException() throws Exception {

            when(eventGatewayService.getEventById(1L)).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/events/{id}", 1L)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(eventGatewayService, times(1)).getEventById(1L);
        }
    }

    @Nested
    @DisplayName("GET /events?account Tests")
    class GetByAccountIdTests {

        @Test
        @DisplayName("Given valid account id when getting then should return OK status with events")
        void testGetByAccountIdSuccess() throws Exception {

            List<Event> events = Arrays.asList(event);
            when(eventGatewayService.getByAccountId("ACC123456")).thenReturn(events);

            mockMvc.perform(get("/events")
                    .param("account", "ACC123456")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].eventId").value(1L))
                    .andExpect(jsonPath("$[0].accountId").value("ACC123456"));

            verify(eventGatewayService, times(1)).getByAccountId("ACC123456");
        }

        @Test
        @DisplayName("Given account with no events when getting then should return NOT_FOUND")
        void testGetByAccountIdWithNoEvents() throws Exception {

            when(eventGatewayService.getByAccountId("ACC999")).thenReturn(Arrays.asList());

            mockMvc.perform(get("/events")
                    .param("account", "ACC999")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(eventGatewayService, times(1)).getByAccountId("ACC999");
        }

        @Test
        @DisplayName("Given service throws exception when  by account then should return INTERNAL_SERVER_ERROR")
        void testGetByAccountIdWithServiceException() throws Exception {

            when(eventGatewayService.getByAccountId("ACC123456")).thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(get("/events")
                    .param("account", "ACC123456")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(eventGatewayService, times(1)).getByAccountId("ACC123456");
        }

        @Test
        @DisplayName("Given  events for account when getting then should return all events")
        void testGetByAccountIdWithMultipleEvents() throws Exception {

            Event event2 = Event.builder()
                    .eventId(2L)
                    .accountId("ACC123456")
                    .type(EventType.DEBIT)
                    .amount(new BigDecimal("500.00"))
                    .currency("USD")
                    .eventTimestamp(timestamp)
                    .eventStatus(EventStatus.PENDING)
                    .build();
            
            List<Event> events = Arrays.asList(event, event2);
            when(eventGatewayService.getByAccountId("ACC123456")).thenReturn(events);

            mockMvc.perform(get("/events")
                    .param("account", "ACC123456")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].eventId").value(1L))
                    .andExpect(jsonPath("$[1].eventId").value(2L));

            verify(eventGatewayService, times(1)).getByAccountId("ACC123456");
        }

        @Test
        @DisplayName("Given null events when getting by account then should return NOT_FOUND")
        void testGetByAccountIdWithNullEvents() throws Exception {

            when(eventGatewayService.getByAccountId("ACC123")).thenReturn(null);

            mockMvc.perform(get("/events")
                    .param("account", "ACC123")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(eventGatewayService, times(1)).getByAccountId("ACC123");
        }
    }
}



