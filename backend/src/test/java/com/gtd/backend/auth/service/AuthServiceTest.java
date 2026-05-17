package com.gtd.backend.auth.service;

import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.exception.EmailAlreadyExistsException;
import com.gtd.backend.auth.exception.InvalidCredentialsException;
import com.gtd.backend.auth.exception.InvalidRefreshTokenException;
import com.gtd.backend.auth.model.RefreshToken;
import com.gtd.backend.auth.model.User;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("securePassword123")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void shouldRegisterUser_whenEmailIsNew() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(validRegisterRequest);

        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldHashPassword_whenRegistering() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.register(validRegisterRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User captured = userCaptor.getValue();
        assertThat(captured.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(captured.getPasswordHash()).isNotEqualTo("securePassword123");
    }

    @Test
    void shouldNormalizeEmail_whenRegistering() {
        validRegisterRequest.setEmail("  Test@Example.COM  ");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.register(validRegisterRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldThrowEmailAlreadyExists_whenEmailIsDuplicate() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRegisterRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldReturnTokens_whenLoginWithValidCredentials() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("securePassword123")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("securePassword123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser.getId(), testUser.getEmail())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn("refresh-token");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(2592000000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String[] tokens = authService.login(loginRequest);

        assertThat(tokens[0]).isEqualTo("access-token");
        assertThat(tokens[1]).isEqualTo("refresh-token");
    }

    @Test
    void shouldThrowInvalidCredentials_whenUserNotFound() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("unknown@example.com")
                .password("password")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldThrowInvalidCredentials_whenPasswordIsWrong() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void shouldRefreshTokens_whenRefreshTokenIsValid() {
        String oldRefreshToken = "old-refresh-token";
        String tokenHash = authService.hashToken(oldRefreshToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(jwtService.isTokenValid(oldRefreshToken)).thenReturn(true);
        when(jwtService.getTokenType(oldRefreshToken)).thenReturn("refresh");
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)).thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(testUser.getId(), testUser.getEmail())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(testUser.getId())).thenReturn("new-refresh-token");
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(2592000000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        String[] tokens = authService.refresh(oldRefreshToken);

        assertThat(tokens[0]).isEqualTo("new-access-token");
        assertThat(tokens[1]).isEqualTo("new-refresh-token");
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void shouldThrowInvalidRefreshToken_whenTokenIsNull() {
        assertThatThrownBy(() -> authService.refresh(null))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldThrowInvalidRefreshToken_whenTokenIsExpired() {
        String refreshToken = "expired-token";
        String tokenHash = authService.hashToken(refreshToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        when(jwtService.isTokenValid(refreshToken)).thenReturn(true);
        when(jwtService.getTokenType(refreshToken)).thenReturn("refresh");
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void shouldRevokeToken_whenLoggingOut() {
        String refreshToken = "valid-refresh-token";
        String tokenHash = authService.hashToken(refreshToken);

        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.logout(refreshToken);

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void shouldDoNothing_whenLoggingOutWithNullToken() {
        authService.logout(null);
        verify(refreshTokenRepository, never()).findByTokenHashAndRevokedFalse(anyString());
    }
}
