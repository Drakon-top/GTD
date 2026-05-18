package com.gtd.backend.category.service;

import com.gtd.backend.auth.model.User;
import com.gtd.backend.category.dto.CategoryResponse;
import com.gtd.backend.category.dto.CreateCategoryRequest;
import com.gtd.backend.category.dto.UpdateCategoryRequest;
import com.gtd.backend.category.exception.CategoryAccessDeniedException;
import com.gtd.backend.category.exception.CategoryNotFoundException;
import com.gtd.backend.category.model.Category;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.model.ContextTheme;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ContextRepository contextRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CategoryService categoryService;

    private final UUID userId = UUID.randomUUID();
    private final UUID contextId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @Test
    void shouldReturnCategoriesForContext() {
        Context context = buildContext(contextId, userId);
        Category category = buildCategory(categoryId, context, "Books");
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of(category));
        when(taskRepository.countByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(3);

        List<CategoryResponse> result = categoryService.getCategories(contextId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Books");
        assertThat(result.get(0).getTaskCount()).isEqualTo(3);
    }

    @Test
    void shouldReturnEmptyListWhenNoCategories() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId))
                .thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getCategories(contextId, userId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowContextNotFoundWhenListingCategories() {
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategories(contextId, userId))
                .isInstanceOf(ContextNotFoundException.class);
    }

    @Test
    void shouldThrowContextAccessDeniedWhenListingCategories() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));

        assertThatThrownBy(() -> categoryService.getCategories(contextId, userId))
                .isInstanceOf(ContextAccessDeniedException.class);
    }

    @Test
    void shouldGetCategoryById() {
        Context context = buildContext(contextId, userId);
        Category category = buildCategory(categoryId, context, "Movies");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.countByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(5);

        CategoryResponse result = categoryService.getCategory(categoryId, userId);

        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getName()).isEqualTo("Movies");
        assertThat(result.getTaskCount()).isEqualTo(5);
    }

    @Test
    void shouldThrowNotFoundWhenCategoryDoesNotExist() {
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategory(categoryId, userId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldThrowAccessDeniedWhenCategoryBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Category category = buildCategory(categoryId, context, "Other");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.getCategory(categoryId, userId))
                .isInstanceOf(CategoryAccessDeniedException.class);
    }

    @Test
    void shouldCreateCategory() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(2);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryId);
            cat.setCreatedAt(Instant.now());
            cat.setUpdatedAt(Instant.now());
            return cat;
        });

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Books")
                .icon("book")
                .color("#FF5733")
                .build();

        CategoryResponse result = categoryService.createCategory(contextId, request, userId);

        assertThat(result.getName()).isEqualTo("Books");
        assertThat(result.getIcon()).isEqualTo("book");
        assertThat(result.getColor()).isEqualTo("#FF5733");
        assertThat(result.getSortOrder()).isEqualTo(2);
        assertThat(result.getTaskCount()).isEqualTo(0);
    }

    @Test
    void shouldTrimNameOnCreate() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryId);
            cat.setCreatedAt(Instant.now());
            cat.setUpdatedAt(Instant.now());
            return cat;
        });

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("  Books  ")
                .build();

        CategoryResponse result = categoryService.createCategory(contextId, request, userId);

        assertThat(result.getName()).isEqualTo("Books");
    }

    @Test
    void shouldCreateCategoryWithNullIconAndColor() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryId);
            cat.setCreatedAt(Instant.now());
            cat.setUpdatedAt(Instant.now());
            return cat;
        });

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Tasks")
                .build();

        CategoryResponse result = categoryService.createCategory(contextId, request, userId);

        assertThat(result.getName()).isEqualTo("Tasks");
        assertThat(result.getIcon()).isNull();
        assertThat(result.getColor()).isNull();
    }

    @Test
    void shouldUppercaseColorOnCreate() {
        Context context = buildContext(contextId, userId);
        when(contextRepository.findByIdAndIsDeletedFalse(contextId)).thenReturn(Optional.of(context));
        when(categoryRepository.countByContextIdAndIsDeletedFalse(contextId)).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category cat = invocation.getArgument(0);
            cat.setId(categoryId);
            cat.setCreatedAt(Instant.now());
            cat.setUpdatedAt(Instant.now());
            return cat;
        });

        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Books")
                .color("#ff5733")
                .build();

        CategoryResponse result = categoryService.createCategory(contextId, request, userId);

        assertThat(result.getColor()).isEqualTo("#FF5733");
    }

    @Test
    void shouldUpdateCategoryFields() {
        Context context = buildContext(contextId, userId);
        Category category = buildCategory(categoryId, context, "Books");
        category.setIcon("book");
        category.setColor("#FF5733");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.countByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(0);

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Movies")
                .color("#3366FF")
                .sortOrder(5)
                .build();

        CategoryResponse result = categoryService.updateCategory(categoryId, request, userId);

        assertThat(result.getName()).isEqualTo("Movies");
        assertThat(result.getColor()).isEqualTo("#3366FF");
        assertThat(result.getSortOrder()).isEqualTo(5);
        assertThat(result.getIcon()).isEqualTo("book");
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Context context = buildContext(contextId, userId);
        Category category = buildCategory(categoryId, context, "Books");
        category.setIcon("book");
        category.setColor("#FF5733");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskRepository.countByCategoryIdAndIsDeletedFalse(categoryId)).thenReturn(0);

        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Movies")
                .build();

        CategoryResponse result = categoryService.updateCategory(categoryId, request, userId);

        assertThat(result.getName()).isEqualTo("Movies");
        assertThat(result.getIcon()).isEqualTo("book");
        assertThat(result.getColor()).isEqualTo("#FF5733");
    }

    @Test
    void shouldSoftDeleteCategory() {
        Context context = buildContext(contextId, userId);
        Category category = buildCategory(categoryId, context, "Books");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.deleteCategory(categoryId, userId);

        assertThat(category.isDeleted()).isTrue();
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowNotFoundOnDeleteWhenCategoryMissing() {
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowAccessDeniedOnDeleteWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        Context context = buildContext(contextId, otherUserId);
        Category category = buildCategory(categoryId, context, "Other");
        when(categoryRepository.findByIdAndIsDeletedFalse(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, userId))
                .isInstanceOf(CategoryAccessDeniedException.class);
        verify(categoryRepository, never()).save(any());
    }

    private Context buildContext(UUID id, UUID ownerId) {
        User user = User.builder().id(ownerId).build();
        return Context.builder()
                .id(id)
                .user(user)
                .name("Work")
                .theme(ContextTheme.FORMAL)
                .icon("briefcase")
                .sortOrder(0)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Category buildCategory(UUID id, Context context, String name) {
        return Category.builder()
                .id(id)
                .context(context)
                .name(name)
                .sortOrder(0)
                .isDeleted(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
