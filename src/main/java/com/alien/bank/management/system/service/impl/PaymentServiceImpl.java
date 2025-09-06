package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.exception.InsufficientFundsException;
import com.alien.bank.management.system.model.payments.UpiPaymentRequest;
import com.alien.bank.management.system.model.payments.UpiPaymentResponse;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.PaymentService;
import com.alien.bank.management.system.service.RewardsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final RewardsService rewardsService;
    private final UpiVpaRepository upiVpaRepository;

    private static final String VPA_REGEX = "^[a-zA-Z0-9.\\-_]{3,}@[a-zA-Z]{2,}$";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public UpiPaymentResponse upiPay(UpiPaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Amount must be > 0");
        }
        if (request.getVpa() == null || !request.getVpa().matches(VPA_REGEX)) {
            throw new IllegalArgumentException("Invalid UPI ID format");
        }

        User user = resolveUser(request.getUserId());

        Account account = accountRepository.findByIdForUpdate(request.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Account does not belong to user");
        }

        PaymentCategory category = categoryRepository.findByCode(request.getCategory())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category"));

        // Validate that the recipient VPA exists
        if (!upiVpaRepository.existsByVpa(request.getVpa())) {
            throw new IllegalArgumentException("Recipient UPI ID not found");
        }

        String refId = generateRefId();
        Payment payment = Payment.builder()
                .user(user)
                .account(account)
                .category(category)
                .method(PaymentMethod.UPI)
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .refId(refId)
                .meta(buildMetaJson(request))
                .build();
        payment = paymentRepository.save(payment);

        double available = account.getBalance();
        if (available < request.getAmount()) {
            // mark failed and return 409
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new InsufficientFundsException(available);
        }

        // debit
        account.setBalance(available - request.getAmount());
        accountRepository.save(account);

        // create transaction record
        Transaction txn = Transaction.builder()
                .type(TransactionType.WITHDRAW)
                .amount(request.getAmount())
                .balanceAfter(account.getBalance())
                .timestamp(new Date())
                .notes(request.getNote() != null ? request.getNote() : ("UPI " + request.getVpa()))
                .account(account)
                .build();
        transactionRepository.save(txn);

        // success
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // rewards in same TX
        int coinsEarned = rewardsService.calculateCoinsEarned(request.getAmount());
        rewardsService.recordEarnAndDailyBonus(user, payment.getId(), coinsEarned);

        return UpiPaymentResponse.builder()
                .id(payment.getId())
                .status(payment.getStatus().name())
                .refId(payment.getRefId())
                .balanceAfter(account.getBalance())
                .coinsEarned(coinsEarned)
                .build();
    }

    private User resolveUser(Long userIdOrNull) {
        if (userIdOrNull != null) {
            return userRepository.findById(userIdOrNull).orElseThrow(() -> new EntityNotFoundException("User not found"));
        }
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User " + email + " Not Found"));
    }

    private String generateRefId() {
        String timePart = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        String rand = randomAlphaNum(6);
        return "UPI" + timePart + "-" + rand;
    }

    private String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }

    private String buildMetaJson(UpiPaymentRequest req) {
        // light-weight build to avoid adding a JSON library; DB column is JSON type
        String note = req.getNote() == null ? "" : req.getNote().replace("\"", "\\\"");
        String category = req.getCategory() == null ? "" : req.getCategory();
        String vpa = req.getVpa() == null ? "" : req.getVpa();
        return "{" +
                "\"vpa\":\"" + vpa + "\"," +
                "\"note\":\"" + note + "\"," +
                "\"category\":\"" + category + "\"" +
                "}";
    }
}


