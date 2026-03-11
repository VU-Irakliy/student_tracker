# Studio Student Management — Backend MVP

A production-ready Spring Boot 3 / Java 21 REST API for managing students, recurring class schedules,
one-off sessions, per-class and package-based payments, and a calendar view.

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
| Testing        | JUnit 5, Mockito                    |

---

## Project Structure

```
src/main/java/com/studio/app/
├── StudentMgmtApplication.java          # Entry point
├── aspect/
│   └── ErrorLoggingAspect.java          # AOP error logger (Desktop file)
├── config/
│   ├── DotenvPostProcessor.java         # Loads .env into Spring Environment
│   └── WebMvcConfig.java                # CORS
├── controller/                          # API interfaces (annotations + Swagger)
│   ├── StudentApi.java                  # /api/students
│   ├── ScheduleApi.java                 # /api/students/{id}/schedules
│   ├── PayerApi.java                    # /api/students/{id}/payers
│   ├── StudentSessionApi.java           # /api/students/{id}/sessions
│   ├── StudentPackageApi.java           # /api/students/{id}/packages
│   ├── SessionApi.java                  # /api/sessions/{id}
│   ├── PackageApi.java                  # /api/packages/{id}
│   ├── CalendarApi.java                 # /api/calendar
│   └── impl/                            # Controller implementations
│       ├── StudentController.java
│       ├── ScheduleController.java
│       ├── PayerController.java
│       ├── StudentSessionController.java
│       ├── StudentPackageController.java
│       ├── SessionController.java
│       ├── PackageController.java
│       └── CalendarController.java
├── service/
│   ├── StudentService.java              # interface
│   ├── ScheduleService.java             # interface
│   ├── ClassSessionService.java         # interface
│   ├── PackageService.java              # interface
│   ├── PayerService.java                # interface
│   └── impl/                            # implementations
├── repository/                          # JpaRepository interfaces
├── entity/
│   ├── BaseEntity.java                  # audit + soft-delete
│   ├── Student.java
│   ├── WeeklySchedule.java              # recurring slots
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
│   └── StudioTimezone.java              # SPAIN | RUSSIA_MOSCOW
└── exception/                           # exceptions + global handler

src/main/resources/
├── application.properties               # app config
└── logback-spring.xml                   # logging (console + Desktop error file)

db/
├── init/                                # auto-run on first Docker startup
│   ├── 00_create_schema.sql             # CREATE SCHEMA studio
│   ├── 01_create_tables.sql             # all tables (IF NOT EXISTS)
│   └── 02_create_indexes.sql            # partial indexes
├── scripts/
│   ├── backup.ps1                       # dump studio schema to timestamped file
│   └── restore.ps1                      # restore from a backup
└── backups/                             # dump output directory (git-ignored)
```

---

## Running the App

### 1. Start PostgreSQL (Docker)

```bash
docker compose up -d
```

This creates a local PostgreSQL 17 instance. On **first startup** (empty `pgdata/`) the
SQL scripts in `db/init/` automatically create the `studio` schema, all tables, and indexes.

Data is persisted in the project's `pgdata/` folder (git-ignored).

```bash
docker compose down          # stop (keeps data)
docker compose down -v       # stop + remove volume
```

To reinitialise the database from scratch:

```powershell
docker compose down
Remove-Item -Recurse pgdata
docker compose up -d
```

### 2. Configure

Environment variables are loaded from the `.env` file at the project root.
Edit it or `application.properties` as needed:

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

Server: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## Database

### Schema

All application tables live in the dedicated **`studio`** PostgreSQL schema (not `public`).
Hibernate is configured with `ddl-auto=validate` — it verifies the schema matches the
entities on startup but never modifies it. The SQL init scripts are the single source of truth.

### Backup & Restore

PowerShell scripts in `db/scripts/` use `docker exec` + `pg_dump`/`psql` to work with the
running container.

**Back up the database:**

```powershell
.\db\scripts\backup.ps1                         # timestamped dump
.\db\scripts\backup.ps1 -Tag "pre-migration"    # labelled dump
```

Output: `db/backups/studio_<timestamp>[_<tag>].sql`

**Restore from a backup:**

```powershell
.\db\scripts\restore.ps1                         # latest backup
.\db\scripts\restore.ps1 -File "db\backups\studio_20260311_140000.sql"
```

The restore runs inside a single transaction — if anything fails, nothing changes.

---

## Error Logging

An AOP aspect (`ErrorLoggingAspect`) intercepts every exception that escapes the controller
layer and writes it to a rolling log file on the Desktop:

```
~/Desktop/studio-error-logs/error.log
```

| Exception type                    | Log level | Detail            |
|-----------------------------------|-----------|-------------------|
| Business (404, 400, 409)          | `WARN`    | message only      |
| Unexpected (NPE, DB errors, etc) | `ERROR`   | full stack trace  |

The aspect and the file appender are **disabled during tests** (`@Profile("!test")`).

Configuration: `src/main/resources/logback-spring.xml`

---

## API Reference

### Students — `/api/students`

| Method | Path                          | Description                        |
|--------|-------------------------------|------------------------------------|
| POST   | `/api/students`               | Create a student                   |
| GET    | `/api/students`               | List all; `?search=name` to filter |
| GET    | `/api/students/{id}`          | Get one student                    |
| POST   | `/api/students/{id}`          | Update student (partial)           |
| POST   | `/api/students/{id}/delete`   | Soft-delete student + all data     |

