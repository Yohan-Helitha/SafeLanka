-- Owner: response module (UC03). Shelter headcount updates & field telemetry notifications.

CREATE TABLE shelter_headcount_updates (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shelter_id          UUID         NOT NULL REFERENCES shelters (id),
    district_id         UUID         NOT NULL REFERENCES districts (id),
    reported_occupancy  INTEGER      NOT NULL CHECK (reported_occupancy >= 0),
    previous_occupancy  INTEGER      CHECK (previous_occupancy >= 0),
    reported_by_name    VARCHAR(120) NOT NULL,
    reported_by_role    VARCHAR(60)  NOT NULL DEFAULT 'SHELTER_COORDINATOR',
    message             VARCHAR(255) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPLIED', 'DISMISSED')),
    reported_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ,
    processed_by        UUID         REFERENCES users (id)
);
CREATE INDEX idx_shelter_headcount_district_status ON shelter_headcount_updates (district_id, status);
CREATE INDEX idx_shelter_headcount_shelter ON shelter_headcount_updates (shelter_id);

