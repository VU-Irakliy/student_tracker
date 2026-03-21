# Studio Student Management — Backend MVP

A production-ready Spring Boot 3 / Java 21 REST API for managing students, recurring class schedules,
one-off sessions, per-class and package-based payments, and a calendar view.

> 📖 **Non-technical guide** (plain English, no coding knowledge needed): [HOW_IT_WORKS.md](./HOW_IT_WORKS.md)  
> 📖 **Понятное руководство на русском языке**: [HOW_IT_WORKS_RU.md](./HOW_IT_WORKS_RU.md)  
> 📖 **Technical business logic & edge cases**: [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md)

---

## Tech Stack

| Concern        | Technology                          |
|----------------|-------------------------------------|
| Framework      | Spring Boot 3.2                     |
| Language       | Java 21 (records, sealed, streams)  |
| Build          | Maven                               |
| Database       | PostgreSQL 17 (Docker)              |
| Schema         | Dedicated `studio` schema           |
| ORM            | Spring Data JPA / Hibernate 6       |
| Mapping        | MapStruct                           |
| Boilerplate    | Lombok                              |
| Validation     | Jakarta Bean Validation             |
| AOP            | Spring AOP / AspectJ                |
| Logging        | Logback (rolling error file)        |
| API Docs       | SpringDoc OpenAPI (Swagger UI)      |
| Testing        | JUnit 5, Mockito, H2 (in-memory)    |

---

## Project Structure

```
src/main/java/com/studio/app/
├── StudentMgmtApplication.java          # Entry point
├── aspect/
│   └── ErrorLoggingAspect.java          # AOP error logger (disabled in tests)
├── config/
│   ├── DotenvPostProcessor.java         # Loads .env into Spring Environment
│   └── WebMvcConfig.java                # CORS configuration
├── controller/                          # API interfaces (annotations + Swagger)
│   ├── StudentApi.java                  # /api/students
│   ├── ScheduleApi.java                 # /api/students/{id}/schedules
│   ├── PayerApi.java                    # /api/students/{id}/payers
│   ├── StudentSessionApi.java           # /api/students/{id}/sessions
│   ├── StudentPackageApi.java           # /api/students/{id}/packages
│   ├── SessionApi.java                  # /api/sessions/{id}
│   ├── PackageApi.java                  # /api/packages/{id}
│   ├── CalendarApi.java                 # /api/calendar
│   ├── EarningsApi.java                 # /api/earnings
│   ├── DataPortabilityApi.java          # /api/data
│   └── impl/                            # Controller implementations
│       ├── StudentController.java
│       ├── ScheduleController.java
│       ├── PayerController.java
│       ├── StudentSessionController.java
│       ├── StudentPackageController.java
│       ├── SessionController.java
│       ├── PackageController.java
│       ├── CalendarController.java
│       ├── EarningsController.java
│       └── DataPortabilityController.java
├── service/
│   ├── StudentService.java
│   ├── ScheduleService.java
│   ├── ClassSessionService.java
│   ├── PackageService.java
│   ├── PayerService.java
│   ├── EarningsService.java
│   ├── DataPortabilityService.java
│   ├── CurrencyConversionService.java
│   └── impl/                            # Service implementations
├── repository/                          # JpaRepository interfaces
├── entity/
│   ├── BaseEntity.java                  # audit timestamps + soft-delete flag
│   ├── Student.java
│   ├── WeeklySchedule.java              # recurring weekly slots
│   ├── ClassSession.java                # concrete class instances
│   ├── PackagePurchase.java             # package payment tracking
│   └── Payer.java                       # bank-transfer payers
├── dto/
│   ├── request/                         # inbound payloads
│   └── response/                        # outbound views
├── mapper/                              # MapStruct mappers
├── enums/
│   ├── PricingType.java                 # PER_CLASS | PACKAGE
│   ├── ClassStatus.java                 # SCHEDULED | COMPLETED | CANCELLED | MOVED
│   ├── PaymentStatus.java               # UNPAID | PAID | PACKAGE | REFUNDED
│   ├── Currency.java                    # EUROS | DOLLARS | RUBLES
│   ├── StudioTimezone.java              # SPAIN | RUSSIA_MOSCOW
│   └── StudentClassType.java            # CASUAL | EGE | OGE | IELTS | TOFEL
└── exception/                           # Custom exceptions + global handler

src/main/resources/
├── application.properties               # app config
└── logback-spring.xml                   # logging (console + Desktop error file)

db/
├── init/                                # additive SQL migrations
│   ├── 00_create_schema.sql
│   ├── 01_create_tables.sql
│   ├── 02_create_indexes.sql
│   └── 03+_*.sql                        # additive migrations
├── scripts/
│   ├── apply-pending-init.sh            # container-native auto migration runner
│   ├── backup.ps1
│   └── restore.ps1
└── backups/                             # dump output directory (git-ignored)
```

