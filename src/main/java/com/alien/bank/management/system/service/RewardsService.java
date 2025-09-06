package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.User;

public interface RewardsService {
    int calculateCoinsEarned(double amount);
    void recordEarnAndDailyBonus(User user, Long paymentId, int coinsEarned);
    int getWalletCoins(User user);
}


