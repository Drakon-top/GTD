CREATE TYPE sync_entity_type AS ENUM ('TASK', 'CONTEXT', 'CATEGORY', 'REMINDER');
CREATE TYPE device_source AS ENUM ('WEB', 'ANDROID', 'IOS', 'API');
CREATE TYPE conflict_status AS ENUM ('NO_CONFLICT', 'RESOLVED_AUTO', 'RESOLVED_NOTIFY', 'UNRESOLVED');

CREATE TABLE sync_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type sync_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    device_source device_source NOT NULL,
    conflict_status conflict_status NOT NULL DEFAULT 'NO_CONFLICT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sync_log_user_id ON sync_log(user_id);
CREATE INDEX idx_sync_log_user_created ON sync_log(user_id, created_at);
CREATE INDEX idx_sync_log_entity ON sync_log(entity_type, entity_id);