---

## Running the App

### 1. Start PostgreSQL (Docker)

```bash
docker compose up -d
```

On **first startup** (empty `pgdata/`) PostgreSQL initializes from `db/init/`.
On every next `docker compose up -d`, the `db-migrator` service checks `db/init/*.sql`
in filename order and applies only scripts missing from `studio.schema_migration_history`.

If you prefer using the helper script:

```powershell
.\db\scripts\start-db.ps1
```

It starts PostgreSQL plus `db-migrator`, so pending scripts are applied automatically.

```bash
docker compose down        # stop (keeps data)
docker compose down -v     # stop + remove volume
```

To reinitialise from scratch:

```powershell
docker compose down
Remove-Item -Recurse pgdata
docker compose up -d
```

### 2. Configure

Edit the `.env` file at the project root:

```dotenv
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5433
POSTGRES_DB=studio_db
POSTGRES_USER=studio_user
POSTGRES_PASSWORD=studio_pass
SERVER_PORT=8080
```

### 3. Build & Run

```bash
mvn clean package
mvn spring-boot:run
```

Or from a JAR:

```bash
java -jar target/student-mgmt-1.0.0.jar
```

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Database

### Schema

All tables live in the dedicated **`studio`** PostgreSQL schema. Hibernate runs with
`ddl-auto=validate` — it verifies entity/schema alignment on startup but never modifies it.
The SQL init scripts are the single source of truth.

### Backup & Restore

```powershell
# Back up
.\db\scripts\backup.ps1
.\db\scripts\backup.ps1 -Tag "pre-migration"

# Restore
.\db\scripts\restore.ps1
.\db\scripts\restore.ps1 -File "db\backups\studio_20260311_140000.sql"
```

The restore runs inside a single transaction — if anything fails, nothing changes.

### Migration Notes

`studio.schema_migration_history` prevents re-running the same script.
If a previously applied file is edited, `db-migrator` logs that checksum differs and skips it,
so the safe path is still to add changes as a new SQL file.

---

## Error Logging

An AOP aspect (`ErrorLoggingAspect`) intercepts every exception that escapes the controller
layer and appends it to a rolling log file on the Desktop:

```
~/Desktop/studio-error-logs/error.log
```

| Exception type                   | Log level | Detail           |
|----------------------------------|-----------|------------------|
| Business (404, 400, 409)         | `WARN`    | message only     |
| Unexpected (NPE, DB errors, etc) | `ERROR`   | full stack trace |

Disabled automatically during tests (`@Profile("!test")`).

---

## Currency Conversion

Student prices and session charges are stored in their **original currency**
(`EUROS`, `DOLLARS`, or `RUBLES`). Every response that contains a monetary amount also
includes a `convertedPrices` / `convertedAmountPaid` map showing the equivalent in all
three currencies.

Rates are fetched from the [Open Exchange Rate API](https://open.er-api.com) (no API key,
~1 500 req/month free tier) and cached in-memory per base currency for 60 minutes
(configurable via `currency.cache-ttl-minutes`). If the API is unreachable, the last known
cached value is used as a fallback.

---

## API Reference

### Students — `/api/students`

| Method | Path                        | Description                        |
|--------|-----------------------------|------------------------------------|
| POST   | `/api/students`             | Create a student                   |
| GET    | `/api/students`             | List all; optional `search` + `debtor` filters |
| GET    | `/api/students/search`      | Search by student or payer name    |
| GET    | `/api/students/{id}`        | Get one student                    |
| PATCH  | `/api/students/{id}`        | Update student (partial)           |
| DELETE | `/api/students/{id}`        | Soft-delete student + related data |

