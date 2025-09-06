package com.alien.bank.management.system.repository;

import com.alien.bank.management.system.entity.Account;
import com.alien.bank.management.system.entity.Role;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.utils.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class AccountRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private EncryptionUtil encryptionUtil;

    private User user;
    private Account account;

    @BeforeEach
    public void setUp() {
        user = userRepository.save(User
                .builder()
                .name("Muhammad Eid")
                .email("mohammed@gmail.com")
                .phone("01552422396")
                .role(Role.USER)
                .password("123456")
                .build()
        );

        String cardNumber1 = "1234567890123456";
        String last4Digits1 = encryptionUtil.extractLast4Digits(cardNumber1);
        String encryptedPan1 = encryptionUtil.encryptPan(cardNumber1);

        account = accountRepository.save(Account
                .builder()
                .encryptedPan(encryptedPan1)
                .last4Digits(last4Digits1)
                .balance(10000.0)
                .user(user)
                .build()
        );

        String cardNumber2 = "1234567890125556";
        String last4Digits2 = encryptionUtil.extractLast4Digits(cardNumber2);
        String encryptedPan2 = encryptionUtil.encryptPan(cardNumber2);

        accountRepository.save(Account
                .builder()
                .encryptedPan(encryptedPan2)
                .last4Digits(last4Digits2)
                .balance(10000.0)
                .user(user)
                .build()
        );

        String cardNumber3 = "1234997890123456";
        String last4Digits3 = encryptionUtil.extractLast4Digits(cardNumber3);
        String encryptedPan3 = encryptionUtil.encryptPan(cardNumber3);

        accountRepository.save(Account
                .builder()
                .encryptedPan(encryptedPan3)
                .last4Digits(last4Digits3)
                .balance(10000.0)
                .user(user)
                .build()
        );
    }

    @Test
    public void shouldExistsByLast4Digits() {
        Boolean exists = accountRepository.existsByLast4Digits(account.getLast4Digits());
        assertThat(exists).isTrue();
    }

    @Test
    public void shouldNotExistsByLast4DigitsWhenAccountDoesNotExist() {
        Boolean exists = accountRepository.existsByLast4Digits("9999");
        assertThat(exists).isFalse();
    }

    @Test
    public void shouldFindAllByUser() {
        List<Account> accounts = accountRepository.findAllByUser(user);
        assertThat(accounts).hasSize(3);
    }

    @Test
    public void shouldReturnEmptyListWhenUserDoesNotExist() {
        List<Account> accounts = accountRepository.findAllByUser(User
                .builder()
                .id(5L)
                .name("user user")
                .email("user@gmail.com")
                .phone("15524223960")
                .role(Role.USER)
                .password("123456")
                .build()
        );
        assertThat(accounts).isEmpty();
    }

    @Test
    public void shouldFindAccountById() {
        Optional<Account> foundAccount = accountRepository.findById(this.account.getId());
        assertThat(foundAccount).isPresent();
        assertThat(foundAccount.get()).isEqualTo(accountRepository.findAllByUser(user).get(0));
    }

    @Test
    public void shouldReturnEmptyOptionalWhenAccountDoesNotExistById() {
        Optional<Account> account = accountRepository.findById(999L);
        assertThat(account).isEmpty();
    }
}
