package com.gtd.backend.task.service;

import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ContextRepository contextRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasks(UUID contextId, GtdList gtdList, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyContextOwnership(context, userId);

        List<Task> tasks;
        if (gtdList != null) {
            tasks = taskRepository.findByContextIdAndGtdListAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId, gtdList);
        } else {
            tasks = taskRepository.findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(contextId);
        }

        return tasks.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);
        return toResponseWithSubtasks(task);
    }

    @Transactional
    public TaskResponse createTask(UUID contextId, CreateTaskRequest request, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyContextOwnership(context, userId);

        int sortOrder = taskRepository.countByContextIdAndIsDeletedFalse(contextId);

        Task task = Task.builder()
                .context(context)
                .title(request.getTitle().trim())
                .notes(request.getNotes())
                .gtdList(request.getGtdList() != null ? request.getGtdList() : GtdList.INBOX)
                .dueDate(request.getDueDate())
                .categoryId(request.getCategoryId())
                .nestingLevel(1)
                .sortOrder(sortOrder)
                .build();

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse updateTask(UUID taskId, UpdateTaskRequest request, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getNotes() != null) {
            task.setNotes(request.getNotes());
        }
        if (request.getGtdList() != null) {
            task.setGtdList(request.getGtdList());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
        if (request.getCategoryId() != null) {
            task.setCategoryId(request.getCategoryId());
        }
        if (request.getSortOrder() != null) {
            task.setSortOrder(request.getSortOrder());
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        task.setDeleted(true);
        taskRepository.save(task);
    }

    private Context findContextOrThrow(UUID contextId) {
        return contextRepository.findByIdAndIsDeletedFalse(contextId)
                .orElseThrow(() -> new ContextNotFoundException(contextId));
    }

    private Task findTaskOrThrow(UUID taskId) {
        return taskRepository.findByIdAndIsDeletedFalse(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void verifyContextOwnership(Context context, UUID userId) {
        if (!context.getUser().getId().equals(userId)) {
            throw new ContextAccessDeniedException(context.getId());
        }
    }

    private void verifyTaskOwnership(Task task, UUID userId) {
        if (!task.getContext().getUser().getId().equals(userId)) {
            throw new TaskAccessDeniedException(task.getId());
        }
    }

    private TaskResponse toResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .contextId(task.getContext().getId())
                .parentTaskId(task.getParentTask() != null ? task.getParentTask().getId() : null)
                .gtdList(task.getGtdList())
                .categoryId(task.getCategoryId())
                .title(task.getTitle())
                .notes(task.getNotes())
                .dueDate(task.getDueDate())
                .nestingLevel(task.getNestingLevel())
                .sortOrder(task.getSortOrder())
                .completed(task.isCompleted())
                .completedAt(task.getCompletedAt())
                .version(task.getVersion())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskResponse toResponseWithSubtasks(Task task) {
        List<Task> subtasks = taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(task.getId());
        List<TaskResponse> subtaskResponses = subtasks.stream().map(this::toResponse).toList();

        TaskResponse response = toResponse(task);
        response.setSubtasks(subtaskResponses);
        return response;
    }
}
