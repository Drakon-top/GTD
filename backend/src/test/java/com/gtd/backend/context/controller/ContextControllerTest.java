package com.gtd.backend.context.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.dto.ContextResponse;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.dto.UpdateContextRequest;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextLimitExceededException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.service.ContextService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContextController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ContextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ContextService contextService;

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
    private final UUID contextId = UUID.randomUUID();

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
    void shouldReturnContextsList() throws Exception {
        ContextResponse response = buildResponse("Work", ContextTheme.FORMAL);
        when(contextService.getContexts(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/contexts").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Work"))
                .andExpect(jsonPath("$[0].theme").value("FORMAL"));
    }

    @Test
    void shouldReturnEmptyList() throws Exception {
        when(contextService.getContexts(userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/contexts").with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnContextById() throws Exception {
        ContextResponse response = buildResponse("Home", ContextTheme.NATURE);
        when(contextService.getContext(contextId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Home"))
                .andExpect(jsonPath("$.theme").value("NATURE"));
    }

    @Test
    void shouldReturn404WhenContextNotFound() throws Exception {
        when(contextService.getContext(contextId, userId))
                .thenThrow(new ContextNotFoundException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn403WhenAccessDenied() throws Exception {
        when(contextService.getContext(contextId, userId))
                .thenThrow(new ContextAccessDeniedException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void shouldCreateContext() throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .build();

        ContextResponse response = buildResponse("Work", ContextTheme.FORMAL);
        when(contextService.createContext(any(CreateContextRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/contexts")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.theme").value("FORMAL"));
    }

    @Test
    void shouldReturn400WhenNameBlank() throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .build();

        mockMvc.perform(post("/api/v1/contexts")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn400WhenThemeNull() throws Exception {
        String json = """
                {"name": "Work", "icon": "briefcase"}
                """;

        mockMvc.perform(post("/api/v1/contexts")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenIconBlank() throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("")
                .build();

        mockMvc.perform(post("/api/v1/contexts")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLimitExceeded() throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Too Many")
                .theme(ContextTheme.DARK)
                .icon("x")
                .build();

        when(contextService.createContext(any(CreateContextRequest.class), eq(userId)))
                .thenThrow(new ContextLimitExceededException());

        mockMvc.perform(post("/api/v1/contexts")
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot create more than 5 contexts"));
    }

    @Test
    void shouldUpdateContext() throws Exception {
        UpdateContextRequest request = UpdateContextRequest.builder()
                .name("Office")
                .build();

        ContextResponse response = buildResponse("Office", ContextTheme.FORMAL);
        when(contextService.updateContext(eq(contextId), any(UpdateContextRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/contexts/{id}", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office"));
    }

    @Test
    void shouldDeleteContext() throws Exception {
        doNothing().when(contextService).deleteContext(contextId, userId);

        mockMvc.perform(delete("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404OnDeleteWhenNotFound() throws Exception {
        doThrow(new ContextNotFoundException(contextId))
                .when(contextService).deleteContext(contextId, userId);

        mockMvc.perform(delete("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnDeleteWhenNotOwner() throws Exception {
        doThrow(new ContextAccessDeniedException(contextId))
                .when(contextService).deleteContext(contextId, userId);

        mockMvc.perform(delete("/api/v1/contexts/{id}", contextId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    private ContextResponse buildResponse(String name, ContextTheme theme) {
        return ContextResponse.builder()
                .id(contextId)
                .name(name)
                .theme(theme)
                .icon("briefcase")
                .sortOrder(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
