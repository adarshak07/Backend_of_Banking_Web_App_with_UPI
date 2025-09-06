package com.alien.bank.management.system.service;

import com.alien.bank.management.system.model.transaction.AiInsightsResponse;

public interface AnalyticsService {
    AiInsightsResponse generateAiInsights(Long accountId, Integer lookbackMonths);
}


