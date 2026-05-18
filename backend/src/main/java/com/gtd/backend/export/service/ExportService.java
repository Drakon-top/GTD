package com.gtd.backend.export.service;

import com.gtd.backend.category.model.Category;
import com.gtd.backend.category.repository.CategoryRepository;
import com.gtd.backend.context.exception.ContextAccessDeniedException;
import com.gtd.backend.context.exception.ContextNotFoundException;
import com.gtd.backend.context.model.Context;
import com.gtd.backend.context.repository.ContextRepository;
import com.gtd.backend.export.dto.ExportCategoryData;
import com.gtd.backend.export.dto.ExportContextData;
import com.gtd.backend.export.dto.ExportReminderData;
import com.gtd.backend.export.dto.ExportResponse;
import com.gtd.backend.export.dto.ExportTaskData;
import com.gtd.backend.reminder.model.Reminder;
import com.gtd.backend.reminder.repository.ReminderRepository;
import com.gtd.backend.task.model.Task;
import com.gtd.backend.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final String EXPORT_VERSION = "1.0";

    private final ContextRepository contextRepository;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final ReminderRepository reminderRepository;

    @Transactional(readOnly = true)
    public ExportResponse exportAll(UUID userId) {
        List<Context> contexts = contextRepository.findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(userId);

        List<ExportContextData> contextDataList = contexts.stream()
                .map(this::toExportContext)
                .toList();

        return ExportResponse.builder()
                .exportDate(Instant.now())
                .version(EXPORT_VERSION)
                .contexts(contextDataList)
                .build();
    }

    @Transactional(readOnly = true)
    public ExportResponse exportContext(UUID contextId, UUID userId) {
        Context context = contextRepository.findByIdAndIsDeletedFalse(contextId)
                .orElseThrow(() -> new ContextNotFoundException(contextId));

        if (!context.getUser().getId().equals(userId)) {
            throw new ContextAccessDeniedException(contextId);
        }

        return ExportResponse.builder()
                .exportDate(Instant.now())
                .version(EXPORT_VERSION)
                .contexts(List.of(toExportContext(context)))
                .build();
    }

    private ExportContextData toExportContext(Context context) {
        List<ExportCategoryData> categories = categoryRepository
                .findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(context.getId())
                .stream()
                .map(this::toExportCategory)
                .toList();

        List<ExportTaskData> tasks = taskRepository
                .findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(context.getId())
                .stream()
                .map(this::toExportTask)
                .toList();

        return ExportContextData.builder()
                .id(context.getId())
                .name(context.getName())
                .theme(context.getTheme())
                .icon(context.getIcon())
                .sortOrder(context.getSortOrder())
                .categories(categories)
                .tasks(tasks)
                .build();
    }

    private ExportTaskData toExportTask(Task task) {
        List<ExportTaskData> subtasks = taskRepository
                .findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(task.getId())
                .stream()
                .map(this::toExportTask)
                .toList();

        List<ExportReminderData> reminders = reminderRepository
                .findByTaskIdOrderByRemindAtAsc(task.getId())
                .stream()
                .map(this::toExportReminder)
                .toList();

        return ExportTaskData.builder()
                .id(task.getId())
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
                .subtasks(subtasks)
                .reminders(reminders)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private ExportCategoryData toExportCategory(Category category) {
        return ExportCategoryData.builder()
                .id(category.getId())
                .name(category.getName())
                .icon(category.getIcon())
                .color(category.getColor())
                .sortOrder(category.getSortOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private ExportReminderData toExportReminder(Reminder reminder) {
        return ExportReminderData.builder()
                .id(reminder.getId())
                .remindAt(reminder.getRemindAt())
                .offsetType(reminder.getOffsetType())
                .offsetValue(reminder.getOffsetValue())
                .isSent(reminder.isSent())
                .createdAt(reminder.getCreatedAt())
                .build();
    }
}
