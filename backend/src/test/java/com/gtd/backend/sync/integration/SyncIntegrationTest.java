package com.gtd.backend.sync.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.sync.dto.SyncChangeRequest;
import com.gtd.backend.sync.dto.SyncPushRequest;
import com.gtd.backend.sync.model.DeviceSource;
import com.gtd.backend.sync.model.SyncEntityType;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SyncLogRepository syncLogRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ReminderRepository reminderRepository;

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

        registerAndLogin("sync-test@test.com", "password123");
        contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");
    }

    @Test
    void shouldPushTitleChangeAndUpdateTask() throws Exception {
        String taskId = createTask("Original Title");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Updated Via Sync")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1))
                .andExpect(jsonPath("$.conflictCount").value(0))
                .andExpect(jsonPath("$.results[0].applied").value(true));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Via Sync"));
    }

    @Test
    void shouldDetectFieldConflict_serverWins() throws Exception {
        String taskId = createTask("Original Title");

        updateTaskTitle(taskId, "Server Updated Title");
        Thread.sleep(50);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Stale Client Change")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.parse("2020-01-01T00:00:00Z"))
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(0))
                .andExpect(jsonPath("$.conflictCount").value(1))
                .andExpect(jsonPath("$.results[0].applied").value(false))
                .andExpect(jsonPath("$.results[0].conflictStatus").value("RESOLVED_NOTIFY"))
                .andExpect(jsonPath("$.results[0].serverValue").value("Server Updated Title"));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server Updated Title"));
    }

    @Test
    void shouldDetectFieldConflict_clientWins() throws Exception {
        String taskId = createTask("Original Title");

        updateTaskTitle(taskId, "Server Updated Title");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Original Title")
                .newValue("Client Wins Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now().plusSeconds(3600))
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1))
                .andExpect(jsonPath("$.conflictCount").value(1))
                .andExpect(jsonPath("$.results[0].applied").value(true))
                .andExpect(jsonPath("$.results[0].conflictStatus").value("RESOLVED_NOTIFY"));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Client Wins Title"));
    }

    @Test
    void shouldMergeNonConflictingFieldChanges() throws Exception {
        String taskId = createTask("Original Title");

        updateTaskTitle(taskId, "Server Changed Title");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("notes")
                .oldValue(null)
                .newValue("Client Added Notes")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1))
                .andExpect(jsonPath("$.conflictCount").value(0))
                .andExpect(jsonPath("$.results[0].applied").value(true))
                .andExpect(jsonPath("$.results[0].conflictStatus").value("NO_CONFLICT"));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Server Changed Title"))
                .andExpect(jsonPath("$.notes").value("Client Added Notes"));
    }

    @Test
    void shouldPullChangesAfterPush() throws Exception {
        Instant beforePush = Instant.now();
        String taskId = createTask("Pull Test");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Pull Test")
                .newValue("Changed Title")
                .deviceSource(DeviceSource.WEB)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sync/pull")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("since", beforePush.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeCount").value(1))
                .andExpect(jsonPath("$.changes[0].entityType").value("TASK"))
                .andExpect(jsonPath("$.changes[0].entityId").value(taskId))
                .andExpect(jsonPath("$.changes[0].fieldName").value("title"))
                .andExpect(jsonPath("$.changes[0].newValue").value("Changed Title"))
                .andExpect(jsonPath("$.serverTimestamp").isNotEmpty());
    }

    @Test
    void shouldReturnEmptyPullWhenNoChanges() throws Exception {
        mockMvc.perform(get("/api/v1/sync/pull")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("since", Instant.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeCount").value(0))
                .andExpect(jsonPath("$.changes").isEmpty());
    }

    @Test
    void shouldIncrementVersionOnSyncPush() throws Exception {
        String taskId = createTask("Version Test");
        int initialVersion = getTaskVersion(taskId);

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("notes")
                .oldValue(null)
                .newValue("Some notes")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].newVersion").value(initialVersion + 1));
    }

    @Test
    void shouldRecordInSyncLog() throws Exception {
        String taskId = createTask("Log Test");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Log Test")
                .newValue("Logged Change")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(syncLogRepository.findByEntityIdOrderByCreatedAtDesc(UUID.fromString(taskId))).hasSize(1);
    }

    @Test
    void shouldNotApplyChangeForOtherUsersTask() throws Exception {
        String taskId = createTask("My Task");

        String otherToken = registerAndGetToken("other@test.com", "password123");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("My Task")
                .newValue("Hacked Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(0))
                .andExpect(jsonPath("$.results[0].applied").value(false))
                .andExpect(jsonPath("$.results[0].error").value("Access denied"));
    }

    @Test
    void shouldPushMultipleFieldChanges() throws Exception {
        String taskId = createTask("Multi Field");

        SyncChangeRequest change1 = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Multi Field")
                .newValue("New Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncChangeRequest change2 = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("notes")
                .oldValue(null)
                .newValue("New Notes")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change1, change2))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(2));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.notes").value("New Notes"));
    }

    @Test
    void shouldHandleDeletedTask() throws Exception {
        String taskId = createTask("Delete Me");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Delete Me")
                .newValue("Too Late")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(0))
                .andExpect(jsonPath("$.results[0].error").value(org.hamcrest.Matchers.containsString("not found")));
    }

    @Test
    void shouldChangeGtdListViaSync() throws Exception {
        String taskId = createTask("Move Me");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("gtdList")
                .oldValue("INBOX")
                .newValue("NEXT_ACTIONS")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(1));

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtdList").value("NEXT_ACTIONS"));
    }

    @Test
    void shouldRecordConflictInSyncLog() throws Exception {
        String taskId = createTask("Conflict Log Test");

        updateTaskTitle(taskId, "Server Changed");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Conflict Log Test")
                .newValue("Client Changed")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.parse("2020-01-01T00:00:00Z"))
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflictCount").value(1));

        var logs = syncLogRepository.findByEntityIdOrderByCreatedAtDesc(UUID.fromString(taskId));
        assertThat(logs).isNotEmpty();
        assertThat(logs.get(0).getConflictStatus().name()).isEqualTo("RESOLVED_NOTIFY");
    }

    @Test
    void shouldSkipWhenNoChangeDetected() throws Exception {
        String taskId = createTask("Same Title");

        SyncChangeRequest change = SyncChangeRequest.builder()
                .entityType(SyncEntityType.TASK)
                .entityId(UUID.fromString(taskId))
                .fieldName("title")
                .oldValue("Same Title")
                .newValue("Same Title")
                .deviceSource(DeviceSource.ANDROID)
                .clientTimestamp(Instant.now())
                .build();

        SyncPushRequest request = SyncPushRequest.builder()
                .changes(List.of(change))
                .build();

        mockMvc.perform(post("/api/v1/sync/push")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(0))
                .andExpect(jsonPath("$.conflictCount").value(0));
    }

    // --- Helper methods ---

    private void registerAndLogin(String email, String password) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private String createContext(String name, ContextTheme theme, String icon) throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name(name)
                .theme(theme)
                .icon(icon)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private String createTask(String title) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title(title)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    private int getTaskVersion(String taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("version").asInt();
    }

    private void updateTaskTitle(String taskId, String newTitle) throws Exception {
        String body = "{\"title\":\"" + newTitle + "\"}";
        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
