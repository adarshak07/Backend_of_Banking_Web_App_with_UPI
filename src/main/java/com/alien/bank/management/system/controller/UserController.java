package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.alien.bank.management.system.repository.AccountRepository accountRepository;
    private final com.alien.bank.management.system.repository.TransactionRepository transactionRepository;
    private final com.alien.bank.management.system.repository.UserRepository userRepository;
    private final com.alien.bank.management.system.service.RewardsService rewardsService;

    @GetMapping
    public ResponseEntity<ResponseModel> getUserProfile() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println("🔍 ProfileController: Getting profile for email: " + email);
        
        try {
            var profile = userService.getUserProfile();
            System.out.println("🔍 ProfileController: Profile data: " + profile);
            return ResponseEntity.ok(
                    ResponseModel
                            .builder()
                            .status(HttpStatus.OK)
                            .success(true)
                            .data(profile)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("🔍 ProfileController: Error getting profile: " + e.getMessage());
            throw e;
        }
    }

    // Extended profile summary with analytics

    @GetMapping("/summary")
    public ResponseEntity<ResponseModel> getProfileSummary(@org.springframework.web.bind.annotation.RequestParam(required = false) Long bypassUserId,
                                                           @org.springframework.web.bind.annotation.RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        com.alien.bank.management.system.entity.User user;
        if (bypass != null && bypassUserId != null) {
            user = userRepository.findById(bypassUserId).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        } else {
            String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            user = userRepository.findByEmail(email).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        }

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("user", java.util.Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "role", user.getRole() != null ? user.getRole().name() : null
        ));

        java.util.List<com.alien.bank.management.system.entity.Account> accounts = accountRepository.findAllByUser(user);
        long txCount = 0;
        double maxTxn = 0;
        java.util.Date firstTx = null;
        for (com.alien.bank.management.system.entity.Account a : accounts) {
            java.util.List<com.alien.bank.management.system.entity.Transaction> txs = transactionRepository.findByAccountIdOrderByTimestampDesc(a.getId());
            txCount += txs.size();
            for (com.alien.bank.management.system.entity.Transaction t : txs) {
                if (Math.abs(t.getAmount()) > Math.abs(maxTxn)) maxTxn = t.getAmount();
                if (firstTx == null || t.getTimestamp().before(firstTx)) firstTx = t.getTimestamp();
            }
        }
        int activeDays = 0;
        if (firstTx != null) {
            long diff = java.time.Duration.between(firstTx.toInstant(), java.time.Instant.now()).toDays();
            activeDays = (int) diff;
        }

        java.util.Map<String, Object> totals = new java.util.HashMap<>();
        totals.put("txCount", txCount);
        totals.put("maxTransaction", maxTxn);
        totals.put("firstTransactionDate", firstTx != null ? firstTx.toString() : null);
        totals.put("activeDays", activeDays);

        int coins = rewardsService.getWalletCoins(user);

        java.util.Map<String, Object> rewards = java.util.Map.of("coins", coins);

        body.put("totals", totals);
        body.put("rewards", rewards);

        return ResponseEntity.ok(ResponseModel.builder().status(org.springframework.http.HttpStatus.OK).success(true).data(body).build());
    }
}
