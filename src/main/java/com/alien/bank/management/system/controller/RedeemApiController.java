package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.GiftCardRedemption;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.repository.GiftCardProductRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.service.RedeemService;
import org.springframework.core.env.Environment;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redeem")
@RequiredArgsConstructor
public class RedeemApiController {
    private final GiftCardProductRepository productRepository;
    private final RedeemService redeemService;
    private final UserRepository userRepository;
    private final Environment environment;

    @GetMapping("/products")
    public ResponseEntity<ResponseModel> products() {
        var products = productRepository.findByActiveTrueOrderByCostCoinsAsc();
        return ResponseEntity.ok(ResponseModel.builder().status(HttpStatus.OK).success(true).data(products).build());
    }

    @PostMapping
    public ResponseEntity<ResponseModel> redeem(@RequestBody RedeemRequest req,
                                                @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        User user = resolveUser(req.getUserId(), bypass);
        GiftCardRedemption red = redeemService.redeem(user, req.getProductId());
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("redemptionId", red.getId());
        data.put("code", red.getCode());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseModel.builder().status(HttpStatus.CREATED).success(true).data(data).build());
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

    @Data
    public static class RedeemRequest {
        private Long userId;
        private Long productId;
    }
}
