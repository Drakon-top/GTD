package com.gtd.backend.context.exception;

public class ContextLimitExceededException extends RuntimeException {

    private static final int MAX_CONTEXTS = 5;

    public ContextLimitExceededException() {
        super("Cannot create more than " + MAX_CONTEXTS + " contexts");
    }
}
