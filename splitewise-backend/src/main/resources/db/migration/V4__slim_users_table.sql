-- =============================================================================
-- V3: Remove auth-internal columns from splitmoney.users.
-- The users table is now a lean read-model populated exclusively by
-- auth.user.events (user.registered / user.activated) via UserSyncConsumer.
-- Auth concerns (passwords, MFA, lockout) stay in the auth-backend schema.
-- phone is made nullable — some users may not have a mobile number.
-- =============================================================================

ALTER TABLE users
    DROP COLUMN IF EXISTS password_hash,
    DROP COLUMN IF EXISTS password_algo,
    DROP COLUMN IF EXISTS account_expires_at,
    DROP COLUMN IF EXISTS locked_until,
    DROP COLUMN IF EXISTS lock_reason,
    DROP COLUMN IF EXISTS is_mfa_enabled,
    DROP COLUMN IF EXISTS mfa_method,
    DROP COLUMN IF EXISTS authentication_method,
    DROP COLUMN IF EXISTS failed_login_count,
    DROP COLUMN IF EXISTS last_failed_login_at,
    DROP COLUMN IF EXISTS last_login_at,
    DROP COLUMN IF EXISTS password_changed_at,
    DROP COLUMN IF EXISTS password_expires_at,
    DROP COLUMN IF EXISTS must_change_password;

-- phone is optional — not every auth user has a mobile number.
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
