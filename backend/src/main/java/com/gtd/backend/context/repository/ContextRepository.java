package com.gtd.backend.context.repository;

import com.gtd.backend.context.model.Context;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContextRepository extends JpaRepository<Context, UUID> {

    List<Context> findByUserIdAndIsDeletedFalseOrderBySortOrderAsc(UUID userId);

    Optional<Context> findByIdAndIsDeletedFalse(UUID id);

    int countByUserIdAndIsDeletedFalse(UUID userId);
}
