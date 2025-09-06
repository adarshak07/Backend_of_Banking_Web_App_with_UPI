package com.alien.bank.management.system.model.payments;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpiPaymentRequest {
    private Long userId; // optional when dev bypass header set

    @NotNull
    private Long accountId;

    @NotBlank
    private String vpa;

    @NotNull
    @DecimalMin(value = "0.01")
    private Double amount;

    @NotBlank
    private String category; // code like FOOD

    private String note;
}


