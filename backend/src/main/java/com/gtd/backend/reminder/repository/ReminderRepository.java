package com.gtd.backend.reminder.repository;

import com.gtd.backend.reminder.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    List<Reminder> findByTaskIdOrderByRemindAtAsc(UUID taskId);

    int countByTaskId(UUID taskId);
}
