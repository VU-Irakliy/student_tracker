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
| Database       | PostgreSQL (local)                  |
| ORM            | Spring Data JPA / Hibernate 6       |
| Mapping        | MapStruct                           |
| Boilerplate    | Lombok                              |
| Validation     | Jakarta Bean Validation             |
| Testing        | JUnit 5, Mockito                    |

---

## Project Structure

```
src/main/java/com/studio/app/
├── StudentMgmtApplication.java        # Entry point
├── config/
│   └── WebMvcConfig.java              # CORS
├── controller/
│   ├── StudentController.java         # /api/students
│   ├── ScheduleController.java        # /api/students/{id}/schedules
│   ├── StudentSessionController.java  # /api/students/{id}/sessions
│   ├── SessionController.java        # /api/sessions/{id}
│   ├── CalendarController.java       # /api/calendar
│   └── PackageController.java         # /api/students/{id}/packages
├── service/
│   ├── StudentService.java            # interface
│   ├── ScheduleService.java           # interface
│   ├── ClassSessionService.java       # interface
│   ├── PackageService.java            # interface
│   └── impl/                          # implementations
├── repository/                        # JpaRepository interfaces
├── entity/
│   ├── BaseEntity.java                # audit + soft-delete
│   ├── Student.java
│   ├── WeeklySchedule.java            # recurring slots
│   ├── ClassSession.java              # concrete class instances
│   └── PackagePurchase.java           # package payment tracking
├── dto/
│   ├── request/                       # inbound payloads
│   └── response/                      # outbound views
├── mapper/                            # MapStruct mappers
├── enums/
│   ├── PricingType.java               # PER_CLASS | PACKAGE
│   ├── ClassStatus.java               # SCHEDULED | COMPLETED | CANCELLED | MOVED
│   ├── PaymentStatus.java             # UNPAID | PAID | PACKAGE | REFUNDED
│   └── StudioTimezone.java            # SPAIN | RUSSIA_MOSCOW
└── exception/                         # exceptions + global handler
```

---

## Running the App

### 1. Start PostgreSQL (Docker — data stored in `./pgdata`)
```bash
docker compose up -d
```
This creates a local PostgreSQL 17 instance whose data lives inside the project's `pgdata/` folder
(already git-ignored). The database, user, and password are pre-configured to match `application.properties`.

To stop the database:
```bash
docker compose down        # keeps data in pgdata/
docker compose down -v     # also removes pgdata/ contents
```

### 2. Configure (edit if needed)
```
src/main/resources/application.properties
```

### 3. Build & run
```bash
mvn clean package
mvn spring-boot:run
```

Server: `http://localhost:8080`

---

## API Reference

### Students — `/api/students`

| Method | Path                      | Description                        |
|--------|---------------------------|------------------------------------|
| POST   | `/api/students`           | Create a student                   |
| GET    | `/api/students`           | List all; `?search=name` to filter |
| GET    | `/api/students/{id}`      | Get one student                    |
| POST   | `/api/students/{id}`      | Update student (partial)           |
| POST   | `/api/students/{id}/delete` | Soft-delete student + all data   |

**Create student body:**
```json
{
  "firstName": "Ana",
  "lastName": "García",
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

| Method | Path                                  | Description             |
|--------|---------------------------------------|-------------------------|
| POST   | `/api/students/{id}/schedules`        | Add a recurring slot    |
| GET    | `/api/students/{id}/schedules`        | Get all active slots    |
| POST   | `/api/students/{id}/schedules/{sid}`  | Update a slot           |
| POST   | `/api/students/{id}/schedules/{sid}/delete` | Remove a slot     |

**Add schedule body:**
```json
{
  "dayOfWeek": "MONDAY",
  "startTime": "10:00",
  "durationMinutes": 60
}
```

---

### Class Sessions

#### Create / List (student-scoped)

| Method | Path                                          | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/api/students/{id}/sessions`                 | Add a one-off (extra/moved) class        |
| GET    | `/api/students/{id}/sessions`                 | All sessions; `?from=&to=` date filter   |
| GET    | `/api/students/{id}/sessions/by-payment`      | Filter by `?paymentStatus=PAID\|UNPAID\|PACKAGE` |

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

| Method | Path                                    | Description                                        |
|--------|-----------------------------------------|----------------------------------------------------|
| GET    | `/api/sessions/{id}`                    | Get session                                        |
| POST   | `/api/sessions/{id}/cancel`             | Cancel session                                     |
| POST   | `/api/sessions/{id}/pay`                | Mark session as paid (auto-deducts package for PACKAGE students) |
| POST   | `/api/sessions/{id}/cancel-payment`     | Revert payment (UNPAID / return to package)        |
| POST   | `/api/sessions/{id}/move-payment`       | Move payment to another session                    |

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

| Method | Path                                       | Description                          |
|--------|--------------------------------------------|--------------------------------------|
| POST   | `/api/students/{id}/packages`              | Record a package purchase            |
| GET    | `/api/students/{id}/packages`              | All purchases (newest first)         |
| GET    | `/api/students/{id}/packages/active`       | Active packages (FIFO order)         |
| GET    | `/api/packages/{packageId}`                | Get one package                      |

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

| Method | Path            | Description                                        |
|--------|-----------------|----------------------------------------------------|
| GET    | `/api/calendar` | All sessions grouped by day; `?from=&to=` required |

**Response structure:**
```json
[
  {
    "date": "2024-09-16",
    "sessions": [
      {
        "id": 5,
        "studentId": 1,
        "studentName": "Ana García",
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

| Scenario                          | Behaviour                                                              |
|-----------------------------------|------------------------------------------------------------------------|
| Cancel + keep as paid             | `status=CANCELLED`, paymentStatus unchanged                           |
| Cancel + release payment (PER_CLASS) | `paymentStatus=UNPAID`; use move-payment to reassign              |
| Cancel + package                  | Package slot returned (`classesRemaining + 1`)                        |
| Cancel payment for paid session   | Reverts to UNPAID (PER_CLASS) or returns slot (PACKAGE)              |
| Assign package to session         | Automatic: `/pay` detects PACKAGE pricing and deducts from oldest active package (FIFO) |
| Student leaves (soft delete)      | Student + schedules + payers soft-deleted; only **future** sessions are deleted — past sessions are kept as historical records |
| Price capture                     | `priceCharged` copied from student at session creation time           |

---

## Running Tests

```bash
mvn test
```
