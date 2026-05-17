CREATE TYPE gtd_list AS ENUM (
    'INBOX',
    'NEXT_ACTIONS',
    'PROJECTS',
    'WAITING_FOR',
    'SOMEDAY_MAYBE',
    'REFERENCE',
    'CALENDAR',
    'DONE'
);

CREATE TABLE tasks (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id        UUID            NOT NULL REFERENCES contexts(id) ON DELETE RESTRICT,
    parent_task_id    UUID            REFERENCES tasks(id) ON DELETE RESTRICT,
    gtd_list          gtd_list        NOT NULL DEFAULT 'INBOX',
    category_id       UUID,
    title             VARCHAR(500)    NOT NULL,
    notes             TEXT,
    due_date          TIMESTAMPTZ,
    reminder_settings JSONB,
    recurrence_rule   JSONB,
    nesting_level     INTEGER         NOT NULL DEFAULT 1 CHECK (nesting_level BETWEEN 1 AND 4),
    sort_order        INTEGER         NOT NULL DEFAULT 0,
    is_completed      BOOLEAN         NOT NULL DEFAULT FALSE,
    completed_at      TIMESTAMPTZ,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version           INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_tasks_context_id ON tasks(context_id);
CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id) WHERE parent_task_id IS NOT NULL;
CREATE INDEX idx_tasks_context_gtd_list ON tasks(context_id, gtd_list) WHERE is_deleted = FALSE;
CREATE INDEX idx_tasks_context_not_deleted ON tasks(context_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_tasks_due_date ON tasks(due_date) WHERE due_date IS NOT NULL AND is_deleted = FALSE AND is_completed = FALSE;
