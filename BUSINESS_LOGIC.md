# Business Logic & Scenario Reference

This document describes the full behaviour of the Studio Student Management system
for every meaningful use-case, including edge cases and what the API returns in each
situation.

---

## Table of Contents

1. [Core Concepts](#1-core-concepts)
2. [Pricing Models](#2-pricing-models)
3. [Session Lifecycle](#3-session-lifecycle)
4. [Cancellation Scenarios](#4-cancellation-scenarios)
5. [Moving a Lesson](#5-moving-a-lesson)
6. [Package Scenarios](#6-package-scenarios)
7. [Payment Operations](#7-payment-operations)
8. [Earnings Calculation](#8-earnings-calculation)
9. [Student Deletion](#9-student-deletion)
10. [Debtor Status Batch](#10-debtor-status-batch)
11. [Currency Conversion](#11-currency-conversion)
12. [Error Reference](#12-error-reference)

---

## 1. Core Concepts

| Entity            | What it represents                                                                     |
|-------------------|----------------------------------------------------------------------------------------|
| `Student`         | A person taking classes. Has a pricing type, currency, and optional price per class.   |
| `WeeklySchedule`  | A recurring day-of-week slot (e.g. every Monday at 10:00). Does not create sessions automatically. |
| `ClassSession`    | A concrete class instance on a specific date. Either generated from a schedule or a one-off. |
| `PackagePurchase` | A prepaid block of N classes. Tracks `classesRemaining` as sessions are consumed.      |
| `Payer`           | A third-party contact (e.g. parent) who pays on behalf of the student.                 |

### Status Enums

**`ClassStatus`** — the status of the class itself:
- `SCHEDULED` — upcoming or in-progress
- `COMPLETED` — class took place
- `CANCELLED` — class did not take place
- `MOVED` — reserved for future use (lesson rescheduled)

**`PaymentStatus`** — how the class was (or will be) paid:
- `UNPAID` — no payment recorded yet
- `PAID` — paid per-class (cash, transfer, etc.)
- `PACKAGE` — deducted from a prepaid package
- `REFUNDED` — reserved for future use

---

## 2. Pricing Models

A student is assigned one of two pricing types at creation time (changeable via update).

### `PER_CLASS`
- Each session is paid individually.
- `priceCharged` is captured from `student.pricePerClass` at session creation time.
- Payment can be overridden at pay-time using `amountOverride`.
- Sessions start as `UNPAID` and are manually marked paid via `POST /api/sessions/{id}/pay`.

### `PACKAGE`
- Student buys a block of N classes upfront.
- `student.pricePerClass` is `null` — the cost is recorded on the `PackagePurchase`.
- When `/pay` is called, the system automatically deducts one class from the oldest active package (**FIFO**).
- `paymentStatus` becomes `PACKAGE` and the session is linked to the specific package via `packagePurchaseId`.

> ⚠️ A student's pricing type can be changed at any time. Existing session records are not retroactively affected — only new payments use the new model.

---

## 3. Session Lifecycle

```
                  ┌──────────────────────────────────┐
                  │           SCHEDULED               │
                  │  status=SCHEDULED, payment=UNPAID │
                  └──────────────┬───────────────────┘
                                 │
              ┌──────────────────┴──────────────────┐
              │                                     │
        /pay endpoint                         /cancel endpoint
              │                                     │
              ▼                                     ▼
  ┌───────────────────────┐           ┌─────────────────────────────┐
  │  status=SCHEDULED     │           │     status=CANCELLED        │
  │  payment=PAID         │           │  payment=UNPAID (or kept)   │
  │  (or PACKAGE)         │           └─────────────────────────────┘
  └───────────────────────┘
```

Sessions are created with `status=SCHEDULED` and `paymentStatus=UNPAID`.  
There is no automatic status transition — status/payment changes are manual.

Supported manual actions:
- `PUT /api/sessions/{id}` — unified partial update (date/time/duration/status/payment toggle/note)
- `POST /api/sessions/{id}/completion?completed=true|false` — sets `status` to `COMPLETED` or `SCHEDULED`
- `POST /api/sessions/{id}/pay`, `/cancel`, `/cancel-payment`, `/move-payment` remain available

---

## 4. Cancellation Scenarios

### 4.1 Cancel a session — keep payment as-is (`keepAsPaid: true`)

**Use case:** Student cancelled last-minute but the teacher still charges for the class.

**Request:**
```json
POST /api/sessions/{id}/cancel
{ "keepAsPaid": true, "note": "Late cancellation — charged anyway" }
```

**Result:**
- `status` → `CANCELLED`
- `paymentStatus` — **unchanged** (stays `PAID`, `PACKAGE`, or `UNPAID`)
- If it was a `PACKAGE` session: the package slot is **NOT** returned
- The session remains linked to its `packagePurchaseId`

---

### 4.2 Cancel a session — release payment (`keepAsPaid: false`)

**Use case:** Student cancelled and the class should not count.

**Request:**
```json
POST /api/sessions/{id}/cancel
{ "keepAsPaid": false, "note": "Student sick" }
```

**Result (PER_CLASS student):**
- `status` → `CANCELLED`
- `paymentStatus` → `UNPAID`
- The money is not automatically transferred — use `/move-payment` to apply it elsewhere

**Result (PACKAGE student):**
- `status` → `CANCELLED`
- `paymentStatus` → `UNPAID`
- `classesRemaining` on the linked package → incremented by 1 (slot returned)
- `packagePurchaseId` on the session → set to `null`

---

### 4.3 Cancel a session that was never paid (`UNPAID`)

**Use case:** The session was scheduled but payment was not recorded before cancellation.

**Request:**
```json
POST /api/sessions/{id}/cancel
{ "keepAsPaid": false }
```

**Result:**
- `status` → `CANCELLED`
- `paymentStatus` stays `UNPAID` (no change — nothing to release)
- No package is affected

---

### 4.4 Attempt to cancel an already-cancelled session

**Result:** `400 Bad Request` — "Session is already cancelled"

---

## 5. Moving a Lesson

"Moving a lesson" means rescheduling a class to a different date/time. The system handles
this through a combination of **cancellation** and **one-off session creation**.

### Scenario: Student wants to move Monday's class to Wednesday

**Step 1 — Cancel the original session (optionally keeping payment):**
```json
POST /api/sessions/{mondaySessionId}/cancel
{ "keepAsPaid": true }
```
> If `keepAsPaid: true`, the payment stays on the cancelled session and must be manually moved.

**Step 2 — Create a one-off session for the new date:**
```json
POST /api/students/{studentId}/sessions
{
  "classDate": "2026-04-16",
  "startTime": "10:00",
  "durationMinutes": 60,
  "note": "Moved from Monday 2026-04-14"
}
```
> One-off sessions are created as `UNPAID`. The new session gets its `priceCharged` from the student's current price.

**Step 3 — Move the payment from the cancelled session to the new one:**
```json
POST /api/sessions/{mondaySessionId}/move-payment
{ "targetSessionId": {wednesdaySessionId} }
```

**Result:**
- Monday session: `paymentStatus` → `UNPAID`
- Wednesday session: `paymentStatus` → `PAID`, `priceCharged` copied from source

> ⚠️ Move-payment only works when the **source** session has `paymentStatus=PAID`.
> If the original session was cancelled with `keepAsPaid: false` first, it becomes `UNPAID`
> and the move cannot happen — mark the new session as paid directly instead.

### Scenario: Move a PACKAGE session

PACKAGE sessions **cannot** use `/move-payment` directly (move-payment only handles `PAID` status).

**Recommended flow:**

1. Cancel the original with `keepAsPaid: false` → slot is returned to the package
2. Create the new one-off session
3. Call `/pay` on the new session → auto-deducts from the package again

This keeps `classesRemaining` accurate throughout.

---

## 6. Package Scenarios

### 6.1 Purchasing a package

```json
POST /api/students/{id}/packages
{
  "totalClasses": 10,
  "amountPaid": 280.00,
  "currency": "EUROS",
  "paymentDate": "2026-04-01",
  "description": "Spring bundle"
}
```

- `classesRemaining` is initialised to `totalClasses`
- The package is immediately **active** (available for session deduction)
- `currency` defaults to the student's own currency if omitted

---

### 6.2 Paying with a package (FIFO deduction)

```json
POST /api/sessions/{id}/pay
{}
```

- The system finds the student's oldest package with `classesRemaining > 0` (by `paymentDate`, ascending)
- `classesRemaining` decremented by 1
- Session `paymentStatus` → `PACKAGE`
- Session `packagePurchaseId` → set to the consumed package's ID

**If no active package exists:** `400 Bad Request` — "No active package with remaining classes found"

---

### 6.3 Student cancels a class from a package — class was NOT yet paid

**Use case:** Student cancels a scheduled session that has `paymentStatus=UNPAID` (package session not yet marked).

**Result:**
- No package is affected — `classesRemaining` is unchanged
- `status` → `CANCELLED`, `paymentStatus` stays `UNPAID`

> The package slot is only returned when a session that is **already** linked to a package (`paymentStatus=PACKAGE`) is cancelled with `keepAsPaid: false`.

---

### 6.4 Student cancels a class from a package — class WAS paid (PACKAGE)

**Use case:** Student cancels a session that was already marked as consumed from a package.

**Request:**
```json
POST /api/sessions/{id}/cancel
{ "keepAsPaid": false }
```

**Result:**
- `classesRemaining` on the linked package → incremented by 1 (slot returned)
- Session `packagePurchaseId` → set to `null`
- `paymentStatus` → `UNPAID`

---

### 6.5 Student cancels but teacher keeps it as paid

**Request:**
```json
POST /api/sessions/{id}/cancel
{ "keepAsPaid": true }
```

**Result:**
- `status` → `CANCELLED`, `paymentStatus` stays `PACKAGE`
- Package slot **NOT returned** — the class counts as consumed

---

### 6.6 Reverting a package payment (cancel-payment)

```json
POST /api/sessions/{id}/cancel-payment
```

- Only works when `paymentStatus=PACKAGE`
- `classesRemaining` on the linked package → incremented by 1
- Session `packagePurchaseId` → `null`
- `paymentStatus` → `UNPAID`
- The session `status` is **not changed** (it remains `SCHEDULED` or `COMPLETED`)

**If session is already UNPAID:** `400 Bad Request` — "Session has no payment to cancel"

---

### 6.7 Package exhausted

When `classesRemaining` reaches 0 the package is **inactive** — it is excluded from the
FIFO deduction query. The student must purchase a new package before the next `/pay` call.

Active packages can be listed via:
```
GET /api/students/{id}/packages/active
```

---

## 7. Payment Operations

### 7.1 Mark a session as paid (PER_CLASS)

```json
POST /api/sessions/{id}/pay
{ "amountOverride": 30.00 }
```

- `amountOverride` is optional — if omitted, `priceCharged` retains the value captured at session creation
- `paymentStatus` → `PAID`

**Guards:**
- Already `PAID` or `PACKAGE` → `400 Bad Request`

---

### 7.2 Cancel a payment (revert to UNPAID)

```json
POST /api/sessions/{id}/cancel-payment
```

| Current `paymentStatus` | Result                                                          |
|-------------------------|-----------------------------------------------------------------|
| `UNPAID`                | `400 Bad Request` — nothing to cancel                          |
| `PAID`                  | → `UNPAID`                                                     |
| `PACKAGE`               | → `UNPAID` + package slot returned (`classesRemaining + 1`)   |

Session `status` is **not** affected.

---

### 7.3 Move a payment between sessions

```json
POST /api/sessions/{sourceId}/move-payment
{ "targetSessionId": 99 }
```

**Guards:**
- Source must be `paymentStatus=PAID` → otherwise `400 Bad Request`
- Target must not be already `PAID` → otherwise `400 Bad Request`

**Result:**
- Source → `paymentStatus=UNPAID`
- Target → `paymentStatus=PAID`, `priceCharged` copied from source

> Move-payment only handles per-class (`PAID`) sessions. For package sessions, use
> cancel-payment + pay on the target.

---

## 8. Earnings Calculation

### Daily earnings (`GET /api/earnings/daily`)

- Returns a **period response** with:
  - `dailyBreakdown` (grouped by date)
  - `totalEarned*`
  - `totalCouldHaveEarnedExcludingCancellations*`
  - `totalCouldHaveEarnedIncludingCancellations*`
- `dailyBreakdown` counts only sessions with `paymentStatus=PAID` (per-class payments)
- Package-covered sessions (`paymentStatus=PACKAGE`) are excluded from daily rows
- Package purchases are included in period totals when `paymentDate` is within `from..to`
- `baseCurrency` converts per-currency totals into one normalised total

Potential totals are computed as:

| Total | Included per-class sessions | Packages |
|-------|-----------------------------|----------|
| `totalCouldHaveEarnedExcludingCancellations*` | `PAID` + `UNPAID`, excluding `status=CANCELLED` | Included by `paymentDate` |
| `totalCouldHaveEarnedIncludingCancellations*` | `PAID` + `UNPAID`, including cancelled sessions | Included by `paymentDate` |

> Weekly earnings are obtained by calling this endpoint with a 7-day range.

### Monthly earnings (`GET /api/earnings/monthly`)

Combines two sources:

| Source                        | Included when                        |
|-------------------------------|--------------------------------------|
| Per-class sessions (`PAID`)   | `classDate` falls within the month   |
| Package purchases             | `paymentDate` falls within the month |

This means:
- A package bought in April counts in April's earnings — even if some of those classes are taken in May.
- Sessions covered by that package in May count **zero** toward May's earnings (they were already counted with the package purchase).

---

## 9. Student Deletion

Deleting a student is a **soft-delete** — data is never removed from the database.

```
DELETE /api/students/{id}
```

**What gets soft-deleted:**
- The student record (`deleted=true`)
- All weekly schedule entries
- All payer records
- All **future** class sessions (where `classDate > today`)

**What is preserved:**
- Past class sessions — these are historical records of lessons that took place
- Package purchases — needed for financial history

Soft-deleted records are excluded from all queries by default but remain in the database
for auditing and financial reporting.

---

## 10. Debtor Status Batch

Each student has a `debtor` flag (`true/false`) recalculated by a batch process.

### 10.1 When a student becomes debtor

A student is marked `debtor=true` when they have at least one session that:
- is not soft-deleted,
- has `paymentStatus=UNPAID`,
- is not `status=CANCELLED`, and
- has already happened in the student's local timezone (`classDate < localDate`, or same date with `startTime <= localTime`).

This applies to both pricing models:
- `PER_CLASS`: unpaid happened class means debt.
- `PACKAGE`: debt exists only when the class happened but no package deduction/payment was recorded yet (`UNPAID`).

### 10.2 When debtor is cleared

When all qualifying unpaid happened sessions are resolved, the next recomputation sets `debtor=false`.

### 10.3 Batch timing

- Scheduled job runs by `debtor.batch.cron` (default hourly).
- Scheduled run applies updates only for students whose local time is `22:00` or later.
- Startup catch-up (`debtor.batch.run-on-startup=true`) runs once after app boot and bypasses the 22:00 gate.

---

## 11. Currency Conversion

Every API response that includes a monetary amount (`priceCharged`, `pricePerClass`, `amountPaid`)
also includes a `convertedPrices` / `convertedAmountPaid` map with the equivalent value in
all three supported currencies (`EUROS`, `DOLLARS`, `RUBLES`).

**How it works:**
1. The source amount + currency are sent to `CurrencyConversionService`
2. Exchange rates are fetched from the [Open Exchange Rate API](https://open.er-api.com) and cached for 60 minutes
3. Each target currency value is calculated and rounded to 2 decimal places

**Fallback behaviour:**

| Situation                        | Result                                                      |
|----------------------------------|-------------------------------------------------------------|
| Cache is fresh                   | Cached rates used — no API call                            |
| Cache expired, API reachable     | Fresh rates fetched and cached                             |
| Cache expired, API unreachable   | Stale cached rates used (with a WARN log)                  |
| No cache at all, API unreachable | `convertedPrices` map is **empty** — no crash, no error    |
| `amount` or `currency` is null   | `convertedPrices` map is **empty**                         |

The `convertedPrices` field being empty or absent is safe — the original currency value
is always present.

---

## 12. Error Reference

| HTTP Status | When it occurs                                                              |
|-------------|-----------------------------------------------------------------------------|
| `400`       | Invalid request body, business rule violated (e.g. pay an already-paid session) |
| `404`       | Student, session, schedule, package, or payer not found (or soft-deleted)   |
| `409`       | Conflict — e.g. adding a weekly schedule on a day the student already has   |
| `500`       | Unexpected server error (logged with full stack trace to error log file)    |

All error responses follow a consistent JSON shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Session is already paid",
  "path": "/api/sessions/5/pay",
  "timestamp": "2026-04-12T10:30:00"
}
```

