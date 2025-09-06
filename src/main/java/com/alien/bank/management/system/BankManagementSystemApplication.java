package com.alien.bank.management.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.entity.Role;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BankManagementSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankManagementSystemApplication.class, args);
	}

    @Bean
    CommandLineRunner seedAdmin(@Value("${ADMIN_SEED_EMAIL:}") String adminEmail,
                                @Value("${ADMIN_SEED_PASSWORD:}") String adminPassword,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                if (userRepository.countByRole(Role.ADMIN) == 0) {
                    User admin = User.builder()
                            .name("Admin")
                            .email(adminEmail)
                            .phone("0000000000")
                            .password(passwordEncoder.encode(adminPassword))
                            .role(Role.ADMIN)
                            .build();
                    userRepository.save(admin);
                }
            }
        };
    }

}
