CREATE TYPE reminder_offset_type AS ENUM ('MINUTES_BEFORE', 'HOURS_BEFORE', 'DAYS_BEFORE', 'EXACT_TIME');

CREATE TABLE reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    remind_at TIMESTAMPTZ NOT NULL,
    offset_type reminder_offset_type,
    offset_value INTEGER,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_reminders_task_id FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

CREATE INDEX idx_reminders_task_id ON reminders(task_id);
CREATE INDEX idx_reminders_pending ON reminders(remind_at) WHERE is_sent = FALSE;
