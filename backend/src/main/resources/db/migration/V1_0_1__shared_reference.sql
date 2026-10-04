-- Owner: group. Reference data used by every module.

CREATE TABLE districts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(10)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    province    VARCHAR(60)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE river_basins (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(60)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE district_river_basins (
    district_id     UUID NOT NULL REFERENCES districts (id),
    river_basin_id  UUID NOT NULL REFERENCES river_basins (id),
    PRIMARY KEY (district_id, river_basin_id)
);

CREATE TABLE hazard_types (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(30)  NOT NULL UNIQUE,
    name               VARCHAR(60)  NOT NULL,
    onset_speed        VARCHAR(10)  NOT NULL CHECK (onset_speed IN ('RAPID', 'SLOW')),
    report_categories  TEXT[]       NOT NULL,
    active             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE organisations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(120) NOT NULL,
    type           VARCHAR(20)  NOT NULL
                   CHECK (type IN ('GOVERNMENT', 'ARMED_FORCES', 'POLICE', 'NGO', 'PRIVATE_DONOR')),
    contact_phone  VARCHAR(20),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE relief_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    unit        VARCHAR(20)  NOT NULL,
    category    VARCHAR(20)  NOT NULL
                CHECK (category IN ('FOOD', 'WATER', 'MEDICINE', 'HYGIENE', 'SHELTER_KIT')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role                VARCHAR(30)  NOT NULL
                        CHECK (role IN ('CITIZEN', 'VOLUNTEER', 'DMC_OFFICER', 'DISTRICT_OFFICER',
                                        'RESCUE_MEMBER', 'SHELTER_COORDINATOR')),
    full_name           VARCHAR(120) NOT NULL,
    phone               VARCHAR(20),
    nic                 VARCHAR(12)  UNIQUE,
    home_address        VARCHAR(200),
    district_id         UUID         NOT NULL REFERENCES districts (id),
    river_basin_id      UUID         REFERENCES river_basins (id),
    preferred_language  VARCHAR(2)   NOT NULL DEFAULT 'en' CHECK (preferred_language IN ('en', 'si', 'ta')),
    organisation_id     UUID         REFERENCES organisations (id),
    rescue_team_id      UUID,        -- foreign key added in V4 (response)
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role_district ON users (role, district_id);
CREATE INDEX idx_users_river_basin   ON users (river_basin_id);

CREATE TABLE disaster_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(120) NOT NULL,
    hazard_type_id  UUID         NOT NULL REFERENCES hazard_types (id),
    status          VARCHAR(10)  NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED')),
    started_at      TIMESTAMPTZ  NOT NULL,
    ended_at        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_dates  CHECK (ended_at IS NULL OR ended_at >= started_at),
    CONSTRAINT chk_event_closed CHECK (status = 'ACTIVE' OR ended_at IS NOT NULL)
);

CREATE TABLE event_districts (
    event_id     UUID NOT NULL REFERENCES disaster_events (id),
    district_id  UUID NOT NULL REFERENCES districts (id),
    PRIMARY KEY (event_id, district_id)
);
