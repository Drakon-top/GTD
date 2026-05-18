package com.gtd.backend.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.notification.dto.DeviceTokenResponse;
import com.gtd.backend.notification.dto.RegisterDeviceTokenRequest;
import com.gtd.backend.notification.model.DeviceType;
import com.gtd.backend.notification.service.DeviceTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceTokenController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeviceTokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceTokenService deviceTokenService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    @MockitoBean
    private CorsProperties corsProperties;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor withUser() {
        return (MockHttpServletRequest request) -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    void shouldRegisterDeviceToken() throws Exception {
        RegisterDeviceTokenRequest request = RegisterDeviceTokenRequest.builder()
                .token("fcm-token-abc")
                .deviceType(DeviceType.ANDROID)
                .deviceName("Pixel 8")
                .build();

        DeviceTokenResponse response = DeviceTokenResponse.builder()
                .id(UUID.randomUUID())
                .token("fcm-token-abc")
                .deviceType(DeviceType.ANDROID)
                .deviceName("Pixel 8")
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(deviceTokenService.registerToken(eq(userId), any(RegisterDeviceTokenRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/devices/token")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fcm-token-abc"))
                .andExpect(jsonPath("$.deviceType").value("ANDROID"))
                .andExpect(jsonPath("$.deviceName").value("Pixel 8"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturn400_whenTokenBlank() throws Exception {
        RegisterDeviceTokenRequest request = RegisterDeviceTokenRequest.builder()
                .token("")
                .deviceType(DeviceType.ANDROID)
                .build();

        mockMvc.perform(post("/api/v1/devices/token")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenDeviceTypeNull() throws Exception {
        String jsonWithoutDeviceType = "{\"token\":\"fcm-token\"}";

        mockMvc.perform(post("/api/v1/devices/token")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithoutDeviceType))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUnregisterDeviceToken() throws Exception {
        mockMvc.perform(delete("/api/v1/devices/token")
                        .with(withUser())
                        .param("token", "fcm-token-to-remove"))
                .andExpect(status().isNoContent());

        verify(deviceTokenService).unregisterToken(userId, "fcm-token-to-remove");
    }

    @Test
    void shouldGetUserTokens() throws Exception {
        DeviceTokenResponse response = DeviceTokenResponse.builder()
                .id(UUID.randomUUID())
                .token("token-1")
                .deviceType(DeviceType.WEB)
                .deviceName("Chrome")
                .active(true)
                .createdAt(Instant.now())
                .build();

        when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/devices/tokens")
                        .with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").value("token-1"))
                .andExpect(jsonPath("$[0].deviceType").value("WEB"))
                .andExpect(jsonPath("$[0].deviceName").value("Chrome"));
    }

    @Test
    void shouldReturnEmptyList_whenNoTokens() throws Exception {
        when(deviceTokenService.getUserTokens(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/devices/tokens")
                        .with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
