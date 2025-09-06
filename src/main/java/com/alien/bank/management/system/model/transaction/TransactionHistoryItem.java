package com.alien.bank.management.system.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHistoryItem {
    private Long transactionId;
    private String type;          // "DEPOSIT" / "WITHDRAW"
    private Double amount;
    private Date dateTime;
    private Double balanceAfter;
}