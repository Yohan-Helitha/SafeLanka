-- Owner: group (authentication). Credentials, phone verification codes and refresh tokens.
-- Applied out of order on databases that already ran V6 (spring.flyway.out-of-order=true).

ALTER TABLE users
    ADD COLUMN email                  VARCHAR(120),
    ADD COLUMN password_hash          VARCHAR(100),
    ADD COLUMN status                 VARCHAR(25)  NOT NULL DEFAULT 'ACTIVE'
                                      CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'DISABLED')),
    ADD COLUMN phone_verified         BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN failed_login_attempts  INTEGER      NOT NULL DEFAULT 0,
    ADD COLUMN locked_until           TIMESTAMPTZ;

-- Phones are stored in E.164 (+947XXXXXXXX) and emails in lower case.
ALTER TABLE users
    ADD CONSTRAINT uq_users_phone UNIQUE (phone),
    ADD CONSTRAINT uq_users_email UNIQUE (email),
    ADD CONSTRAINT chk_users_email_lower CHECK (email = lower(email));

CREATE TABLE phone_verification_codes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash   VARCHAR(64)  NOT NULL,                  -- SHA-256 of the 6-digit code
    expires_at  TIMESTAMPTZ  NOT NULL,
    attempts    INTEGER      NOT NULL DEFAULT 0,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_phone_codes_user ON phone_verification_codes (user_id, created_at DESC);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,           -- SHA-256 of the cookie value
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

-- Demo accounts: every named seed user can log in with the password Demo@1234 (BCrypt, strength 10).
-- Staff use @dmc.lk addresses; the citizens and the volunteer sign in with their mobile number.
UPDATE users
SET password_hash = '$2a$10$o3fzylSuAioHJSsulhHbZOmQ8PHqSDV3q4m6XVmnp1SR5Wfhh1DGq',
    email = CASE id
        WHEN '00000000-0000-0000-0006-000000000001' THEN 'nimal.perera@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000002' THEN 'kasun.jayawardena@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000003' THEN 'sanduni.wickramasinghe@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000004' THEN 'ruwan.fernando@example.lk'
        WHEN '00000000-0000-0000-0006-000000000005' THEN 'tharindu.silva@example.lk'
        WHEN '00000000-0000-0000-0006-000000000006' THEN 'dilani.gunasekara@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000007' THEN 'asanka.bandara@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000008' THEN 'mahesh.kumara@dmc.lk'
        WHEN '00000000-0000-0000-0006-000000000009' THEN 'priya.shanmugam@example.lk'
    END
WHERE id IN (
    '00000000-0000-0000-0006-000000000001', '00000000-0000-0000-0006-000000000002',
    '00000000-0000-0000-0006-000000000003', '00000000-0000-0000-0006-000000000004',
    '00000000-0000-0000-0006-000000000005', '00000000-0000-0000-0006-000000000006',
    '00000000-0000-0000-0006-000000000007', '00000000-0000-0000-0006-000000000008',
    '00000000-0000-0000-0006-000000000009');
