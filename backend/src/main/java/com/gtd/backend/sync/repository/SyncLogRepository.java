package com.gtd.backend.sync.repository;

import com.gtd.backend.sync.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SyncLogRepository extends JpaRepository<SyncLog, UUID> {

    @Query("SELECT s FROM SyncLog s WHERE s.user.id = :userId AND s.createdAt > :since ORDER BY s.createdAt ASC")
    List<SyncLog> findByUserIdAndCreatedAtAfter(@Param("userId") UUID userId, @Param("since") Instant since);

    List<SyncLog> findByEntityIdOrderByCreatedAtDesc(UUID entityId);
}
