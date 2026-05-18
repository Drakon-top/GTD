package com.gtd.backend.category.service;

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
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ContextRepository contextRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(UUID contextId, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyContextOwnership(context, userId);

        return categoryRepository.findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(contextId)
                .stream()
                .map(this::toResponseWithTaskCount)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID categoryId, UUID userId) {
        Category category = findCategoryOrThrow(categoryId);
        verifyCategoryOwnership(category, userId);
        return toResponseWithTaskCount(category);
    }

    @Transactional
    public CategoryResponse createCategory(UUID contextId, CreateCategoryRequest request, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyContextOwnership(context, userId);

        int sortOrder = categoryRepository.countByContextIdAndIsDeletedFalse(contextId);

        Category category = Category.builder()
                .context(context)
                .name(request.getName().trim())
                .icon(request.getIcon() != null ? request.getIcon().trim() : null)
                .color(request.getColor() != null ? request.getColor().trim().toUpperCase() : null)
                .sortOrder(sortOrder)
                .build();

        Category saved = categoryRepository.save(category);
        return toResponse(saved, 0);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request, UUID userId) {
        Category category = findCategoryOrThrow(categoryId);
        verifyCategoryOwnership(category, userId);

        if (request.getName() != null) {
            category.setName(request.getName().trim());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon().trim());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor().trim().toUpperCase());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        Category saved = categoryRepository.save(category);
        return toResponseWithTaskCount(saved);
    }

    @Transactional
    public void deleteCategory(UUID categoryId, UUID userId) {
        Category category = findCategoryOrThrow(categoryId);
        verifyCategoryOwnership(category, userId);

        category.setDeleted(true);
        categoryRepository.save(category);
    }

    private Context findContextOrThrow(UUID contextId) {
        return contextRepository.findByIdAndIsDeletedFalse(contextId)
                .orElseThrow(() -> new ContextNotFoundException(contextId));
    }

    private Category findCategoryOrThrow(UUID categoryId) {
        return categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private void verifyContextOwnership(Context context, UUID userId) {
        if (!context.getUser().getId().equals(userId)) {
            throw new ContextAccessDeniedException(context.getId());
        }
    }

    private void verifyCategoryOwnership(Category category, UUID userId) {
        if (!category.getContext().getUser().getId().equals(userId)) {
            throw new CategoryAccessDeniedException(category.getId());
        }
    }

    private CategoryResponse toResponseWithTaskCount(Category category) {
        int taskCount = taskRepository.countByCategoryIdAndIsDeletedFalse(category.getId());
        return toResponse(category, taskCount);
    }

    private CategoryResponse toResponse(Category category, long taskCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .contextId(category.getContext().getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .sortOrder(category.getSortOrder())
                .taskCount(taskCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
