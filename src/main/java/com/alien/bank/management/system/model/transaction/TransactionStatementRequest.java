package com.alien.bank.management.system.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatementRequest {
    
    @NotNull(message = "Account ID is required")
    private Long accountId;
    
    // Date range filtering
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date fromDate;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date toDate;
    
    // Amount range filtering
    private Double minAmount;
    private Double maxAmount;
    
    // Transaction type filtering
    private List<String> transactionTypes;
    
    // Search by notes
    private String searchText;
    
    // Pagination
    @Min(value = 1, message = "Page number must be at least 1")
    @Builder.Default
    private Integer page = 1;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Builder.Default
    private Integer pageSize = 20;
    
    // Sorting
    @Builder.Default
    private String sortBy = "timestamp";
    
    @Builder.Default
    private String sortDirection = "DESC"; // ASC or DESC
}
