package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.*;
import com.alien.bank.management.system.exception.InsufficientCoinsException;
import com.alien.bank.management.system.repository.*;
import com.alien.bank.management.system.service.RedeemService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class RedeemServiceImpl implements RedeemService {
    private final RewardWalletRepository walletRepository;
    private final RewardLedgerRepository ledgerRepository;
    private final GiftCardProductRepository productRepository;
    private final GiftCardRedemptionRepository redemptionRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public GiftCardRedemption redeem(User user, Long productId) {
        GiftCardProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new IllegalArgumentException("Product inactive");
        }

        RewardWallet wallet = walletRepository.findByUserForUpdate(user)
                .orElseGet(() -> RewardWallet.builder().user(user).coins(0).build());
        if (wallet.getCoins() < product.getCostCoins()) {
            throw new InsufficientCoinsException(wallet.getCoins());
        }

        wallet.setCoins(wallet.getCoins() - product.getCostCoins());
        walletRepository.save(wallet);

        // ledger negative
        RewardLedger redeem = RewardLedger.builder()
                .user(user)
                .payment(null)
                .type(RewardType.REDEEM)
                .coins(-product.getCostCoins())
                .note("Redeem " + product.getBrand() + " " + product.getValueRupees())
                .build();
        ledgerRepository.save(redeem);

        GiftCardRedemption redemption = GiftCardRedemption.builder()
                .user(user)
                .product(product)
                .code(generateCode())
                .status(GiftCardStatus.ISSUED)
                .createdAt(new Date())
                .build();
        return redemptionRepository.save(redemption);
    }

    private String generateCode() {
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        String rand = randomAlphaNum(8);
        return "FK-" + year + "-" + rand;
    }

    private String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        return sb.toString();
    }
}


