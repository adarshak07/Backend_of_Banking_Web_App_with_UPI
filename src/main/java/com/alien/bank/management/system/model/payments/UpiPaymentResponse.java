package com.alien.bank.management.system.model.payments;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpiPaymentResponse {
    private Long id;
    private String status;
    private String refId;
    private Double balanceAfter;
    private Integer coinsEarned;
}


