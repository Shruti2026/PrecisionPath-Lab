package com.precisionpath.user_service.service;

import com.precisionpath.user_service.dto.LoginRequest;
import com.precisionpath.user_service.dto.LoginResponse;
import com.precisionpath.user_service.dto.RegisterRequest;
import com.precisionpath.user_service.dto.RegisterResponse;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import com.precisionpath.user_service.exception.DuplicateResourceException;
import com.precisionpath.user_service.exception.InvalidCredentialsException;
import com.precisionpath.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .gender(request.gender())
                .age(request.age())
                .role(Role.PATIENT)
                .build();

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                "Registration successful"
        );
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password")
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                "Login successful"
        );
    }

    static String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }
}
