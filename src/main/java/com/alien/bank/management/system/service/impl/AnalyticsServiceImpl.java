package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Transaction;
import com.alien.bank.management.system.entity.TransactionType;
import com.alien.bank.management.system.model.transaction.AiInsightsResponse;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.TransactionRepository;
import com.alien.bank.management.system.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import jakarta.persistence.EntityNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    public AiInsightsResponse generateAiInsights(Long accountId, Integer lookbackMonths) {
        if (lookbackMonths == null || lookbackMonths <= 0) lookbackMonths = 6;

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));

        // Ensure the current authenticated user owns this account
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if (account.getUser() == null || account.getUser().getEmail() == null || !account.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You cannot view another user's analytics");
        }

        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.MONTH, -lookbackMonths);
        Date fromDate = cal.getTime();

        List<Transaction> txs = transactionRepository
                .findByAccountIdAndTimestampBetweenOrderByTimestampAsc(accountId, fromDate, toDate);

        // Categorize by type as a simple heuristic
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Transaction t : txs) {
            String category = determineCategory(t.getType());
            categoryTotals.merge(category, t.getAmount(), Double::sum);
        }

        List<Map<String, Object>> topCategories = categoryTotals.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category", e.getKey());
                    m.put("total", round2(e.getValue()));
                    return m;
                })
                .collect(Collectors.toList());

        // Monthly trends
        Map<String, Double> monthNet = new LinkedHashMap<>();
        Calendar iter = Calendar.getInstance();
        iter.setTime(fromDate);
        while (!iter.getTime().after(toDate)) {
            String key = iter.get(Calendar.YEAR) + "-" + String.format("%02d", iter.get(Calendar.MONTH) + 1);
            monthNet.put(key, 0.0);
            iter.add(Calendar.MONTH, 1);
        }
        for (Transaction t : txs) {
            Calendar ts = Calendar.getInstance();
            ts.setTime(t.getTimestamp());
            String key = ts.get(Calendar.YEAR) + "-" + String.format("%02d", ts.get(Calendar.MONTH) + 1);
            double delta = 0.0;
            if (t.getType() == TransactionType.DEPOSIT || t.getType() == TransactionType.INTEREST || t.getType() == TransactionType.REFUND || t.getType() == TransactionType.TRANSFER_IN) {
                delta = t.getAmount();
            } else if (t.getType() == TransactionType.WITHDRAW || t.getType() == TransactionType.FEE || t.getType() == TransactionType.PAYMENT || t.getType() == TransactionType.TRANSFER_OUT) {
                delta = -t.getAmount();
            }
            monthNet.merge(key, delta, Double::sum);
        }

        List<Map<String, Object>> monthlyTrends = monthNet.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("month", e.getKey());
                    m.put("netChange", round2(e.getValue()));
                    return m;
                })
                .collect(Collectors.toList());

        // Simple projection: current balance + average monthly net
        double avgMonthlyNet = monthNet.isEmpty() ? 0.0 : monthNet.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double projected = account.getBalance() + avgMonthlyNet;

        // Generate risk alerts and suggestions heuristically
        List<String> alerts = new ArrayList<>();
        if (avgMonthlyNet < 0) {
            alerts.add("Spending exceeds income on average. Consider budgeting.");
        }
        double totalFees = categoryTotals.getOrDefault("Bank Charges", 0.0);
        if (totalFees > 0) {
            alerts.add("You incurred " + toCurrency(totalFees) + " in fees. Avoid out-of-network withdrawals or maintain minimum balance.");
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Automate a monthly transfer to savings equal to 10% of income.");
        suggestions.add("Set alerts for large withdrawals over a chosen threshold.");

        String summary = buildSummary(account.getBalance(), topCategories, avgMonthlyNet, alerts);

        return AiInsightsResponse.builder()
                .accountId(account.getId())
                .currentBalance(account.getBalance())
                .naturalLanguageSummary(summary)
                .topSpendingCategories(topCategories)
                .monthlyTrends(monthlyTrends)
                .projectedMonthEndBalance(round2(projected))
                .riskAlerts(alerts)
                .savingsSuggestions(suggestions)
                .build();
    }

    private String determineCategory(TransactionType type) {
        switch (type) {
            case DEPOSIT: return "Income";
            case WITHDRAW: return "Expense";
            case FEE: return "Bank Charges";
            case INTEREST: return "Interest";
            case TRANSFER_IN: return "Transfer";
            case TRANSFER_OUT: return "Transfer";
            case REFUND: return "Refund";
            case PAYMENT: return "Payment";
            default: return "Other";
        }
    }

    private String buildSummary(Double balance, List<Map<String, Object>> topCategories, double avgMonthlyNet, List<String> alerts) {
        String topCat = topCategories.isEmpty() ? "N/A" : String.valueOf(topCategories.get(0).get("category"));
        String trend = avgMonthlyNet >= 0 ? "positive" : "negative";
        return "Your current balance is " + toCurrency(balance) + 
                ". Top spending category: " + topCat + ". Average monthly net is " + toCurrency(avgMonthlyNet) + 
                " indicating a " + trend + " trend. " + (alerts.isEmpty() ? "No immediate risks detected." : alerts.get(0));
    }

    private String toCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}


