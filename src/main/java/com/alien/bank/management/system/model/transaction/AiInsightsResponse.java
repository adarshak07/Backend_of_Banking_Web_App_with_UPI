package com.alien.bank.management.system.model.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiInsightsResponse {
    private Long accountId;
    private Double currentBalance;
    private String naturalLanguageSummary;
    private List<Map<String, Object>> topSpendingCategories;
    private List<Map<String, Object>> monthlyTrends;
    private Double projectedMonthEndBalance;
    private List<String> riskAlerts;
    private List<String> savingsSuggestions;
}


