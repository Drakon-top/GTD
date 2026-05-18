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
import com.gtd.backend.task.dto.MoveTaskRequest;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.exception.MaxNestingLevelException;
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

    @Test
    void shouldMoveTask() throws Exception {
        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.NEXT_ACTIONS)
                .build();

        TaskResponse response = buildResponse("My task", GtdList.NEXT_ACTIONS);
        when(taskService.moveTask(eq(taskId), eq(GtdList.NEXT_ACTIONS), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtdList").value("NEXT_ACTIONS"));
    }

    @Test
    void shouldReturn400WhenMoveWithNullGtdList() throws Exception {
        String json = "{}";

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn404WhenMoveDeletedTask() throws Exception {
        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.NEXT_ACTIONS)
                .build();

        when(taskService.moveTask(eq(taskId), eq(GtdList.NEXT_ACTIONS), eq(userId)))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCompleteTask() throws Exception {
        TaskResponse response = buildResponse("Completed task", GtdList.DONE);
        response.setCompleted(true);
        response.setCompletedAt(Instant.now());
        when(taskService.completeTask(eq(taskId), eq(userId))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.gtdList").value("DONE"));
    }

    @Test
    void shouldReturn404WhenCompleteDeletedTask() throws Exception {
        when(taskService.completeTask(eq(taskId), eq(userId)))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenCompleteNotOwned() throws Exception {
        when(taskService.completeTask(eq(taskId), eq(userId)))
                .thenThrow(new TaskAccessDeniedException(taskId));

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetSubtasks() throws Exception {
        TaskResponse subtask = buildResponse("Subtask 1", GtdList.INBOX);
        subtask.setNestingLevel(2);
        subtask.setParentTaskId(taskId);
        when(taskService.getSubtasks(eq(taskId), eq(userId)))
                .thenReturn(List.of(subtask));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/subtasks", taskId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Subtask 1"))
                .andExpect(jsonPath("$[0].nestingLevel").value(2))
                .andExpect(jsonPath("$[0].parentTaskId").value(taskId.toString()));
    }

    @Test
    void shouldReturn404WhenGetSubtasksParentNotFound() throws Exception {
        when(taskService.getSubtasks(eq(taskId), eq(userId)))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/subtasks", taskId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateSubtask() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New subtask")
                .build();

        TaskResponse response = buildResponse("New subtask", GtdList.INBOX);
        response.setNestingLevel(2);
        response.setParentTaskId(taskId);
        when(taskService.createSubtask(eq(taskId), any(CreateTaskRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New subtask"))
                .andExpect(jsonPath("$.nestingLevel").value(2))
                .andExpect(jsonPath("$.parentTaskId").value(taskId.toString()));
    }

    @Test
    void shouldReturn400WhenMaxNestingLevelExceeded() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Too deep")
                .build();

        when(taskService.createSubtask(eq(taskId), any(CreateTaskRequest.class), eq(userId)))
                .thenThrow(new MaxNestingLevelException());

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Maximum nesting level (4) exceeded. Cannot create subtask at level 5."));
    }

    @Test
    void shouldReturn400WhenSubtaskTitleBlank() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenCreateSubtaskParentNotFound() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Orphan subtask")
                .build();

        when(taskService.createSubtask(eq(taskId), any(CreateTaskRequest.class), eq(userId)))
                .thenThrow(new TaskNotFoundException(taskId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenCreateSubtaskNotOwner() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Not my subtask")
                .build();

        when(taskService.createSubtask(eq(taskId), any(CreateTaskRequest.class), eq(userId)))
                .thenThrow(new TaskAccessDeniedException(taskId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", taskId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
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
