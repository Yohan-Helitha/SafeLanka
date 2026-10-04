-- Owner: reports module (UC02).

CREATE SEQUENCE report_reference_seq START WITH 1 INCREMENT BY 1;   -- RPT-2026-0001, RPT-2026-0002 ...

CREATE TABLE hazard_reports (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_no          VARCHAR(20)  NOT NULL UNIQUE,
    reporter_id           UUID         NOT NULL REFERENCES users (id),
    hazard_type_id        UUID         NOT NULL REFERENCES hazard_types (id),
    category              VARCHAR(40)  NOT NULL,
    description           VARCHAR(500) NOT NULL CHECK (char_length(description) BETWEEN 10 AND 500),
    latitude              DOUBLE PRECISION,
    longitude             DOUBLE PRECISION,
    is_manual_location    BOOLEAN      NOT NULL DEFAULT FALSE,
    manual_location_text  VARCHAR(200),
    district_id           UUID         NOT NULL REFERENCES districts (id),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'NEEDS_MORE_INFO', 'VERIFIED', 'REJECTED')),
    client_ref            UUID         NOT NULL UNIQUE,          -- offline sync idempotency key
    captured_at           TIMESTAMPTZ  NOT NULL,                 -- device time
    synced_at             TIMESTAMPTZ  NOT NULL,                 -- server receipt time
    reviewed_by           UUID         REFERENCES users (id),
    reviewed_at           TIMESTAMPTZ,
    rejection_reason      VARCHAR(30)
                          CHECK (rejection_reason IN ('INSUFFICIENT_EVIDENCE', 'DUPLICATE', 'LOCATION_MISMATCH',
                                                      'NOT_A_HAZARD', 'OTHER')),
    review_comment        VARCHAR(300),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_report_location  CHECK ((latitude IS NOT NULL AND longitude IS NOT NULL)
                                           OR (is_manual_location AND manual_location_text IS NOT NULL)),
    CONSTRAINT chk_report_rejection CHECK (status <> 'REJECTED' OR rejection_reason IS NOT NULL),
    CONSTRAINT chk_report_review    CHECK (status = 'PENDING' OR reviewed_by IS NOT NULL)
);
CREATE INDEX idx_reports_status_captured      ON hazard_reports (status, captured_at);
CREATE INDEX idx_reports_type_district_status ON hazard_reports (hazard_type_id, district_id, status);
CREATE INDEX idx_reports_reporter             ON hazard_reports (reporter_id, captured_at DESC);

CREATE TABLE report_photos (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id   UUID         NOT NULL REFERENCES hazard_reports (id) ON DELETE CASCADE,
    file_path   VARCHAR(255) NOT NULL,
    mime_type   VARCHAR(20)  NOT NULL CHECK (mime_type IN ('image/jpeg', 'image/png')),
    size_bytes  INTEGER      NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_report_photos_report ON report_photos (report_id);
