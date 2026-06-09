package com.event.event_gateway_service.repository;

import com.event.event_gateway_service.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository
        extends JpaRepository<Event, Long> {

    List<Event> findByAccountIdOrderByEventTimestampAsc(String accountId);
    Optional<Event> findByEventId(Long id);
    List<Event> findByAccountId(String id);
}