package com.alien.bank.management.system.model.upi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUpiIdResponse {
    private String status;
    private String upiId;
    private String linkedAccount;
    private String message;
}
