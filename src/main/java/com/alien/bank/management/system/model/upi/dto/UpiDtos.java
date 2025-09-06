package com.alien.bank.management.system.model.upi.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CreateVpaRequest {
    @NotNull
    private Long accountId;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SetDefaultVpaRequest {
    @NotBlank
    private String vpa;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SendMoneyRequest {
    @NotBlank
    private String fromVpa;
    @NotBlank
    private String toVpa;
    @Positive
    private Double amount;
    private String note;
    @NotBlank
    private String pin;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RequestMoneyRequest {
    @NotBlank
    private String payerVpa;
    @Positive
    private Double amount;
    private String reason;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class SetUpiPinRequest {
    @NotBlank
    private String newPin;
}


