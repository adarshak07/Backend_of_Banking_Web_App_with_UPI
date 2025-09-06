package com.alien.bank.management.system.controller;

import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.model.ResponseModel;
import com.alien.bank.management.system.model.payments.UpiPaymentRequest;
import com.alien.bank.management.system.model.payments.UpiPaymentResponse;
import com.alien.bank.management.system.repository.PaymentRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.service.PaymentService;
import org.springframework.core.env.Environment;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentsApiController {
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    @PostMapping("/upi")
    public ResponseEntity<ResponseModel> upi(@Valid @RequestBody UpiPaymentRequest request,
                                             @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        // Allow payload userId only if bypass header present (dev convenience)
        if (!isDevProfile() || bypass == null) {
            request.setUserId(null);
        }
        UpiPaymentResponse resp = paymentService.upiPay(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseModel.builder().status(HttpStatus.CREATED).success(true).data(resp).build()
        );
    }

    @GetMapping("/recent")
    public ResponseEntity<ResponseModel> recent(@RequestParam(required = false) Long userId,
                                                @RequestParam(defaultValue = "10") int limit,
                                                @RequestHeader(value = "X-Bypass-User", required = false) String bypass) {
        User user = resolveUser(userId, bypass);
        var list = paymentRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, Math.max(1, Math.min(50, limit))));
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
