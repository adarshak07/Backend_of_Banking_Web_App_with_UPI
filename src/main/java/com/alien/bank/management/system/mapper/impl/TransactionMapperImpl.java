package com.alien.bank.management.system.mapper.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Transaction;
import com.alien.bank.management.system.entity.TransactionType;
import com.alien.bank.management.system.mapper.TransactionMapper;
import com.alien.bank.management.system.model.transaction.DepositRequestModel;
import com.alien.bank.management.system.model.transaction.TransactionResponseModel;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TransactionMapperImpl implements TransactionMapper {
    @Override
    public Transaction toEntity(double amount, Account account, TransactionType type) {
        return Transaction.builder()
                .type(type)
                .amount(amount)
                .balanceAfter(account.getBalance()) // AFTER the service updates balance
                .timestamp(new Date())
                .notes("Account Balance " + account.getBalance())
                .account(account)
                .build();
    }

    @Override
    public TransactionResponseModel toResponseModel(Long id, double amount, double balance) {
        return TransactionResponseModel.builder()
                .id(id)
                .amount(amount)
                .balance(balance)
                .build();
    }
}