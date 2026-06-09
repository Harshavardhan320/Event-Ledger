package com.account_service.dto;

import com.account_service.entity.EventStatus;
import com.account_service.entity.EventType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionsDTO {
    
    private Long id;
    private BigDecimal transactionAmount;
    private EventType type;
    private Long accountId;
    private EventStatus transactionStatus;
}

