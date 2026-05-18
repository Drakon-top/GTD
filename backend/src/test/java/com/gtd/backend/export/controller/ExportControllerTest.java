package com.gtd.backend.export.controller;

import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.export.dto.ExportCategoryData;
import com.gtd.backend.export.dto.ExportContextData;
import com.gtd.backend.export.dto.ExportReminderData;
import com.gtd.backend.export.dto.ExportResponse;
import com.gtd.backend.export.dto.ExportTaskData;
import com.gtd.backend.export.service.ExportService;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.task.model.GtdList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExportService exportService;

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
    void exportAll_shouldReturn200WithAllContexts() throws Exception {
        ExportResponse response = ExportResponse.builder()
                .exportDate(Instant.parse("2026-05-18T10:00:00Z"))
                .version("1.0")
                .contexts(List.of(
                        ExportContextData.builder()
                                .id(UUID.randomUUID())
                                .name("Work")
                                .theme(ContextTheme.FORMAL)
                                .icon("briefcase")
                                .sortOrder(0)
                                .categories(List.of(
                                        ExportCategoryData.builder()
                                                .id(UUID.randomUUID())
                                                .name("Books")
                                                .icon("book")
                                                .color("#FF5733")
                                                .sortOrder(0)
                                                .build()))
                                .tasks(List.of(
                                        ExportTaskData.builder()
                                                .id(UUID.randomUUID())
                                                .title("Read Dune")
                                                .gtdList(GtdList.NEXT_ACTIONS)
                                                .nestingLevel(1)
                                                .sortOrder(0)
                                                .subtasks(Collections.emptyList())
                                                .reminders(List.of(
                                                        ExportReminderData.builder()
                                                                .id(UUID.randomUUID())
                                                                .remindAt(Instant.now())
                                                                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                                                                .offsetValue(1)
                                                                .build()))
                                                .build()))
                                .build()))
                .build();

        when(exportService.exportAll(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/export")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.exportDate").isNotEmpty())
                .andExpect(jsonPath("$.contexts").isArray())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].name").value("Work"))
                .andExpect(jsonPath("$.contexts[0].categories.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks[0].reminders.length()").value(1));
    }

    @Test
    void exportAll_shouldReturnEmptyContextsList() throws Exception {
        ExportResponse response = ExportResponse.builder()
                .exportDate(Instant.now())
                .version("1.0")
                .contexts(Collections.emptyList())
                .build();

        when(exportService.exportAll(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/export")
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts").isEmpty());
    }

    @Test
    void exportContext_shouldReturn200WithSingleContext() throws Exception {
        UUID contextId = UUID.randomUUID();

        ExportResponse response = ExportResponse.builder()
                .exportDate(Instant.now())
                .version("1.0")
                .contexts(List.of(
                        ExportContextData.builder()
                                .id(contextId)
                                .name("Work")
                                .theme(ContextTheme.FORMAL)
                                .icon("briefcase")
                                .sortOrder(0)
                                .categories(Collections.emptyList())
                                .tasks(Collections.emptyList())
                                .build()))
                .build();

        when(exportService.exportContext(contextId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", contextId.toString())
                        .with(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].id").value(contextId.toString()));
    }

    @Test
    void exportContext_shouldReturn404WhenNotFound() throws Exception {
        UUID contextId = UUID.randomUUID();

        when(exportService.exportContext(contextId, userId))
                .thenThrow(new ContextNotFoundException(contextId));

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", contextId.toString())
                        .with(authenticatedUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportContext_shouldReturn403WhenNotOwner() throws Exception {
        UUID contextId = UUID.randomUUID();

        when(exportService.exportContext(contextId, userId))
                .thenThrow(new ContextAccessDeniedException(contextId));

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", contextId.toString())
                        .with(authenticatedUser()))
                .andExpect(status().isForbidden());
    }
}
