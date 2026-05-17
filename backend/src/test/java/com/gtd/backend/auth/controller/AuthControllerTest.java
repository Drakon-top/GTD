package com.gtd.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.exception.EmailAlreadyExistsException;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.exception.InvalidCredentialsException;
import com.gtd.backend.auth.exception.InvalidRefreshTokenException;
import com.gtd.backend.auth.service.AuthService;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    @MockitoBean
    private CorsProperties corsProperties;

    @Test
    void shouldReturn201_whenRegistrationIsSuccessful() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        RegisterResponse response = RegisterResponse.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .createdAt(Instant.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("password123")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("existing@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User with email 'existing@example.com' already exists"));
    }

    @Test
    void shouldReturn400_whenEmailIsInvalid() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn400_whenEmailIsBlank() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn400_whenPasswordIsTooShort() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("short")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn400_whenPasswordIsBlank() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void shouldReturn400_whenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn200WithAccessToken_whenLoginIsSuccessful() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new String[]{"access-token-value", "refresh-token-value"});
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(2592000000L);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true));
    }

    @Test
    void shouldReturn401_whenLoginCredentialsAreInvalid() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("wrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void shouldReturn200WithNewTokens_whenRefreshIsSuccessful() throws Exception {
        when(authService.refresh("valid-refresh-token"))
                .thenReturn(new String[]{"new-access-token", "new-refresh-token"});
        when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(2592000000L);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "valid-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void shouldReturn401_whenRefreshTokenIsInvalid() throws Exception {
        when(authService.refresh("bad-token"))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "bad-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldReturn401_whenNoRefreshTokenCookie() throws Exception {
        when(authService.refresh(null))
                .thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200_whenLogoutIsSuccessful() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refresh_token", "some-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refresh_token", 0));

        verify(authService).logout("some-refresh-token");
    }

    @Test
    void shouldReturn200_whenLogoutWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());

        verify(authService).logout(null);
    }
}
