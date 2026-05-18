package com.gtd.backend.reminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.dto.ReminderResponse;
import com.gtd.backend.reminder.dto.UpdateReminderRequest;
import com.gtd.backend.reminder.exception.ReminderAccessDeniedException;
import com.gtd.backend.reminder.exception.ReminderNotFoundException;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.service.ReminderService;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
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
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService reminderService;

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
    private final UUID taskId = UUID.randomUUID();
    private final UUID reminderId = UUID.randomUUID();

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
    void shouldReturnRemindersList() throws Exception {
        ReminderResponse response = buildResponse();
        when(reminderService.getReminders(taskId, userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reminderId.toString()))
                .andExpect(jsonPath("$[0].taskId").value(taskId.toString()));
    }

    @Test
    void shouldReturnEmptyRemindersList() throws Exception {
        when(reminderService.getReminders(taskId, userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnList() throws Exception {
        when(reminderService.getReminders(taskId, userId))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenTaskAccessDeniedOnList() throws Exception {
        when(reminderService.getReminders(taskId, userId))
                .thenThrow(new TaskAccessDeniedException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateReminder() throws Exception {
        Instant remindAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                .offsetValue(2)
                .build();

        ReminderResponse response = buildResponse();
        response.setRemindAt(remindAt);
        response.setOffsetType(ReminderOffsetType.HOURS_BEFORE);
        response.setOffsetValue(2);
        when(reminderService.createReminder(eq(taskId), any(CreateReminderRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reminderId.toString()))
                .andExpect(jsonPath("$.offsetType").value("HOURS_BEFORE"))
                .andExpect(jsonPath("$.offsetValue").value(2));
    }

    @Test
    void shouldReturn400WhenRemindAtNull() throws Exception {
        CreateReminderRequest request = CreateReminderRequest.builder()
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn404WhenTaskNotFoundOnCreate() throws Exception {
        Instant remindAt = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .build();

        when(reminderService.createReminder(eq(taskId), any(CreateReminderRequest.class), eq(userId)))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenTaskAccessDeniedOnCreate() throws Exception {
        Instant remindAt = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .build();

        when(reminderService.createReminder(eq(taskId), any(CreateReminderRequest.class), eq(userId)))
                .thenThrow(new TaskAccessDeniedException(taskId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateReminder() throws Exception {
        Instant newRemindAt = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(newRemindAt)
                .build();

        ReminderResponse response = buildResponse();
        response.setRemindAt(newRemindAt);
        when(reminderService.updateReminder(eq(reminderId), any(UpdateReminderRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/reminders/{id}", reminderId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reminderId.toString()));
    }

    @Test
    void shouldReturn404WhenReminderNotFoundOnUpdate() throws Exception {
        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(reminderService.updateReminder(eq(reminderId), any(UpdateReminderRequest.class), eq(userId)))
                .thenThrow(new ReminderNotFoundException(reminderId));

        mockMvc.perform(put("/api/v1/reminders/{id}", reminderId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenReminderAccessDeniedOnUpdate() throws Exception {
        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        when(reminderService.updateReminder(eq(reminderId), any(UpdateReminderRequest.class), eq(userId)))
                .thenThrow(new ReminderAccessDeniedException(reminderId));

        mockMvc.perform(put("/api/v1/reminders/{id}", reminderId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteReminder() throws Exception {
        doNothing().when(reminderService).deleteReminder(reminderId, userId);

        mockMvc.perform(delete("/api/v1/reminders/{id}", reminderId).with(withUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404OnDeleteWhenNotFound() throws Exception {
        doThrow(new ReminderNotFoundException(reminderId))
                .when(reminderService).deleteReminder(reminderId, userId);

        mockMvc.perform(delete("/api/v1/reminders/{id}", reminderId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnDeleteWhenNotOwner() throws Exception {
        doThrow(new ReminderAccessDeniedException(reminderId))
                .when(reminderService).deleteReminder(reminderId, userId);

        mockMvc.perform(delete("/api/v1/reminders/{id}", reminderId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    private ReminderResponse buildResponse() {
        return ReminderResponse.builder()
                .id(reminderId)
                .taskId(taskId)
                .remindAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .isSent(false)
                .createdAt(Instant.now())
                .build();
    }
}
