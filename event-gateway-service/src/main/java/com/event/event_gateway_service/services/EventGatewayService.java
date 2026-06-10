package com.event.event_gateway_service.services;

import com.event.event_gateway_service.entity.Event;
import com.event.event_gateway_service.entity.EventStatus;
import com.event.event_gateway_service.repository.EventRepository;
import com.event.event_gateway_service.requestORresponse.EventRequest;
import com.event.event_gateway_service.servicesCommunition.ServiceConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventGatewayService {

    private final static Logger log = LoggerFactory.getLogger(EventGatewayService.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ServiceConnection serviceConnection;

    public List<Event> processAllEvent(List<EventRequest> events) {
        log.info("processing in comings events.");
        return events.stream()
                .sorted(Comparator.comparing(EventRequest::eventTimestamp))
                .map(this::processEvent)
                .collect(Collectors.toList());
    }

    public Event processEvent(EventRequest event) {
        if (!validation(event)) {
            throw new IllegalArgumentException("Invalid event request");
        }

        Event event1 = getEventById(event.eventId());
        if (event1 != null) {
            log.warn("Duplicate Event found with existing ID, cannot process [ id: {} ]", event.eventId());
            return event1;
        }

        Event ev = Event.builder()
                .eventId(event.eventId())
                .accountId(event.accountId())
                .type(event.type())
                .amount(event.amount())
                .currency(event.currency())
                .eventTimestamp(event.eventTimestamp())
                .eventStatus(event.eventStatus())
                .build();

        HttpStatusCode response = serviceConnection.createTransaction(event, ev.getAccountId()).getStatusCode();
        if(response.equals(HttpStatusCode.valueOf(200))){
            ev.setEventStatus(EventStatus.PROCESSED);
        }
        return eventRepository.save(ev);
    }


    public Event getEventById(Long id) {

        if (id == null) {
            throw new IllegalArgumentException("Invalid Input Id");
        }
        Optional<Event> event = eventRepository.findByEventId(id);
        return event.orElse(null);
    }

    private boolean validation(EventRequest event) {
        if (event == null) return false;
        if (event.accountId() == null || event.accountId().isBlank()) return false;
        if (event.type() == null) return false;
        if (event.amount() == null || event.amount().signum() <= 0) {
            log.warn("Invalid transaction amount for account: {}", event.accountId());
            throw new IllegalArgumentException("Invalid transactions amount for account: " + event.accountId());
        }
        if (event.currency() == null || event.currency().isBlank()) return false;
        return event.eventTimestamp() != null;
    }

    public List<Event> getByAccountId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Invalid Input Id");
        }
        return eventRepository.findByAccountId(id)
                .stream()
                .sorted(Comparator.comparing(Event::getEventTimestamp))
                .collect(Collectors.toList());
    }
}
