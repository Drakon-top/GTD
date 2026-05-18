package com.gtd.backend.context.exception;

import java.util.UUID;

public class ContextNotFoundException extends RuntimeException {

    public ContextNotFoundException(UUID id) {
        super("Context not found: " + id);
    }
}
