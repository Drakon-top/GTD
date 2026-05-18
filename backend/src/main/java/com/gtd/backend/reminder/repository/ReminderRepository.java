package com.gtd.backend.reminder.repository;

import com.gtd.backend.reminder.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByTaskIdOrderByRemindAtAsc(UUID taskId);

    int countByTaskId(UUID taskId);

    @Query("SELECT r FROM Reminder r JOIN FETCH r.task t JOIN FETCH t.context c JOIN FETCH c.user " +
            "WHERE r.isSent = false AND r.remindAt <= :now AND t.isDeleted = false")
    List<Reminder> findPendingReminders(@Param("now") Instant now);
}
