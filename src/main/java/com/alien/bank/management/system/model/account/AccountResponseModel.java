package com.alien.bank.management.system.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponseModel {
    private Long accountId;
    private String maskedCard;
    private Double balance;
}
