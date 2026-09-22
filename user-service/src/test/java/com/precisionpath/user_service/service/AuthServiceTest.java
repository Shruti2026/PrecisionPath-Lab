package com.precisionpath.user_service.service;

import com.precisionpath.user_service.dto.LoginRequest;
import com.precisionpath.user_service.dto.LoginResponse;
import com.precisionpath.user_service.dto.RegisterRequest;
import com.precisionpath.user_service.dto.RegisterResponse;
import com.precisionpath.user_service.entity.Gender;
import com.precisionpath.user_service.entity.Role;
import com.precisionpath.user_service.entity.User;
import com.precisionpath.user_service.exception.DuplicateResourceException;
import com.precisionpath.user_service.exception.InvalidCredentialsException;
import com.precisionpath.user_service.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndAssignsPatientRole() {

        RegisterRequest request = new RegisterRequest(
                " Asha Rao ", " Asha@Example.COM ", "password123",
                "9876543210", Gender.FEMALE, 29
        );

        when(userRepository.existsByEmail("asha@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        assertThat(saved.getValue().getEmail()).isEqualTo("asha@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Asha Rao");
        assertThat(saved.getValue().getPassword()).isEqualTo("hashed");
        assertThat(response.role()).isEqualTo(Role.PATIENT);
    }

    @Test
    void registerRejectsDuplicateEmailRegardlessOfCase() {

        RegisterRequest request = new RegisterRequest(
                "Asha", "ASHA@example.com", "password123",
                "9876543210", Gender.FEMALE, 29
        );

        when(userRepository.existsByEmail("asha@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {

        User user = User.builder()
                .id(UUID.randomUUID())
                .fullName("Asha")
                .email("asha@example.com")
                .password("hashed")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user.getId(), user.getEmail(), "PATIENT"))
                .thenReturn("jwt-token");

        LoginResponse response = authService.login(
                new LoginRequest("Asha@Example.com", "password123")
        );

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(user.getId());
    }

    @Test
    void loginRejectsWrongPassword() {

        User user = User.builder()
                .email("asha@example.com")
                .password("hashed")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "hashed")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("asha@example.com", "wrong-pass"))
        ).isInstanceOf(InvalidCredentialsException.class);
    }
}
