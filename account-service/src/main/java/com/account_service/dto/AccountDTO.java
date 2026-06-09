package com.account_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDTO {
    
    private Long id;
    private String accountId;
    private BigDecimal balance;
    private List<TransactionsDTO> transactions;
}