`GET /api/students` query rules:
- `search` is optional (case-insensitive match by student first/last name).
- `debtor` is optional boolean (`true` or `false`).
- If both are provided, both filters are applied (`AND`).

Examples:
```http
GET /api/students
GET /api/students?debtor=true
GET /api/students?debtor=false
GET /api/students?search=petrov&debtor=true
```

`GET /api/students/search` query rules:
- `query` is required and must be non-blank.
- Matching is case-insensitive.
- Matches active students by:
  - student first name,
  - student last name,
  - student full name (`firstName + " " + lastName`),
  - active payer `fullName`.

Examples:
```http
GET /api/students/search?query=ana
GET /api/students/search?query=ana%20garcia
GET /api/students/search?query=olga
```

**Create / update body (all fields optional on update):**
```json
{
  "firstName": "Ana",
  "lastName": "Garcia",
  "phoneNumber": "+34 600 000 000",
  "pricingType": "PER_CLASS",
  "pricePerClass": 35.00,
  "currency": "EUROS",
  "timezone": "SPAIN",
  "classType": "CASUAL",
  "startDate": "2026-03-01",
  "holidayMode": false,
  "holidayFrom": null,
  "holidayTo": null,
  "stoppedAttending": false,
  "notes": "Prefers morning classes"
}
```
`pricingType`: `PER_CLASS` | `PACKAGE`  
`currency`: `EUROS` | `DOLLARS` | `RUBLES`  
`timezone`: `SPAIN` | `RUSSIA_MOSCOW`
`classType`: `CASUAL` | `EGE` | `OGE` | `IELTS` | `TOFEL`

Partial update semantics:
- sending `null` for `pricePerClass` and/or `currency` keeps existing stored values unchanged.
- exception: when `pricingType` is set to `PACKAGE`, the backend always clears student `pricePerClass` and `currency`.

Pricing invariants:
- `PER_CLASS`: `pricePerClass` is required; `currency` is required when `pricePerClass` is provided.
- `PACKAGE`: student-level `pricePerClass` and `currency` must be `null`; package purchase stores payment amount/currency.

Student responses also include `debtor` (boolean), maintained by the debtor batch process.

Lifecycle fields:
- `startDate` blocks creating classes before that date.
- `holidayMode=true` requires `holidayFrom`; classes are auto-cancelled from that date.
- turning holiday off requires `holidayTo` (return date), and auto-cancelled future sessions from that day are restored.
- `stoppedAttending=true` keeps student visible but blocks new sessions/schedule edits.

---

### Weekly Schedules — `/api/students/{studentId}/schedules`

| Method | Path                                        | Description          |
|--------|---------------------------------------------|----------------------|
| POST   | `/api/students/{id}/schedules`              | Add a recurring slot |
| GET    | `/api/students/{id}/schedules`              | Get all active slots |
| POST   | `/api/students/{id}/schedules/{sid}`        | Update a slot        |
| POST   | `/api/students/{id}/schedules/{sid}/delete` | Remove a slot        |

```json
{ "dayOfWeek": "MONDAY", "startTime": "10:00", "durationMinutes": 60 }
```

---

### Payers — `/api/students/{studentId}/payers`

| Method | Path                                      | Description    |
|--------|-------------------------------------------|----------------|
| POST   | `/api/students/{id}/payers`               | Add a payer    |
| GET    | `/api/students/{id}/payers`               | List payers    |
| POST   | `/api/students/{id}/payers/{pid}`         | Update a payer |
| POST   | `/api/students/{id}/payers/{pid}/delete`  | Remove a payer |

```json
{ "fullName": "Maria Garcia", "phoneNumber": "+34 600 111 222", "note": "Mother" }
```

---

### Class Sessions

#### Create / List (student-scoped)

| Method | Path                                         | Description                                      |
|--------|----------------------------------------------|--------------------------------------------------|
| POST   | `/api/students/{id}/sessions`                | Add a one-off (extra/moved) class                |
| GET    | `/api/students/{id}/sessions`                | All sessions; `?from=&to=` date filter           |
| GET    | `/api/students/{id}/sessions/by-payment`     | Filter by `?paymentStatus=PAID\|UNPAID\|PACKAGE` |