**Create student body:**
```json
{
  "firstName": "Ana",
  "lastName": "Garcia",
  "email": "ana@studio.com",
  "phoneNumber": "+34 600 000 000",
  "pricingType": "PER_CLASS",
  "pricePerClass": 35.00,
  "timezone": "SPAIN",
  "notes": "Prefers morning classes"
}
```
`pricingType`: `PER_CLASS` | `PACKAGE`
`timezone`: `SPAIN` | `RUSSIA_MOSCOW`

---

### Weekly Schedules — `/api/students/{studentId}/schedules`

| Method | Path                                          | Description         |
|--------|-----------------------------------------------|---------------------|
| POST   | `/api/students/{id}/schedules`                | Add a recurring slot |
| GET    | `/api/students/{id}/schedules`                | Get all active slots |
| POST   | `/api/students/{id}/schedules/{sid}`          | Update a slot        |
| POST   | `/api/students/{id}/schedules/{sid}/delete`   | Remove a slot        |

**Add schedule body:**
```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "10:00",
  "durationMinutes": 60
}
```

---

### Payers — `/api/students/{studentId}/payers`

| Method | Path                                          | Description         |
|--------|-----------------------------------------------|---------------------|
| POST   | `/api/students/{id}/payers`                   | Add a payer          |
| GET    | `/api/students/{id}/payers`                   | List payers          |
| POST   | `/api/students/{id}/payers/{pid}`             | Update a payer       |
| POST   | `/api/students/{id}/payers/{pid}/delete`      | Remove a payer       |

**Add payer body:**
```json
{
  "fullName": "Maria Garcia",
  "phoneNumber": "+34 600 111 222",
  "note": "Mother"
}
```

---

### Class Sessions

#### Create / List (student-scoped)

| Method | Path                                             | Description                                       |
|--------|--------------------------------------------------|---------------------------------------------------|
| POST   | `/api/students/{id}/sessions`                    | Add a one-off (extra/moved) class                 |
| GET    | `/api/students/{id}/sessions`                    | All sessions; `?from=&to=` date filter            |
| GET    | `/api/students/{id}/sessions/by-payment`         | Filter by `?paymentStatus=PAID\|UNPAID\|PACKAGE`  |

**One-off session body:**
```json
{
  "classDate": "2024-09-20",
  "startTime": "11:00",
  "durationMinutes": 60,
  "note": "Moved from Tuesday due to holiday"
}
```

#### Single session actions — `/api/sessions/{sessionId}`

| Method | Path                                    | Description                                                        |
|--------|-----------------------------------------|--------------------------------------------------------------------|
| GET    | `/api/sessions/{id}`                    | Get session                                                        |
| POST   | `/api/sessions/{id}/cancel`             | Cancel session                                                     |
| POST   | `/api/sessions/{id}/pay`                | Mark session as paid (auto-deducts package for PACKAGE students)   |
| POST   | `/api/sessions/{id}/cancel-payment`     | Revert payment (UNPAID / return to package)                        |
| POST   | `/api/sessions/{id}/move-payment`       | Move payment to another session                                    |

**Cancel session body:**
```json
{ "keepAsPaid": true, "note": "Student cancelled last minute" }
```

**Move payment body:**
```json
{ "targetSessionId": 42 }
```

---

### Packages — `/api/students/{studentId}/packages`

| Method | Path                                       | Description                  |
|--------|--------------------------------------------|------------------------------|
| POST   | `/api/students/{id}/packages`              | Record a package purchase    |
| GET    | `/api/students/{id}/packages`              | All purchases (newest first) |
| GET    | `/api/students/{id}/packages/active`       | Active packages (FIFO order) |
| GET    | `/api/packages/{packageId}`                | Get one package              |

**Purchase package body:**
```json
{
  "totalClasses": 10,
  "amountPaid": 280.00,
  "paymentDate": "2024-09-01",
  "description": "Autumn 10-class bundle"
}
```

> `amountPaid` is what the student actually paid — it can be any amount.

---

### Calendar — `/api/calendar`

| Method | Path            | Description                                           |
|--------|-----------------|-------------------------------------------------------|
| GET    | `/api/calendar` | All sessions grouped by day; `?from=&to=` date filter |

Defaults to today through the next 30 days if dates are omitted.

**Response structure:**
```json
[
  {
    "date": "2024-09-16",
    "sessions": [
      {
        "id": 5,
        "studentId": 1,
        "studentName": "Ana Garcia",
        "classDate": "2024-09-16",
        "startTime": "10:00",
        "durationMinutes": 60,
        "status": "SCHEDULED",
        "paymentStatus": "PAID",
        "priceCharged": 35.00,
        "packagePurchaseId": null,
        "oneOff": false
      }
    ]
  }
]
```

---

## Business Rules Summary

| Scenario                             | Behaviour                                                                                                    |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------|
| Cancel + keep as paid                | `status=CANCELLED`, paymentStatus unchanged                                                                  |
| Cancel + release payment (PER_CLASS) | `paymentStatus=UNPAID`; use move-payment to reassign                                                         |
| Cancel + package                     | Package slot returned (`classesRemaining + 1`)                                                               |
| Cancel payment for paid session      | Reverts to UNPAID (PER_CLASS) or returns slot (PACKAGE)                                                      |
| Assign package to session            | Automatic: `/pay` detects PACKAGE pricing and deducts from oldest active package (FIFO)                      |
| Student leaves (soft delete)         | Student + schedules + payers soft-deleted; only **future** sessions are deleted — past sessions are preserved |
| Price capture                        | `priceCharged` copied from student at session creation time                                                  |

---

## Running Tests

```bash
mvn test
```

Tests run with the `test` profile — the AOP error logging aspect and Desktop file
appender are automatically disabled.
