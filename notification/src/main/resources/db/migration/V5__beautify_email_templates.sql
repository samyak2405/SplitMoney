-- =============================================================================
-- V5: Replace all plain-text email templates with branded HTML versions.
-- All templates use {{placeholder}} substitution via EmailTemplateService.
-- is_html = true instructs GmailSmtpEmailGateway to send as HTML.
-- =============================================================================

-- ── Shared base (logo header + footer) is inlined into each template ────────
-- Placeholders in use:
--   OTP emails        → {{otp}}, {{expiryMinutes}}, {{email}}
--   Link emails       → {{verificationLink}}, {{resetLink}}
--   Generic           → {{message}}
-- =============================================================================

-- ── 1. REGISTRATION_OTP ─────────────────────────────────────────────────────
UPDATE notificationdb.email_template SET
  subject_template = 'Your Splitmoney verification code',
  is_html          = true,
  updated_at       = NOW(),
  body_template    = $BODY$<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Splitmoney</title></head>
<body style="margin:0;padding:0;background-color:#F0F4F8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F0F4F8;">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

  <!-- Header -->
  <tr><td style="background-color:#0D1117;padding:28px 40px;text-align:center;">
    <span style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">split<span style="color:#12B35E;">money</span></span>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;">
    <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Verify your account</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">Use the code below to complete your Splitmoney registration. It expires in <strong>{{expiryMinutes}} minutes</strong>.</p>

    <!-- OTP Box -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 28px;">
    <tr><td style="background-color:#F0FDF4;border:2px solid #12B35E;border-radius:12px;padding:20px 48px;text-align:center;">
      <span style="font-size:38px;font-weight:800;letter-spacing:14px;color:#0D1117;font-family:'Courier New',Courier,monospace;">{{otp}}</span>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">
      <strong style="color:#0D1117;">Never share this code</strong> with anyone. Splitmoney will never ask for your OTP via phone or email.<br>
      If you did not create an account, you can safely ignore this email.
    </p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$
WHERE event_type = 'REGISTRATION_OTP';


-- ── 2. PASSWORD_RESET_OTP ────────────────────────────────────────────────────
UPDATE notificationdb.email_template SET
  subject_template = 'Reset your Splitmoney password',
  is_html          = true,
  updated_at       = NOW(),
  body_template    = $BODY$<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Splitmoney</title></head>
<body style="margin:0;padding:0;background-color:#F0F4F8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F0F4F8;">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

  <!-- Header -->
  <tr><td style="background-color:#0D1117;padding:28px 40px;text-align:center;">
    <span style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">split<span style="color:#12B35E;">money</span></span>
  </td></tr>

  <!-- Warning banner -->
  <tr><td style="background-color:#FFF7ED;border-bottom:2px solid #F97316;padding:12px 40px;">
    <p style="margin:0;font-size:13px;color:#92400E;font-weight:600;">&#x26A0;&#xFE0F;&nbsp; Security alert — password reset requested</p>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;">
    <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Reset your password</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">We received a request to reset the password for your account. Enter the code below in the app. It expires in <strong>{{expiryMinutes}} minutes</strong>.</p>

    <!-- OTP Box -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 28px;">
    <tr><td style="background-color:#FFF7ED;border:2px solid #F97316;border-radius:12px;padding:20px 48px;text-align:center;">
      <span style="font-size:38px;font-weight:800;letter-spacing:14px;color:#0D1117;font-family:'Courier New',Courier,monospace;">{{otp}}</span>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">
      <strong style="color:#0D1117;">Did not request this?</strong> Your account is still secure. You can safely ignore this email — your password will not be changed unless you complete the reset.<br><br>
      Never share this code with anyone. Splitmoney support will never ask for your OTP.
    </p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated security message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$
WHERE event_type = 'PASSWORD_RESET_OTP';


-- ── 3. EMAIL_VERIFICATION ────────────────────────────────────────────────────
UPDATE notificationdb.email_template SET
  subject_template = 'Verify your Splitmoney email',
  is_html          = true,
  updated_at       = NOW(),
  body_template    = $BODY$<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Splitmoney</title></head>
