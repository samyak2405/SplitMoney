-- =============================================================================
-- Auth Backend V2 — seed auth_parameters
-- These rows are required on startup. Services read them via AuthParameterService
-- (L1 Caffeine → L2 Redis → L3 DB). Add new rows here as features need them.
-- =============================================================================

INSERT INTO auth_parameters (param_id, param_value, is_enabled)
VALUES
    ('IS_MFA_ENABLED', 'true',  true),   -- Set to 'false' to disable MFA globally
    ('MFA_TYPE',       'EMAIL', true)    -- Default MFA channel: EMAIL | SMS | TOTP
ON CONFLICT DO NOTHING;
