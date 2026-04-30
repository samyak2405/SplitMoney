CREATE TABLE email_template (
    id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL UNIQUE,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    is_html BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
);

INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000000101',
        'REGISTRATION_OTP',
        'Your Splitmoney OTP',
        'Hello,\n\nYour registration OTP is {{otp}}.\nThis OTP expires in {{expiryMinutes}} minutes.\n\nThanks,\nSplitmoney Team',
        FALSE,
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000102',
        'EMAIL_VERIFICATION',
        'Verify your email address',
        'Hello,\n\nPlease verify your email by clicking this link:\n{{verificationLink}}\n\nIf you did not request this, you can ignore this email.\n\nThanks,\nSplitmoney Team',
        FALSE,
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000103',
        'PASSWORD_RESET',
        'Reset your password',
        'Hello,\n\nReset your password by clicking this link:\n{{resetLink}}\n\nIf this was not you, please ignore this email.\n\nThanks,\nSplitmoney Team',
        FALSE,
        TRUE,
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000104',
        'GENERIC',
        'Notification from Splitmoney',
        '{{message}}',
        FALSE,
        TRUE,
        NOW()
    );
