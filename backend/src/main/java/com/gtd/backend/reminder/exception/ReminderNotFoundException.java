package com.gtd.backend.reminder.exception;

import java.util.UUID;

public class ReminderNotFoundException extends RuntimeException {

    public ReminderNotFoundException(UUID id) {
        super("Reminder not found: " + id);
    }
}
