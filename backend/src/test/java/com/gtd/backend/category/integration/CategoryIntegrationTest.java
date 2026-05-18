package com.gtd.backend.category.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.category.dto.CreateCategoryRequest;
import com.gtd.backend.category.dto.UpdateCategoryRequest;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        taskRepository.deleteAll();
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();

        registerAndLogin("cat-test@test.com", "password123");
        contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");
    }

    @Test
    void shouldCreateCategoryAndReturnIt() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Books")
                .icon("book")
                .color("#FF5733")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.icon").value("book"))
                .andExpect(jsonPath("$.color").value("#FF5733"))
                .andExpect(jsonPath("$.contextId").value(contextId))
                .andExpect(jsonPath("$.taskCount").value(0))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldListCategoriesInContext() throws Exception {
        createCategory("Books", "book", "#FF5733");
        createCategory("Movies", "film", "#3366FF");

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Books"))
                .andExpect(jsonPath("$[1].name").value("Movies"));
    }

    @Test
    void shouldGetCategoryByIdWithTaskCount() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        createTaskWithCategory("Read Dune", catId);
        createTaskWithCategory("Read Foundation", catId);

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.taskCount").value(2));
    }

    @Test
    void shouldReturnTaskCountZeroForNewCategory() throws Exception {
        String catId = createCategory("Empty", "box", "#000000");

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskCount").value(0));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Novels")
                .color("#00FF00")
                .build();

        mockMvc.perform(put("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Novels"))
                .andExpect(jsonPath("$.color").value("#00FF00"))
                .andExpect(jsonPath("$.icon").value("book"));
    }

    @Test
    void shouldSoftDeleteCategory() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        mockMvc.perform(delete("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404ForDeletedCategory() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        mockMvc.perform(delete("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersCategory() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        String otherToken = registerAndLoginOther("other-cat@test.com", "password123");

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldIsolateCategoriesBetweenContexts() throws Exception {
        createCategory("Books", "book", "#FF5733");

        String otherContextId = createContext("Home", ContextTheme.NATURE, "house");

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", otherContextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldChangeSortOrder() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .sortOrder(10)
                .build();

        mockMvc.perform(put("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortOrder").value(10));
    }

    @Test
    void shouldDecrementTaskCountAfterSoftDeleteTask() throws Exception {
        String catId = createCategory("Books", "book", "#FF5733");
        String taskId = createTaskWithCategory("Read Dune", catId);

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.taskCount").value(1));

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories/{id}", catId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.taskCount").value(0));
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

    private String createCategory(String name, String icon, String color) throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name(name)
                .icon(icon)
                .color(color)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createTaskWithCategory(String title, String categoryId) throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title(title)
                .categoryId(java.util.UUID.fromString(categoryId))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/contexts/{contextId}/tasks", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
