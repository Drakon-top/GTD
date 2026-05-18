CREATE TABLE categories (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    context_id      UUID            NOT NULL REFERENCES contexts(id) ON DELETE RESTRICT,
    name            VARCHAR(100)    NOT NULL,
    icon            VARCHAR(50),
    color           VARCHAR(7),
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_context_id ON categories(context_id);
CREATE INDEX idx_categories_context_not_deleted ON categories(context_id) WHERE is_deleted = FALSE;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_category_id
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL;
