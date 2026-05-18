package com.gtd.backend.sync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.sync.dto.SyncChangeRequest;
import com.gtd.backend.sync.dto.SyncChangeResult;
import com.gtd.backend.sync.dto.SyncLogResponse;
import com.gtd.backend.sync.dto.SyncPullResponse;
import com.gtd.backend.sync.dto.SyncPushRequest;
import com.gtd.backend.sync.dto.SyncPushResponse;
import com.gtd.backend.sync.model.ConflictStatus;
import com.gtd.backend.sync.model.DeviceSource;
import com.gtd.backend.sync.model.SyncEntityType;
import com.gtd.backend.sync.service.SyncService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyncController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SyncService syncService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtProperties jwtProperties;

    @MockitoBean
    private RateLimitingFilter rateLimitingFilter;

    @MockitoBean
    private CorsProperties corsProperties;

    @MockitoBean
    private RateLimitProperties rateLimitProperties;

    private final UUID userId = UUID.randomUUID();

    private RequestPostProcessor authenticatedUser() {
        return (MockHttpServletRequest request) -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    void pushChanges_shouldReturn200WithResults() throws Exception {
        UUID taskId = UUID.randomUUID();

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("Updated")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(1)
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = SyncPushResponse.builder()
                .serverTimestamp(Instant.now())
                .results(List.of(SyncChangeResult.builder()
                        .entityType(SyncEntityType.TASK)
                        .entityId(taskId)
                        .fieldName("title")
                        .applied(true)
                        .conflictStatus(ConflictStatus.NO_CONFLICT)
                        .newVersion(2)
                        .build()))
                .appliedCount(1)
                .conflictCount(0)
                .build();

        when(syncService.pushChanges(any(SyncPushRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/push")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1))
                .andExpect(jsonPath("$.conflictCount").value(0))
                .andExpect(jsonPath("$.results[0].applied").value(true))
                .andExpect(jsonPath("$.results[0].fieldName").value("title"))
                .andExpect(jsonPath("$.results[0].newVersion").value(2));
    }

    @Test
    void pushChanges_shouldReturn400ForEmptyChanges() throws Exception {
        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of())
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pushChanges_shouldReturn400ForNullEntityType() throws Exception {
        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityId(UUID.randomUUID())
                .fieldName("title")
                .newValue("Test")
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build(); // entityType is null

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pushChanges_shouldReturn400ForBlankFieldName() throws Exception {
        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.randomUUID())
                .fieldName("")
                .newValue("Test")
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pushChanges_shouldReturnConflictResult() throws Exception {
        UUID taskId = UUID.randomUUID();

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(taskId)
                .fieldName("title")
                .newValue("Client Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .expectedVersion(0)
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        SyncPushResponse response = SyncPushResponse.builder()
                .serverTimestamp(Instant.now())
                .results(List.of(SyncChangeResult.builder()
                        .entityType(SyncEntityType.TASK)
                        .entityId(taskId)
                        .fieldName("title")
                        .applied(false)
                        .conflictStatus(ConflictStatus.RESOLVED_NOTIFY)
                        .serverValue("Server Title")
                        .newVersion(2)
                        .build()))
                .appliedCount(0)
                .conflictCount(1)
                .build();

        when(syncService.pushChanges(any(SyncPushRequest.class), eq(userId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sync/push")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictCount").value(1))
                .andExpect(jsonPath("$.results[0].applied").value(false))
                .andExpect(jsonPath("$.results[0].conflictStatus").value("RESOLVED_NOTIFY"))
                .andExpect(jsonPath("$.results[0].serverValue").value("Server Title"));
    }

    @Test
    void pullChanges_shouldReturn200WithChanges() throws Exception {
        Instant since = Instant.parse("2026-05-01T00:00:00Z");
        UUID taskId = UUID.randomUUID();

        SyncPullResponse response = SyncPullResponse.builder()
                .serverTimestamp(Instant.now())
                .changes(List.of(SyncLogResponse.builder()
                        .id(UUID.randomUUID())
                        .entityType(SyncEntityType.TASK)
                        .entityId(taskId)
                        .fieldName("title")
                        .oldValue("Old")
                        .newValue("New")
                        .deviceSource(DeviceSource.WEB)
                        .conflictStatus(ConflictStatus.NO_CONFLICT)
                        .createdAt(Instant.parse("2026-05-02T00:00:00Z"))
                        .build()))
                .changeCount(1)
                .build();

        when(syncService.pullChanges(eq(since), eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/sync/pull")
                        .with(authenticatedUser())
                        .param("since", since.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeCount").value(1))
                .andExpect(jsonPath("$.changes[0].entityType").value("TASK"))
                .andExpect(jsonPath("$.changes[0].fieldName").value("title"))
                .andExpect(jsonPath("$.changes[0].oldValue").value("Old"))
                .andExpect(jsonPath("$.changes[0].newValue").value("New"));
    }

    @Test
    void pullChanges_shouldReturn200WithEmptyChanges() throws Exception {
        Instant since = Instant.now();

        SyncPullResponse response = SyncPullResponse.builder()
                .serverTimestamp(Instant.now())
                .changes(List.of())
                .changeCount(0)
                .build();

        when(syncService.pullChanges(eq(since), eq(userId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/sync/pull")
                        .with(authenticatedUser())
                        .param("since", since.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeCount").value(0))
                .andExpect(jsonPath("$.changes").isEmpty());
    }

    @Test
    void pullChanges_shouldReturn400WithoutSinceParam() throws Exception {
        mockMvc.perform(get("/api/v1/sync/pull")
                        .with(authenticatedUser()))
                .andExpect(status().isBadRequest());
    }
}
