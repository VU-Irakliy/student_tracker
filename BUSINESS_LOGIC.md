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
12. [Data Export/Import](#12-data-exportimport)
13. [Error Reference](#13-error-reference)

---

## 1. Core Concepts

| Entity            | What it represents                                                                     |
|-------------------|----------------------------------------------------------------------------------------|
| `Student`         | A person taking classes. Has a pricing type, currency, and optional price per class.   |
| `WeeklySchedule`  | A recurring day-of-week slot (e.g. every Monday at 10:00). Does not create sessions automatically. |
| `ClassSession`    | A concrete class instance on a specific date. Either generated from a schedule or a one-off. |
| `PackagePurchase` | A prepaid block of N classes. Tracks `classesRemaining` as sessions are consumed.      |
| `Payer`           | A third-party contact (e.g. parent) who pays on behalf of the student.                 |

Student availability fields:
- `startDate` — earliest date when sessions can be created for the student.
- `holidayMode`, `holidayFrom`, `holidayTo` — controls temporary pause in attendance.
- `stoppedAttending` — blocks new sessions/schedule edits while keeping student visible in API lists.

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

**`StudentClassType`** — student's learning track:
- `CASUAL`, `EGE`, `OGE`, `IELTS`, `TOFEL`

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

### Pricing Invariants (Validation Rules)

- `PER_CLASS`: `pricePerClass` is required.
- If `pricePerClass` is provided, `currency` is required.
- `PACKAGE`: student-level `pricePerClass` and `currency` must be `null`.
- When switching a student to `PACKAGE`, backend update logic clears stored `pricePerClass` and `currency` on the student profile.

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

Supported paid-on-cancelled combinations:
- `status=CANCELLED` + `paymentStatus=PAID` is valid for **PER_CLASS** students.
- `status=CANCELLED` + `paymentStatus=PACKAGE` is valid for **PACKAGE** students.
- These combinations are intentional (for example, late cancellation that is still charged).

Supported manual actions:
- `PUT /api/sessions/{id}` — unified partial update (date/time/duration/status/payment toggle/note)
- `POST /api/sessions/{id}/completion?completed=true|false` — sets `status` to `COMPLETED` or `SCHEDULED`
- `POST /api/sessions/{id}/pay`, `/cancel`, and `/cancel-payment` remain available

Availability checks before creating/updating class dates:
- reject if `classDate < student.startDate`
- reject if `student.holidayMode = true` and `classDate >= holidayFrom`
- reject if `student.stoppedAttending = true`

Holiday update behavior (`PUT /api/students/{id}`):
- `holidayMode=true` requires `holidayFrom`; existing sessions from that date are auto-cancelled.
- `holidayMode=false` requires `holidayTo`; auto-cancelled sessions on/after return date are restored to `SCHEDULED`.

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
- The payment is not transferred automatically — if needed, mark another session as paid manually

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

> Note: `CANCELLED` sessions are still allowed to remain paid when cancellation is done with
> `keepAsPaid: true` (both `PAID` and `PACKAGE` are supported).

---

### 4.4 Attempt to cancel an already-cancelled session

**Result:** `400 Bad Request` — "Session is already cancelled"

---

## 5. Moving a Lesson

"Moving a lesson" means rescheduling a class to a different date/time. The system handles
this through a combination of **cancellation** and **one-off session creation**.

### Scenario: Student wants to move Monday's class to Wednesday (PER_CLASS)

**Step 1 — Cancel the original session:**
```json
POST /api/sessions/{mondaySessionId}/cancel
{ "keepAsPaid": false }
```

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

**Step 3 — Mark the new session as paid:**
```json
POST /api/sessions/{wednesdaySessionId}/pay
{ "amountOverride": 30.00 }
```
> `amountOverride` is optional. Use it when you need to preserve/override the charge amount explicitly.

**Result:**
- Monday session: `paymentStatus` → `UNPAID`
- Wednesday session: `paymentStatus` → `PAID`

> ⚠️ There is no direct "transfer payment" endpoint. Reassigning payment is a two-step operation:
> cancel/unpay old session + pay the new one.

### Scenario: Move a PACKAGE session

PACKAGE sessions use slot reallocation (not money transfer):

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
- Required fields: `totalClasses`, `amountPaid`, `currency`, `paymentDate`

Validation failures return `400 Bad Request` (for example: missing `currency` or `paymentDate`).

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

### 7.3 Reassigning payment between sessions (manual flow)

There is no dedicated `/move-payment` endpoint.

**Per-class flow:**
1. `POST /api/sessions/{sourceId}/cancel-payment` (source becomes `UNPAID`)
2. `POST /api/sessions/{targetId}/pay` (optionally with `amountOverride`)

**Package flow:**
1. `POST /api/sessions/{sourceId}/cancel-payment` (slot returned)
2. `POST /api/sessions/{targetId}/pay` (slot deducted again by FIFO)

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

## 12. Data Export/Import

### Export (`GET /api/data/export`)

- Returns one compressed `.json.gz` snapshot containing all core tables:
  - students
  - weekly schedules
  - package purchases
  - class sessions
  - payers
- Includes active and soft-deleted rows.
- Uses deterministic ordering by ID for stable snapshots.

### Import (`POST /api/data/import`)

- Replaces current data with the supplied snapshot.
- Executes in one transaction (all-or-nothing).
- Validates references while importing (e.g., session -> student/package/schedule links).
- Intended for migration to a fresh deployment/database.

### Import file (`POST /api/data/import-file`)

- Accepts uploaded `.json.gz` (or plain `.json`) snapshot as multipart form-data.
- Applies the same validation and all-or-nothing transaction semantics as `/import`.

---

## 13. Error Reference

| HTTP Status | When it occurs                                                              |
|-------------|-----------------------------------------------------------------------------|
| `400`       | Invalid request body, business rule violated (e.g. pay an already-paid session) |
| `404`       | Student, session, schedule, package, or payer not found (or soft-deleted)   |
| `409`       | Conflict — e.g. adding a weekly schedule on a day the student already has   |
| `500`       | Unexpected server error (logged with full stack trace to error log file)    |

Business/technical errors (`400`, `404`, `409`, `500`) follow this JSON shape:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Session is already paid",
  "path": "/api/sessions/5/pay",
  "timestamp": "2026-04-12T10:30:00"
}
```

Bean-validation errors (`MethodArgumentNotValidException`) include a field map:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": {
    "fieldName": "validation message"
  },
  "path": "/api/students",
  "timestamp": "2026-04-12T10:30:00"
}
```

