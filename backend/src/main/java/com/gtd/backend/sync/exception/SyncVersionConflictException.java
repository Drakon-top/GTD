package com.gtd.backend.sync.exception;

import java.util.UUID;

public class SyncVersionConflictException extends RuntimeException {

    public SyncVersionConflictException(UUID entityId, int expectedVersion, int actualVersion) {
        super(String.format("Version conflict for entity %s: expected %d, actual %d",
                entityId, expectedVersion, actualVersion));
    }
}