One-off creation now validates student availability: date must be on/after `startDate`, student must not be in active holiday mode, and must not be marked as stopped attending.

Student-scoped list behaviour:
- if student does not exist (or is soft-deleted), `GET /sessions` and `GET /sessions/by-payment` return `404`.
- date filtering supports full range (`from` + `to`) and single-sided filters (`from` only or `to` only).

```json
{
  "classDate": "2026-04-20",
  "startTime": "11:00",
  "durationMinutes": 60,
  "note": "Moved from Tuesday due to holiday"
}
```

#### Single session actions — `/api/sessions/{sessionId}`

| Method | Path                                    | Description                                                      |
|--------|-----------------------------------------|------------------------------------------------------------------|
| GET    | `/api/sessions/{id}`                    | Get session details                                              |
| PUT    | `/api/sessions/{id}`                    | Update date/time/duration/status/payment/note in one request     |
| POST   | `/api/sessions/{id}/cancel`             | Cancel session (optionally keep payment)                         |
| POST   | `/api/sessions/{id}/pay`                | Mark as paid (auto-deducts from active package for PACKAGE type) |
| POST   | `/api/sessions/{id}/completion`         | Set completion state via `?completed=true\|false`               |
| POST   | `/api/sessions/{id}/cancel-payment`     | Revert payment (→ UNPAID or return slot to package)              |

**Cancel:**
```json
{ "keepAsPaid": false, "note": "Student cancelled last minute" }
```

**Pay (PER_CLASS with optional price override):**
```json
{ "amountOverride": 30.00 }
```

**Unified update (`PUT /api/sessions/{id}`):**
```json
{
  "classDate": "2026-04-20",
  "startTime": "11:00",
  "durationMinutes": 60,
  "status": "COMPLETED",
  "paid": true,
  "amountOverride": 30.00,
  "note": "Conducted and paid"
}
```

**Set completion state:**
```
POST /api/sessions/{id}/completion?completed=true
POST /api/sessions/{id}/completion?completed=false
```

---

### Packages — `/api/students/{studentId}/packages`

| Method | Path                                  | Description                  |
|--------|---------------------------------------|------------------------------|
| POST   | `/api/students/{id}/packages`         | Record a package purchase    |
| GET    | `/api/students/{id}/packages`         | All purchases (newest first) |
| GET    | `/api/students/{id}/packages/active`  | Active packages (FIFO order) |
| GET    | `/api/packages/{packageId}`           | Get one package              |

```json
{
  "totalClasses": 10,
  "amountPaid": 280.00,
  "currency": "EUROS",
  "paymentDate": "2026-04-01",
  "description": "Spring 10-class bundle"
}
```

> `amountPaid` is what the student actually paid — it can be any negotiated amount.  
> Required fields: `totalClasses`, `amountPaid`, `currency`, `paymentDate`.

---

### Calendar — `/api/calendar`

| Method | Path            | Description                                           |
|--------|-----------------|-------------------------------------------------------|
| GET    | `/api/calendar` | All sessions grouped by day; `?from=&to=` date filter |

Defaults to today → next 30 days if dates are omitted.

Each day entry includes:
- `totalHours` — total class duration for that day
- `completedHours` — duration for sessions with `status=COMPLETED`

---

### Earnings — `/api/earnings`

| Method | Path                 | Description                                    |
|--------|----------------------|------------------------------------------------|
| GET    | `/api/earnings/daily`   | Selected-period earnings + daily breakdown  |
| GET    | `/api/earnings/monthly` | Monthly summary with optional base currency |

Query parameters:
- `from`, `to` — date range (`YYYY-MM-DD`)
- `year`, `month` — for monthly endpoint
- `baseCurrency` — `EUROS` | `DOLLARS` | `RUBLES` (optional; returns a normalised total)

`/api/earnings/daily` returns a period object with:
- `dailyBreakdown` (per-day rows for `PAID` per-class sessions)
- `totalEarned*` (includes paid per-class sessions + package purchases in range)
- `totalCouldHaveEarnedExcludingCancellations*`
- `totalCouldHaveEarnedIncludingCancellations*`

