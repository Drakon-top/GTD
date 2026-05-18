package com.gtd.backend.reminder.integration;

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
import com.gtd.backend.reminder.dto.CreateReminderRequest;
import com.gtd.backend.reminder.dto.UpdateReminderRequest;
import com.gtd.backend.reminder.model.ReminderOffsetType;
import com.gtd.backend.reminder.repository.ReminderRepository;
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
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReminderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private String taskId;

    @BeforeEach
    void setUp() throws Exception {
        reminderRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();

        registerAndLogin("reminder-test@test.com", "password123");
        contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");
        taskId = createTask("Buy groceries");
    }

    @Test
    void shouldCreateReminderAndReturnIt() throws Exception {
        Instant remindAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .offsetType(ReminderOffsetType.HOURS_BEFORE)
                .offsetValue(2)
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.offsetType").value("HOURS_BEFORE"))
                .andExpect(jsonPath("$.offsetValue").value(2))
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldCreateMultipleRemindersForSameTask() throws Exception {
        createReminder(Instant.now().plus(1, ChronoUnit.HOURS));
        createReminder(Instant.now().plus(2, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldListRemindersOrderedByRemindAt() throws Exception {
        Instant later = Instant.now().plus(3, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
        Instant earlier = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

        createReminder(later);
        createReminder(earlier);

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].remindAt").value(earlier.toString()))
                .andExpect(jsonPath("$[1].remindAt").value(later.toString()));
    }

    @Test
    void shouldUpdateReminderRemindAt() throws Exception {
        String reminderId = createReminder(Instant.now().plus(1, ChronoUnit.HOURS));
        Instant newRemindAt = Instant.now().plus(5, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .remindAt(newRemindAt)
                .build();

        mockMvc.perform(put("/api/v1/reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remindAt").value(newRemindAt.toString()))
                .andExpect(jsonPath("$.sent").value(false));
    }

    @Test
    void shouldUpdateReminderOffsetFields() throws Exception {
        String reminderId = createReminder(Instant.now().plus(1, ChronoUnit.HOURS));

        UpdateReminderRequest request = UpdateReminderRequest.builder()
                .offsetType(ReminderOffsetType.DAYS_BEFORE)
                .offsetValue(3)
                .build();

        mockMvc.perform(put("/api/v1/reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offsetType").value("DAYS_BEFORE"))
                .andExpect(jsonPath("$.offsetValue").value(3));
    }

    @Test
    void shouldDeleteReminder() throws Exception {
        String reminderId = createReminder(Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(delete("/api/v1/reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404WhenReminderNotFound() throws Exception {
        String fakeId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(delete("/api/v1/reminders/{id}", fakeId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenTaskNotFound() throws Exception {
        String fakeTaskId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", fakeTaskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersReminder() throws Exception {
        String reminderId = createReminder(Instant.now().plus(1, ChronoUnit.HOURS));

        String otherToken = registerAndLoginOther("other-reminder@test.com", "password123");

        mockMvc.perform(delete("/api/v1/reminders/{id}", reminderId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersTaskReminders() throws Exception {
        String otherToken = registerAndLoginOther("other2-reminder@test.com", "password123");

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteRemindersWhenTaskIsDeletedViaCascade() throws Exception {
        createReminder(Instant.now().plus(1, ChronoUnit.HOURS));
        createReminder(Instant.now().plus(2, ChronoUnit.HOURS));

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    private void registerAndLogin(String email, String password) throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
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

        String body = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(body).get("accessToken").asText();
    }

    private String registerAndLoginOther(String email, String password) throws Exception {
        RegisterRequest regRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
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

        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
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

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createReminder(Instant remindAt) throws Exception {
        CreateReminderRequest request = CreateReminderRequest.builder()
                .remindAt(remindAt)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
