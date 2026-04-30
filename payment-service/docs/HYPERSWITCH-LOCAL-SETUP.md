# HyperSwitch + Payment Service — Local Setup

## 1. Exact URLs to configure

### Webhook URL (HyperSwitch → your payment service)

HyperSwitch runs **inside Docker**. Your payment service runs on the **host**. From inside the container, `localhost` is the container itself, so the webhook URL must point at the host.

Use this value:

```text
http://host.docker.internal:8080/v1/webhooks/hyperswitch
```

- **host.docker.internal** — Resolves to your Mac/Windows host from the HyperSwitch container (Docker Desktop).
- **8080** — Default port of the payment service. Change if you run it on another port (e.g. `server.port=9090` → use `9090`).
- **/v1/webhooks/hyperswitch** — Path of the payment service webhook endpoint.

On **Linux** Docker (without Desktop), `host.docker.internal` may not exist. Then use your machine’s IP (e.g. `192.168.1.10`) or run the payment service in the same Docker network and use the service name and port.

---

### Return URL (browser redirect after payment)

This is where the **user’s browser** is sent after a redirect-based flow (e.g. 3DS, bank redirect). It must be a URL the user can open (e.g. your frontend).

Examples:

- Frontend on port 3000:  
  `http://localhost:3000/payment/return`
- Or simple:  
  `http://localhost:3000/`

You can either:

- Pass **return_url** in the payment request body when creating a payment, or  
- Rely on the **default return URL** configured in HyperSwitch (see below).

---

## 2. Where to set these in HyperSwitch

### Webhook URL and secret (Dashboard)

1. Open **HyperSwitch Control Center**: `http://localhost:9000`
2. Go to **Developer** → **Payment Settings**
3. Select your **business profile**
4. In **Webhook Setup** (or “Webhooks”):
   - **Webhook URL**: `http://host.docker.internal:8080/v1/webhooks/hyperswitch`
   - If there is a **payments_response_hash_key** (or “webhook secret”): copy that value and set it in the payment service (see below). If the dashboard generates one, use that.

If you **don’t see a Webhook / Webhook Setup** section:

- It may be under another tab (e.g. “Developers”, “Integrations”, or inside a specific profile).
- Some builds hide it behind a feature flag (e.g. `dev_webhooks` in `config/dashboard.toml`). In that case you may need to enable it or use the same config file used by the Control Center.

### Return URL (default)

- **Option A — Dashboard:** If the profile or “Payment settings” has a “Default return URL” or “Return URL”, set it to e.g. `http://localhost:3000/` or `http://localhost:3000/payment/return`.
- **Option B — Config file:** In the HyperSwitch repo, the router is started with a config (e.g. `config/docker_compose.toml`). In that file, set:
  - `default_return_url = "http://localhost:3000/"`  
  (or your frontend URL). Then restart the HyperSwitch router container so the change is picked up.

---

## 3. Payment service config (webhook secret)

Put the **same secret** you use in the dashboard (`payments_response_hash_key`) into the payment service so it can verify webhooks.

In `application.yaml` (or via env):

```yaml
app:
  hyperswitch:
    webhook-secret: "<paste the same value as in Dashboard here>"
```

Or with an env var (recommended for real environments):

```bash
export APP_HYPERSWITCH_WEBHOOK_SECRET="<same value as in Dashboard>"
```

**Signature algorithm:** Hyperswitch sends **x-webhook-signature-256** (HMAC-SHA256). The payment service validates that header. Use the same secret in both places.

---

## 4. Quick checklist

| What              | Value                                                                 |
|-------------------|-----------------------------------------------------------------------|
| Webhook URL       | `http://host.docker.internal:8080/v1/webhooks/hyperswitch`           |
| Return URL        | `http://localhost:3000/` or `http://localhost:3000/payment/return`   |
| Webhook secret    | Same as **payments_response_hash_key** in Dashboard → Payment Settings |
| Where to set      | Dashboard: **Developer → Payment Settings** → profile → **Webhook Setup** |

---

## 5. Verifying webhooks

1. Start payment service on the host (e.g. `./mvnw spring-boot:run`), port 8080.
2. Create a test payment that triggers a webhook (e.g. complete a test payment in HyperSwitch).
3. Check payment service logs for the incoming POST to `/v1/webhooks/hyperswitch` and any signature or parsing errors.

If signature verification fails, confirm:

- The secret in `app.hyperswitch.webhook-secret` is exactly the same as in the Dashboard (no extra spaces/newlines).
- Hyperswitch is sending **x-webhook-signature-256** and the payment service is reading that header (see code/docs).
