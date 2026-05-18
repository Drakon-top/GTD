package com.gtd.backend.config;

import com.gtd.backend.auth.repository.RefreshTokenRepository;
import com.gtd.backend.auth.repository.UserRepository;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.context.repository.ContextRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        contextRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        rateLimitingFilter.clearBuckets();
    }

    @Test
    void shouldReturn401_whenAccessingProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/contexts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessToAuthEndpoints_withoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAllowAccessToSwaggerUi_withoutToken() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldIncludeCorsHeaders_whenOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    void shouldAllowAccessToActuatorHealth_withoutToken() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToActuatorInfo_withoutToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectCorsRequest_whenOriginIsNotAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/auth/register")
                        .header("Origin", "http://evil.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
