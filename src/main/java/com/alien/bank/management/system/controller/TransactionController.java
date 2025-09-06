package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.model.transaction.DepositRequestModel;
import com.alien.bank.management.system.model.transaction.WithdrawRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionHistoryItem;
import com.alien.bank.management.system.model.transaction.TransactionStatementRequest;
import com.alien.bank.management.system.model.transaction.TransactionStatementResponse;
import com.alien.bank.management.system.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<ResponseModel> deposit(@Valid @RequestBody DepositRequestModel request) {
        return ResponseEntity.ok(
                ResponseModel
                        .builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(transactionService.deposit(request))
                        .build()
        );
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ResponseModel> withdraw(@Valid @RequestBody WithdrawRequestModel request) {
        return ResponseEntity.ok(
                ResponseModel
                        .builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(transactionService.withdraw(request))
                        .build()
        );
    }

    // ✅ Existing endpoint: Basic Transaction History
    @GetMapping("/history/{accountId}")
    public ResponseEntity<ResponseModel> getHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(
                ResponseModel.builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(transactionService.getHistory(accountId))
                        .errors(null)
                        .build()
        );
    }
    
    // 🆕 NEW: Enhanced Transaction Statement with filtering and pagination
    @PostMapping("/statement")
    public ResponseEntity<ResponseModel> getTransactionStatement(@Valid @RequestBody TransactionStatementRequest request) {
        TransactionStatementResponse statement = transactionService.getTransactionStatement(request);
        return ResponseEntity.ok(
                ResponseModel.builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(statement)
                        .build()
        );
    }
    
    // 🆕 NEW: Get statement for specific account with query parameters
    @GetMapping("/statement/{accountId}")
    public ResponseEntity<ResponseModel> getStatementByAccount(
            @PathVariable Long accountId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) List<String> transactionTypes,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        TransactionStatementRequest request = TransactionStatementRequest.builder()
                .accountId(accountId)
                .fromDate(parseDate(fromDate))
                .toDate(parseDate(toDate))
                .minAmount(minAmount)
                .maxAmount(maxAmount)
                .transactionTypes(transactionTypes)
                .searchText(searchText)
                .page(page)
                .pageSize(pageSize)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        
        TransactionStatementResponse statement = transactionService.getTransactionStatement(request);
        return ResponseEntity.ok(
                ResponseModel.builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(statement)
                        .build()
        );
    }
    
    // 🆕 NEW: Export statement as PDF
    @PostMapping(value = "/statement/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportStatementAsPdf(@Valid @RequestBody TransactionStatementRequest request) {
        byte[] pdfContent = transactionService.exportStatementAsPdf(request);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"statement.pdf\"")
                .body(pdfContent);
    }
    
    // 🆕 NEW: Export statement as CSV
    @PostMapping(value = "/statement/export/csv", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<byte[]> exportStatementAsCsv(@Valid @RequestBody TransactionStatementRequest request) {
        TransactionStatementResponse statement = transactionService.getTransactionStatement(request);
        byte[] csvContent = transactionService.exportStatementAsCsv(statement);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"statement.csv\"")
                .body(csvContent);
    }
    
    // 🆕 NEW: Quick statement for last 30 days
    @GetMapping("/statement/{accountId}/quick")
    public ResponseEntity<ResponseModel> getQuickStatement(@PathVariable Long accountId) {
        TransactionStatementRequest request = TransactionStatementRequest.builder()
                .accountId(accountId)
                .page(1)
                .pageSize(50)
                .build();
        
        TransactionStatementResponse statement = transactionService.getTransactionStatement(request);
        return ResponseEntity.ok(
                ResponseModel.builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(statement)
                        .build()
        );
    }
    
    // Helper method to parse date strings
    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }
}