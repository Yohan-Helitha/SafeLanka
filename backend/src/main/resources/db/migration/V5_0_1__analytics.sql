-- Owner: analytics module (UC04). Reads every other table read-only; writes only this one.

CREATE TABLE disaster_reports (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id              UUID        NOT NULL REFERENCES disaster_events (id),
    filters               JSONB       NOT NULL DEFAULT '{}'::jsonb,   -- {districtIds, from, to}
    sections              JSONB       NOT NULL,                        -- the four section payloads
    unavailable_sections  JSONB       NOT NULL DEFAULT '[]'::jsonb,   -- [{key, reason}]
    generated_by          UUID        NOT NULL REFERENCES users (id),
    generated_at          TIMESTAMPTZ NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_disaster_reports_event ON disaster_reports (event_id, generated_at DESC);
