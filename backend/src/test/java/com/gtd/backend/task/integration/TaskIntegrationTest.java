package com.gtd.backend.task.integration;

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
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.MoveTaskRequest;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.model.GtdList;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String accessToken;
    private String contextId;

    @BeforeEach
    void setUp() throws Exception {
        reminderRepository.deleteAll();
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();

        registerAndLogin("task-test@test.com", "password123");
        contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");
    }

    @Test
    void shouldCreateTaskWithOnlyTitle() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Buy groceries")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Buy groceries"))
                .andExpect(jsonPath("$.gtdList").value("INBOX"))
                .andExpect(jsonPath("$.nestingLevel").value(1))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.contextId").value(contextId))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldCreateTaskInSpecificGtdList() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Important task")
                .gtdList(GtdList.NEXT_ACTIONS)
                .notes("Do this first")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gtdList").value("NEXT_ACTIONS"))
                .andExpect(jsonPath("$.notes").value("Do this first"));
    }

    @Test
    void shouldListTasksInContext() throws Exception {
        createTask("Task 1", null);
        createTask("Task 2", null);

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Task 1"))
                .andExpect(jsonPath("$[1].title").value("Task 2"));
    }

    @Test
    void shouldFilterTasksByGtdList() throws Exception {
        createTask("Inbox task", null);
        createTaskWithGtdList("Next task", GtdList.NEXT_ACTIONS);

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "INBOX")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Inbox task"));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "NEXT_ACTIONS")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Next task"));
    }

    @Test
    void shouldGetTaskByIdWithSubtasks() throws Exception {
        String taskId = createTask("Parent task", null);

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Parent task"))
                .andExpect(jsonPath("$.subtasks").isArray())
                .andExpect(jsonPath("$.subtasks.length()").value(0));
    }

    @Test
    void shouldUpdateTask() throws Exception {
        String taskId = createTask("Old title", null);

        Instant dueDate = Instant.parse("2026-06-15T10:00:00Z");
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("New title")
                .notes("Updated notes")
                .dueDate(dueDate)
                .build();

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.notes").value("Updated notes"))
                .andExpect(jsonPath("$.dueDate").isNotEmpty());
    }

    @Test
    void shouldSoftDeleteTask() throws Exception {
        String taskId = createTask("To delete", null);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404ForDeletedTask() throws Exception {
        String taskId = createTask("To delete", null);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersTask() throws Exception {
        String taskId = createTask("My task", null);

        String otherToken = registerAndLoginOther("other-task@test.com", "password123");

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIsolateTasksBetweenContexts() throws Exception {
        createTask("Task in Work", null);

        String otherContextId = createContext("Home", ContextTheme.NATURE, "house");

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", otherContextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn400WhenTitleBlank() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMoveTaskToAnotherGtdList() throws Exception {
        String taskId = createTask("Inbox task", null);

        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.NEXT_ACTIONS)
                .build();

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtdList").value("NEXT_ACTIONS"))
                .andExpect(jsonPath("$.id").value(taskId));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "NEXT_ACTIONS")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Inbox task"));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "INBOX")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldCompleteTask() throws Exception {
        String taskId = createTask("Task to complete", null);

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.gtdList").value("DONE"));
    }

    @Test
    void shouldReturn404WhenMovingDeletedTask() throws Exception {
        String taskId = createTask("To delete then move", null);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.NEXT_ACTIONS)
                .build();

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldIncrementVersionOnMove() throws Exception {
        String taskId = createTask("Version test", null);

        MvcResult getResult = mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        int versionBefore = objectMapper.readTree(getResult.getResponse().getContentAsString())
                .get("version").asInt();

        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.PROJECTS)
                .build();

        MvcResult moveResult = mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        int versionAfter = objectMapper.readTree(moveResult.getResponse().getContentAsString())
                .get("version").asInt();

        assertThat(versionAfter).isGreaterThan(versionBefore);
    }

    @Test
    void shouldIncrementVersionOnComplete() throws Exception {
        String taskId = createTask("Complete version test", null);

        MvcResult getResult = mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        int versionBefore = objectMapper.readTree(getResult.getResponse().getContentAsString())
                .get("version").asInt();

        MvcResult completeResult = mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        int versionAfter = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("version").asInt();

        assertThat(versionAfter).isGreaterThan(versionBefore);
    }

    @Test
    void shouldReturn403WhenMovingOtherUsersTask() throws Exception {
        String taskId = createTask("My task", null);

        String otherToken = registerAndLoginOther("other-move@test.com", "password123");

        MoveTaskRequest request = MoveTaskRequest.builder()
                .gtdList(GtdList.NEXT_ACTIONS)
                .build();

        mockMvc.perform(patch("/api/v1/tasks/{id}/move", taskId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateSubtaskWithCorrectNestingLevel() throws Exception {
        String parentId = createTask("Parent task", null);

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Subtask level 2")
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", parentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Subtask level 2"))
                .andExpect(jsonPath("$.nestingLevel").value(2))
                .andExpect(jsonPath("$.parentTaskId").value(parentId))
                .andExpect(jsonPath("$.contextId").value(contextId));
    }

    @Test
    void shouldCreateNestedSubtasksUpToLevel4() throws Exception {
        String level1 = createTask("Level 1", null);
        String level2 = createSubtask(level1, "Level 2");
        String level3 = createSubtask(level2, "Level 3");
        String level4 = createSubtask(level3, "Level 4");

        mockMvc.perform(get("/api/v1/tasks/{id}", level4)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nestingLevel").value(4));
    }

    @Test
    void shouldReturn400WhenCreatingSubtaskBeyondLevel4() throws Exception {
        String level1 = createTask("Level 1", null);
        String level2 = createSubtask(level1, "Level 2");
        String level3 = createSubtask(level2, "Level 3");
        String level4 = createSubtask(level3, "Level 4");

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Level 5 attempt")
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", level4)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Maximum nesting level (4) exceeded. Cannot create subtask at level 5."));
    }

    @Test
    void shouldGetSubtasksList() throws Exception {
        String parentId = createTask("Parent", null);
        createSubtask(parentId, "Sub 1");
        createSubtask(parentId, "Sub 2");

        mockMvc.perform(get("/api/v1/tasks/{taskId}/subtasks", parentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Sub 1"))
                .andExpect(jsonPath("$[1].title").value("Sub 2"));
    }

    @Test
    void shouldCascadeSoftDeleteToSubtasks() throws Exception {
        String parentId = createTask("Parent", null);
        String childId = createSubtask(parentId, "Child");
        String grandchildId = createSubtask(childId, "Grandchild");

        mockMvc.perform(delete("/api/v1/tasks/{id}", parentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", parentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tasks/{id}", childId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tasks/{id}", grandchildId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnHasIncompleteSubtasksOnComplete() throws Exception {
        String parentId = createTask("Parent", null);
        createSubtask(parentId, "Incomplete subtask");

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", parentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.hasIncompleteSubtasks").value(true));
    }

    @Test
    void shouldNotReturnHasIncompleteSubtasksWhenNoSubtasks() throws Exception {
        String taskId = createTask("No children", null);

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.hasIncompleteSubtasks").doesNotExist());
    }

    @Test
    void shouldInheritContextFromParentOnSubtaskCreation() throws Exception {
        String parentId = createTask("Parent in Work", null);

        String otherContextId = createContext("Home", ContextTheme.NATURE, "house");

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Subtask inherits context")
                .build();

        mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", parentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contextId").value(contextId));
    }

    @Test
    void shouldReturnProgressForProjectTask() throws Exception {
        String projectId = createTask("My Project", GtdList.PROJECTS);
        String sub1 = createSubtask(projectId, "Subtask 1");
        String sub2 = createSubtask(projectId, "Subtask 2");
        String sub3 = createSubtask(projectId, "Subtask 3");
        createSubtask(projectId, "Subtask 4");

        // Complete 2 out of 4 subtasks
        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", sub1)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", sub2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // GET single project task — should have progress=50%
        mockMvc.perform(get("/api/v1/tasks/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(50));
    }

    @Test
    void shouldReturnProgress0WhenNoSubtasksCompleted() throws Exception {
        String projectId = createTask("Empty Project", GtdList.PROJECTS);
        createSubtask(projectId, "Subtask 1");
        createSubtask(projectId, "Subtask 2");

        mockMvc.perform(get("/api/v1/tasks/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    void shouldReturnNullProgressForProjectWithNoSubtasks() throws Exception {
        String projectId = createTask("Lonely Project", GtdList.PROJECTS);

        mockMvc.perform(get("/api/v1/tasks/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").doesNotExist());
    }

    @Test
    void shouldReturnNullProgressForInboxTask() throws Exception {
        String inboxTaskId = createTask("Inbox Task", null);

        mockMvc.perform(get("/api/v1/tasks/{id}", inboxTaskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").doesNotExist());
    }

    @Test
    void shouldReturnProgressIncludingNestedSubtasks() throws Exception {
        String projectId = createTask("Deep Project", GtdList.PROJECTS);
        String sub1 = createSubtask(projectId, "Sub 1");
        String sub2 = createSubtask(projectId, "Sub 2");
        String grandchild1 = createSubtask(sub1, "Grandchild 1");
        createSubtask(sub1, "Grandchild 2");

        // Complete sub2 and grandchild1 — 2 out of 4 descendants = 50%
        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", sub2)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", grandchild1)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/tasks/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(50));
    }

    @Test
    void shouldReturnProgressInTaskList() throws Exception {
        String projectId = createTask("Listed Project", GtdList.PROJECTS);
        String sub1 = createSubtask(projectId, "Sub 1");
        createSubtask(projectId, "Sub 2");

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", sub1)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks", contextId)
                        .param("gtd_list", "PROJECTS")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].progress").value(50));
    }

    @Test
    void shouldReturnTaskCountsByGtdList() throws Exception {
        createTask("Inbox 1", null);
        createTask("Inbox 2", null);
        createTaskWithGtdList("Next 1", GtdList.NEXT_ACTIONS);
        createTaskWithGtdList("Project 1", GtdList.PROJECTS);

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks/counts", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").value(contextId))
                .andExpect(jsonPath("$.byGtdList.INBOX").value(2))
                .andExpect(jsonPath("$.byGtdList.NEXT_ACTIONS").value(1))
                .andExpect(jsonPath("$.byGtdList.PROJECTS").value(1))
                .andExpect(jsonPath("$.byGtdList.DONE").value(0))
                .andExpect(jsonPath("$.total").value(4));
    }

    @Test
    void shouldUpdateCountsAfterSoftDelete() throws Exception {
        String taskId = createTask("To delete", null);
        createTask("Keep", null);

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks/counts", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byGtdList.INBOX").value(1))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldReturn404OnCountsForNonexistentContext() throws Exception {
        String fakeContextId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks/counts", fakeContextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnCountsForOtherUsersContext() throws Exception {
        String otherToken = registerAndLoginOther("counts-other@test.com", "password123");

        mockMvc.perform(get("/api/v1/contexts/{contextId}/tasks/counts", contextId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
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

    private String createTask(String title, GtdList gtdList) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title(title)
                .gtdList(gtdList)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTaskWithGtdList(String title, GtdList gtdList) throws Exception {
        return createTask(title, gtdList);
    }

    private String createSubtask(String parentTaskId, String title) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title(title)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/tasks/{taskId}/subtasks", parentTaskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void shouldCreateTaskWithRecurrenceRule() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Daily standup")
                .recurrenceRule("{\"type\":\"daily\",\"time\":\"09:00\"}")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Daily standup"))
                .andReturn();
        assertRecurrenceRule(result, "{\"type\":\"daily\",\"time\":\"09:00\"}");
    }

    @Test
    void shouldCreateNextInstanceWhenCompletingRecurringTask() throws Exception {
        String recurrenceRule = "{\"type\":\"daily\",\"time\":\"09:00\"}";
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Daily standup")
                .recurrenceRule(recurrenceRule)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult completeResult = mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.gtdList").value("DONE"))
                .andExpect(jsonPath("$.isRecurring").value(true))
                .andExpect(jsonPath("$.nextInstanceId").isNotEmpty())
                .andReturn();

        String nextInstanceId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("nextInstanceId").asText();

        MvcResult nextResult = mockMvc.perform(get("/api/v1/tasks/{id}", nextInstanceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Daily standup"))
                .andExpect(jsonPath("$.isCompleted").value(false))
                .andExpect(jsonPath("$.gtdList").value("INBOX"))
                .andReturn();
        assertRecurrenceRule(nextResult, "{\"type\":\"daily\",\"time\":\"09:00\"}");
    }

    @Test
    void shouldPreserveOriginalGtdListInNextRecurringInstance() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Weekly review")
                .gtdList(GtdList.NEXT_ACTIONS)
                .recurrenceRule("{\"type\":\"weekly\"}")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult completeResult = mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();

        String nextInstanceId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("nextInstanceId").asText();

        mockMvc.perform(get("/api/v1/tasks/{id}", nextInstanceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gtdList").value("NEXT_ACTIONS"));
    }

    @Test
    void shouldNotCreateNextInstanceForNonRecurringTask() throws Exception {
        String taskId = createTask("One-off task", null);

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.nextInstanceId").doesNotExist())
                .andExpect(jsonPath("$.isRecurring").doesNotExist());
    }

    @Test
    void shouldCopyRemindersToNextRecurringInstance() throws Exception {
        CreateTaskRequest taskRequest = CreateTaskRequest.builder()
                .title("Daily task with reminders")
                .recurrenceRule("{\"type\":\"daily\"}")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String reminderJson = "{\"remindAt\":\"2026-06-01T08:00:00Z\",\"offsetType\":\"HOURS_BEFORE\",\"offsetValue\":1}";
        mockMvc.perform(post("/api/v1/tasks/{taskId}/reminders", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reminderJson))
                .andExpect(status().isCreated());

        MvcResult completeResult = mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        String nextInstanceId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("nextInstanceId").asText();

        mockMvc.perform(get("/api/v1/tasks/{taskId}/reminders", nextInstanceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].offsetType").value("HOURS_BEFORE"))
                .andExpect(jsonPath("$[0].offsetValue").value(1));
    }

    @Test
    void shouldStopRecurrenceBySettingEmptyRule() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Recurring task")
                .recurrenceRule("{\"type\":\"daily\"}")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        String updateJson = "{\"recurrenceRule\":\"\"}";
        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurrenceRule").doesNotExist());

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCompleted").value(true))
                .andExpect(jsonPath("$.nextInstanceId").doesNotExist())
                .andExpect(jsonPath("$.isRecurring").doesNotExist());
    }

    @Test
    void shouldInheritContextAndCategoryInNextRecurringInstance() throws Exception {
        String categoryJson = "{\"name\":\"Work\",\"icon\":\"briefcase\",\"color\":\"#FF0000\"}";
        MvcResult catResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson))
                .andExpect(status().isCreated())
                .andReturn();
        String categoryId = objectMapper.readTree(catResult.getResponse().getContentAsString()).get("id").asText();

        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Weekly review")
                .recurrenceRule("{\"type\":\"weekly\"}")
                .categoryId(UUID.fromString(categoryId))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        MvcResult completeResult = mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn();
        String nextInstanceId = objectMapper.readTree(completeResult.getResponse().getContentAsString())
                .get("nextInstanceId").asText();

        MvcResult inheritResult = mockMvc.perform(get("/api/v1/tasks/{id}", nextInstanceId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextId").value(contextId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andReturn();
        assertRecurrenceRule(inheritResult, "{\"type\":\"weekly\"}");
    }

    private void assertRecurrenceRule(MvcResult result, String expectedJson) throws Exception {
        var responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        var actual = objectMapper.readTree(responseJson.get("recurrenceRule").asText());
        var expected = objectMapper.readTree(expectedJson);
        assertThat(actual).isEqualTo(expected);
    }
}
