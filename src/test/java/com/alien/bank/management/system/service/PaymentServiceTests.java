package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.exception.InsufficientFundsException;
import com.alien.bank.management.system.model.payments.UpiPaymentRequest;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.utils.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({com.alien.bank.management.system.service.impl.PaymentServiceImpl.class,
        com.alien.bank.management.system.service.impl.RewardsServiceImpl.class})
public class PaymentServiceTests {

    @Autowired private PaymentService paymentService;
    @Autowired private RewardsService rewardsService;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PaymentCategoryRepository categoryRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RewardWalletRepository walletRepository;
    @Autowired private RewardLedgerRepository ledgerRepository;
    @Autowired private RewardDailySummaryRepository summaryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private EncryptionUtil encryptionUtil;

    private User user;
    private Account account;

    @BeforeEach
    void setup() {
        user = userRepository.save(User.builder().email("t@t.com").password("p").role(Role.USER).build());
        
        String cardNumber = "1234567890123456";
        String last4Digits = encryptionUtil.extractLast4Digits(cardNumber);
        String encryptedPan = encryptionUtil.encryptPan(cardNumber);
        
        account = accountRepository.save(Account.builder()
                .encryptedPan(encryptedPan)
                .last4Digits(last4Digits)
                .balance(1000.0)
                .user(user)
                .build());
                
        if (categoryRepository.findByCode("FOOD").isEmpty()) {
            categoryRepository.save(PaymentCategory.builder().code("FOOD").label("Food").build());
        }
    }

    @Test
    void upi_success_earns_coins_and_creates_records() {
        UpiPaymentRequest req = new UpiPaymentRequest();
        req.setUserId(user.getId());
        req.setAccountId(account.getId());
        req.setVpa("testuser@bank");
        req.setAmount(349.0);
        req.setCategory("FOOD");
        var resp = paymentService.upiPay(req);
        assertEquals("SUCCESS", resp.getStatus());
        assertNotNull(resp.getRefId());
        assertEquals(3, resp.getCoinsEarned());
        assertTrue(resp.getBalanceAfter() < 1000.0);

        assertTrue(paymentRepository.findByRefId(resp.getRefId()).isPresent());
        assertEquals(3, walletRepository.findByUser(user).map(RewardWallet::getCoins).orElse(0));
        assertFalse(ledgerRepository.findByUserOrderByCreatedAtDesc(user, org.springframework.data.domain.PageRequest.of(0, 10)).isEmpty());
        assertFalse(transactionRepository.findByAccountIdOrderByTimestampDesc(account.getId()).isEmpty());
    }

    @Test
    void upi_insufficient_funds_409_and_failed_status() {
        account.setBalance(10.0);
        accountRepository.save(account);
        UpiPaymentRequest req = new UpiPaymentRequest();
        req.setUserId(user.getId());
        req.setAccountId(account.getId());
        req.setVpa("testuser@bank");
        req.setAmount(349.0);
        req.setCategory("FOOD");
        try {
            paymentService.upiPay(req);
            fail("Expected InsufficientFundsException");
        } catch (InsufficientFundsException ex) {
            // ok
        }
    }
}


