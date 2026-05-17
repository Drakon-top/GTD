CREATE TYPE context_theme AS ENUM ('MINIMALIST', 'DESIGN', 'FORMAL', 'NATURE', 'DARK');

CREATE TABLE contexts (
    id          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID            NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name        VARCHAR(100)    NOT NULL,
    theme       context_theme   NOT NULL,
    icon        VARCHAR(50),
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_contexts_user_id ON contexts(user_id);
CREATE INDEX idx_contexts_user_id_not_deleted ON contexts(user_id) WHERE is_deleted = FALSE;
