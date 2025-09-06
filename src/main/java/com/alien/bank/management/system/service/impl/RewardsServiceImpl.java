package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.RewardsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class RewardsServiceImpl implements RewardsService {
    private final RewardWalletRepository walletRepository;
    private final RewardLedgerRepository ledgerRepository;
    private final RewardDailySummaryRepository summaryRepository;

    @Override
    public int calculateCoinsEarned(double amount) {
        int coins = (int) Math.floor(amount / 100.0);
        return Math.max(1, coins);
    }

    @Override
    @Transactional
    public void recordEarnAndDailyBonus(User user, Long paymentId, int coinsEarned) {
        // Upsert wallet (with lock)
        RewardWallet wallet = walletRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> {
                    RewardWallet w = RewardWallet.builder().userId(user.getId()).coins(0).build();
                    return w;
                });
        wallet.setCoins(wallet.getCoins() + coinsEarned);
        walletRepository.save(wallet);

        // Ledger: EARN
        RewardLedger earn = RewardLedger.builder()
                .user(user)
                .payment(paymentId != null ? Payment.builder().id(paymentId).build() : null)
                .type(RewardType.EARN)
                .coins(coinsEarned)
                .note("Payment reward")
                .build();
        ledgerRepository.save(earn);

        // Daily summary and potential bonus
        Date dayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        RewardDailySummary summary = summaryRepository.findByUserAndDayForUpdate(user, dayDate)
                .orElseGet(() -> RewardDailySummary.builder().user(user).day(dayDate).txCount(0).bonusGiven(false).build());
        summary.setTxCount(summary.getTxCount() + 1);

        if (!summary.getBonusGiven() && summary.getTxCount() >= 5) {
            // give bonus of 20
            RewardLedger bonus = RewardLedger.builder()
                    .user(user)
                    .payment(null)
                    .type(RewardType.BONUS)
                    .coins(20)
                    .note("Daily 5 payments bonus")
                    .build();
            ledgerRepository.save(bonus);

            wallet.setCoins(wallet.getCoins() + 20);
            walletRepository.save(wallet);

            summary.setBonusGiven(true);
        }
        summaryRepository.save(summary);
    }

    @Override
    public int getWalletCoins(User user) {
        return walletRepository.findByUser(user).map(RewardWallet::getCoins).orElse(0);
    }
}


