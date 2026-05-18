package com.gtd.backend.context.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.dto.LoginRequest;
import com.gtd.backend.auth.dto.RegisterRequest;
import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.context.dto.CreateContextRequest;
import com.gtd.backend.context.dto.UpdateContextRequest;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.repository.ContextRepository;
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
class ContextIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ContextRepository contextRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();

        registerAndLogin("ctx@test.com", "password123");
    }

    @Test
    void shouldCreateContextAndReturnIt() throws Exception {
        CreateContextRequest request = CreateContextRequest.builder()
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .build();

        mockMvc.perform(post("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.theme").value("FORMAL"))
                .andExpect(jsonPath("$.icon").value("briefcase"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shouldListContexts() throws Exception {
        createContext("Work", ContextTheme.FORMAL, "briefcase");
        createContext("Home", ContextTheme.NATURE, "house");

        mockMvc.perform(get("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Work"))
                .andExpect(jsonPath("$[1].name").value("Home"));
    }

    @Test
    void shouldGetContextById() throws Exception {
        String contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Work"))
                .andExpect(jsonPath("$.id").value(contextId));
    }

    @Test
    void shouldReturn403WhenAccessingOtherUsersContext() throws Exception {
        String contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");

        // Register and login as another user
        String otherToken = registerAndLoginOther("other@test.com", "password123");

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateContext() throws Exception {
        String contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");

        UpdateContextRequest request = UpdateContextRequest.builder()
                .name("Office")
                .theme(ContextTheme.DARK)
                .build();

        mockMvc.perform(put("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office"))
                .andExpect(jsonPath("$.theme").value("DARK"))
                .andExpect(jsonPath("$.icon").value("briefcase"));
    }

    @Test
    void shouldSoftDeleteContext() throws Exception {
        String contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");

        mockMvc.perform(delete("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturn404ForDeletedContext() throws Exception {
        String contextId = createContext("Work", ContextTheme.FORMAL, "briefcase");

        mockMvc.perform(delete("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/contexts/{id}", contextId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenCreating6thContext() throws Exception {
        createContext("One", ContextTheme.MINIMALIST, "1");
        createContext("Two", ContextTheme.DESIGN, "2");
        createContext("Three", ContextTheme.FORMAL, "3");
        createContext("Four", ContextTheme.NATURE, "4");
        createContext("Five", ContextTheme.DARK, "5");

        CreateContextRequest request = CreateContextRequest.builder()
                .name("Six")
                .theme(ContextTheme.MINIMALIST)
                .icon("6")
                .build();

        mockMvc.perform(post("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot create more than 5 contexts"));
    }

    @Test
    void shouldAllowCreate6thAfterDeletingOne() throws Exception {
        createContext("One", ContextTheme.MINIMALIST, "1");
        createContext("Two", ContextTheme.DESIGN, "2");
        createContext("Three", ContextTheme.FORMAL, "3");
        createContext("Four", ContextTheme.NATURE, "4");
        String fifthId = createContext("Five", ContextTheme.DARK, "5");

        mockMvc.perform(delete("/api/v1/contexts/{id}", fifthId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        CreateContextRequest request = CreateContextRequest.builder()
                .name("Six")
                .theme(ContextTheme.MINIMALIST)
                .icon("6")
                .build();

        mockMvc.perform(post("/api/v1/contexts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldIsolateContextsBetweenUsers() throws Exception {
        createContext("Work", ContextTheme.FORMAL, "briefcase");

        String otherToken = registerAndLoginOther("other2@test.com", "password123");

        mockMvc.perform(get("/api/v1/contexts")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
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
}
