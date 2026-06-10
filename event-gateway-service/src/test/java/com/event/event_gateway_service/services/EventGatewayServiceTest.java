package com.event.event_gateway_service.services;

import com.event.event_gateway_service.entity.Event;
import com.event.event_gateway_service.entity.EventStatus;
import com.event.event_gateway_service.entity.EventType;
import com.event.event_gateway_service.repository.EventRepository;
import com.event.event_gateway_service.requestORresponse.EventRequest;
import com.event.event_gateway_service.servicesCommunition.ServiceConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventGatewayService Tests")
class EventGatewayServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ServiceConnection serviceConnection;

    @InjectMocks
    private EventGatewayService eventGatewayService;

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
    @DisplayName("processAllEvent Tests")
    class ProcessAllEventTests {

        @Test
        @DisplayName("Given list of events when processing then should return sorted and processed events")
        void testProcessAllEventSuccess() {
            LocalDateTime time1 = LocalDateTime.now().minusHours(2);
            LocalDateTime time2 = LocalDateTime.now().minusHours(1);
            LocalDateTime time3 = LocalDateTime.now();

            EventRequest event1 = new EventRequest(1L, "ACC1", EventType.DEBIT, new BigDecimal("100"), "USD", time3, EventStatus.PENDING);
            EventRequest event2 = new EventRequest(2L, "ACC2", EventType.CREDIT, new BigDecimal("200"), "USD", time1, EventStatus.PENDING);
            EventRequest event3 = new EventRequest(3L, "ACC3", EventType.DEBIT, new BigDecimal("300"), "USD", time2, EventStatus.PENDING);
            List<EventRequest> events = Arrays.asList(event1, event2, event3);

            Event savedEvent1 = Event.builder().eventId(event1.eventId()).accountId(event1.accountId())
                    .type(event1.type()).amount(event1.amount()).currency(event1.currency())
                    .eventTimestamp(event1.eventTimestamp()).eventStatus(EventStatus.PROCESSED).build();
            Event savedEvent2 = Event.builder().eventId(event2.eventId()).accountId(event2.accountId())
                    .type(event2.type()).amount(event2.amount()).currency(event2.currency())
                    .eventTimestamp(event2.eventTimestamp()).eventStatus(EventStatus.PROCESSED).build();
            Event savedEvent3 = Event.builder().eventId(event3.eventId()).accountId(event3.accountId())
                    .type(event3.type()).amount(event3.amount()).currency(event3.currency())
                    .eventTimestamp(event3.eventTimestamp()).eventStatus(EventStatus.PROCESSED).build();

            when(eventRepository.findByEventId(any())).thenReturn(Optional.empty());
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            when(eventRepository.save(any())).thenReturn(savedEvent1, savedEvent2, savedEvent3);

            List<Event> result = eventGatewayService.processAllEvent(events);

            assertNotNull(result);
            assertEquals(3, result.size());
            verify(eventRepository, times(3)).save(any());
            verify(serviceConnection, times(3)).createTransaction(any(), any());
        }

        @Test
        @DisplayName("Given empty list when processing then should return empty list")
        void testProcessAllEventWithEmptyList() {
            List<EventRequest> events = List.of();

            List<Event> result = eventGatewayService.processAllEvent(events);

            assertNotNull(result);
            assertEquals(0, result.size());
            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("processEvent Tests")
    class ProcessEventTests {

        @Test
        @DisplayName("Given valid event request when processing then should save event with PROCESSED status")
        void testProcessEventSuccess() {
            when(eventRepository.findByEventId(1L)).thenReturn(Optional.empty());
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.OK));
            when(eventRepository.save(any())).thenReturn(event);

            Event result = eventGatewayService.processEvent(eventRequest);

            assertNotNull(result);
            assertEquals(EventStatus.PROCESSED, result.getEventStatus());
            assertEquals("ACC123456", result.getAccountId());
            verify(eventRepository, times(1)).save(any());
            verify(serviceConnection, times(1)).createTransaction(eventRequest, "ACC123456");
        }

        @Test
        @DisplayName("Given invalid event when processing then should throw IllegalArgumentException")
        void testProcessEventWithInvalidEventRequest() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    null,
                    EventType.CREDIT,
                    new BigDecimal("100"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
            verify(eventRepository, never()).save(any());
        }

        @Test
        @DisplayName("Given event with blank account id when processing then should throw IllegalArgumentException")
        void testProcessEventWithBlankAccountId() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "   ",
                    EventType.CREDIT,
                    new BigDecimal("100"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with null type when processing then should throw IllegalArgumentException")
        void testProcessEventWithNullType() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    null,
                    new BigDecimal("100"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with negative amount when processing then should throw IllegalArgumentException")
        void testProcessEventWithNegativeAmount() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    EventType.CREDIT,
                    new BigDecimal("-100"),
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with zero amount when processing then should throw IllegalArgumentException")
        void testProcessEventWithZeroAmount() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    EventType.CREDIT,
                    BigDecimal.ZERO,
                    "USD",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with null currency when processing then should throw IllegalArgumentException")
        void testProcessEventWithNullCurrency() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    EventType.CREDIT,
                    new BigDecimal("100"),
                    null,
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with blank currency when processing then should throw IllegalArgumentException")
        void testProcessEventWithBlankCurrency() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    EventType.CREDIT,
                    new BigDecimal("100"),
                    "   ",
                    timestamp,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given event with null timestamp when processing then should throw IllegalArgumentException")
        void testProcessEventWithNullTimestamp() {
            EventRequest invalidEvent = new EventRequest(
                    1L,
                    "ACC123",
                    EventType.CREDIT,
                    new BigDecimal("100"),
                    "USD",
                    null,
                    EventStatus.PENDING
            );

            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(invalidEvent));
        }

        @Test
        @DisplayName("Given null event request when processing then should throw IllegalArgumentException")
        void testProcessEventWithNullRequest() {
            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.processEvent(null));
        }

        @Test
        @DisplayName("Given event with non-200 response when processing then should save with PENDING status")
        void testProcessEventWithNonSuccessfulResponse() {
            when(eventRepository.findByEventId(1L)).thenReturn(Optional.empty());
            when(serviceConnection.createTransaction(any(), any())).thenReturn(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
            when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Event result = eventGatewayService.processEvent(eventRequest);

            assertNotNull(result);
            assertEquals(EventStatus.PENDING, result.getEventStatus());
            verify(eventRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Given existing pending event when processing then should return existing event without reprocessing")
        void testProcessEventWithExistingPendingEvent() {
            Event existingEvent = Event.builder()
                    .eventId(1L)
                    .accountId("ACC123456")
                    .type(EventType.CREDIT)
                    .amount(new BigDecimal("1000.00"))
                    .currency("USD")
                    .eventTimestamp(timestamp)
                    .eventStatus(EventStatus.PENDING)
                    .build();

            when(eventRepository.findByEventId(1L)).thenReturn(Optional.of(existingEvent));

            Event result = eventGatewayService.processEvent(eventRequest);

            assertNotNull(result);
            assertEquals(existingEvent, result);
            verify(serviceConnection, never()).createTransaction(any(), any());
            verify(eventRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getEventById Tests")
    class GetEventByIdTests {

        @Test
        @DisplayName("Given valid event id when getting then should return event")
        void testGetEventByIdSuccess() {
            when(eventRepository.findByEventId(1L)).thenReturn(Optional.of(event));

            Event result = eventGatewayService.getEventById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getEventId());
            assertEquals("ACC123456", result.getAccountId());
            verify(eventRepository, times(1)).findByEventId(1L);
        }

        @Test
        @DisplayName("Given non-existing event id when getting then should return null")
        void testGetEventByIdNotFound() {
            when(eventRepository.findByEventId(999L)).thenReturn(Optional.empty());

            Event result = eventGatewayService.getEventById(999L);

            assertNull(result);
            verify(eventRepository, times(1)).findByEventId(999L);
        }

        @Test
        @DisplayName("Given null id when getting then should throw IllegalArgumentException")
        void testGetEventByIdWithNullId() {
            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.getEventById(null));
            verify(eventRepository, never()).findByEventId(any());
        }
    }

    @Nested
    @DisplayName("getByAccountId Tests")
    class GetByAccountIdTests {

        @Test
        @DisplayName("Given valid account id when getting then should return sorted events")
        void testGetByAccountIdSuccess() {
            LocalDateTime time1 = LocalDateTime.now().minusHours(2);
            LocalDateTime time2 = LocalDateTime.now().minusHours(1);
            LocalDateTime time3 = LocalDateTime.now();

            Event event1 = Event.builder().eventId(1L).accountId("ACC123").type(EventType.CREDIT).amount(new BigDecimal("100"))
                    .currency("USD").eventTimestamp(time3).eventStatus(EventStatus.PENDING).build();
            Event event2 = Event.builder().eventId(2L).accountId("ACC123").type(EventType.DEBIT).amount(new BigDecimal("200"))
                    .currency("USD").eventTimestamp(time1).eventStatus(EventStatus.PROCESSED).build();
            Event event3 = Event.builder().eventId(3L).accountId("ACC123").type(EventType.CREDIT).amount(new BigDecimal("300"))
                    .currency("USD").eventTimestamp(time2).eventStatus(EventStatus.PENDING).build();

            List<Event> events = Arrays.asList(event1, event2, event3);
            when(eventRepository.findByAccountId("ACC123")).thenReturn(events);

            List<Event> result = eventGatewayService.getByAccountId("ACC123");

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(time1, result.get(0).getEventTimestamp());
            assertEquals(time2, result.get(1).getEventTimestamp());
            assertEquals(time3, result.get(2).getEventTimestamp());
            verify(eventRepository, times(1)).findByAccountId("ACC123");
        }

        @Test
        @DisplayName("Given account with no events when getting then should return empty list")
        void testGetByAccountIdWithNoEvents() {
            when(eventRepository.findByAccountId("ACC999")).thenReturn(Arrays.asList());

            List<Event> result = eventGatewayService.getByAccountId("ACC999");

            assertNotNull(result);
            assertEquals(0, result.size());
            verify(eventRepository, times(1)).findByAccountId("ACC999");
        }

        @Test
        @DisplayName("Given null account id when getting then should throw IllegalArgumentException")
        void testGetByAccountIdWithNullId() {
            assertThrows(IllegalArgumentException.class, () -> eventGatewayService.getByAccountId(null));
            verify(eventRepository, never()).findByAccountId(any());
        }

        @Test
        @DisplayName("Given single event when getting by account then should return list with single event")
        void testGetByAccountIdWithSingleEvent() {
            Event singleEvent = Event.builder().eventId(1L).accountId("ACC123").type(EventType.CREDIT).amount(new BigDecimal("100"))
                    .currency("USD").eventTimestamp(timestamp).eventStatus(EventStatus.PROCESSED).build();
            when(eventRepository.findByAccountId("ACC123")).thenReturn(Collections.singletonList(singleEvent));

            List<Event> result = eventGatewayService.getByAccountId("ACC123");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(singleEvent, result.get(0));
        }
    }
}






