package com.gtd.backend.auth.service;

import com.gtd.backend.auth.dto.AuthResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);

        return RegisterResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Returns a two-element array: [accessToken, refreshToken].
     */
    @Transactional
    public String[] login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId());

        persistRefreshToken(user, refreshTokenValue);

        return new String[]{accessToken, refreshTokenValue};
    }

    /**
     * Returns a two-element array: [newAccessToken, newRefreshToken].
     */
    @Transactional
    public String[] refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        if (!jwtService.isTokenValid(refreshTokenValue)) {
            throw new InvalidRefreshTokenException();
        }

        String tokenType = jwtService.getTokenType(refreshTokenValue);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidRefreshTokenException();
        }

        String tokenHash = hashToken(refreshTokenValue);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isExpired()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new InvalidRefreshTokenException();
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshTokenValue = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, newRefreshTokenValue);

        return new String[]{newAccessToken, newRefreshTokenValue};
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        String tokenHash = hashToken(refreshTokenValue);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private void persistRefreshToken(User user, String refreshTokenValue) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshTokenValue))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs()))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
