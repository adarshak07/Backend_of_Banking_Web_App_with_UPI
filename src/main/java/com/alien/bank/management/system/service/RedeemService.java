package com.alien.bank.management.system.service;

import com.alien.bank.management.system.entity.GiftCardRedemption;
import com.alien.bank.management.system.entity.User;

public interface RedeemService {
    GiftCardRedemption redeem(User user, Long productId);
}


