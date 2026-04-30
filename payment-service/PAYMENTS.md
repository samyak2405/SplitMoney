# SplitMoney Payment Service — Design & Implementation Plan

> Complete reference for the payment layer: architecture, gateway choice,
> API surface, state machine, data model, and implementation checklist.

---

## Table of Contents
1. [Why Hyperswitch](#1-why-hyperswitch)
2. [Architecture Overview](#2-architecture-overview)
3. [Payment Lifecycle](#3-payment-lifecycle)
4. [State Machine](#4-state-machine)
5. [Complete API Surface](#5-complete-api-surface)
6. [Database Schema](#6-database-schema)
7. [Kafka Topics](#7-kafka-topics)
8. [Settlement Bridge](#8-settlement-bridge)
9. [Refund Flow](#9-refund-flow)
10. [Implementation Plan](#10-implementation-plan)

---

## 1. Why Hyperswitch

[Hyperswitch](https://hyperswitch.io) is an **open-source payment router** (Apache 2.0) that acts as a unified layer on top of 50+ payment processors (Stripe, Razorpay, PayPal, Braintree, Adyen, etc.). It is self-hostable via Docker.

### What It Gives Us

| Capability | Without Hyperswitch | With Hyperswitch |
|---|---|---|
| Payment processors | Hard-code one (e.g. Stripe) | 50+ via config |
| Checkout UI | Build our own | Hosted page (Hyperswitch Unified Checkout) |
| PCI compliance | Our servers in scope | Hyperswitch handles card data |
| Failover | Manual | Automatic routing to backup processor |
| Retries | Manual | Built-in smart retry logic |
| Reconciliation | Manual | Dashboard + webhook events |
| Refunds | Per-processor API | One unified `/refunds` API |
| International | Complex | Multi-currency, multi-processor |

### How Hyperswitch Fits In

```
User browser
    │
    │ 1. POST /v1/payments (our API)
    ▼
Payment Service ──────────────────────────→ Hyperswitch (self-hosted :8080)
    │          POST /payments               │
    │          ← { checkoutUrl, id }        │ routes to configured processor
    │                                       │ (Stripe / Razorpay / etc.)
    │ 2. redirect user to checkoutUrl       │
    ▼                                       │
Hyperswitch Checkout UI                    │
    │                                       │
    │ 3. user enters card/UPI               │
    │ 4. processor charges card             │
    │ 5. redirect to our returnUrl          ▼
    │                          Hyperswitch fires webhook →
    │                          POST /v1/webhooks/hyperswitch
    ▼
Our returnUrl (/payment/return?paymentId=...)
    → poll status → show success/failure
```

### Running Hyperswitch Locally

```bash
# One-command start (Docker Compose)
git clone https://github.com/juspay/hyperswitch
cd hyperswitch
docker compose up -d

# API available at http://localhost:8080
# Dashboard at http://localhost:9000
```

Configure a processor in the dashboard, copy the API key, set `app.hyperswitch.api-key` in `application.yaml`.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Payment Service (port 8086)                    │
│                                                                       │
│  REST API                State Machine          Hyperswitch Client   │
│  ─────────               ─────────────          ────────────────────│
│  POST /payments          INITIATED               POST /payments      │
│  GET  /payments/{id}     VALIDATING              GET  /payments/{id} │
│  POST /payments/{id}/cancel  PROCESSING          POST /refunds       │
│  POST /payments/{id}/refund  PENDING             POST /payments/{id}/cancel│
│  GET  /payments?userId=  COMPLETED                                    │
│  GET  /payment/return    FAILED                  Webhook Validation  │
│  POST /webhooks/hs       CANCELLED               HMAC-SHA256         │
│                          REFUND_INITIATED                             │
│                          REFUNDED                                     │
│                                                                       │
│  Outbox (PostgreSQL) ──→ OutboxRelayWorker ──→ Kafka                 │
│  Kafka Consumer ←── payment.lifecycle / payment.webhook_received     │
│                                                                       │
│  Settlement Notifier ──→ POST splitwise-backend/internal/settlements  │
└─────────────────────────────────────────────────────────────────────┘
         │                              │
         │ PostgreSQL (paymentdb)       │ Redis (locks + idempotency)
         ▼                              ▼
   payments                       distributed locks
   payment_state_history          idempotency cache
   idempotency_records
   outbox_events
   processed_webhooks
   refunds                        ← NEW
   payment_methods                ← NEW
```

---

## 3. Payment Lifecycle

### Happy Path — Card Payment

```
Client                    Payment Service             Hyperswitch
  │                            │                           │
  │── POST /v1/payments ──────→│                           │
  │   { amount, currency,      │                           │
  │     paymentMethod: CARD,   │                           │
  │     returnUrl }            │                           │
  │                            │── POST /payments ────────→│
  │                            │                           │ creates intent
  │                            │← { id, checkoutUrl } ───│
  │                            │                           │
  │← 201 { checkoutUrl } ─────│                           │
  │                            │                           │
  │── redirect to checkoutUrl──────────────────────────→ │
  │                            │                           │ user enters card
  │                            │                           │ processor charges
  │← redirect to returnUrl ──────────────────────────── │
  │                            │                           │
  │── GET /payment/return?paymentId= ─────────────────────│
  │                            │                           │
  │                            │  (webhook arrives)        │
  │                            │← POST /webhooks/hs ──────│
  │                            │  { status: SUCCEEDED }    │
  │                            │                           │
  │                            │── update DB to COMPLETED  │
  │                            │── publish outbox event    │
  │                            │── notify Splitwise ───────────────────→
  │                            │     balance settled                    │
  │                            │                                        │
  │← 200 { status: COMPLETED }─│                           │
```

### Failure Path

```
Hyperswitch returns FAILED / REQUIRES_ACTION / times out
  → payment transitions to FAILED
  → outbox event published
  → splitwise backend NOT notified (balance remains)
  → user sees failure message on returnUrl
```

---

## 4. State Machine

```
                    ┌──────────┐
                    │ INITIATED│
                    └────┬─────┘
                         │ validate → gateway call
                         ▼
                    ┌──────────┐
                    │VALIDATING│
                    └────┬─────┘
                         │ gateway accepted
                         ▼
                    ┌──────────┐
                    │PROCESSING│◄──── webhook retrigger
                    └────┬─────┘
              ┌──────────┴──────────┐
              │ awaiting user action│ gateway processed
              ▼                     ▼
         ┌─────────┐         ┌──────────┐
         │ PENDING │         │COMPLETED │
         └────┬────┘         └──────────┘
              │ webhook success
              ▼
         ┌──────────┐
         │COMPLETED │
         └──────────┘

From INITIATED only:
  ┌───────────┐
  │ CANCELLED │  (user cancels before checkout, or timeout)
  └───────────┘

From COMPLETED only:
  ┌────────────────────┐     ┌──────────┐
  │ REFUND_INITIATED   │────→│ REFUNDED │
  └────────────────────┘     └──────────┘

From any in-progress state:
  ┌────────┐
  │ FAILED │  (gateway error, webhook failure, timeout)
  └────────┘
```

### Valid Transitions Table

| From | To | Trigger |
|---|---|---|
| INITIATED | VALIDATING | `createPayment()` called |
| VALIDATING | PROCESSING | Gateway accepted request |
| VALIDATING | FAILED | Gateway rejected immediately |
| PROCESSING | PENDING | Gateway returns requires_action |
| PROCESSING | COMPLETED | Gateway returned success |
| PROCESSING | FAILED | Gateway returned failure |
| PENDING | COMPLETED | Webhook `SUCCEEDED` |
| PENDING | FAILED | Webhook `FAILED` |
| INITIATED | CANCELLED | Client cancels before checkout |
| PROCESSING | CANCELLED | Client cancels + gateway cancel succeeds |
| COMPLETED | REFUND_INITIATED | Refund requested |
| REFUND_INITIATED | REFUNDED | Hyperswitch refund webhook |

---

## 5. Complete API Surface

### POST /v1/payments — Initiate Payment
```
Headers: Idempotency-Key: <uuid>

Request:
{
  "payerUserId":    "UUID",
  "payeeUserId":    "UUID",
  "amount":         "150.00",
  "currency":       "INR",
  "paymentMethod":  "CARD" | "UPI",
  "clientRequestId":"<unique string>",
  "returnUrl":      "http://localhost:5173/payment/return"  // optional
}

Response 201:
{
  "paymentId":            "UUID",
  "status":               "PROCESSING",
  "checkoutUrl":          "https://checkout.hyperswitch.io/...",
  "hyperswitchPaymentId": "pay_xxx",
  "amount":               "150.00",
  "currency":             "INR",
  "createdAt":            "2026-04-26T10:00:00Z"
}

Errors:
  400 — validation failure (amount < 1, bad currency, missing fields)
  409 — idempotency conflict (same key, different payload)
```

### GET /v1/payments/{id} — Get Payment Status
```
Response 200: (same shape as above + updatedAt)
Errors: 404 Not Found
```

### GET /v1/payments — List Payments for a User
```
Query params:
  userId=<UUID>     (required)
  status=COMPLETED  (optional filter)
  limit=20          (default 20, max 100)
  after=<cursor>    (cursor-based pagination)

Response 200:
{
  "payments": [ ...PaymentResponse ],
  "pageInfo": {
    "hasNextPage": true,
    "endCursor":   "pay_cursor_abc"
  }
}
```

### POST /v1/payments/{id}/cancel — Cancel Payment
```
Request: (empty body)

Response 200: PaymentResponse with status=CANCELLED
Errors:
  404 — not found
  409 — payment not in a cancellable state (only INITIATED is cancellable without hitting gateway)
```

### POST /v1/payments/{id}/refund — Initiate Refund
```
Request:
{
  "amount":  "50.00",   // optional — full refund if omitted
  "reason":  "DUPLICATE" | "FRAUDULENT" | "REQUESTED_BY_CUSTOMER"
}

Response 201:
{
  "refundId":   "UUID",
  "paymentId":  "UUID",
  "amount":     "50.00",
  "status":     "REFUND_INITIATED",
  "reason":     "REQUESTED_BY_CUSTOMER",
  "createdAt":  "2026-04-26T10:00:00Z"
}

Errors:
  404 — payment not found
  409 — payment not in COMPLETED state
  422 — refund amount exceeds original payment amount
```

### GET /payment/return — Checkout Return Handler (browser redirect)
```
Query params: paymentId=<UUID>

This endpoint is the returnUrl that Hyperswitch redirects to after checkout.
It fetches current status from DB (updated by webhook) and returns a
polling-friendly JSON response. Frontend polls until status is terminal.

Response 200:
{
  "paymentId": "UUID",
  "status":    "COMPLETED" | "FAILED" | "PENDING" | "PROCESSING",
  "message":   "Payment successful" | "Payment failed" | "Processing..."
}
```

### POST /v1/webhooks/hyperswitch — Hyperswitch Webhook
```
Headers:
  x-webhook-signature-256: sha256=<hmac>

Request: Hyperswitch event payload (varies by event type)

Response: 200 OK (always — webhook providers retry on non-2xx)
```

---

## 6. Database Schema

### Existing Tables (keeping as-is)
- `payments` — payment aggregate
- `payment_state_history` — audit trail
- `idempotency_records` — deduplication
- `outbox_events` — reliable event publishing
- `processed_webhooks` — webhook deduplication

### New Tables

#### refunds
```sql
CREATE TABLE refunds (
    id                  UUID         PRIMARY KEY,
    payment_id          UUID         NOT NULL REFERENCES payments(id),
    hyperswitch_refund_id VARCHAR(120) UNIQUE,
    amount              NUMERIC(19,4) NOT NULL,
    currency            VARCHAR(3)   NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'REFUND_INITIATED',
    reason              VARCHAR(50)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_refunds_payment_id ON refunds(payment_id);
```

#### payment_methods (for future saved card/UPI vault)
```sql
CREATE TABLE payment_methods (
    id                  UUID         PRIMARY KEY,
    user_id             UUID         NOT NULL,
    type                VARCHAR(20)  NOT NULL,  -- CARD, UPI
    display_name        VARCHAR(120),           -- "•••• 4242", "alice@upi"
    hyperswitch_pm_id   VARCHAR(120) UNIQUE NOT NULL,
    is_default          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_pm_user_id ON payment_methods(user_id);
```

---

## 7. Kafka Topics

| Topic | Producer | Consumer | Event types |
|---|---|---|---|
| `payment.lifecycle` | OutboxRelayWorker | PaymentLifecycleConsumer | VALIDATING, PROCESSING, PENDING, COMPLETED, FAILED, CANCELLED |
| `payment.webhook_received` | WebhookController | PaymentWebhookConsumer | Raw Hyperswitch webhook |
| `payment.notification.events` | OutboxRelayWorker | (Notification bridge) | PAYMENT_COMPLETED, PAYMENT_FAILED, PAYMENT_CANCELLED |
| `payment.refund.events` | OutboxRelayWorker | (future) | REFUND_INITIATED, REFUNDED |

---

## 8. Settlement Bridge

When a payment reaches COMPLETED, the Splitwise backend must be notified to
mark the group balance as settled. Since both payment-service (Kafka) and
notification-service (RabbitMQ) speak different brokers, the cleanest
approach is a direct HTTP callback from payment-service to splitwise-backend.

```
payment COMPLETED
    ↓
SettlementNotifier.notify(paymentId, payerUserId, payeeUserId, amount)
    ↓
POST http://splitwise-backend:8081/internal/settlements/complete
{
  "paymentId":   "UUID",
  "payerUserId": "UUID",
  "payeeUserId": "UUID",
  "amount":      "150.00",
  "currency":    "INR"
}
    ↓
Splitwise backend:
  - marks relevant balance rows as settled
  - publishes PAYMENT_RECEIVED notification event to RabbitMQ
  - notification service picks it up → emails both parties
```

This avoids adding RabbitMQ as a dependency to the payment service and
keeps the settlement logic in the splitwise backend where balances live.

---

## 9. Refund Flow

```
Client → POST /v1/payments/{id}/refund
    ↓
RefundService:
  1. Load payment, assert status == COMPLETED
  2. Assert refund amount ≤ original amount
  3. Create refund record in DB (status=REFUND_INITIATED)
  4. POST {hyperswitch}/refunds
     {
       "payment_id": hyperswitchPaymentId,
       "amount": refundAmount,
       "reason": reason
     }
  5. Update payment status → REFUND_INITIATED
  6. Enqueue outbox event

Hyperswitch fires webhook when refund settles:
  → WebhookController receives refund event
  → update refund record status → REFUNDED
  → update payment status → REFUNDED
  → notify splitwise backend to reverse balance
```

---

## 10. Implementation Plan

### Phase 1 — Complete Existing Flows ✅ Priority: High
- [x] Payment creation (existing)
- [x] State machine (existing)
- [x] Hyperswitch integration base (existing)
- [x] Outbox + Kafka (existing)
- [ ] **Fix payment cancellation** — call `POST /payments/{id}/cancel` on Hyperswitch
- [ ] **Add return URL handler** — `GET /payment/return` endpoint for post-checkout redirect
- [ ] **Add payment list endpoint** — `GET /v1/payments?userId=` with cursor pagination

### Phase 2 — Refund Support ✅ Priority: High
- [ ] **Flyway migration** — create `refunds` table (V4)
- [ ] **Refund entity + repository**
- [ ] **HyperswitchClient.refund()** — `POST /refunds`
- [ ] **RefundService** — validation, DB write, gateway call
- [ ] **RefundController** — `POST /v1/payments/{id}/refund`
- [ ] **Webhook handler for refund events**
- [ ] **Outbox event for refund status**

### Phase 3 — Settlement Bridge ✅ Priority: High
- [ ] **SettlementNotifier** — HTTP client that calls splitwise-backend
- [ ] **Invoke from PaymentProcessingService** when payment → COMPLETED
- [ ] **Splitwise backend** — `POST /internal/settlements/complete` endpoint
- [ ] **Balance update logic** — mark group balance row as settled
- [ ] **Notification event** — publish PAYMENT_RECEIVED to RabbitMQ from splitwise

### Phase 4 — Payment Methods (Saved Cards) ✅ Priority: Medium
- [ ] **Flyway migration** — create `payment_methods` table (V5)
- [ ] **PaymentMethod entity + repository**
- [ ] **List saved methods** — `GET /v1/payment-methods?userId=`
- [ ] **Delete saved method** — `DELETE /v1/payment-methods/{id}`
- [ ] **Use saved method on payment** — pass `payment_method_id` instead of re-entering card

### Phase 5 — Frontend Integration ✅ Priority: Medium
- [ ] **Payment API client** in `frontend-splitmoney/src/api/payment.ts`
- [ ] **Settle-up flow** — call payment API instead of current stub
- [ ] **Checkout redirect** — open `checkoutUrl` in new tab or redirect
- [ ] **Return URL page** — `/payment/return` page that polls status
- [ ] **Payment status page** — show success/failure with group balance update
- [ ] **Payment history** — list on profile/activity page
