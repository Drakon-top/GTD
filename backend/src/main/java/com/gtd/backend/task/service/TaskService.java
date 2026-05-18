package com.gtd.backend.task.service;

import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.dto.CreateTaskRequest;
import com.gtd.backend.task.dto.TaskCountsResponse;
import com.gtd.backend.task.dto.TaskResponse;
import com.gtd.backend.task.dto.UpdateTaskRequest;
import com.gtd.backend.task.exception.MaxNestingLevelException;
import com.gtd.backend.task.exception.TaskAccessDeniedException;
import com.gtd.backend.task.exception.TaskNotFoundException;
import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ContextRepository contextRepository;
    private final ReminderRepository reminderRepository;

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

        return tasks.stream().map(this::toResponseWithProgress).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);
        return toResponseWithSubtasksAndProgress(task);
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
                .recurrenceRule(request.getRecurrenceRule())
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
        if (request.isRecurrenceRuleProvided()) {
            String rule = request.getRecurrenceRule();
            task.setRecurrenceRule(rule != null && !rule.isBlank() ? rule : null);
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse moveTask(UUID taskId, GtdList targetList, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        task.setGtdList(targetList);
        Task saved = taskRepository.saveAndFlush(task);
        return toResponse(saved);
    }

    @Transactional
    public TaskResponse completeTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        GtdList originalGtdList = task.getGtdList();
        String recurrenceRule = task.getRecurrenceRule();

        task.setCompleted(true);
        task.setCompletedAt(Instant.now());
        task.setGtdList(GtdList.DONE);
        Task saved = taskRepository.saveAndFlush(task);

        TaskResponse response = toResponse(saved);
        int incompleteSubtasks = taskRepository.countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(taskId);
        if (incompleteSubtasks > 0) {
            response.setHasIncompleteSubtasks(true);
        }

        if (recurrenceRule != null && !recurrenceRule.isBlank()) {
            Task nextInstance = createNextRecurringInstance(task, originalGtdList);
            response.setNextInstanceId(nextInstance.getId());
            response.setIsRecurring(true);
        }

        return response;
    }

    private Task createNextRecurringInstance(Task completedTask, GtdList originalGtdList) {
        int sortOrder = taskRepository.countByContextIdAndIsDeletedFalse(completedTask.getContext().getId());

        Task nextInstance = Task.builder()
                .context(completedTask.getContext())
                .parentTask(completedTask.getParentTask())
                .title(completedTask.getTitle())
                .notes(completedTask.getNotes())
                .gtdList(originalGtdList)
                .categoryId(completedTask.getCategoryId())
                .recurrenceRule(completedTask.getRecurrenceRule())
                .nestingLevel(completedTask.getNestingLevel())
                .sortOrder(sortOrder)
                .build();

        Task savedNext = taskRepository.saveAndFlush(nextInstance);

        List<Reminder> reminders = reminderRepository.findByTaskIdOrderByRemindAtAsc(completedTask.getId());
        for (Reminder original : reminders) {
            Reminder copy = Reminder.builder()
                    .task(savedNext)
                    .remindAt(original.getRemindAt())
                    .offsetType(original.getOffsetType())
                    .offsetValue(original.getOffsetValue())
                    .build();
            reminderRepository.save(copy);
        }

        return savedNext;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getSubtasks(UUID parentTaskId, UUID userId) {
        Task parentTask = findTaskOrThrow(parentTaskId);
        verifyTaskOwnership(parentTask, userId);

        List<Task> subtasks = taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(parentTaskId);
        return subtasks.stream().map(this::toResponseWithProgress).toList();
    }

    @Transactional(readOnly = true)
    public TaskCountsResponse getTaskCounts(UUID contextId, UUID userId) {
        Context context = findContextOrThrow(contextId);
        verifyContextOwnership(context, userId);

        Map<String, Integer> byGtdList = new LinkedHashMap<>();
        for (GtdList list : GtdList.values()) {
            byGtdList.put(list.name(), 0);
        }
        for (Object[] row : taskRepository.countByContextIdGroupedByGtdList(contextId)) {
            GtdList list = (GtdList) row[0];
            int count = ((Long) row[1]).intValue();
            byGtdList.put(list.name(), count);
        }

        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (Object[] row : taskRepository.countByContextIdGroupedByCategory(contextId)) {
            String categoryId = (String) row[0];
            int count = ((Long) row[1]).intValue();
            byCategory.put(categoryId, count);
        }

        int total = taskRepository.countByContextIdAndIsDeletedFalse(contextId);

        return TaskCountsResponse.builder()
                .contextId(contextId)
                .byGtdList(byGtdList)
                .byCategory(byCategory)
                .total(total)
                .build();
    }

    @Transactional
    public TaskResponse createSubtask(UUID parentTaskId, CreateTaskRequest request, UUID userId) {
        Task parentTask = findTaskOrThrow(parentTaskId);
        verifyTaskOwnership(parentTask, userId);

        if (parentTask.getNestingLevel() >= 4) {
            throw new MaxNestingLevelException();
        }

        int sortOrder = taskRepository.countByParentTaskIdAndIsDeletedFalse(parentTaskId);

        Task subtask = Task.builder()
                .context(parentTask.getContext())
                .parentTask(parentTask)
                .title(request.getTitle().trim())
                .notes(request.getNotes())
                .gtdList(request.getGtdList() != null ? request.getGtdList() : GtdList.INBOX)
                .dueDate(request.getDueDate())
                .categoryId(request.getCategoryId())
                .recurrenceRule(request.getRecurrenceRule())
                .nestingLevel(parentTask.getNestingLevel() + 1)
                .sortOrder(sortOrder)
                .build();

        Task saved = taskRepository.save(subtask);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTask(UUID taskId, UUID userId) {
        Task task = findTaskOrThrow(taskId);
        verifyTaskOwnership(task, userId);

        task.setDeleted(true);
        taskRepository.save(task);

        cascadeSoftDelete(taskId);
    }

    private void cascadeSoftDelete(UUID parentTaskId) {
        List<Task> children = taskRepository.findByParentTaskIdAndIsDeletedFalse(parentTaskId);
        for (Task child : children) {
            child.setDeleted(true);
            taskRepository.save(child);
            cascadeSoftDelete(child.getId());
        }
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

    Integer computeProgress(UUID taskId) {
        int[] counts = countAllDescendants(taskId);
        int total = counts[0];
        int completed = counts[1];
        if (total == 0) {
            return null;
        }
        return Math.round((float) completed / total * 100);
    }

    private int[] countAllDescendants(UUID parentTaskId) {
        List<Task> children = taskRepository.findByParentTaskIdAndIsDeletedFalse(parentTaskId);
        int total = 0;
        int completed = 0;
        for (Task child : children) {
            total++;
            if (child.isCompleted()) {
                completed++;
            }
            int[] childCounts = countAllDescendants(child.getId());
            total += childCounts[0];
            completed += childCounts[1];
        }
        return new int[]{total, completed};
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
                .recurrenceRule(task.getRecurrenceRule())
                .nestingLevel(task.getNestingLevel())
                .sortOrder(task.getSortOrder())
                .completed(task.isCompleted())
                .completedAt(task.getCompletedAt())
                .version(task.getVersion())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private TaskResponse toResponseWithProgress(Task task) {
        TaskResponse response = toResponse(task);
        if (task.getGtdList() == GtdList.PROJECTS) {
            response.setProgress(computeProgress(task.getId()));
        }
        return response;
    }

    private TaskResponse toResponseWithSubtasksAndProgress(Task task) {
        List<Task> subtasks = taskRepository.findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(task.getId());
        List<TaskResponse> subtaskResponses = subtasks.stream().map(this::toResponseWithProgress).toList();

        TaskResponse response = toResponseWithProgress(task);
        response.setSubtasks(subtaskResponses);
        return response;
    }
}
