package com.gtd.backend.task.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
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

    private String accessToken;
    private String contextId;

    @BeforeEach
    void setUp() throws Exception {
        taskRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

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
                .andExpect(jsonPath("$.completed").value(false))
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
                .andExpect(jsonPath("$.completed").value(true))
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
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.hasIncompleteSubtasks").value(true));
    }

    @Test
    void shouldNotReturnHasIncompleteSubtasksWhenNoSubtasks() throws Exception {
        String taskId = createTask("No children", null);

        mockMvc.perform(patch("/api/v1/tasks/{id}/complete", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
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
}
