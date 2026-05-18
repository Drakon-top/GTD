package com.gtd.backend.task.exception;

public class MaxNestingLevelException extends RuntimeException {

    public MaxNestingLevelException() {
        super("Maximum nesting level (4) exceeded. Cannot create subtask at level 5.");
    }
}
