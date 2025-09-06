package com.alien.bank.management.system.service;

import com.alien.bank.management.system.model.transaction.DepositRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionResponseModel;
import com.alien.bank.management.system.model.transaction.WithdrawRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionHistoryItem;
import com.alien.bank.management.system.model.transaction.TransactionStatementRequest;
import com.alien.bank.management.system.model.transaction.TransactionStatementResponse;

import java.util.List;

public interface TransactionService {
    TransactionResponseModel deposit(DepositRequestModel request);
    TransactionResponseModel withdraw(WithdrawRequestModel request);

    // Existing method
    List<TransactionHistoryItem> getHistory(Long accountId);
    
    // NEW: Enhanced statement functionality
    TransactionStatementResponse getTransactionStatement(TransactionStatementRequest request);
    
    // NEW: Export functionality
    byte[] exportStatementAsPdf(TransactionStatementRequest request);
    byte[] exportStatementAsCsv(TransactionStatementResponse statement);
}