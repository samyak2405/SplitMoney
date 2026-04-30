-- =============================================================================
-- V6: Insert branded HTML email templates for Splitwise event notifications.
-- Covers the six event types fired by the Splitwise backend via RabbitMQ.
-- Placeholders resolved by EmailTemplateService using the event payload map.
--
-- Payload keys per event type:
--   EXPENSE_ADDED_AGAINST_USER → description, currency, shareAmount
--   GROUP_MEMBER_ADDED         → groupName, addedBy
--   GROUP_MEMBER_REMOVED       → groupName, removedBy
--   EXPENSE_CREATED            → description, currency, totalAmount
--   SETTLEMENT_DUE             → groupName, amount, currency
--   PAYMENT_RECEIVED           → amount, currency
-- =============================================================================

-- ── 1. EXPENSE_ADDED_AGAINST_USER ───────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000201',
  'EXPENSE_ADDED_AGAINST_USER',
  'New expense: {{description}}',
  $BODY$<!DOCTYPE html>
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
    <div style="width:48px;height:48px;background-color:#F0FDF4;border-radius:12px;margin:0 0 20px;font-size:24px;line-height:48px;text-align:center;">&#x1F4B8;</div>
    <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">You have a new expense</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">A new expense has been added to your group and you owe a share.</p>

    <!-- Expense card -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:28px;border:1px solid #E2E8F0;border-radius:12px;overflow:hidden;">
      <tr><td style="background-color:#F8FAFC;padding:16px 20px;">
        <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#94A3B8;text-transform:uppercase;letter-spacing:0.5px;">Expense</p>
        <p style="margin:0;font-size:17px;font-weight:700;color:#0D1117;">{{description}}</p>
      </td></tr>
      <tr><td style="padding:16px 20px;border-top:1px solid #E2E8F0;">
        <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#94A3B8;text-transform:uppercase;letter-spacing:0.5px;">Your share</p>
        <p style="margin:0;font-size:24px;font-weight:800;color:#12B35E;font-family:'Courier New',Courier,monospace;">{{shareAmount}} <span style="font-size:14px;font-weight:600;color:#6B7280;">{{currency}}</span></p>
      </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">Open the Splitmoney app to view the full expense breakdown and settle your balance.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();


-- ── 2. GROUP_MEMBER_ADDED ────────────────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000202',
  'GROUP_MEMBER_ADDED',
  'You''ve been added to {{groupName}}',
  $BODY$<!DOCTYPE html>
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
    <div style="width:64px;height:64px;background-color:#F0FDF4;border-radius:50%;margin:0 auto 20px;font-size:32px;line-height:64px;text-align:center;">&#x1F465;</div>
    <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Welcome to the group!</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;text-align:left;"><strong style="color:#0D1117;">{{addedBy}}</strong> has added you to the Splitmoney group <strong style="color:#12B35E;">{{groupName}}</strong>. You can now split expenses and track balances with your group members.</p>

    <!-- Group badge -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 28px;">
    <tr><td style="background-color:#F0FDF4;border:2px solid #12B35E;border-radius:10px;padding:12px 32px;text-align:center;">
      <span style="font-size:16px;font-weight:700;color:#0D1117;">{{groupName}}</span>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;text-align:left;">Open the Splitmoney app to view your group and start tracking shared expenses.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();


-- ── 3. GROUP_MEMBER_REMOVED ──────────────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000203',
  'GROUP_MEMBER_REMOVED',
  'You''ve been removed from {{groupName}}',
  $BODY$<!DOCTYPE html>
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
    <p style="margin:0;font-size:13px;color:#92400E;font-weight:600;">&#x1F6AB;&nbsp; Group membership update</p>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;">
    <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">You've been removed from a group</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;"><strong style="color:#0D1117;">{{removedBy}}</strong> has removed you from the Splitmoney group <strong style="color:#0D1117;">{{groupName}}</strong>. You will no longer receive expense notifications for this group.</p>

    <!-- Group badge -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 28px;">
    <tr><td style="background-color:#FFF7ED;border:2px solid #F97316;border-radius:10px;padding:12px 32px;text-align:center;">
      <span style="font-size:16px;font-weight:700;color:#0D1117;">{{groupName}}</span>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">If you believe this was a mistake, please contact a group admin directly.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();


