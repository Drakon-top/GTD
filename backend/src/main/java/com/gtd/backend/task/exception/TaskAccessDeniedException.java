package com.gtd.backend.task.exception;

import java.util.UUID;

public class TaskAccessDeniedException extends RuntimeException {

    public TaskAccessDeniedException(UUID id) {
        super("Access denied to task: " + id);
    }
}
