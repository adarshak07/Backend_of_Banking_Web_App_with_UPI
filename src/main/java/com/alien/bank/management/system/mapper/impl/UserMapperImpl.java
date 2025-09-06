package com.alien.bank.management.system.mapper.impl;

import com.alien.bank.management.system.entity.Role;
import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.mapper.UserMapper;
import com.alien.bank.management.system.model.authentication.RegisterRequestModel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapperImpl implements UserMapper {

    private final PasswordEncoder passwordEncoder;

    @Override
    public User toUser(RegisterRequestModel request) {
        Role assigned = Role.USER;
        if (request.getRole() != null) {
            try {
                assigned = Role.valueOf(request.getRole().toUpperCase());
            } catch (Exception ignored) {}
        }
        return User
                .builder()
                .name(request.getName())
                .role(assigned)
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
    }
}
