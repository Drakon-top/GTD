package com.gtd.backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.dto.RegisterResponse;
import com.gtd.backend.auth.exception.EmailAlreadyExistsException;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
