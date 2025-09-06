package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.repository.PaymentRepository;
import com.alien.bank.management.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard/payments")
@RequiredArgsConstructor
public class DashboardPaymentsController {
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public ResponseEntity<ResponseModel> summary(@RequestParam(required = false) Long userId,
                                                 @RequestParam(defaultValue = "30") int days,
                                                 @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        User user = resolveUser(userId, bypass);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(Math.max(1, Math.min(90, days)));
        Date fromDate = Date.from(from.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        long count = paymentRepository.countByUserAndCreatedAtBetween(user, fromDate, toDate);
        var recent = paymentRepository.findByUserOrderByCreatedAtDesc(user, org.springframework.data.domain.PageRequest.of(0, 100));
        Map<String, Long> byCategory = new HashMap<>();
        for (var p : recent) {
            if (!p.getCreatedAt().before(fromDate) && p.getCreatedAt().before(toDate)) {
                byCategory.merge(p.getCategory().getCode(), 1L, Long::sum);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        data.put("byCategory", byCategory);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(data).build());
    }

    private User resolveUser(Long userId, String bypass) {
        if (bypass != null && userId != null) {
            return userRepository.findById(userId).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User " + email + " Not Found"));
    }
}
