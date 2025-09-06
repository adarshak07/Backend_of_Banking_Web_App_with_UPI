package com.alien.bank.management.system.config;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.PaymentService;
import com.alien.bank.management.system.service.RewardsService;
import com.alien.bank.management.system.utils.EncryptionUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PaymentCategoryRepository categoryRepository;
    private final GiftCardProductRepository productRepository;
    private final RewardWalletRepository walletRepository;
    private final RewardLedgerRepository ledgerRepository;
    private final RewardDailySummaryRepository summaryRepository;
    private final TransactionRepository transactionRepository;
    private final EncryptionUtil encryptionUtil;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostConstruct
    public void seed() {
        if (userRepository.count() == 0) return; // assume app already has users
        ensureCategories();
        ensureProducts();
        ensureAccounts();
        generatePaymentsAndRewards();
        log.info("DevDataSeeder: seeding complete");
    }

    private void ensureCategories() {
        // Already inserted via migration; ensure present
        categoryRepository.findByCode("FOOD");
    }

    private void ensureProducts() {
        // Already inserted via migration; touch repo
        productRepository.findByActiveTrueOrderByCostCoinsAsc();
    }

    private void ensureAccounts() {
        List<User> users = userRepository.findAll();
        for (User u : users) {
            List<Account> accounts = accountRepository.findAllByUser(u);
            if (accounts.isEmpty()) {
                String cardNumber = randomCard();
                String last4Digits = encryptionUtil.extractLast4Digits(cardNumber);
                String encryptedPan = encryptionUtil.encryptPan(cardNumber);
                
                Account a = Account.builder()
                        .encryptedPan(encryptedPan)
                        .last4Digits(last4Digits)
                        .balance((double) RANDOM.nextInt(100_001))
                        .user(u)
                        .build();
                accountRepository.save(a);
            }
        }
    }

    private void generatePaymentsAndRewards() {
        List<User> users = userRepository.findAll();
        List<PaymentCategory> categories = categoryRepository.findAll();
        for (User u : users) {
            List<Account> accounts = accountRepository.findAllByUser(u);
            if (accounts.isEmpty()) continue;
            Account a = accounts.get(0);
            // create transactions and ledger directly to avoid invoking services in init
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -30);
            for (int i = 0; i < 200; i++) {
                cal.add(Calendar.HOUR_OF_DAY, RANDOM.nextInt(6));
                Date ts = cal.getTime();
                double amount = 10 + RANDOM.nextInt(900);
                PaymentCategory cat = categories.get(RANDOM.nextInt(categories.size()));

                if (a.getBalance() < amount) {
                    a.setBalance(a.getBalance() + amount + RANDOM.nextInt(200));
                    accountRepository.save(a);
                    Transaction dep = Transaction.builder()
                            .type(TransactionType.DEPOSIT)
                            .amount(amount)
                            .balanceAfter(a.getBalance())
                            .timestamp(ts)
                            .notes("Seed deposit")
                            .account(a)
                            .build();
                    transactionRepository.save(dep);
                }

                a.setBalance(a.getBalance() - amount);
                accountRepository.save(a);
                Transaction out = Transaction.builder()
                        .type(TransactionType.WITHDRAW)
                        .amount(amount)
                        .balanceAfter(a.getBalance())
                        .timestamp(ts)
                        .notes(cat.getCode())
                        .account(a)
                        .build();
                transactionRepository.save(out);

                // wallet and ledger
                RewardWallet wallet = walletRepository.findByUserId(u.getId()).orElseGet(() -> RewardWallet.builder().userId(u.getId()).coins(0).build());
                int earn = Math.max(1, (int) Math.floor(amount / 100));
                wallet.setCoins(wallet.getCoins() + earn);
                walletRepository.save(wallet);

                RewardLedger earnEntry = RewardLedger.builder()
                        .user(u)
                        .type(RewardType.EARN)
                        .coins(earn)
                        .note("Seed earn")
                        .build();
                ledgerRepository.save(earnEntry);

                Date day = truncate(ts);
                RewardDailySummary ds = summaryRepository.findByUserAndDay(u, day)
                        .orElseGet(() -> RewardDailySummary.builder().user(u).day(day).txCount(0).bonusGiven(false).build());
                ds.setTxCount(ds.getTxCount() + 1);
                if (!ds.getBonusGiven() && ds.getTxCount() >= 5) {
                    ds.setBonusGiven(true);
                    wallet.setCoins(wallet.getCoins() + 20);
                    walletRepository.save(wallet);
                    ledgerRepository.save(RewardLedger.builder().user(u).type(RewardType.BONUS).coins(20).note("Seed bonus").build());
                }
                summaryRepository.save(ds);
            }
        }
    }

    private Date truncate(Date ts) {
        Calendar c = Calendar.getInstance();
        c.setTime(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private String randomCard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }
}


