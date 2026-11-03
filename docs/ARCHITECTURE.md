# Architecture

> **Language notice:** The application itself (all user-facing text) is in Azerbaijani. This documentation is written in English.

## Overview

This application is designed for use by one person and works offline. All business data is stored in the Room database on the phone. There is no account, server, analytics, or network connection.

## Application layers

1. **Compose UI** — summary, customers, customer operations, separate expenses section, calendar, simple calculator, and settings.
2. **Business rules** — AZN/qəpik conversion, calculator calculations, day/week/month boundaries, and backup validation.
3. **Data storage** — Room stores customers, dated operations, separate expenses, and settings. Schema changes must be done with migrations that preserve data.
4. **Backup** — Versioned JSON is written to the file selected by the user via Android Storage Access Framework, and restored from the file.

## Core data

- **Customer:** fixed ID, name, optional contact information and note, changeable rate for kg, active/archived status.
- **Operation:** fixed ID, customer ID, local calendar date (`yyyy-MM-dd`), service charge/payment direction, amount in qəpik, applied rate, optional reason, kilograms, or separate meters and meter rate for each curtain job.
- **Expense:** fixed ID, name, local calendar date (`yyyy-MM-dd`), amount in qəpik.
- **Application settings:** Azerbaijani language and backup format version.

Each customer can have multiple operations. Amount is always stored as a non-negative integer in qəpik. In a service charge operation, if kilograms and the customer's rate exist, the amount is calculated as kilograms × rate and the rate amount is stored in that operation. In curtain debt, meter and separately entered meter rate for each record are multiplied; the rate does not change the customer's permanent rate and is stored in the operation. The customer's outstanding balance is found by subtracting the sum of payments from the sum of service charges. Total income/expense sums are stored in the application.

## Totals

Operations and expenses are grouped according to the device's local calendar date. Week starts from the first day of the local week. Summary shows the selected day and month, or the entire history; calendar shows the selected day, week, and month. Period totals include service charge, payment, balance, kg, and curtain meters. Balance is calculated by subtracting the sum of payments from the sum of service charges. The Expenses section shows expense records for the selected day or month and the total for that period. The customer list shows each customer's current outstanding balance. From the customer page, operation history can be deleted separately with confirmation; this keeps the customer's data and rate. Archiving the customer preserves history. Permanent deletion is confirmed separately and deletes that customer's operations too.

## Navigation and dependencies

The bottom menu has Summary, Customers, Expenses, Calendar, Calculator, and Settings. On the customer page, the operation journal is located. Network permission and background service are not required. The project should build easily in Android Studio and create a directly installable APK.
