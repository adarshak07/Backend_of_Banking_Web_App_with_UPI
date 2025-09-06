package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Role;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.mapper.AccountMapper;
import com.alien.bank.management.system.model.account.AccountResponseModel;
import com.alien.bank.management.system.repository.AccountRepository;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.utils.Utils;
import com.alien.bank.management.system.utils.EncryptionUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User user;
    private Account account;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        accountService = new AccountServiceImpl(userRepository, accountRepository, accountMapper, encryptionUtil);

        user = User.
                builder()
                .id(1L)
                .name("Muhammad Eid")
                .email("mohammed@gmail.com")
                .phone("01552422396")
                .password("123456")
                .role(Role.USER)
                .build();

        account = Account
                .builder()
                .id(1L)
                .encryptedPan("<KMS_KEY_REF>:encrypted1234567890123456")
                .last4Digits("3456")
                .balance(0.0)
                .build();
    }

    @Test
    public void createNewAccountShouldCreateNewAccountSuccessfully() {
        String cardNumber = "0123456789123456";
        String last4Digits = "3456";
        String encryptedPan = "<KMS_KEY_REF>:encrypted1234567890123456";
        
        AccountResponseModel expectedResponse = AccountResponseModel
                .builder()
                .accountId(account.getId())
                .maskedCard("**** **** **** " + last4Digits)
                .balance(0.0)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(encryptionUtil.extractLast4Digits(anyString())).thenReturn(last4Digits);
        when(encryptionUtil.encryptPan(anyString())).thenReturn(encryptedPan);
        when(accountRepository.existsByLast4Digits(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(accountMapper.toResponseModel(account)).thenReturn(expectedResponse);

        AccountResponseModel response = accountService.createNewAccount();

        assertThat(response).isNotNull();
        assertThat(response.getBalance()).isEqualTo(expectedResponse.getBalance());

        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    public void createNewAccountShouldThrowEntityNotFoundExceptionWhenUserNotFound() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createNewAccount())
                .isInstanceOf(EntityNotFoundException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    public void getMyAccountsSuccessfully() {
        AccountResponseModel accountResponse = AccountResponseModel
                .builder()
                .accountId(account.getId())
                .maskedCard("**** **** **** " + account.getLast4Digits())
                .balance(0.0)
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(accountRepository.findAllByUser(user)).thenReturn(Collections.singletonList(account));
        when(accountMapper.toResponseModel(account)).thenReturn(accountResponse);

        List<AccountResponseModel> response = accountService.getMyAccounts();

        assertThat(response).isNotNull();
        assertThat(response).hasSize(1);
        assertThat(response).containsExactly(accountResponse);

        verify(accountRepository, times(1)).findAllByUser(user);
    }

    @Test
    public void getMyAccountsShouldThrowEntityNotFoundExceptionWhenUserNotFound() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getMyAccounts())
                .isInstanceOf(EntityNotFoundException.class);

        verify(accountRepository, never()).findAllByUser(user);
    }

    @Test
    void generateUniqueCardNumber_FirstAttempt() {
        String cardNumber = "1234567890123456";
        String last4Digits = "3456";

        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::generateCardNumber).thenReturn(cardNumber);
            when(encryptionUtil.extractLast4Digits(cardNumber)).thenReturn(last4Digits);
            when(accountRepository.existsByLast4Digits(last4Digits)).thenReturn(false);

            String uniqueCardNumber = accountService.generateUniqueCardNumber();

            assertThat(uniqueCardNumber).isNotNull();
            assertThat(uniqueCardNumber).isEqualTo(cardNumber);
        }
    }

    @Test
    void generateUniqueCardNumber_SecondAttempt() {
        String cardNumber1 = "1234567890123456";
        String cardNumber2 = "1596321596321596";
        String last4Digits1 = "3456";
        String last4Digits2 = "1596";

        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {
            utilsMockedStatic.when(Utils::generateCardNumber).thenReturn(cardNumber1, cardNumber2);
            when(encryptionUtil.extractLast4Digits(cardNumber1)).thenReturn(last4Digits1);
            when(encryptionUtil.extractLast4Digits(cardNumber2)).thenReturn(last4Digits2);
            when(accountRepository.existsByLast4Digits(anyString())).thenReturn(true, false);

            String uniqueCardNumber = accountService.generateUniqueCardNumber();

            assertThat(uniqueCardNumber).isNotNull();
            assertThat(uniqueCardNumber).isEqualTo(cardNumber2);

            verify(accountRepository, times(2)).existsByLast4Digits(anyString());
        }
    }
}
