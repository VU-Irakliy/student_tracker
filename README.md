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
│   └── impl/                            # Controller implementations
│       ├── StudentController.java
│       ├── ScheduleController.java
│       ├── PayerController.java
│       ├── StudentSessionController.java
│       ├── StudentPackageController.java
│       ├── SessionController.java
│       ├── PackageController.java
│       ├── CalendarController.java
│       └── EarningsController.java
├── service/
│   ├── StudentService.java
│   ├── ScheduleService.java
│   ├── ClassSessionService.java
│   ├── PackageService.java
│   ├── PayerService.java
│   ├── EarningsService.java
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
├── init/                                # auto-run on first Docker startup
│   ├── 00_create_schema.sql
│   ├── 01_create_tables.sql
│   ├── 02_create_indexes.sql
│   └── 03–06_*.sql                      # additive migrations
├── scripts/
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

On **first startup** (empty `pgdata/`) the SQL scripts in `db/init/` automatically create
the `studio` schema, all tables, and indexes. Data is persisted in `pgdata/` (git-ignored).

For existing databases, run the startup helper script to automate migration checks:

```powershell
.\db\scripts\start-db.ps1
```

It starts PostgreSQL, checks `studio.schema_migration_history`, runs only pending
`db/init/*.sql` scripts in filename order, and records each applied script.

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

### Apply Pending Init Scripts Manually

If you want to trigger migration checks manually:

```powershell
.\db\scripts\apply-pending-init.ps1
```

The script skips already-applied files using `studio.schema_migration_history`.

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
| GET    | `/api/students`             | List all; `?search=name` to filter |
| GET    | `/api/students/{id}`        | Get one student                    |
| PUT    | `/api/students/{id}`        | Update student (partial)           |
| DELETE | `/api/students/{id}`        | Soft-delete student + related data |

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
  "notes": "Prefers morning classes"
}
```
`pricingType`: `PER_CLASS` | `PACKAGE`  
`currency`: `EUROS` | `DOLLARS` | `RUBLES`  
`timezone`: `SPAIN` | `RUSSIA_MOSCOW`
`classType`: `CASUAL` | `EGE` | `OGE` | `IELTS` | `TOFEL`

Student responses also include `debtor` (boolean), maintained by the debtor batch process.

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
| POST   | `/api/sessions/{id}/move-payment`       | Move payment to another session                                  |

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

**Move payment:**
```json
{ "targetSessionId": 42 }
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
> `currency` defaults to the student's own currency if omitted.

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

## Business Rules Summary

| Scenario                                | Behaviour                                                                                  |
|-----------------------------------------|--------------------------------------------------------------------------------------------|
| Cancel + keep as paid                   | `status=CANCELLED`, payment status unchanged, package slot NOT returned                    |
| Cancel + release payment (PER_CLASS)    | `paymentStatus=UNPAID`; use `/move-payment` to reassign to another session                 |
| Cancel + release payment (PACKAGE)      | Package slot returned (`classesRemaining + 1`); session unlinked from package              |
| Cancel already-cancelled session        | `400 Bad Request`                                                                          |
| Cancel payment on UNPAID session        | `400 Bad Request`                                                                          |
| Pay session (PACKAGE student)           | Auto-deducts from oldest active package (FIFO); `paymentStatus=PACKAGE`                   |
| Pay session (no active package)         | `400 Bad Request`                                                                          |
| Pay already-paid session                | `400 Bad Request`                                                                          |
| Set completion state                    | `/completion?completed=true\|false` switches `status` between `COMPLETED` and `SCHEDULED` |
| Unified session update                  | `PUT /api/sessions/{id}` can update schedule fields, status, payment toggle, and note     |
| Move payment (source must be PAID)      | Source → `UNPAID`, target → `PAID`, price transferred                                     |
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

---

## Running Tests

```bash
mvn test
```

Tests use the `test` Spring profile with an **H2 in-memory database**. The AOP error
logging aspect and Desktop file appender are automatically disabled. A stub
`CurrencyConversionService` with fixed rates is injected for all integration tests so no
real HTTP calls are made.
