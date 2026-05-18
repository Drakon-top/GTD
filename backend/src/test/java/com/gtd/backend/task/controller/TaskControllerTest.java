package com.gtd.backend.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.service.TaskService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TaskService taskService;

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
    private final UUID taskId = UUID.randomUUID();

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
    void shouldReturnTasksList() throws Exception {
        TaskResponse response = buildResponse("Buy groceries", GtdList.INBOX);
        when(taskService.getTasks(eq(contextId), isNull(), eq(userId)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Buy groceries"))
                .andExpect(jsonPath("$[0].gtdList").value("INBOX"));
    }

    @Test
    void shouldReturnTasksFilteredByGtdList() throws Exception {
        TaskResponse response = buildResponse("Next task", GtdList.NEXT_ACTIONS);
        when(taskService.getTasks(eq(contextId), eq(GtdList.NEXT_ACTIONS), eq(userId)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "NEXT_ACTIONS")
                        .with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gtdList").value("NEXT_ACTIONS"));
    }

    @Test
    void shouldReturnEmptyTasksList() throws Exception {
        when(taskService.getTasks(eq(contextId), isNull(), eq(userId)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn404WhenContextNotFoundOnGetTasks() throws Exception {
        when(taskService.getTasks(eq(contextId), isNull(), eq(userId)))
                .thenThrow(new ContextNotFoundException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId).with(withUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn403WhenContextAccessDeniedOnGetTasks() throws Exception {
        when(taskService.getTasks(eq(contextId), isNull(), eq(userId)))
                .thenThrow(new ContextAccessDeniedException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId).with(withUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void shouldCreateTask() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Buy groceries")
                .build();

        TaskResponse response = buildResponse("Buy groceries", GtdList.INBOX);
        when(taskService.createTask(eq(contextId), any(CreateTaskRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy groceries"))
                .andExpect(jsonPath("$.gtdList").value("INBOX"))
                .andExpect(jsonPath("$.nestingLevel").value(1));
    }

    @Test
    void shouldReturn400WhenTitleBlank() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn400WhenTitleMissing() throws Exception {
        String json = """
                {"notes": "some notes"}
                """;

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetTaskById() throws Exception {
        TaskResponse response = buildResponse("My task", GtdList.INBOX);
        response.setSubtasks(List.of());
        when(taskService.getTask(taskId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My task"))
                .andExpect(jsonPath("$.subtasks").isArray());
    }

    @Test
    void shouldReturn404WhenTaskNotFound() throws Exception {
        when(taskService.getTask(taskId, userId)).thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn403WhenTaskAccessDenied() throws Exception {
        when(taskService.getTask(taskId, userId)).thenThrow(new TaskAccessDeniedException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void shouldUpdateTask() throws Exception {
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated title")
                .notes("New notes")
                .build();

        TaskResponse response = buildResponse("Updated title", GtdList.INBOX);
        response.setNotes("New notes");
        when(taskService.updateTask(eq(taskId), any(UpdateTaskRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.notes").value("New notes"));
    }

    @Test
    void shouldDeleteTask() throws Exception {
        doNothing().when(taskService).deleteTask(taskId, userId);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404OnDeleteWhenNotFound() throws Exception {
        doThrow(new TaskNotFoundException(taskId))
                .when(taskService).deleteTask(taskId, userId);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnDeleteWhenNotOwner() throws Exception {
        doThrow(new TaskAccessDeniedException(taskId))
                .when(taskService).deleteTask(taskId, userId);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    private TaskResponse buildResponse(String title, GtdList gtdList) {
        return TaskResponse.builder()
                .id(taskId)
                .contextId(contextId)
                .gtdList(gtdList)
                .title(title)
                .nestingLevel(1)
                .sortOrder(0)
                .completed(false)
                .version(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
