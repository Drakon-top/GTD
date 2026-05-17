package com.gtd.backend.auth.controller;

import com.gtd.backend.auth.dto.AuthResponse;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.service.AuthService;
import com.gtd.backend.config.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse httpResponse) {
        String[] tokens = authService.login(request);

        addRefreshTokenCookie(httpResponse, tokens[1]);

        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens[0])
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest,
                                                HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshTokenFromCookies(httpRequest);
        String[] tokens = authService.refresh(refreshToken);

        addRefreshTokenCookie(httpResponse, tokens[1]);

        AuthResponse response = AuthResponse.builder()
                .accessToken(tokens[0])
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        String refreshToken = extractRefreshTokenFromCookies(httpRequest);
        authService.logout(refreshToken);
        clearRefreshTokenCookie(httpResponse);
        return ResponseEntity.ok().build();
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_TOKEN_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge((int) (jwtProperties.getRefreshTokenExpirationMs() / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