<body style="margin:0;padding:0;background-color:#F0F4F8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F0F4F8;">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

  <!-- Header -->
  <tr><td style="background-color:#0D1117;padding:28px 40px;text-align:center;">
    <span style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">split<span style="color:#12B35E;">money</span></span>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;text-align:center;">
    <div style="width:64px;height:64px;background-color:#F0FDF4;border-radius:50%;margin:0 auto 20px;display:flex;align-items:center;justify-content:center;font-size:32px;line-height:64px;">&#x2709;&#xFE0F;</div>
    <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Confirm your email</h1>
    <p style="margin:0 0 32px;font-size:15px;line-height:1.6;color:#4B5563;">Click the button below to verify your email address and activate your Splitmoney account. This link will expire in 24 hours.</p>

    <!-- CTA Button -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 32px;">
    <tr><td style="border-radius:10px;background-color:#12B35E;">
      <a href="{{verificationLink}}" style="display:inline-block;padding:14px 36px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;letter-spacing:-0.2px;">Verify my email &rarr;</a>
    </td></tr>
    </table>

    <p style="margin:0 0 12px;font-size:13px;color:#6B7280;">If the button does not work, copy and paste this link into your browser:</p>
    <p style="margin:0;font-size:12px;color:#12B35E;word-break:break-all;">{{verificationLink}}</p>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin:24px 0 20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;text-align:left;">If you did not create a Splitmoney account, you can safely ignore this email.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$
WHERE event_type = 'EMAIL_VERIFICATION';


-- ── 4. PASSWORD_RESET (link-based) ──────────────────────────────────────────
UPDATE notificationdb.email_template SET
  subject_template = 'Reset your Splitmoney password',
  is_html          = true,
  updated_at       = NOW(),
  body_template    = $BODY$<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Splitmoney</title></head>
<body style="margin:0;padding:0;background-color:#F0F4F8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F0F4F8;">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

  <!-- Header -->
  <tr><td style="background-color:#0D1117;padding:28px 40px;text-align:center;">
    <span style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">split<span style="color:#12B35E;">money</span></span>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;text-align:center;">
    <div style="width:64px;height:64px;background-color:#FFF7ED;border-radius:50%;margin:0 auto 20px;font-size:32px;line-height:64px;text-align:center;">&#x1F512;</div>
    <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Reset your password</h1>
    <p style="margin:0 0 32px;font-size:15px;line-height:1.6;color:#4B5563;">We received a request to reset your Splitmoney password. Click the button below to choose a new one. This link expires in <strong>1 hour</strong>.</p>

    <!-- CTA Button -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 32px;">
    <tr><td style="border-radius:10px;background-color:#12B35E;">
      <a href="{{resetLink}}" style="display:inline-block;padding:14px 36px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;letter-spacing:-0.2px;">Reset my password &rarr;</a>
    </td></tr>
    </table>

    <p style="margin:0 0 12px;font-size:13px;color:#6B7280;">If the button does not work, copy and paste this link:</p>
    <p style="margin:0;font-size:12px;color:#12B35E;word-break:break-all;">{{resetLink}}</p>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin:24px 0 20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;text-align:left;">
      <strong style="color:#0D1117;">Didn't request this?</strong> Your account is safe — just ignore this email. Your password will not be changed.
    </p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated security message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$
WHERE event_type = 'PASSWORD_RESET';


-- ── 5. GENERIC ───────────────────────────────────────────────────────────────
UPDATE notificationdb.email_template SET
  subject_template = 'A message from Splitmoney',
  is_html          = true,
  updated_at       = NOW(),
  body_template    = $BODY$<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0"><title>Splitmoney</title></head>
<body style="margin:0;padding:0;background-color:#F0F4F8;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F0F4F8;">
<tr><td align="center" style="padding:40px 16px;">
<table role="presentation" width="100%" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.1);">

  <!-- Header -->
  <tr><td style="background-color:#0D1117;padding:28px 40px;text-align:center;">
    <span style="font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">split<span style="color:#12B35E;">money</span></span>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;">
    <h1 style="margin:0 0 20px;font-size:20px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">You have a new notification</h1>
    <div style="font-size:15px;line-height:1.7;color:#374151;">{{message}}</div>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin:28px 0 0;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; You are receiving this because you have an active Splitmoney account.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$
WHERE event_type = 'GENERIC';
