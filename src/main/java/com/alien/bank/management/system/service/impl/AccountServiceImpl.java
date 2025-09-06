package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.mapper.AccountMapper;
import com.alien.bank.management.system.model.account.AccountResponseModel;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.service.AccountService;
import com.alien.bank.management.system.utils.Utils;
import com.alien.bank.management.system.utils.EncryptionUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final EncryptionUtil encryptionUtil;

    @Override
    public AccountResponseModel createNewAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " Not Found"));

        // Check account limit (3 accounts max)
        long accountCount = accountRepository.countByUser(user);
        if (accountCount >= 3) {
            throw new IllegalArgumentException("Maximum account limit reached (3 accounts)");
        }

        String cardNumber = generateUniqueCardNumber();
        String last4Digits = encryptionUtil.extractLast4Digits(cardNumber);
        String encryptedPan = encryptionUtil.encryptPan(cardNumber);

        Account account = accountRepository.save(
            Account
                .builder()
                .encryptedPan(encryptedPan)
                .last4Digits(last4Digits)
                .balance(0.0)
                .user(user)
                .build()
        );

        return accountMapper.toResponseModel(account);
    }

    @Override
    public List<AccountResponseModel> getMyAccounts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " Not Found"));

        return accountRepository
                .findAllByUser(user)
                .stream()
                .map(accountMapper::toResponseModel)
                .toList();
    }

    @Override
    public String deleteAccount(Long accountId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User " + email + " Not Found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account " + accountId + " Not Found"));

        // Check if the account belongs to the current user
        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only delete your own accounts");
        }

        // Check if account has balance
        if (account.getBalance() > 0) {
            throw new IllegalArgumentException("Cannot delete account with balance. Please withdraw all funds first.");
        }

        accountRepository.delete(account);
        return "Account " + accountId + " deleted successfully";
    }

    public String generateUniqueCardNumber() {
        String cardNumber = Utils.generateCardNumber();

        while (accountRepository.existsByLast4Digits(encryptionUtil.extractLast4Digits(cardNumber))) {
            cardNumber = Utils.generateCardNumber();
        }

        return cardNumber;
    }
}