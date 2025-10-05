CREATE TABLE IF NOT EXISTS route_definitions (
    id UUID PRIMARY KEY,
    path_pattern TEXT NOT NULL,
    service_uri TEXT NOT NULL,
    predicates JSONB NOT NULL DEFAULT '[]'::jsonb,
    filters JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS route_definition_audit (
    audit_id UUID PRIMARY KEY,
    route_id UUID NOT NULL,
    change_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    changed_by TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INTEGER NOT NULL,
    CONSTRAINT fk_route_definition
      FOREIGN KEY(route_id) REFERENCES route_definitions(id)
);
