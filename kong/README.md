# Kong OSS Local Gateway Runbook

This setup runs Kong OSS in Docker and routes all frontend API traffic through `http://localhost:8000`.

## 1) Prerequisites

- `auth-backend` running on `localhost:8080`
- `splitewise-backend` running on `localhost:8081`
- `notification` running on `localhost:8083`
- Docker Desktop running

## 2) Start Kong

```bash
cd "/Users/apple/Documents/Projects/Splitwise APP/kong"
docker compose up -d
```

If this is the first setup (or after `kong.yml` changes), run config import once:

```bash
docker compose --profile init up kong-config
docker compose up -d
```

Kong endpoints:

- Proxy: `http://localhost:8000`
- Admin API (local only): `http://localhost:8001`
- Status/Metrics endpoint: `http://localhost:8100/metrics`
- Kong UI (Konga): `http://localhost:1337`
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3001` (`admin` / `admin`)

## 2.1) Configure Kong UI (first time only)

1. Open `http://localhost:1337`.
2. Create an admin user for Konga UI.
3. Add a new connection:
   - **Name**: `local-kong`
   - **Kong Admin URL**: `http://kong:8001` (inside Docker network)
4. Save and connect.

## 3) Route map

- `/api/auth/*` -> auth service (`8080`)
- `/api/splitwise/*` -> splitwise service (`8081`)
- `/api/v1/expenses/*` -> splitwise service (`8081`)
- `/api/notifications/*` -> notification service (`8083`)

## 3.1) Metrics and observability

- Kong `prometheus` plugin is enabled globally.
- Prometheus scrapes Kong metrics from `kong:8100/metrics`.
- Grafana is pre-wired to Prometheus as default datasource.

### Generate traffic

```bash
curl -i "http://localhost:8000/api/auth/v1/login" \
  -H "Content-Type: application/json" \
  --data '{"requestId":"req-metrics-1","email":"test@example.com","password":"secret123"}'
```

### Observe metrics

Open Prometheus (`http://localhost:9090`) and run:

- `sum by (service,route,code) (rate(kong_http_requests_total[1m]))`
- `histogram_quantile(0.95, sum(rate(kong_kong_latency_ms_bucket[5m])) by (le,service))`
- `histogram_quantile(0.95, sum(rate(kong_upstream_latency_ms_bucket[5m])) by (le,service))`

Open Grafana (`http://localhost:3001`) and create panels with the same queries to visualize per-service request rate, error rate, and latency.

### Prebuilt dashboard

A dashboard is auto-provisioned at startup:

- **Folder**: `Kong`
- **Dashboard**: `Kong Gateway Overview`
- **UID**: `kong-gateway-overview`

If it does not appear after first run, restart Grafana:

```bash
docker compose restart grafana
```

## 4) Security model in this rollout

- Public auth routes (`register/login/otp`) are open.
- Protected routes are guarded by Kong JWT plugin.
- Splitwise + Notification services also validate JWT and derive user identity from token claims.
- Client-supplied `X-User-Id` is no longer trusted for authorization.

## 5) Quick verification (curl)

### Public endpoint works without token

```bash
curl -i "http://localhost:8000/api/auth/v1/login" \
  -H "Content-Type: application/json" \
  --data '{"requestId":"req-1","email":"test@example.com","password":"secret"}'
```

### Protected endpoint fails without token

```bash
curl -i "http://localhost:8000/api/notifications/v1/unread-count"
```

Expected: `401`.

### Protected endpoint with cookie token

After login from frontend, browser gets `access_token` cookie.
Calls to protected endpoints through Kong should succeed with `credentials: include`.

## 6) Security checks

- Token tampering -> `401` at Kong and/or service.
- Expired token -> `401`.
- Forged `X-User-Id` header does not grant cross-user access.
- Cross-user reads are blocked because controllers derive user identity from JWT `sub`.

## 7) Troubleshooting

- `401 Invalid access token`:
  - Ensure issuer is `auth-backend`.
  - Ensure services and Kong use matching public key.
- Route mismatch:
  - Check `kong.yml` paths and restart compose.
- CORS issues:
  - Validate allowed origins in Kong CORS plugin.
- Cookie not sent:
  - Use `credentials: include` from frontend.
  - Confirm cookie domain/path/sameSite values for local dev.
