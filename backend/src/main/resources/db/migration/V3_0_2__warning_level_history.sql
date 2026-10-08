-- UC01: an escalation now changes the level of the same warning instead of creating a new one.
-- The history of level changes lives in its own table, and every delivery records the level it
-- was sent at, so a warning that is escalated is sent again without breaking the unique rule.

CREATE TABLE warning_level_changes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warning_id  UUID        NOT NULL REFERENCES warnings (id) ON DELETE CASCADE,
    from_level  VARCHAR(10) CHECK (from_level IN ('ADVISORY', 'WATCH', 'WARNING', 'EVACUATE')),
    to_level    VARCHAR(10) NOT NULL CHECK (to_level IN ('ADVISORY', 'WATCH', 'WARNING', 'EVACUATE')),
    changed_by  UUID        NOT NULL REFERENCES users (id),
    changed_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_level_changes_warning ON warning_level_changes (warning_id, changed_at);

-- Every existing warning starts its history with the level it was issued at.
INSERT INTO warning_level_changes (warning_id, from_level, to_level, changed_by, changed_at)
SELECT id, NULL, level, issued_by, issued_at FROM warnings;

ALTER TABLE notification_deliveries ADD COLUMN level VARCHAR(10);
UPDATE notification_deliveries d SET level = w.level FROM warnings w WHERE w.id = d.warning_id;
ALTER TABLE notification_deliveries ALTER COLUMN level SET NOT NULL;
ALTER TABLE notification_deliveries
    ADD CONSTRAINT chk_delivery_level CHECK (level IN ('ADVISORY', 'WATCH', 'WARNING', 'EVACUATE'));

ALTER TABLE notification_deliveries DROP CONSTRAINT uq_delivery;
ALTER TABLE notification_deliveries
    ADD CONSTRAINT uq_delivery UNIQUE (warning_id, citizen_id, channel, level);
