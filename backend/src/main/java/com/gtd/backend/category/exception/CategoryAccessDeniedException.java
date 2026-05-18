package com.gtd.backend.category.exception;

import java.util.UUID;

public class CategoryAccessDeniedException extends RuntimeException {

    public CategoryAccessDeniedException(UUID id) {
        super("Access denied to category: " + id);
    }
}
