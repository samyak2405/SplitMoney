# Payment Service Operations Guide

## SLO Targets

- Availability: `99.95%`
- `POST /v1/payments` p95 latency: `< 400ms` (without downstream timeout)
- Webhook ack latency p95: `< 100ms`
- Duplicate charge rate: `0`
- Outbox lag p95: `< 5s`

## Key Metrics

- `http.server.requests` by endpoint/status
- `jvm.threads.*`, `jvm.memory.*`
- `process.cpu.usage`
- `kafka.consumer.records.lag.max`
- custom counters:
  - idempotency replay hits
  - invalid state transitions
  - webhook duplicate drops
  - outbox relay publish failures

## Alert Conditions

- Outbox unpublished rows > 10,000 for 5 minutes
- Payments in `PROCESSING`/`PENDING` > threshold for 10 minutes
- Webhook 5xx ratio > 2% for 5 minutes
- DB pool exhaustion > 80% for 5 minutes

## Incident Runbook

### Stuck Payments
1. Query `authdb.payments` where `status in ('PROCESSING','PENDING')` and `updated_at` stale.
2. Validate scheduler execution logs.
3. Replay status retrieval from HyperSwitch for affected records.
4. Verify terminal transition events emitted in `authdb.outbox_events`.

### Duplicate Requests Spike
1. Inspect idempotency key collisions in `authdb.idempotency_records`.
2. Confirm Redis availability and latency.
3. Validate clients send stable `Idempotency-Key`.

### Webhook Replay Flood
1. Verify dedup table `authdb.processed_webhooks` uniqueness behavior.
2. Check webhook signature validation failures.
3. Ensure webhook consumer lag is not increasing.
