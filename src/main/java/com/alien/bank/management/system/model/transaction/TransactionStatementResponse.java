package com.alien.bank.management.system.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatementResponse {
    
    // Account Information
    private Long accountId;
    private String cardNumber;
    private Double currentBalance;
    private Date statementDate;
    
    // Statement Period
    private Date fromDate;
    private Date toDate;
    
    // Transaction Summary
    private TransactionSummary summary;
    
    // Pagination Info
    private PaginationInfo pagination;
    
    // Transactions
    private List<EnhancedTransactionItem> transactions;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionSummary {
        private Integer totalTransactions;
        private Double totalDeposits;
        private Double totalWithdrawals;
        private Double totalFees;
        private Double totalInterest;
        private Double netChange;
        private Double openingBalance;
        private Double closingBalance;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaginationInfo {
        private Integer currentPage;
        private Integer pageSize;
        private Integer totalPages;
        private Long totalElements;
        private Boolean hasNext;
        private Boolean hasPrevious;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EnhancedTransactionItem {
        private Long transactionId;
        private String type;
        private Double amount;
        private Date timestamp;
        private Double balanceAfter;
        private String notes;
        private String category;
        private String referenceNumber;
        private String counterpartyAccount; // for transfers
    }
}
