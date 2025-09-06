package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseModel> summary() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        var accounts = accountRepository.findAllByUser(user);
        double totalBalance = accounts.stream().map(a -> a.getBalance()).filter(Objects::nonNull).mapToDouble(Double::doubleValue).sum();
        int totalAccounts = accounts.size();
        // gather recent transactions across all accounts (simple approach)
        var recent = accounts.isEmpty() ? List.<com.alien.bank.management.system.entity.Transaction>of() :
                transactionRepository.findAll().stream()
                        .filter(t -> t.getAccount()!=null && accounts.stream().anyMatch(a->a.getId().equals(t.getAccount().getId())))
                        .sorted((a,b)-> b.getTimestamp().compareTo(a.getTimestamp()))
                        .limit(10)
                        .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalBalance", totalBalance);
        payload.put("totalAccounts", totalAccounts);
        payload.put("lastTransactions", recent != null ? recent.stream().limit(10).toList() : List.of());

        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(payload).build());
    }
}


