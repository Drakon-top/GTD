package com.gtd.backend.task.repository;

import com.gtd.backend.task.model.GtdList;
import com.gtd.backend.task.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByContextIdAndIsDeletedFalseOrderBySortOrderAsc(UUID contextId);

    List<Task> findByContextIdAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(UUID contextId);

    List<Task> findByContextIdAndGtdListAndIsDeletedFalseOrderBySortOrderAsc(UUID contextId, GtdList gtdList);

    List<Task> findByContextIdAndGtdListAndParentTaskIsNullAndIsDeletedFalseOrderBySortOrderAsc(UUID contextId, GtdList gtdList);

    List<Task> findByParentTaskIdAndIsDeletedFalseOrderBySortOrderAsc(UUID parentTaskId);

    Optional<Task> findByIdAndIsDeletedFalse(UUID id);

    List<Task> findByParentTaskIdAndIsDeletedFalse(UUID parentTaskId);

    int countByContextIdAndIsDeletedFalse(UUID contextId);

    int countByContextIdAndGtdListAndIsDeletedFalse(UUID contextId, GtdList gtdList);

    int countByParentTaskIdAndIsDeletedFalse(UUID parentTaskId);

    int countByParentTaskIdAndIsCompletedFalseAndIsDeletedFalse(UUID parentTaskId);

    int countByCategoryIdAndIsDeletedFalse(UUID categoryId);

    @Query("SELECT t.gtdList, COUNT(t) FROM Task t WHERE t.context.id = :contextId AND t.isDeleted = false GROUP BY t.gtdList")
    List<Object[]> countByContextIdGroupedByGtdList(@Param("contextId") UUID contextId);

    @Query("SELECT CAST(t.categoryId AS string), COUNT(t) FROM Task t WHERE t.context.id = :contextId AND t.isDeleted = false AND t.categoryId IS NOT NULL GROUP BY t.categoryId")
    List<Object[]> countByContextIdGroupedByCategory(@Param("contextId") UUID contextId);

    int countByParentTaskIdAndIsCompletedTrueAndIsDeletedFalse(UUID parentTaskId);
}
