# Splitwise Microservice - API Contracts (v1)

## 1) Conventions

- Base path: `/api/v1`
- Content type: `application/json`
- Time format: ISO-8601 UTC string (example: `2026-02-12T09:30:00Z`)
- Money precision: decimal string with up to 2 fractional digits (example: `"1250.50"`)
- Currency: ISO-4217 uppercase (example: `INR`, `USD`)
- Authentication header (assumed): `Authorization: Bearer <token>`
- Pagination style: cursor-based (`limit`, `next_cursor`)

### Standard Error Schema

All error responses use this shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "sum of split shares must equal total amount",
    "details": {
      "field": "splits"
    },
    "request_id": "req_01J9R6Q6Q9RQ8W4SZ4"
  }
}
```

Common HTTP status usage:

- `400` bad request / validation failure
- `401` unauthenticated
- `403` forbidden
- `404` entity not found
- `409` conflict
- `422` semantically invalid request
- `429` rate limited
- `500` internal server error

---

## 2) Create User

### Endpoint

`POST /api/v1/users`

### Request Schema

```json
{
  "name": "string, required, 1-120 chars",
  "email": "string, required, unique, valid email",
  "phone": "string, optional, E.164 format",
  "default_currency": "string, optional, ISO-4217, default=INR"
}
```

### Response Schema (201)

```json
{
  "user_id": "usr_01J9R5X8Z7...",
  "name": "Aman Gupta",
  "email": "aman@example.com",
  "phone": "+919999999999",
  "default_currency": "INR",
  "created_at": "2026-02-12T09:30:00Z"
}
```

### ErrorCodes

- `VALIDATION_ERROR` (`400`)
- `EMAIL_ALREADY_EXISTS` (`409`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Optional support via `Idempotency-Key` header.
- If provided, same key + same authenticated principal + same request body within 24 hours returns the original successful response.
- Same key with different body returns `409 IDEMPOTENCY_KEY_REUSED`.

---

## 3) Create Group
- Create a group along with members

### Endpoint

`POST /api/v1/groups`

### Request Schema

```json
{
  "name": "string, required, 1-120 chars",
  "description": "string, optional, max 500 chars",
  "currency": "string, optional, ISO-4217, default from creator profile",
  "created_by_user_id": "string, required",
  "users": [
    {
      "user_id": "string, required"
    }
  ]
}
```

### Response Schema (201)

```json
{
  "group_id": "grp_01J9R5Y2UQ...",
  "name": "Goa Trip",
  "description": "Trip expenses",
  "currency": "INR",
  "created_by_user_id": "usr_01J9R5X8Z7...",
  "created_at": "2026-02-12T09:35:00Z",
  "member_count": 1
}
```

### ErrorCodes

- `VALIDATION_ERROR` (`400`)
- `USER_NOT_FOUND` (`404`)
- `UNAUTHORIZED` (`401`)
- `FORBIDDEN` (`403`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Optional via `Idempotency-Key`.
- Duplicate key with identical payload returns original `201` response.
- Duplicate key with changed payload returns `409 IDEMPOTENCY_KEY_REUSED`.

---

## 4) Add Members to Group

### Endpoint

`POST /api/v1/groups/{group_id}/members`

### Request Schema

```json
{
  "members": [
    {
      "user_id": "string, required"
    }
  ],
  "added_by_user_id": "string, required"
}
```

Constraints:

- `members` size: 1 to 100 per request (batch add)
- Group max members: 1000

### Response Schema (200)

```json
{
  "group_id": "grp_01J9R5Y2UQ...",
  "added_members": [
    {
      "user_id": "usr_01J9R60P3V...",
      "joined_at": "2026-02-12T09:40:00Z"
    }
  ],
  "already_members": [
    "usr_01J9R60P3V..."
  ],
  "member_count": 12
}
```

### ErrorCodes

- `VALIDATION_ERROR` (`400`)
- `GROUP_NOT_FOUND` (`404`)
- `USER_NOT_FOUND` (`404`)
- `GROUP_MEMBER_LIMIT_EXCEEDED` (`409`)
- `FORBIDDEN_GROUP_ACCESS` (`403`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Recommended via `Idempotency-Key`.
- Repeating same key and payload is safe and returns same semantic result.
- The endpoint is also logically idempotent for existing members (duplicates are returned in `already_members`, not errors).

---

## 5) Add Expense (Equal / Exact / Percentage)

### Endpoint

`POST /api/v1/expenses`

### Request Schema

```json
{
  "group_id": "string, required",
  "paid_by_user_id": "string, required",
  "total_amount": "string decimal, required, > 0",
  "currency": "string, required, ISO-4217",
  "description": "string, required, 1-280 chars",
  "expense_date": "string datetime, optional, default=now",
  "split_type": "string, required, one of: EQUAL | EXACT | PERCENTAGE",
  "participants": [
    {
      "user_id": "string, required",
      "exact_amount": "string decimal, required iff split_type=EXACT",
      "percentage": "number, required iff split_type=PERCENTAGE, 0-100"
    }
  ],
  "metadata": {
    "receipt_url": "string, optional"
  }
}
```

Validation rules:

- `participants` must be non-empty and all members of group.
- For `EQUAL`: server computes equal shares; rounding residue applied deterministically to earliest participant IDs.
- For `EXACT`: sum(`exact_amount`) must equal `total_amount`.
- For `PERCENTAGE`: sum(`percentage`) must equal `100`, and derived shares must sum to `total_amount` after rounding strategy.

### Response Schema (201)

```json
{
  "expense_id": "exp_01J9R64J3B...",
  "group_id": "grp_01J9R5Y2UQ...",
  "paid_by_user_id": "usr_01J9R5X8Z7...",
  "total_amount": "1800.00",
  "currency": "INR",
  "description": "Dinner",
  "split_type": "EXACT",
  "splits": [
    {
      "user_id": "usr_01J9R5X8Z7...",
      "share_amount": "600.00"
    },
    {
      "user_id": "usr_01J9R60P3V...",
      "share_amount": "1200.00"
    }
  ],
  "created_at": "2026-02-12T09:45:00Z"
}
```

### ErrorCodes

- `VALIDATION_ERROR` (`400`)
- `GROUP_NOT_FOUND` (`404`)
- `USER_NOT_IN_GROUP` (`403`)
- `INVALID_SPLIT_CONFIGURATION` (`422`)
- `CURRENCY_MISMATCH` (`422`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Required header: `Idempotency-Key`.
- Mandatory for all `POST /expenses` calls to prevent duplicate financial writes.
- Same key + same caller + same payload within 48 hours returns original `201` response.
- Same key with different payload returns `409 IDEMPOTENCY_KEY_REUSED`.
- Missing key returns `400 IDEMPOTENCY_KEY_REQUIRED`.

---

## 6) Get Group Balances

### Endpoint

`GET /api/v1/groups/{group_id}/balances`

### Request Schema

Path params:

```json
{
  "group_id": "string, required"
}
```

Optional query params:

```json
{
  "as_of": "string datetime, optional"
}
```

### Response Schema (200)

```json
{
  "group_id": "grp_01J9R5Y2UQ...",
  "currency": "INR",
  "as_of": "2026-02-12T09:50:00Z",
  "balances": [
    {
      "user_id": "usr_01...",
      "net_balance": "-1200.00"
    },
    {
      "user_id": "usr_02...",
      "net_balance": "1200.00"
    }
  ]
}
```

Meaning:

- Positive `net_balance`: user should receive money.
- Negative `net_balance`: user owes money.

### ErrorCodes

- `GROUP_NOT_FOUND` (`404`)
- `FORBIDDEN_GROUP_ACCESS` (`403`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Not required (`GET` is read-only and idempotent).

---

## 7) Get Simplified Settlements (Minimum Transactions)

### Endpoint

`GET /api/v1/groups/{group_id}/settlements/simplified`

### Request Schema

Path params:

```json
{
  "group_id": "string, required"
}
```

Optional query params:

```json
{
  "as_of": "string datetime, optional",
  "max_transactions": "integer, optional, default=unbounded"
}
```

### Response Schema (200)

```json
{
  "group_id": "grp_01J9R5Y2UQ...",
  "currency": "INR",
  "as_of": "2026-02-12T10:00:00Z",
  "settlements": [
    {
      "from_user_id": "usr_01...",
      "to_user_id": "usr_02...",
      "amount": "1200.00"
    }
  ],
  "total_transactions": 1
}
```

### ErrorCodes

- `GROUP_NOT_FOUND` (`404`)
- `FORBIDDEN_GROUP_ACCESS` (`403`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Not required (`GET` is read-only and idempotent).

---

## 8) View Expense History

### Endpoint

`GET /api/v1/groups/{group_id}/expenses`

### Request Schema

Path params:

```json
{
  "group_id": "string, required"
}
```

Query params:

```json
{
  "limit": "integer, optional, default=50, max=200",
  "next_cursor": "string, optional",
  "from_date": "string datetime, optional",
  "to_date": "string datetime, optional",
  "paid_by_user_id": "string, optional",
  "split_type": "string, optional, EQUAL|EXACT|PERCENTAGE"
}
```

### Response Schema (200)

```json
{
  "group_id": "grp_01J9R5Y2UQ...",
  "items": [
    {
      "expense_id": "exp_01J9R64J3B...",
      "description": "Dinner",
      "paid_by_user_id": "usr_01...",
      "total_amount": "1800.00",
      "currency": "INR",
      "split_type": "EXACT",
      "expense_date": "2026-02-12T09:45:00Z",
      "created_at": "2026-02-12T09:45:10Z"
    }
  ],
  "next_cursor": "eyJvZmZzZXQiOjUwfQ=="
}
```

### ErrorCodes

- `GROUP_NOT_FOUND` (`404`)
- `FORBIDDEN_GROUP_ACCESS` (`403`)
- `INVALID_CURSOR` (`400`)
- `UNAUTHORIZED` (`401`)
- `RATE_LIMITED` (`429`)
- `INTERNAL_ERROR` (`500`)

### Idempotency Rules

- Not required (`GET` is read-only and idempotent).

---

## 9) Error Code Catalog

| Code | HTTP | Meaning |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | Request field validation failed |
| `UNAUTHORIZED` | 401 | Missing/invalid auth token |
| `FORBIDDEN` | 403 | Caller lacks permission |
| `FORBIDDEN_GROUP_ACCESS` | 403 | Caller not allowed to access group data |
| `USER_NOT_FOUND` | 404 | User does not exist |
| `GROUP_NOT_FOUND` | 404 | Group does not exist |
| `EMAIL_ALREADY_EXISTS` | 409 | Email already registered |
| `GROUP_MEMBER_LIMIT_EXCEEDED` | 409 | Group size exceeds max members |
| `IDEMPOTENCY_KEY_REUSED` | 409 | Same key used with different payload |
| `INVALID_SPLIT_CONFIGURATION` | 422 | Split math/rules invalid |
| `CURRENCY_MISMATCH` | 422 | Expense currency violates group rules |
| `INVALID_CURSOR` | 400 | Cursor malformed/expired |
| `RATE_LIMITED` | 429 | Throttling limit exceeded |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

---

## 10) Idempotency Header Contract

Header:

`Idempotency-Key: <opaque-string-up-to-128-chars>`

Server behavior:

- Scope uniqueness by `(caller_identity, endpoint, idempotency_key)`.
- Store request hash and response for key TTL window.
- Return same status and body for replayed identical request.
- Reject hash mismatch with `409 IDEMPOTENCY_KEY_REUSED`.

Recommended client behavior:

- Use UUIDv4 for each mutating operation attempt.
- Reuse the same key during network retry of the same logical operation.
- Never reuse keys across different business actions.
