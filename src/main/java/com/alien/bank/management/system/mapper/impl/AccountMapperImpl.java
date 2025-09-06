package com.alien.bank.management.system.mapper.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.mapper.AccountMapper;
import com.alien.bank.management.system.model.account.AccountResponseModel;
import org.springframework.stereotype.Component;

@Component
public class AccountMapperImpl implements AccountMapper {
    @Override
    public AccountResponseModel toResponseModel(Account account) {
        return AccountResponseModel
                .builder()
                .accountId(account.getId())
                .maskedCard(maskCard(account.getLast4Digits()))
                .balance(account.getBalance())
                .build();
    }

    private String maskCard(String last4Digits) {
        if (last4Digits == null || last4Digits.length() < 4) {
            return "****";
        }
        return "**** **** **** " + last4Digits;
    }
}