Package purchases are included in period totals when `paymentDate` is inside `from..to`.

To get **weekly earnings**, call `/api/earnings/daily` with any 7-day range.

**Monthly earnings** include both per-class session payments **and** package purchase payments
(matched by `paymentDate` within the month).

---

### Data Portability — `/api/data`

| Method | Path                | Description                                               |
|--------|---------------------|-----------------------------------------------------------|
| GET    | `/api/data/export`      | Download full data snapshot file (compressed JSON GZIP) |
| POST   | `/api/data/import`      | Replace current data with `.json.gz` bytes or plain JSON |
| POST   | `/api/data/import-file` | Replace current data using uploaded snapshot file      |

Use this when deploying a new app version into a fresh database and you need to move data from the old instance.

Recommended flow:
1. Call `GET /api/data/export` on the old environment and save the `.json.gz` file.
2. Start the new environment with an empty DB.
3. Import using one of:
   - `POST /api/data/import` with the downloaded file content directly
   - `POST /api/data/import-file` with multipart file upload

Exported file name format:
- `student-mgmt-export-YYYYMMDD_HHMMSS-utc.json.gz`
- Example: `student-mgmt-export-20260315_184530-utc.json.gz`

---

## Business Rules Summary

| Scenario                                | Behaviour                                                                                  |
|-----------------------------------------|--------------------------------------------------------------------------------------------|
| Cancel + keep as paid                   | `status=CANCELLED`, payment status unchanged, package slot NOT returned                    |
| Cancel + release payment (PER_CLASS)    | `paymentStatus=UNPAID`; then mark target session as paid (optionally with `amountOverride`) |
| Cancel + release payment (PACKAGE)      | Package slot returned (`classesRemaining + 1`); session unlinked from package              |
| Cancel already-cancelled session        | `400 Bad Request`                                                                          |
| Cancel payment on UNPAID session        | `400 Bad Request`                                                                          |
| Pay session (PACKAGE student)           | Auto-deducts from oldest active package (FIFO); `paymentStatus=PACKAGE`                   |
| Pay session (no active package)         | `400 Bad Request`                                                                          |
| Pay already-paid session                | `400 Bad Request`                                                                          |
| Set completion state                    | `/completion?completed=true\|false` switches `status` between `COMPLETED` and `SCHEDULED` |
| Unified session update                  | `PUT /api/sessions/{id}` can update schedule fields, status, payment toggle, and note     |
| Student soft-delete                     | Student + schedules + payers soft-deleted; only **future** sessions deleted, past kept     |
| Debtor status                           | After 22:00 local time, students with already-happened `UNPAID` sessions are marked debtor |
| Debtor startup catch-up                 | On app startup, debtor recomputation runs once without waiting for 22:00                   |
| Price capture                           | `priceCharged` copied from student at session creation time                                |
| Package FIFO deduction                  | Oldest active package (by `paymentDate`) is consumed first                                 |
| Currency conversion unavailable         | Stale cache used if available; otherwise `convertedPrices` map is empty (no crash)         |

> 📖 See [BUSINESS_LOGIC.md](./BUSINESS_LOGIC.md) for full scenario walkthroughs.

### Debtor Batch Configuration

- `debtor.batch.cron` (default `0 5 * * * *`) schedules periodic debtor recomputation.
- `debtor.batch.run-on-startup` (default `true`) triggers a catch-up run at startup.
- Scheduled runs only apply updates for students whose local time is `22:00` or later.
- Startup catch-up ignores the 22:00 gate so statuses are corrected immediately after downtime.

### Environment Variables

Core variables used by Docker + Spring:
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `SERVER_PORT`

Optional operational overrides:
- `DEBTOR_BATCH_CRON` (default `0 5 * * * *`)
- `DEBTOR_BATCH_RUN_ON_STARTUP` (default `true`)

---

## Running Tests

```bash
mvn test
```

Tests use the `test` Spring profile with an **H2 in-memory database**. The AOP error
logging aspect and Desktop file appender are automatically disabled. A stub
`CurrencyConversionService` with fixed rates is injected for all integration tests so no
real HTTP calls are made.
