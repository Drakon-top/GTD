package com.gtd.backend.category.repository;

import com.gtd.backend.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(UUID contextId);

    Optional<Category> findByIdAndIsDeletedFalse(UUID id);

    int countByContextIdAndIsDeletedFalse(UUID contextId);
}
