# How the Studio App Works — Plain English Guide

This guide explains everything the app can do, written for someone who just wants to
understand the logic — no technical knowledge needed.

---

## Table of Contents

1. [The Big Picture](#1-the-big-picture)
2. [Students](#2-students)
3. [Schedules](#3-schedules)
4. [Classes (Sessions)](#4-classes-sessions)
5. [Payments — Pay Per Class](#5-payments--pay-per-class)
6. [Payments — Packages](#6-payments--packages)
7. [Cancelling a Class](#7-cancelling-a-class)
8. [Moving a Class to a Different Day](#8-moving-a-class-to-a-different-day)
9. [Payers (Someone Else Pays)](#9-payers-someone-else-pays)
10. [Earnings](#10-earnings)
11. [Deleting a Student](#11-deleting-a-student)
12. [Currencies](#12-currencies)
13. [Common Situations — Quick Answers](#13-common-situations--quick-answers)

---

## 1. The Big Picture

The app is a **record-keeping system for a private teaching studio**. It helps you keep
track of:

- Who your students are
- When they have class (their weekly schedule)
- Which classes have happened
- Whether those classes have been paid for
- How much money you've earned
- How to move all your data to a fresh app instance (export/import)

Think of it like a **digital notebook** — but one that understands the difference between
"Ana paid for this class" and "Ivan's class came out of his prepaid package."

---

## 2. Students

Each student has a profile with:

- **Name and phone number**
- **How they pay** — either per class, or with a prepaid package (see sections 5 and 6)
- **Price per class** — only if they pay per class; package students don't have a per-class price
- **Currency** — Euros, Dollars, or Rubles
- **Timezone** — Spain or Russia (Moscow) — used for display purposes
- **Class type** — Casual, EGE, OGE, IELTS, or TOFEL
- **Start date** — classes cannot be created before this date
- **Holiday mode** — temporary pause with a start date (and return date when the student comes back)
- **Stopped attending** — keeps the student in the list but blocks new schedules/classes
- **Notes** — anything you want to remember about them
- **Debtor flag** — automatically turns on if a lesson has happened and is still unpaid

You can update any of this at any time. Changing the pricing type (e.g. from per-class to
package) only affects new payments going forward — old records stay as they were.

If you mark a student as stopped attending, the app also hides/removes their upcoming sessions
from the active workflow so no new future teaching records are created by accident.

The debtor flag is not edited manually. The system updates it in batch mode:
- after 10:00 PM in each student's local timezone, and
- once at app startup (catch-up), so status is corrected after downtime.

---

## 3. Schedules

A schedule is a **recurring weekly slot** — for example, "Ana has class every Monday at 10:00
for 60 minutes."

- You can add as many weekly slots as you want per student (e.g. Monday + Wednesday)
- A student **cannot** have two slots on the same day of the week
- A schedule is just a reminder of the pattern — it **does not automatically create
  individual class records**. You add those manually (see section 4)
- You can remove a schedule slot at any time (it is just hidden, not deleted)

---

## 4. Classes (Sessions)

A **class** (called a "session" in the app) is a record of one specific lesson on a
specific date.

Every class record shows:
- The date and time
- How long it was
- Whether it was a regular class or a one-off (extra) class
- Its **status**: Scheduled, Completed, Cancelled
- Its **payment status**: Unpaid, Paid, Package, or Refunded

### Class status

| Status      | Meaning                                        |
|-------------|------------------------------------------------|
| Scheduled   | The class is planned or happened — not cancelled |
| Completed   | The class took place (optional label)          |
| Cancelled   | The class did not happen                       |

### Payment status

| Status    | Meaning                                                     |
|-----------|-------------------------------------------------------------|
| Unpaid    | No payment recorded yet                                     |
| Paid      | Paid individually (cash, bank transfer, etc.)               |
| Package   | Counted against a prepaid block of classes                  |
| Refunded  | Marked for future use                                       |

### One-off classes

If a student has an extra class that's not part of their regular schedule — for example,
a make-up lesson or a class moved from another day — you can add it as a **one-off**.
One-off classes work exactly like regular ones but are flagged separately so you can tell
them apart.

Before creating/updating a class date, the app checks:
- the date is not before the student's start date,
- the student is not currently on holiday,
- the student is not marked as stopped attending.

### Updating class details in one place

You can update key class information in a single action: date, time, duration, note,
class status, and whether it is paid/unpaid.

There is also a separate quick action to toggle completion state:
- completed = class conducted
- incompleted = back to scheduled

---

## 5. Payments — Pay Per Class

If a student pays **per class**, here's how it works:

1. Each class starts as **Unpaid**
2. When the student pays, you mark the class as **Paid**
3. You can optionally enter a different amount if the price was different that day
   (e.g. a discounted session)
4. The price is **locked in at the time the class is created** — if you later change
   the student's price, old classes are not affected

### Changing your mind about a payment

If you marked something as paid by mistake, you can **undo it** — the class goes back to
Unpaid. This does not affect any package.

---

## 6. Payments — Packages

A **package** (or "bundle") is when a student pays upfront for a set number of classes —
for example, 10 classes for €280.

### Buying a package

When the student pays, you record the package:
- How many classes it covers (e.g. 10)
- How much they paid (can be any amount — whatever was agreed)
- The currency
- The payment date
- An optional description (e.g. "Spring bundle")

The app tracks how many classes are **remaining** in the package.

### Using a package

When you mark a class as paid for a package student, the app automatically:
1. Finds the **oldest package** that still has classes left
2. Deducts one class from it
3. Links that class to the package

This is called **FIFO** (first in, first out) — the oldest package is always used up
first before moving to a newer one.

### What happens when a package runs out?

When a package reaches 0 remaining classes, it becomes **inactive**. If the student has
another package, the next class will be deducted from that one. If there is no active
package at all, you'll get an error until you record a new package purchase.

### Can I have multiple packages at the same time?

Yes. The app handles multiple packages and uses the oldest one first.

---

## 7. Cancelling a Class

When a class is cancelled, you have a choice: **keep the payment or release it**.

---

### Option A — Keep the payment (student is still charged)

Use this when the student cancelled last-minute and you still charge for the class.

**What happens:**
- The class is marked as Cancelled
- The payment status stays exactly as it was (Paid, Package, or Unpaid — unchanged)
- If it was a package class, **the class is NOT given back to the package**
- The student "loses" that class from their package

---

### Option B — Release the payment (student is not charged)

Use this when the student cancelled with enough notice and should not be charged.

**What happens for per-class students:**
- The class is marked as Cancelled
- The payment status goes back to Unpaid
- The money is not automatically moved — you decide what to do with it (see section 8
  for moving it to another class)

**What happens for package students:**
- The class is marked as Cancelled
- The package class is **given back** — the remaining count goes up by 1
- The class is unlinked from the package

---

### What if the class was never paid and then gets cancelled?

No problem — it just gets cancelled with Unpaid status. Nothing changes with any package.

---

### Can I cancel a class that is already cancelled?

No — the app will show an error. You cannot cancel twice.

---

## 8. Moving a Class to a Different Day

The app doesn't have a single "move" button. Instead, you do it in three steps:

---

### For per-class students

**Step 1 — Cancel the original class:**
> Mark the old class as Cancelled (usually with release payment).

**Step 2 — Add a new one-off class on the new date:**
> Create a new class for the day the student is actually coming.

**Step 3 — Mark the new class as paid:**
> If needed, enter the same amount manually so totals stay consistent.

There is no single "transfer payment" button/endpoint.
In practice, reassignment is done as: old class payment back to Unpaid, then new class to Paid.

---

### For package students

Package classes work a bit differently because the payment is a slot, not a money amount.

**Step 1 — Cancel the original class, releasing the payment:**
> Mark the old class as Cancelled and choose "release payment" — this gives the package
> slot back (remaining count goes up by 1).

**Step 2 — Add a new one-off class on the new date.**

**Step 3 — Mark the new class as paid:**
> The app will deduct one slot from the package again.

This way the package count stays accurate throughout.

---

### Important note about moving payments

There is no direct payment-transfer action. To reassign payment, unpay/cancel payment on the
old class and then mark the new class as paid.

---

## 9. Payers (Someone Else Pays)

Sometimes it's not the student who pays — for example, a parent pays on behalf of their
child. You can add one or more **payers** to a student's profile.

A payer record stores:
- Their full name
- Phone number
- A note (e.g. "Mother", "Father", "Sponsor")

Payers are just contact information — they don't affect how payments are recorded or
calculated.

---

## 10. Earnings

The app can show you how much money you've earned.

### Daily view (selected period)

Shows a selected period (for example, a week) with:

- **Daily breakdown** day by day (only individually paid classes, status = Paid)
- **Total earned** for the whole selected period
- **Total could have earned (excluding cancellations)**
- **Total could have earned (including cancellations)**

Package classes are still not counted in each day row. But package **purchases** are
included in period totals when their payment date is inside the selected date range.

If you want a weekly report, just request a 7-day range.

### Monthly view

Shows a summary for a whole month.

- Includes both **individual class payments** and **package purchases made that month**
- Example: if a student bought a 10-class package in April, the full package amount
  counts toward April's earnings — even if some of those classes are taken in May

### Multiple currencies

If you have students paying in different currencies (e.g. some in Euros, some in Rubles),
the earnings are shown broken down by currency. You can also ask for a single total
converted into one currency.

---

## 11. Deleting a Student

If a student leaves, you can **delete their profile**. This is a "soft delete" — the
student is hidden from your lists, but their history is not erased.

**What gets hidden:**
- The student's profile
- Their weekly schedule slots
- Their payer contacts
- Any **future** classes that hadn't happened yet

**What is kept (for your records):**
- All **past classes** — so you have a history of lessons that took place
- All package purchases — so your financial records stay intact

You will not be able to find or edit the student after deletion, but the historical data
remains in the system for reporting purposes.

---

## 12. Currencies

The app supports three currencies:
- 🇪🇺 **Euros (EUR)**
- 🇺🇸 **Dollars (USD)**
- 🇷🇺 **Rubles (RUB)**

Each student has their own currency — this is used when recording prices and payments.

Whenever you look up a student's price, a class charge, or a package payment, the app
also shows the **equivalent in the other two currencies** automatically. These conversion
rates are updated every hour from an online source.

If the internet is unavailable, the app uses the most recently fetched rates. If it has
never fetched rates before and the internet is down, the converted amounts simply won't
show — but all the original values are still there.

---

## 13. Common Situations — Quick Answers

**"The student cancelled with short notice — I still want to charge them."**
> Cancel the class and choose "keep as paid." The payment stays. If it was a package
> class, the slot is not returned.

**"The student cancelled in advance — they should not be charged."**
> Cancel the class and choose "release payment." For package students, the slot is
> returned automatically. For per-class students, the payment goes back to Unpaid.

**"The student wants to move their Monday class to Wednesday."**
> Cancel Monday, create a new class on Wednesday, then mark Wednesday as paid
> (with the same amount if needed). See section 8 for the full steps.

**"I marked a class as paid by mistake."**
> Undo the payment — it goes back to Unpaid. If it was a package class, the slot is
> returned to the package.

**"The student's package is empty but they want to book more classes."**
> Record a new package purchase for them, then mark new classes as paid normally.

**"I have two packages for the same student — which one gets used?"**
> The oldest one (the one with the earliest purchase date) is always used first.

**"I want to see what classes haven't been paid yet."**
> Filter a student's classes by payment status "Unpaid" to see everything outstanding.

**"When does someone become a debtor?"**
> If a class has already happened and is still unpaid, the student is marked as debtor.
> This check runs after 10:00 PM local time and also once on app startup.

**"When does debtor status go back to normal?"**
> As soon as all happened unpaid classes are paid, the next batch check switches debtor off.

**"A student who pays per-class wants to switch to packages."**
> Update their profile to change the pricing type. Old paid classes stay as they are.
> New payments will use the package system.

**"Can I record a package in a different currency than the student's usual one?"**
> Yes — when recording a package purchase you can specify any currency. If you leave
> it blank, it uses the student's default currency.

**"The price shown in other currencies looks wrong."**
> The conversion rates update every hour. If the internet was down, the rates might be
> slightly out of date — but they'll refresh automatically next time connectivity is restored.

**"I installed a new app version and my new DB is empty. Can I transfer everything?"**
> Yes. Export a snapshot from the old app (`GET /api/data/export`) and import it into the new
> app (`POST /api/data/import` or `POST /api/data/import-file`). The import replaces current data
> with the snapshot.

