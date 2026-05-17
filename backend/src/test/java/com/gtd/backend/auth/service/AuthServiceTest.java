package com.gtd.backend.auth.service;

import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.exception.EmailAlreadyExistsException;
import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("securePassword123")
                .build();
    }

    @Test
    void shouldRegisterUser_whenEmailIsNew() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPassword");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterResponse response = authService.register(validRequest);

        assertThat(response.getId()).isEqualTo(savedUser.getId());
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldHashPassword_whenRegistering() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPassword");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(validRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertThat(captured.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(captured.getPasswordHash()).isNotEqualTo("securePassword123");
    }

    @Test
    void shouldNormalizeEmail_whenRegistering() {
        validRequest.setEmail("  Test@Example.COM  ");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(validRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowEmailAlreadyExists_whenEmailIsDuplicate() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
