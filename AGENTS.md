# Instructions for coding agents

> **Language notice:** The application itself (all user-facing text) is in Azerbaijani. This documentation is written in English.

## Project
- A personal, offline Android application that stores laundry operations for restaurant and company customers.
- Use Kotlin, Jetpack Compose, and Room. Do not add cloud, analytics, or authentication systems.
- Read `docs/ARCHITECTURE.md` and `docs/DESIGN.md` before changing the app's behavior.
- All text visible in the application must be in Azerbaijani.

## Product rules
- A customer has a name, optional contact information and note, and active/archived status.
- On the customer page, history and each income/expense operation are shown separately. Each operation stores customer, date, amount, direction, optional reason, and kilograms.
- Any number of operations can be added, edited, and deleted for the same customer.
- Money is stored as an integer in qəpik; the screen shows two-digit AZN with `₼`.
- The calculator works like a simple daily calculator. The result can be sent to a new operation by selecting a customer and date. An amount from history can be loaded into the calculator and that operation updated.
- Summary and calendar totals are based on the device's local day, week, and month boundaries. Archiving keeps the customer's operations. Permanent deletion of a customer must confirm deletion of both the customer and their operations.
- Backups must include customers, operations, and application settings. Restore is performed from the user-selected file after confirmation.

## Engineering
- Keep field rules as separate as possible from the Compose UI and database.
- Use fixed IDs and explicit date values. Write Room migrations that preserve data when the schema changes; do not silently drop the database.
- Do not add unnecessary dependencies. Internet permission is not required.
- Keep README build, install, and update instructions up to date. When updating the APK, the app ID and signing key must remain the same.
- Do not commit secret keys, signing files, local SDK paths, or generated build files to Git.
