-- Owner: response module (UC03).

CREATE TABLE rescue_teams (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(100) NOT NULL,
    organisation_id  UUID         NOT NULL REFERENCES organisations (id),
    district_id      UUID         NOT NULL REFERENCES districts (id),
    team_type        VARCHAR(10)  NOT NULL CHECK (team_type IN ('BOAT', 'MEDICAL', 'SEARCH')),
    capacity         INTEGER      NOT NULL CHECK (capacity > 0),
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE'
                     CHECK (status IN ('AVAILABLE', 'DISPATCHED', 'EN_ROUTE', 'ACTIVE', 'OFFLINE_UNKNOWN')),
    last_status_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_rescue_teams_district_status ON rescue_teams (district_id, status);

ALTER TABLE users
    ADD CONSTRAINT fk_users_rescue_team FOREIGN KEY (rescue_team_id) REFERENCES rescue_teams (id);

CREATE TABLE shelters (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(120)     NOT NULL,
    district_id        UUID             NOT NULL REFERENCES districts (id),
    address            VARCHAR(200)     NOT NULL,
    latitude           DOUBLE PRECISION NOT NULL,
    longitude          DOUBLE PRECISION NOT NULL,
    capacity           INTEGER          NOT NULL CHECK (capacity > 0),
    current_occupancy  INTEGER          NOT NULL DEFAULT 0,
    status             VARCHAR(10)      NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'FULL', 'CLOSED')),
    coordinator_id     UUID             REFERENCES users (id),
    version            BIGINT           NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_shelter_occupancy CHECK (current_occupancy BETWEEN 0 AND capacity)
);
CREATE INDEX idx_shelters_district_status ON shelters (district_id, status);

CREATE TABLE rescue_assignments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id                UUID             NOT NULL REFERENCES disaster_events (id),
    warning_id              UUID             REFERENCES warnings (id),
    team_id                 UUID             REFERENCES rescue_teams (id),
    created_by              UUID             NOT NULL REFERENCES users (id),
    latitude                DOUBLE PRECISION NOT NULL,
    longitude               DOUBLE PRECISION NOT NULL,
    location_text           VARCHAR(200)     NOT NULL,
    task                    VARCHAR(500)     NOT NULL,
    priority                SMALLINT         NOT NULL CHECK (priority BETWEEN 1 AND 3),
    people_estimated        INTEGER          NOT NULL DEFAULT 0 CHECK (people_estimated >= 0),
    destination_shelter_id  UUID             REFERENCES shelters (id),
    status                  VARCHAR(20)      NOT NULL DEFAULT 'UNASSIGNED'
                            CHECK (status IN ('UNASSIGNED', 'PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE',
                                              'COMPLETED', 'CANCELLED')),
    decline_reason          VARCHAR(200),
    assigned_at             TIMESTAMPTZ,
    acknowledged_at         TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    version                 BIGINT           NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ      NOT NULL DEFAULT now(),
    CONSTRAINT chk_assignment_team CHECK (status IN ('UNASSIGNED', 'CANCELLED') OR team_id IS NOT NULL)
);
-- A team can hold only one live assignment at a time.
CREATE UNIQUE INDEX ux_one_live_assignment_per_team ON rescue_assignments (team_id)
    WHERE status IN ('PENDING_ACK', 'ACCEPTED', 'EN_ROUTE', 'ACTIVE');
CREATE INDEX idx_assignments_event_status ON rescue_assignments (event_id, status);

CREATE TABLE team_status_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id           UUID        NOT NULL REFERENCES rescue_teams (id),
    assignment_id     UUID        REFERENCES rescue_assignments (id),
    from_status       VARCHAR(20) NOT NULL,
    to_status         VARCHAR(20) NOT NULL,
    changed_by        UUID        NOT NULL REFERENCES users (id),
    changed_at        TIMESTAMPTZ NOT NULL,            -- device time (may be offline)
    recorded_offline  BOOLEAN     NOT NULL DEFAULT FALSE,
    client_ref        UUID        UNIQUE,              -- offline sync idempotency key
    synced_at         TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_team_status_logs_team ON team_status_logs (team_id, changed_at);

CREATE TABLE occupancy_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shelter_id   UUID        NOT NULL REFERENCES shelters (id),
    event_id     UUID        REFERENCES disaster_events (id),
    occupancy    INTEGER     NOT NULL CHECK (occupancy >= 0),
    delta        INTEGER     NOT NULL,
    recorded_by  UUID        NOT NULL REFERENCES users (id),
    recorded_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_occupancy_logs_shelter_time ON occupancy_logs (shelter_id, recorded_at);

CREATE TABLE relief_stocks (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id             UUID        NOT NULL REFERENCES relief_items (id),
    organisation_id     UUID        NOT NULL REFERENCES organisations (id),
    district_id         UUID        NOT NULL REFERENCES districts (id),
    quantity_available  INTEGER     NOT NULL CHECK (quantity_available >= 0),
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_stock_item_owner_district UNIQUE (item_id, organisation_id, district_id)
);

CREATE TABLE resource_allocations (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_id      UUID        NOT NULL REFERENCES relief_stocks (id),
    shelter_id    UUID        NOT NULL REFERENCES shelters (id),
    event_id      UUID        NOT NULL REFERENCES disaster_events (id),
    quantity      INTEGER     NOT NULL CHECK (quantity > 0),
    status        VARCHAR(25) NOT NULL DEFAULT 'ALLOCATED'
                  CHECK (status IN ('ALLOCATED', 'PARTIALLY_DISTRIBUTED', 'DISTRIBUTED', 'CANCELLED')),
    allocated_by  UUID        NOT NULL REFERENCES users (id),
    allocated_at  TIMESTAMPTZ NOT NULL,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_allocations_shelter ON resource_allocations (shelter_id);
CREATE INDEX idx_allocations_event   ON resource_allocations (event_id);

CREATE TABLE relief_distributions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    allocation_id         UUID        NOT NULL REFERENCES resource_allocations (id),
    quantity_distributed  INTEGER     NOT NULL CHECK (quantity_distributed > 0),
    distributed_by        UUID        NOT NULL REFERENCES users (id),
    distributed_at        TIMESTAMPTZ NOT NULL,
    recorded_offline      BOOLEAN     NOT NULL DEFAULT FALSE,
    client_ref            UUID        UNIQUE,
    synced_at             TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_distributions_allocation ON relief_distributions (allocation_id);

-- Feeds "Recent Disaster Updates & Actions" on the district dashboard.
CREATE TABLE activity_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    district_id  UUID         NOT NULL REFERENCES districts (id),
    event_id     UUID         REFERENCES disaster_events (id),
    type         VARCHAR(15)  NOT NULL CHECK (type IN ('WARNING', 'DISPATCH', 'TEAM_STATUS', 'SHELTER', 'RELIEF')),
    message      VARCHAR(200) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_activity_logs_district_time ON activity_logs (district_id, occurred_at DESC);