-- ── 4. EXPENSE_CREATED ───────────────────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000204',
  'EXPENSE_CREATED',
  'Expense recorded: {{description}}',
  $BODY$<!DOCTYPE html>
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
    <div style="width:48px;height:48px;background-color:#F0FDF4;border-radius:12px;margin:0 0 20px;font-size:24px;line-height:48px;text-align:center;">&#x2705;</div>
    <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Expense recorded</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">Your expense has been successfully added to your group. The amounts have been split and balances updated.</p>

    <!-- Expense card -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:28px;border:1px solid #E2E8F0;border-radius:12px;overflow:hidden;">
      <tr><td style="background-color:#F8FAFC;padding:16px 20px;">
        <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#94A3B8;text-transform:uppercase;letter-spacing:0.5px;">Description</p>
        <p style="margin:0;font-size:17px;font-weight:700;color:#0D1117;">{{description}}</p>
      </td></tr>
      <tr><td style="padding:16px 20px;border-top:1px solid #E2E8F0;">
        <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#94A3B8;text-transform:uppercase;letter-spacing:0.5px;">Total amount</p>
        <p style="margin:0;font-size:24px;font-weight:800;color:#12B35E;font-family:'Courier New',Courier,monospace;">{{totalAmount}} <span style="font-size:14px;font-weight:600;color:#6B7280;">{{currency}}</span></p>
      </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">Open the Splitmoney app to see the full breakdown and who owes what.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();


-- ── 5. SETTLEMENT_DUE ────────────────────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000205',
  'SETTLEMENT_DUE',
  'You have an outstanding balance in {{groupName}}',
  $BODY$<!DOCTYPE html>
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

  <!-- Reminder banner -->
  <tr><td style="background-color:#FFF7ED;border-bottom:2px solid #F97316;padding:12px 40px;">
    <p style="margin:0;font-size:13px;color:#92400E;font-weight:600;">&#x23F0;&nbsp; Settlement reminder</p>
  </td></tr>

  <!-- Content -->
  <tr><td style="padding:40px 40px 32px;">
    <h1 style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">You have an outstanding balance</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">There is an outstanding balance in the group <strong style="color:#0D1117;">{{groupName}}</strong>. Settle up with your group members to keep things even.</p>

    <!-- Amount card -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:28px;">
    <tr><td style="background-color:#FFF7ED;border:2px solid #F97316;border-radius:12px;padding:20px 24px;text-align:center;">
      <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#92400E;text-transform:uppercase;letter-spacing:0.5px;">Amount due</p>
      <p style="margin:0;font-size:34px;font-weight:800;color:#0D1117;font-family:'Courier New',Courier,monospace;">{{amount}} <span style="font-size:16px;font-weight:600;color:#6B7280;">{{currency}}</span></p>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;">Open the Splitmoney app to settle your balance directly with your group members.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();


-- ── 6. PAYMENT_RECEIVED ──────────────────────────────────────────────────────
INSERT INTO email_template (id, event_type, subject_template, body_template, is_html, active, updated_at)
VALUES (
  '00000000-0000-0000-0000-000000000206',
  'PAYMENT_RECEIVED',
  'You received a payment of {{amount}} {{currency}}',
  $BODY$<!DOCTYPE html>
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
    <div style="width:64px;height:64px;background-color:#F0FDF4;border-radius:50%;margin:0 auto 20px;font-size:32px;line-height:64px;text-align:center;">&#x1F4B0;</div>
    <h1 style="margin:0 0 12px;font-size:22px;font-weight:700;color:#0D1117;letter-spacing:-0.3px;">Payment received!</h1>
    <p style="margin:0 0 28px;font-size:15px;line-height:1.6;color:#4B5563;">You've received a payment from a group member. Your balance has been updated.</p>

    <!-- Amount display -->
    <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 28px;">
    <tr><td style="background-color:#F0FDF4;border:2px solid #12B35E;border-radius:12px;padding:20px 48px;text-align:center;">
      <p style="margin:0 0 4px;font-size:12px;font-weight:600;color:#15803D;text-transform:uppercase;letter-spacing:0.5px;">Amount received</p>
      <p style="margin:0;font-size:34px;font-weight:800;color:#12B35E;font-family:'Courier New',Courier,monospace;">{{amount}} <span style="font-size:16px;font-weight:600;color:#6B7280;">{{currency}}</span></p>
    </td></tr>
    </table>

    <!-- Divider -->
    <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="margin-bottom:20px;">
    <tr><td style="border-top:1px solid #E2E8F0;"></td></tr>
    </table>

    <p style="margin:0;font-size:13px;line-height:1.6;color:#6B7280;text-align:left;">Open the Splitmoney app to view your updated balance and payment history.</p>
  </td></tr>

  <!-- Footer -->
  <tr><td style="background-color:#F8FAFC;border-top:1px solid #E2E8F0;padding:20px 40px;text-align:center;">
    <p style="margin:0;font-size:12px;line-height:1.6;color:#94A3B8;">&copy; 2026 Splitmoney &nbsp;·&nbsp; This is an automated message, please do not reply.</p>
  </td></tr>

</table>
</td></tr>
</table>
</body>
</html>$BODY$,
  TRUE, TRUE, NOW()
) ON CONFLICT (event_type) DO UPDATE SET
  subject_template = EXCLUDED.subject_template,
  body_template    = EXCLUDED.body_template,
  is_html          = EXCLUDED.is_html,
  updated_at       = NOW();
