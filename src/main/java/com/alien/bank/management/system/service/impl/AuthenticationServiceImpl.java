package com.alien.bank.management.system.service.impl;

import com.alien.bank.management.system.entity.User;
import com.alien.bank.management.system.entity.Role;
import com.alien.bank.management.system.mapper.UserMapper;
import com.alien.bank.management.system.model.authentication.AuthenticationResponseModel;
import com.alien.bank.management.system.model.authentication.LoginRequestModel;
import com.alien.bank.management.system.model.authentication.RegisterRequestModel;
import com.alien.bank.management.system.repository.UserRepository;
import com.alien.bank.management.system.security.JwtService;
import com.alien.bank.management.system.service.AuthenticationService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    @Value("${admin.registration.key:}")
    private String adminRegistrationKey;


    @Override
    public AuthenticationResponseModel register(RegisterRequestModel request) {
        if (isEmailOrPhoneAlreadyExists(request.getEmail(), request.getPhone())) {
            throw new EntityExistsException("Email or Phone Number is already exists");
        }

        Role requested = Role.USER;
        if (request.getRole() != null) {
            try { requested = Role.valueOf(request.getRole().toUpperCase()); } catch (Exception ignored) {}
        }

        if (requested == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount == 0) {
                // bootstrap first admin, allowed without key
            } else {
                if (request.getAdminKey() == null || !request.getAdminKey().equals(adminRegistrationKey)) {
                    throw new AccessDeniedException("Invalid admin key");
                }
            }
        }

        User user = userMapper.toUser(request);
        user.setRole(requested);
        user = userRepository.save(user);

        return AuthenticationResponseModel.builder().token(jwtService.generateToken(user)).build();
    }

    @Override
    public AuthenticationResponseModel login(LoginRequestModel request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User " + request.getEmail() + " Not Found"));

        return AuthenticationResponseModel
                .builder()
                .token(jwtService.generateToken(user))
                .build();
    }

    private boolean isEmailOrPhoneAlreadyExists(String email, String phone) {
        return userRepository.existsByEmail(email) || userRepository.existsByPhone(phone);
    }
}
