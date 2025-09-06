package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.model.transaction.AiInsightsResponse;
import com.alien.bank.management.system.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // AI-powered insights endpoint
    @GetMapping("/ai-insights/{accountId}")
    public ResponseEntity<ResponseModel> getAiInsights(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "6") Integer months
    ) {
        AiInsightsResponse insights = analyticsService.generateAiInsights(accountId, months);
        return ResponseEntity.ok(
                ResponseModel.builder()
                        .status(HttpStatus.OK)
                        .success(true)
                        .data(insights)
                        .build()
        );
    }
}
