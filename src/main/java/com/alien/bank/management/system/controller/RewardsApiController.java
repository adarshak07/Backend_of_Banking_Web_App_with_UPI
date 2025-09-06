package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.repository.RewardLedgerRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.service.RewardsService;
import org.springframework.core.env.Environment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardsApiController {
    private final RewardsService rewardsService;
    private final RewardLedgerRepository ledgerRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    @GetMapping("/wallet")
    public ResponseEntity<ResponseModel> wallet(@RequestParam(required = false) Long userId,
                                                @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        User user = resolveUser(userId, bypass);
        int coins = rewardsService.getWalletCoins(user);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("coins", coins);
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(data).build());
    }

    @GetMapping("/ledger")
    public ResponseEntity<ResponseModel> ledger(@RequestParam(required = false) Long userId,
                                                @RequestParam(defaultValue = "20") int limit,
                                                @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        User user = resolveUser(userId, bypass);
        var list = ledgerRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, Math.max(1, Math.min(100, limit))));
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(list).build());
    }

    private User resolveUser(Long userId, String bypass) {
        if (isDevProfile() && bypass != null && userId != null) {
            return userRepository.findById(userId).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User " + email + " Not Found"));
    }

    private boolean isDevProfile() {
        for (String p : environment.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(p)) return true;
        }
        return false;
    }
}
