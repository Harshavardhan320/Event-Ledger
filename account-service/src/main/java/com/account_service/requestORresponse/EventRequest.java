package com.account_service.requestORresponse;

import com.account_service.entity.EventStatus;
import com.account_service.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventRequest(
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
