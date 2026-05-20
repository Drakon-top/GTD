package com.gtd.backend.export.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.category.dto.CreateCategoryRequest;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.sync.repository.SyncLogRepository;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String accessToken;
    private String contextId;

    @BeforeEach
    void setUp() throws Exception {
        syncLogRepository.deleteAll();
        reminderRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();

        registerAndLogin("export-test@test.com", "password123");
        contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");
    }

    @Test
    void shouldExportAllContextsWithFullData() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");
        String taskId = createTask("Read Dune");
        createReminder(taskId, Instant.now().plusSeconds(3600));
        createSubtask(taskId, "Chapter 1");

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andExpect(jsonPath("$.exportDate").isNotEmpty())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].name").value("Work"))
                .andExpect(jsonPath("$.contexts[0].theme").value("FORMAL"))
                .andExpect(jsonPath("$.contexts[0].categories.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].categories[0].name").value("Books"))
                .andExpect(jsonPath("$.contexts[0].tasks.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks[0].title").value("Read Dune"))
                .andExpect(jsonPath("$.contexts[0].tasks[0].subtasks.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks[0].subtasks[0].title").value("Chapter 1"))
                .andExpect(jsonPath("$.contexts[0].tasks[0].reminders.length()").value(1));
    }

    @Test
    void shouldExportSpecificContext() throws Exception {
        String context2Id = createContext("Home", ContextTheme.NATURE, "house");
        createTaskInContext(context2Id, "Clean kitchen");

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].name").value("Work"));
    }

    @Test
    void shouldExcludeSoftDeletedTasks() throws Exception {
        String taskId = createTask("Active task");
        String deletedTaskId = createTask("Deleted task");

        mockMvc.perform(delete("/api/v1/tasks/{id}", deletedTaskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts[0].tasks.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks[0].title").value("Active task"));
    }

    @Test
    void shouldExcludeSoftDeletedCategories() throws Exception {
        String catId = createCategory("Active", "check", "#00FF00");
        String deletedCatId = createCategory("Deleted", "x", "#FF0000");

        mockMvc.perform(delete("/api/v1/categories/{id}", deletedCatId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts[0].categories.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].categories[0].name").value("Active"));
    }

    @Test
    void shouldReturnEmptyExportForUserWithNoContexts() throws Exception {
        reminderRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        contextRepository.deleteAll();

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts").isEmpty())
                .andExpect(jsonPath("$.version").value("1.0"));
    }

    @Test
    void shouldReturn404ForNonExistentContext() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", unknownId.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403ForOtherUsersContext() throws Exception {
        String otherToken = registerAndLoginOther("other-export@test.com", "password123");
        String otherContextId = createContextWithToken(otherToken, "Private", ContextTheme.DARK, "lock");

        mockMvc.perform(get("/api/v1/export")
                        .param("context_id", otherContextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldExportMultipleContexts() throws Exception {
        createContext("Home", ContextTheme.NATURE, "house");

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.length()").value(2));
    }

    @Test
    void shouldExportNestedSubtasksRecursively() throws Exception {
        String taskId = createTask("Level 1");
        String level2Id = createSubtask(taskId, "Level 2");
        String level3Id = createSubtask(level2Id, "Level 3");

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts[0].tasks[0].title").value("Level 1"))
                .andExpect(jsonPath("$.contexts[0].tasks[0].subtasks[0].title").value("Level 2"))
                .andExpect(jsonPath("$.contexts[0].tasks[0].subtasks[0].subtasks[0].title").value("Level 3"));
    }

    @Test
    void shouldExportMultipleRemindersForTask() throws Exception {
        String taskId = createTask("Important task");
        createReminder(taskId, Instant.now().plusSeconds(3600));
        createReminder(taskId, Instant.now().plusSeconds(7200));

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts[0].tasks[0].reminders.length()").value(2));
    }

    @Test
    void shouldIsolateExportBetweenUsers() throws Exception {
        createTask("My task");

        String otherToken = registerAndLoginOther("other-user-export@test.com", "password123");
        String otherContextId = createContextWithToken(otherToken, "Other", ContextTheme.MINIMALIST, "star");
        createTaskInContextWithToken(otherToken, otherContextId, "Other task");

        mockMvc.perform(get("/api/v1/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts.length()").value(1))
                .andExpect(jsonPath("$.contexts[0].tasks[0].title").value("My task"));
    }

    // --- Helper methods ---

    private void registerAndLogin(String email, String password) throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email(email).password(password).build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email).password(password).build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String registerAndLoginOther(String email, String password) throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email(email).password(password).build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email).password(password).build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createContext(String name, ContextTheme theme, String icon) throws Exception {
        return createContextWithToken(accessToken, name, theme, icon);
    }

    private String createContextWithToken(String token, String name, ContextTheme theme, String icon) throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name(name).theme(theme).icon(icon).build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTask(String title) throws Exception {
        return createTaskInContext(contextId, title);
    }

    private String createTaskInContext(String ctxId, String title) throws Exception {
        return createTaskInContextWithToken(accessToken, ctxId, title);
    }

    private String createTaskInContextWithToken(String token, String ctxId, String title) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder().title(title).build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", ctxId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createSubtask(String parentTaskId, String title) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder().title(title).build();

        MvcResult result = mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", parentTaskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCategory(String name, String icon, String color) throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(name).icon(icon).color(color).build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createReminder(String taskId, Instant remindAt) throws Exception {
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt).build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
