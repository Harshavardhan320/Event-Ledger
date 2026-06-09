package com.event.event_gateway_service.requestORresponse;

import com.event.event_gateway_service.entity.EventStatus;
import com.event.event_gateway_service.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventRequest(
        @NotNull
        Long eventId,
        @NotBlank
        String accountId,
        @NotNull
        EventType type,
        @NotNull
        @Positive
        BigDecimal amount,
        @NotBlank
        String currency,
        @NotNull
        LocalDateTime eventTimestamp,
        EventStatus eventStatus
) {
}
