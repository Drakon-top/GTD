package com.gtd.backend.auth.service;

import com.gtd.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-testing-only");
        props.setAccessTokenExpirationMs(900000);
        props.setRefreshTokenExpirationMs(2592000000L);
        jwtService = new JwtService(props);
    }

    @Test
    void shouldGenerateValidAccessToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtService.getTokenType(token)).isEqualTo("access");
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.getUserIdFromToken(token)).isEqualTo(userId);
        assertThat(jwtService.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    void shouldReturnFalse_whenTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("invalid-token")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsTampered() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsExpired() {
        JwtProperties props = new JwtProperties();
        props.setSecret("test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha-testing-only");
        props.setAccessTokenExpirationMs(0);
        props.setRefreshTokenExpirationMs(0);
        JwtService expiredService = new JwtService(props);

        UUID userId = UUID.randomUUID();
        String token = expiredService.generateAccessToken(userId, "user@example.com");

        assertThat(expiredService.isTokenValid(token)).isFalse();
    }

    @Test
    void shouldGenerateDifferentTokensEachTime() {
        UUID userId = UUID.randomUUID();
        String token1 = jwtService.generateRefreshToken(userId);
        String token2 = jwtService.generateRefreshToken(userId);

        assertThat(token1).isNotEqualTo(token2);
    }
}
