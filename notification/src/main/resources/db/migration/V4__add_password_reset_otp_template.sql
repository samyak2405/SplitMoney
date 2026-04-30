INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000105',
    'PASSWORD_RESET_OTP',
    'Reset your Splitmoney password',
    'Hello,

You requested a password reset for your Splitmoney account.

Your OTP is: {{otp}}

This OTP expires in {{expiryMinutes}} minutes. Do not share it with anyone.

If you did not request this, please ignore this email — your password will not change.

Thanks,
Splitmoney Team',
    FALSE,
    TRUE,
    NOW()
) ON CONFLICT (event_type) DO NOTHING;
