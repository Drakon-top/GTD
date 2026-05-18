package com.gtd.backend.reminder.exception;

import java.util.UUID;

public class ReminderAccessDeniedException extends RuntimeException {

    public ReminderAccessDeniedException(UUID id) {
        super("Access denied to reminder: " + id);
    }
}
