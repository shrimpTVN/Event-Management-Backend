-- Enable extension if you plan to use UUIDs or hashing in SQL (optional)
-- CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE TABLE IF NOT EXISTS refresh_tokens
(
    -- Modern SQL standard identity column (recommended over BIGSERIAL)
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- SHA-256 hashed token (64 hex characters)
    token      VARCHAR(64) NOT NULL,
    -- Reference to the user entity
    user_id    BIGINT      NOT NULL,
    -- Token lifecycle management
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,

    -- Optional: Enforce foreign key constraint if a 'users' table exists
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- Inline unique constraint for token lookups
    CONSTRAINT uq_refresh_tokens_token UNIQUE (token)
);


ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_role
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

CREATE TRIGGER trg_refresh_token_updated_at
    BEFORE UPDATE
    ON refresh_tokens
    FOR EACH ROW
EXECUTE FUNCTION fn_set_updated_at();

-- Index for revoking all tokens for a specific user (e.g., global logout, password reset)
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens (user_id);
-- Optional: Partial index optimized for queries fetching only valid, non-expired tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active_lookup
    ON refresh_tokens (user_id, expires_at)
    WHERE revoked = FALSE AND is_active = TRUE;