package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.exception.LowBalanceException;
import com.alien.bank.management.system.model.upi.CreateUpiIdRequest;
import com.alien.bank.management.system.model.upi.CreateUpiIdResponse;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.UpiService;
import com.alien.bank.management.system.utils.LogRedactionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UpiServiceImpl implements UpiService {
    private static final Logger logger = LoggerFactory.getLogger(UpiServiceImpl.class);
    
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UpiVpaRepository upiVpaRepository;
    private final UpiRequestRepository upiRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogRedactionUtil logRedactionUtil;

    // Configurable daily limit (could move to properties)
    private static final double DAILY_LIMIT = 50000.0;

    @Override
    @Transactional
    public UpiVpa createVpa(Long accountId) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadCredentialsException("Account does not belong to user");
        }

        // generate VPA as username@mybank (use part before @ from email or name)
        String handle = user.getEmail() != null ? user.getEmail().split("@")[0] : user.getName().replaceAll("\\s+", "").toLowerCase();
        String baseVpa = handle + "@mybank";
        String vpa = baseVpa;
        int suffix = 1;
        while (upiVpaRepository.existsByVpa(vpa)) {
            vpa = handle + suffix + "@mybank";
            suffix++;
        }

        // If no existing VPAs, mark as default
        boolean isDefault = upiVpaRepository.findByUser(user).isEmpty();

        UpiVpa entity = UpiVpa.builder()
                .vpa(vpa)
                .user(user)
                .account(account)
                .isDefault(isDefault)
                .createdAt(new Date())
                .build();
        return upiVpaRepository.save(entity);
    }

    @Override
    public List<UpiVpa> getMyVpas() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        return upiVpaRepository.findByUser(user);
    }

    @Override
    @Transactional
    public void setDefaultVpa(String vpa) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        UpiVpa target = upiVpaRepository.findByVpa(vpa).orElseThrow(() -> new EntityNotFoundException("VPA not found"));
        if (!target.getUser().getId().equals(user.getId())) {
            throw new BadCredentialsException("Cannot set default for another user's VPA");
        }
        // unset others
        List<UpiVpa> vpas = upiVpaRepository.findByUser(user);
        for (UpiVpa v : vpas) {
            v.setIsDefault(v.getId().equals(target.getId()));
        }
        upiVpaRepository.saveAll(vpas);
    }

    @Override
    @Transactional
    public Long sendMoney(String fromVpa, String toVpa, double amount, String note, String pin) {
        logger.info("Starting UPI transfer from {} to {} for amount {}", 
            logRedactionUtil.redactSensitiveData(fromVpa), 
            logRedactionUtil.redactSensitiveData(toVpa), 
            amount);
            
        if (amount <= 0) {
            logger.warn("Invalid amount {} for UPI transfer", amount);
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        UpiVpa from = upiVpaRepository.findByVpa(fromVpa)
            .orElseThrow(() -> {
                logger.warn("Sender VPA not found: {}", logRedactionUtil.redactSensitiveData(fromVpa));
                return new EntityNotFoundException("Sender VPA not found");
            });
        UpiVpa to = upiVpaRepository.findByVpa(toVpa)
            .orElseThrow(() -> {
                logger.warn("Recipient VPA not found: {}", logRedactionUtil.redactSensitiveData(toVpa));
                return new EntityNotFoundException("Recipient VPA not found");
            });

        // PIN check - use individual UPI PIN if available, otherwise fall back to user's global PIN
        String pinHash = from.getUpiPinHash();
        if (pinHash == null) {
            // Fall back to user's global UPI PIN
            pinHash = from.getUser().getUpiPinHash();
        }
        
        if (pinHash == null || !passwordEncoder.matches(pin, pinHash)) {
            logger.warn("Invalid UPI PIN for VPA: {}", logRedactionUtil.redactSensitiveData(fromVpa));
            throw new BadCredentialsException("Invalid UPI PIN");
        }

        // Daily limit (sum of TRANSFER_OUT and PAYMENT for today)
        Date startOfDay = getStartOfDay();
        Date endOfDay = getEndOfDay();
        Double spentToday = transactionRepository.sumOutgoingForRange(from.getAccount().getId(), startOfDay, endOfDay);
        if (spentToday + amount > DAILY_LIMIT) {
            logger.warn("Daily limit exceeded for VPA: {}, spent: {}, attempting: {}", 
                logRedactionUtil.redactSensitiveData(fromVpa), spentToday, amount);
            throw new org.springframework.security.access.AccessDeniedException("Exceeded daily transfer limit");
        }

        Account sender = from.getAccount();
        Account receiver = to.getAccount();
        if (sender.getBalance() < amount) {
            logger.warn("Insufficient balance for VPA: {}, balance: {}, required: {}", 
                logRedactionUtil.redactSensitiveData(fromVpa), sender.getBalance(), amount);
            throw new LowBalanceException("Insufficient balance");
        }

        try {
            // Perform transfer
            sender.setBalance(sender.getBalance() - amount);
            receiver.setBalance(receiver.getBalance() + amount);
            accountRepository.save(sender);
            accountRepository.save(receiver);

            // Record transactions for both sides
            Transaction debit = Transaction.builder()
                    .type(TransactionType.TRANSFER_OUT)
                    .amount(amount)
                    .balanceAfter(sender.getBalance())
                    .timestamp(new Date())
                    .notes(note != null ? note : ("To " + toVpa))
                    .account(sender)
                    .build();
            transactionRepository.save(debit);

            Transaction credit = Transaction.builder()
                    .type(TransactionType.TRANSFER_IN)
                    .amount(amount)
                    .balanceAfter(receiver.getBalance())
                    .timestamp(new Date())
                    .notes(note != null ? note : ("From " + fromVpa))
                    .account(receiver)
                    .build();
            transactionRepository.save(credit);

            logger.info("UPI transfer completed successfully. Transaction ID: {}", debit.getId());
            return debit.getId();
        } catch (Exception e) {
            logger.error("UPI transfer failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public UpiRequest createCollectRequest(String payerVpa, String payeeVpa, double amount, String reason) {
        logger.info("Creating UPI collect request from {} to {} for amount {}", 
            logRedactionUtil.redactSensitiveData(payerVpa), 
            logRedactionUtil.redactSensitiveData(payeeVpa), 
            amount);
            
        if (amount <= 0) {
            logger.warn("Invalid amount {} for UPI collect request", amount);
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // validate both VPAs
        upiVpaRepository.findByVpa(payerVpa)
            .orElseThrow(() -> {
                logger.warn("Payer VPA not found: {}", logRedactionUtil.redactSensitiveData(payerVpa));
                return new EntityNotFoundException("Payer VPA not found");
            });
        upiVpaRepository.findByVpa(payeeVpa)
            .orElseThrow(() -> {
                logger.warn("Payee VPA not found: {}", logRedactionUtil.redactSensitiveData(payeeVpa));
                return new EntityNotFoundException("Payee VPA not found");
            });

        UpiRequest req = UpiRequest.builder()
                .payerVpa(payerVpa)
                .payeeVpa(payeeVpa)
                .amount(amount)
                .status(UpiRequestStatus.PENDING)
                .createdAt(new Date())
                .build();
        
        UpiRequest savedReq = upiRequestRepository.save(req);
        logger.info("UPI collect request created successfully with ID: {}", savedReq.getId());
        return savedReq;
    }

    @Override
    public List<UpiRequest> getPendingRequests(String vpa) {
        return upiRequestRepository.findByPayerVpaAndStatus(vpa, UpiRequestStatus.PENDING);
    }

    @Override
    @Transactional
    public Long approveRequest(Long requestId, String pin) {
        logger.info("Approving UPI request ID: {}", requestId);
        
        UpiRequest req = upiRequestRepository.findById(requestId)
            .orElseThrow(() -> {
                logger.warn("UPI request not found: {}", requestId);
                return new EntityNotFoundException("Request not found");
            });
            
        if (req.getStatus() != UpiRequestStatus.PENDING) {
            logger.warn("Cannot approve UPI request {} with status: {}", requestId, req.getStatus());
            throw new IllegalStateException("Request is not in PENDING status");
        }
        
        try {
            Long txnId = sendMoney(req.getPayerVpa(), req.getPayeeVpa(), req.getAmount(), "UPI Collect", pin);
            req.setStatus(UpiRequestStatus.APPROVED);
            upiRequestRepository.save(req);
            logger.info("UPI request {} approved successfully. Transaction ID: {}", requestId, txnId);
            return txnId;
        } catch (Exception e) {
            logger.error("Failed to approve UPI request {}: {}", requestId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void rejectRequest(Long requestId) {
        logger.info("Rejecting UPI request ID: {}", requestId);
        
        UpiRequest req = upiRequestRepository.findById(requestId)
            .orElseThrow(() -> {
                logger.warn("UPI request not found: {}", requestId);
                return new EntityNotFoundException("Request not found");
            });
            
        if (req.getStatus() != UpiRequestStatus.PENDING) {
            logger.warn("Cannot reject UPI request {} with status: {}", requestId, req.getStatus());
            throw new IllegalStateException("Request is not in PENDING status");
        }
        
        req.setStatus(UpiRequestStatus.REJECTED);
        upiRequestRepository.save(req);
        logger.info("UPI request {} rejected successfully", requestId);
    }

    @Override
    @Transactional
    public void setOrChangeUpiPin(String newPin) {
        if (newPin == null || newPin.length() < 4) throw new IllegalArgumentException("PIN too short");
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        user.setUpiPinHash(passwordEncoder.encode(newPin));
        userRepository.save(user);
    }

    private Date getStartOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
        cal.set(java.util.Calendar.MINUTE, 59);
        cal.set(java.util.Calendar.SECOND, 59);
        cal.set(java.util.Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    @Override
    public java.util.List<Transaction> getTransactionsByVpa(String vpa) {
        UpiVpa ref = upiVpaRepository.findByVpa(vpa).orElseThrow(() -> new EntityNotFoundException("VPA not found"));
        return transactionRepository.findByAccountIdOrderByTimestampDesc(ref.getAccount().getId());
    }

    @Override
    public List<UpiVpa> searchVpas(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return upiVpaRepository.findByVpaContainingIgnoreCase(query.trim()).stream()
                .limit(10)
                .toList();
    }

    @Override
    @Transactional
    public CreateUpiIdResponse createUpiId(CreateUpiIdRequest request) {
        logger.info("Creating UPI ID: {} for account: {}", 
            logRedactionUtil.redactSensitiveData(request.getVpa()), request.getAccountId());
            
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        // Validate account belongs to user
        Account account = accountRepository.findById(request.getAccountId())
            .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new BadCredentialsException("Account does not belong to user");
        }
        
        // Check if VPA already exists
        if (upiVpaRepository.existsByVpa(request.getVpa())) {
            logger.warn("UPI ID already exists: {}", logRedactionUtil.redactSensitiveData(request.getVpa()));
            throw new IllegalArgumentException("UPI ID already exists");
        }
        
        // Hash the UPI PIN
        String upiPinHash = passwordEncoder.encode(request.getUpiPin());
        
        // Create UPI ID
        UpiVpa upiVpa = UpiVpa.builder()
                .vpa(request.getVpa())
                .user(user)
                .account(account)
                .upiPinHash(upiPinHash)
                .isDefault(false) // New UPI IDs are not default by default
                .createdAt(new Date())
                .build();
                
        UpiVpa savedUpiVpa = upiVpaRepository.save(upiVpa);
        
        logger.info("UPI ID created successfully: {} linked to account: {}", 
            logRedactionUtil.redactSensitiveData(request.getVpa()), account.getId());
            
        return CreateUpiIdResponse.builder()
                .status("success")
                .upiId(request.getVpa())
                .linkedAccount("Account " + account.getId() + " - Balance: ₹" + account.getBalance())
                .message("UPI ID created successfully")
                .build();
    }

    @Override
    @Transactional
    public void deleteUpiId(String vpa) {
        logger.info("Deleting UPI ID: {}", logRedactionUtil.redactSensitiveData(vpa));
        
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("User not found"));
        
        UpiVpa upiVpa = upiVpaRepository.findByVpa(vpa)
            .orElseThrow(() -> new EntityNotFoundException("UPI ID not found"));
            
        if (!upiVpa.getUser().getId().equals(user.getId())) {
            throw new BadCredentialsException("Cannot delete another user's UPI ID");
        }
        
        upiVpaRepository.delete(upiVpa);
        logger.info("UPI ID deleted successfully: {}", logRedactionUtil.redactSensitiveData(vpa));
    }

    @Override
    public UpiVpa getUpiIdByVpa(String vpa) {
        return upiVpaRepository.findByVpa(vpa)
            .orElseThrow(() -> new EntityNotFoundException("UPI ID not found"));
    }
}

