-- Owner: warnings module (UC01). Sensors are simulated IoT gauges (assignment FAQ: mock with dummy data).

CREATE TABLE sensors (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(30)      NOT NULL UNIQUE,
    name               VARCHAR(80)      NOT NULL,
    kind               VARCHAR(15)      NOT NULL CHECK (kind IN ('RIVER_GAUGE', 'RAIN_GAUGE')),
    river_basin_id     UUID             NOT NULL REFERENCES river_basins (id),
    district_id        UUID             NOT NULL REFERENCES districts (id),
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    alert_level        NUMERIC(6, 2)    NOT NULL,
    major_flood_level  NUMERIC(6, 2)    NOT NULL,
    unit               VARCHAR(10)      NOT NULL,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_sensor_levels CHECK (major_flood_level > alert_level)
);

CREATE TABLE sensor_readings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sensor_id    UUID          NOT NULL REFERENCES sensors (id),
    value        NUMERIC(6, 2) NOT NULL,
    recorded_at  TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_sensor_readings_sensor_time ON sensor_readings (sensor_id, recorded_at);

CREATE TABLE hazards (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID         REFERENCES disaster_events (id),
    hazard_type_id  UUID         NOT NULL REFERENCES hazard_types (id),
    severity        SMALLINT     NOT NULL CHECK (severity BETWEEN 1 AND 5),
    district_id     UUID         REFERENCES districts (id),
    river_basin_id  UUID         REFERENCES river_basins (id),
    description     VARCHAR(500) NOT NULL,
    source          VARCHAR(10)  NOT NULL CHECK (source IN ('MANUAL', 'SENSOR', 'REPORT')),
    sensor_id       UUID         REFERENCES sensors (id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'UNDER_ASSESSMENT'
                    CHECK (status IN ('UNDER_ASSESSMENT', 'WARNED', 'MONITORING', 'RESOLVED')),
    detected_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_hazard_area   CHECK (district_id IS NOT NULL OR river_basin_id IS NOT NULL),
    CONSTRAINT chk_hazard_sensor CHECK (source <> 'SENSOR' OR sensor_id IS NOT NULL)
);
CREATE INDEX idx_hazards_status_type_district ON hazards (status, hazard_type_id, district_id);

-- Verified reports linked to hazards. Written by the ReportVerifiedEvent listener,
-- so the warnings module never writes the reports module's table.
CREATE TABLE hazard_evidence (
    hazard_id  UUID        NOT NULL REFERENCES hazards (id),
    report_id  UUID        NOT NULL REFERENCES hazard_reports (id),
    linked_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (hazard_id, report_id)
);

CREATE TABLE warnings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hazard_id      UUID          NOT NULL REFERENCES hazards (id),
    event_id       UUID          REFERENCES disaster_events (id),
    level          VARCHAR(10)   NOT NULL CHECK (level IN ('ADVISORY', 'WATCH', 'WARNING', 'EVACUATE')),
    status         VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE'
                   CHECK (status IN ('ACTIVE', 'ESCALATED', 'CANCELLED', 'EXPIRED')),
    target_type    VARCHAR(12)   NOT NULL CHECK (target_type IN ('DISTRICT', 'RIVER_BASIN')),
    title          VARCHAR(80)   NOT NULL,
    message        VARCHAR(1000) NOT NULL,
    sms_text       VARCHAR(160)  NOT NULL,
    instructions   VARCHAR(300)  NOT NULL,
    issued_by      UUID          NOT NULL REFERENCES users (id),
    issued_at      TIMESTAMPTZ   NOT NULL,
    supersedes_id  UUID          REFERENCES warnings (id),    -- escalation chain
    cancelled_at   TIMESTAMPTZ,
    cancel_reason  VARCHAR(300),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_warning_cancel CHECK (status <> 'CANCELLED' OR cancel_reason IS NOT NULL)
);
CREATE INDEX idx_warnings_status ON warnings (status);
CREATE INDEX idx_warnings_hazard ON warnings (hazard_id);
CREATE INDEX idx_warnings_event  ON warnings (event_id, issued_at);

CREATE TABLE warning_target_areas (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warning_id      UUID NOT NULL REFERENCES warnings (id) ON DELETE CASCADE,
    district_id     UUID REFERENCES districts (id),
    river_basin_id  UUID REFERENCES river_basins (id),
    CONSTRAINT chk_target_one_area CHECK (num_nonnulls(district_id, river_basin_id) = 1)
);
CREATE INDEX idx_target_areas_warning ON warning_target_areas (warning_id);

CREATE TABLE warning_reports (
    warning_id  UUID NOT NULL REFERENCES warnings (id),
    report_id   UUID NOT NULL REFERENCES hazard_reports (id),
    PRIMARY KEY (warning_id, report_id)
);

CREATE TABLE notification_deliveries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warning_id      UUID         NOT NULL REFERENCES warnings (id),
    citizen_id      UUID         NOT NULL REFERENCES users (id),
    channel         VARCHAR(10)  NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'AUDIBLE')),
    status          VARCHAR(10)  NOT NULL CHECK (status IN ('QUEUED', 'DELIVERED', 'FAILED')),
    attempted_at    TIMESTAMPTZ  NOT NULL,
    delivered_at    TIMESTAMPTZ,
    failure_reason  VARCHAR(200),
    CONSTRAINT uq_delivery UNIQUE (warning_id, citizen_id, channel),
    CONSTRAINT chk_delivery_failure CHECK (status <> 'FAILED' OR failure_reason IS NOT NULL)
);
CREATE INDEX idx_deliveries_warning_status ON notification_deliveries (warning_id, status);

CREATE TABLE channel_settings (
    channel           VARCHAR(10) PRIMARY KEY CHECK (channel IN ('PUSH', 'SMS', 'AUDIBLE')),
    enabled           BOOLEAN     NOT NULL DEFAULT TRUE,
    simulate_failure  BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO channel_settings (channel) VALUES ('PUSH'), ('SMS'), ('AUDIBLE');
