package com.gtd.backend.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gtd.backend.auth.exception.GlobalExceptionHandler;
import com.gtd.backend.auth.service.JwtService;
import com.gtd.backend.category.dto.CategoryResponse;
import com.gtd.backend.category.dto.CreateCategoryRequest;
import com.gtd.backend.category.dto.UpdateCategoryRequest;
import com.gtd.backend.category.exception.CategoryAccessDeniedException;
import com.gtd.backend.category.exception.CategoryNotFoundException;
import com.gtd.backend.category.service.CategoryService;
import com.gtd.backend.config.CorsProperties;
import com.gtd.backend.config.JwtAuthenticationFilter;
import com.gtd.backend.config.JwtProperties;
import com.gtd.backend.config.RateLimitProperties;
import com.gtd.backend.config.RateLimitingFilter;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

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
    private final UUID categoryId = UUID.randomUUID();

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
    void shouldReturnCategoriesList() throws Exception {
        CategoryResponse response = buildResponse("Books");
        when(categoryService.getCategories(contextId, userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Books"))
                .andExpect(jsonPath("$[0].taskCount").value(0));
    }

    @Test
    void shouldReturnEmptyCategoriesList() throws Exception {
        when(categoryService.getCategories(contextId, userId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn404WhenContextNotFoundOnList() throws Exception {
        when(categoryService.getCategories(contextId, userId))
                .thenThrow(new ContextNotFoundException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenContextAccessDeniedOnList() throws Exception {
        when(categoryService.getCategories(contextId, userId))
                .thenThrow(new ContextAccessDeniedException(contextId));

        mockMvc.perform(get("/api/v1/contexts/{contextId}/categories", contextId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCreateCategory() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Books")
                .icon("book")
                .color("#FF5733")
                .build();

        CategoryResponse response = buildResponse("Books");
        response.setIcon("book");
        response.setColor("#FF5733");
        when(categoryService.createCategory(eq(contextId), any(CreateCategoryRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.icon").value("book"))
                .andExpect(jsonPath("$.color").value("#FF5733"));
    }

    @Test
    void shouldReturn400WhenNameBlank() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("")
                .icon("book")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn400WhenColorInvalid() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Books")
                .color("invalid")
                .build();

        mockMvc.perform(post("/api/v1/contexts/{contextId}/categories", contextId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        CategoryResponse response = buildResponse("Books");
        response.setTaskCount(5);
        when(categoryService.getCategory(categoryId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books"))
                .andExpect(jsonPath("$.taskCount").value(5));
    }

    @Test
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        when(categoryService.getCategory(categoryId, userId))
                .thenThrow(new CategoryNotFoundException(categoryId));

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn403WhenCategoryAccessDenied() throws Exception {
        when(categoryService.getCategory(categoryId, userId))
                .thenThrow(new CategoryAccessDeniedException(categoryId));

        mockMvc.perform(get("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Movies")
                .color("#3366FF")
                .build();

        CategoryResponse response = buildResponse("Movies");
        response.setColor("#3366FF");
        when(categoryService.updateCategory(eq(categoryId), any(UpdateCategoryRequest.class), eq(userId)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/{id}", categoryId)
                        .with(withUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Movies"))
                .andExpect(jsonPath("$.color").value("#3366FF"));
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        doNothing().when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn404OnDeleteWhenNotFound() throws Exception {
        doThrow(new CategoryNotFoundException(categoryId))
                .when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403OnDeleteWhenNotOwner() throws Exception {
        doThrow(new CategoryAccessDeniedException(categoryId))
                .when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId).with(withUser()))
                .andExpect(status().isForbidden());
    }

    private CategoryResponse buildResponse(String name) {
        return CategoryResponse.builder()
                .id(categoryId)
                .contextId(contextId)
                .name(name)
                .sortOrder(0)
                .taskCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
