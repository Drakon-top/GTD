package com.gtd.backend.context.exception;

import java.util.UUID;

public class ContextAccessDeniedException extends RuntimeException {

    public ContextAccessDeniedException(UUID contextId) {
        super("Access denied to context: " + contextId);
    }
}
