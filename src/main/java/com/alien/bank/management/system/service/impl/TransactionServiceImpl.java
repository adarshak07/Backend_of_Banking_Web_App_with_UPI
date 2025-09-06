package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Transaction;
import com.alien.bank.management.system.entity.TransactionType;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.exception.LowBalanceException;
import com.alien.bank.management.system.mapper.TransactionMapper;
import com.alien.bank.management.system.model.transaction.DepositRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionResponseModel;
import com.alien.bank.management.system.model.transaction.WithdrawRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionHistoryItem;
import com.alien.bank.management.system.model.transaction.TransactionStatementRequest;
import com.alien.bank.management.system.model.transaction.TransactionStatementResponse;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.TransactionRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Calendar;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserRepository userRepository; // NEW
    
    @Override
    public TransactionResponseModel deposit(DepositRequestModel request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // Ensure the current authenticated user owns this account
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!account.getUser().getEmail().equals(email)) {
            throw new BadCredentialsException("Account does not belong to current user");
        }

        Long transactionId = performDeposit(account, request.getAmount());
        return transactionMapper.toResponseModel(transactionId, request.getAmount(), account.getBalance());
    }

    @Override
    public TransactionResponseModel withdraw(WithdrawRequestModel request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Account not found"));

        // Ensure the current authenticated user owns this account
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        if (!account.getUser().getEmail().equals(email)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Account does not belong to current user");
        }

        if (account.getBalance() < request.getAmount()) {
            throw new LowBalanceException("Insufficient balance");
        }

        Long transactionId = performWithdrawal(account, request.getAmount());
        return transactionMapper.toResponseModel(transactionId, request.getAmount(), account.getBalance());
    }

    private Long performDeposit(Account account, double amount) {
        updateAccountBalance(account, amount);
        Transaction transaction = transactionMapper.toEntity(amount, account, TransactionType.DEPOSIT);
        transactionRepository.save(transaction);
        return transaction.getId();
    }

    private Long performWithdrawal(Account account, double amount) {
        updateAccountBalance(account, -amount);
        Transaction transaction = transactionMapper.toEntity(amount, account, TransactionType.WITHDRAW);
        transactionRepository.save(transaction);
        return transaction.getId();
    }

    private void updateAccountBalance(Account account, double amount) {
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
    }

    @Override
    public List<TransactionHistoryItem> getHistory(Long accountId) {
        // Security check
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account " + accountId + " not found"));

        if (account.getUser() == null || !account.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You cannot view another user's transactions");
        }

        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
        return transactions.stream()
                .map(tx -> TransactionHistoryItem.builder()
                        .transactionId(tx.getId())
                        .type(tx.getType().name())
                        .amount(tx.getAmount())
                        .dateTime(tx.getTimestamp())
                        .balanceAfter(tx.getBalanceAfter())
                        .build())
                .toList();
    }

    @Override
    public TransactionStatementResponse getTransactionStatement(TransactionStatementRequest request) {
        // Validate account exists and belongs to current user
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        
        // Get current user from security context
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Verify account belongs to current user
        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Account does not belong to current user");
        }
        
        // Set default dates if not provided
        Date fromDate = request.getFromDate();
        Date toDate = request.getToDate();
        
        if (fromDate == null) {
            // Default to 30 days ago
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30);
            fromDate = cal.getTime();
        }
        
        if (toDate == null) {
            // Default to current time
            toDate = new Date();
        }
        
        // Create pageable for pagination
        Pageable pageable = PageRequest.of(
                request.getPage() - 1, // Spring Data uses 0-based indexing
                request.getPageSize(),
                Sort.by(Sort.Direction.fromString(request.getSortDirection()), request.getSortBy())
        );
        
        // Get filtered transactions
        List<TransactionType> transactionTypes = null;
        if (request.getTransactionTypes() != null && !request.getTransactionTypes().isEmpty()) {
            transactionTypes = request.getTransactionTypes().stream()
                    .map(type -> TransactionType.valueOf(type.toUpperCase()))
                    .toList();
        }

        Page<Transaction> transactionPage = transactionRepository.findTransactionsWithFilters(
                request.getAccountId(),
                fromDate,
                toDate,
                request.getMinAmount(),
                request.getMaxAmount(),
                transactionTypes,
                request.getSearchText(),
                pageable
        );

        
        // Get summary statistics
        TransactionRepository.TransactionSummaryProjection summaryData = transactionRepository.getTransactionSummary(
                request.getAccountId(),
                fromDate,
                toDate
        );
        
        // Get opening and closing balances
        Double openingBalance = transactionRepository.getOpeningBalance(request.getAccountId(), fromDate);
        if (openingBalance == null) {
            openingBalance = 0.0;
        }
        
        Double closingBalance = transactionRepository.getClosingBalance(request.getAccountId(), toDate);
        if (closingBalance == null) {
            closingBalance = account.getBalance();
        }
        
        // Build response
        return TransactionStatementResponse.builder()
                .accountId(account.getId())
                .cardNumber("****-****-****-" + account.getLast4Digits())
                .currentBalance(account.getBalance())
                .statementDate(new Date())
                .fromDate(fromDate)
                .toDate(toDate)
                .summary(buildTransactionSummary(summaryData, openingBalance, closingBalance))
                .pagination(buildPaginationInfo(transactionPage, request.getPage(), request.getPageSize()))
                .transactions(buildEnhancedTransactionItems(transactionPage.getContent()))
                .build();
    }

    private TransactionStatementResponse.TransactionSummary buildTransactionSummary(
            TransactionRepository.TransactionSummaryProjection summaryData, 
            Double openingBalance, 
            Double closingBalance) {
        
        Double totalDeposits = summaryData.getTotalDeposits() != null ? summaryData.getTotalDeposits() : 0.0;
        Double totalWithdrawals = summaryData.getTotalWithdrawals() != null ? summaryData.getTotalWithdrawals() : 0.0;
        Double totalFees = summaryData.getTotalFees() != null ? summaryData.getTotalFees() : 0.0;
        Double totalInterest = summaryData.getTotalInterest() != null ? summaryData.getTotalInterest() : 0.0;
        
        return TransactionStatementResponse.TransactionSummary.builder()
                .totalTransactions(summaryData.getTotalTransactions() != null ? summaryData.getTotalTransactions().intValue() : 0)
                .totalDeposits(totalDeposits)
                .totalWithdrawals(totalWithdrawals)
                .totalFees(totalFees)
                .totalInterest(totalInterest)
                .netChange(totalDeposits - totalWithdrawals + totalInterest - totalFees)
                .openingBalance(openingBalance)
                .closingBalance(closingBalance)
                .build();
    }

    private TransactionStatementResponse.PaginationInfo buildPaginationInfo(Page<Transaction> transactionPage, Integer currentPage, Integer pageSize) {
        return TransactionStatementResponse.PaginationInfo.builder()
                .currentPage(currentPage)
                .pageSize(pageSize)
                .totalPages(transactionPage.getTotalPages())
                .totalElements(transactionPage.getTotalElements())
                .hasNext(transactionPage.hasNext())
                .hasPrevious(transactionPage.hasPrevious())
                .build();
    }

    private List<TransactionStatementResponse.EnhancedTransactionItem> buildEnhancedTransactionItems(List<Transaction> transactions) {
        return transactions.stream()
                .map(tx -> TransactionStatementResponse.EnhancedTransactionItem.builder()
                        .transactionId(tx.getId())
                        .type(tx.getType().name())
                        .amount(tx.getAmount())
                        .timestamp(tx.getTimestamp())
                        .balanceAfter(tx.getBalanceAfter())
                        .notes(tx.getNotes())
                        .category(determineCategory(tx.getType()))
                        .referenceNumber("TXN" + String.format("%06d", tx.getId()))
                        .counterpartyAccount(null) // Will be implemented for transfers
                        .build())
                .toList();
    }
    
    private String determineCategory(TransactionType type) {
        switch (type) {
            case DEPOSIT:
                return "Income";
            case WITHDRAW:
                return "Expense";
            case FEE:
                return "Bank Charges";
            case INTEREST:
                return "Interest";
            case TRANSFER_IN:
                return "Transfer";
            case TRANSFER_OUT:
                return "Transfer";
            case REFUND:
                return "Refund";
            case PAYMENT:
                return "Payment";
            default:
                return "Other";
        }
    }
    
    @Override
    public byte[] exportStatementAsPdf(TransactionStatementRequest request) {
        // TODO: Implement PDF export using libraries like iText or Apache PDFBox
        // For now, return a simple text representation
        TransactionStatementResponse statement = getTransactionStatement(request);
        String content = "Account Statement\n" +
                "Account: " + statement.getAccountId() + "\n" +
                "Period: " + statement.getFromDate() + " to " + statement.getToDate() + "\n" +
                "Total Transactions: " + statement.getSummary().getTotalTransactions();
        return content.getBytes();
    }
    
    @Override
    public byte[] exportStatementAsCsv(TransactionStatementResponse statement) {
        // TODO: Implement CSV export
        StringBuilder csv = new StringBuilder();
        csv.append("Transaction ID,Type,Amount,Date,Balance After,Notes,Category\n");
        
        for (TransactionStatementResponse.EnhancedTransactionItem item : statement.getTransactions()) {
            csv.append(String.format("%d,%s,%.2f,%s,%.2f,%s,%s\n",
                    item.getTransactionId(),
                    item.getType(),
                    item.getAmount(),
                    item.getTimestamp(),
                    item.getBalanceAfter(),
                    item.getNotes() != null ? item.getNotes() : "",
                    item.getCategory()
            ));
        }
        
        return csv.toString().getBytes();
    }
}