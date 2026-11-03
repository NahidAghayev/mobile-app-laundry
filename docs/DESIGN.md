# Product and UI Design

> **Language notice:** The application itself (all user-facing text) is in Azerbaijani. This documentation is written in English.

## Goal

Quickly record laundry work for restaurants and companies on the phone: find the customer, write the income/expense amount and reason, optionally add kilograms, then view the result for the selected period.

## Main menu

- **Summary:** **Day**, **Month**, and **All** filters. For day and month, the date is selected from the calendar; **All** shows all operations and the overall balance. Service charge, payment, balance, kg, and curtain meter totals are shown. Balance is calculated by subtracting payments from the sum of service charges; overpayment is shown separately.
- **Customers:** active and archived list, search, each customer's outstanding balance, new customer, and editing customer information including the kg rate.
- **Customer page:** contact information, kg rate, total weight and curtain meters, amount paid and outstanding balance; operations — add debt with kg, accept payment, and **Add curtain service**. In curtain debt, meters and a separate ₼/meter rate for that curtain are entered each time, the amount is calculated automatically, and the customer's permanent kg rate does not change. Operation history can be cleared with confirmation, and all operations can be edited/deleted.
- **Expenses:** select a date from the calendar and view expenses for the selected day or month and their total. When adding an expense, enter the name, AZN amount, and date selected from the calendar; the date is pre-filled with today.
- **Calendar:** monthly calendar, marking days with operations, and totals for the selected day and week/month.
- **Calculator:** normal number buttons and `+`, `−`, `×`, `÷`, percent, sign change, delete, and equals operations. The calculator result can be sent to an operation by selecting customer and date; an amount from history can be loaded into the calculator and that operation updated.

## Operation flow

1. Set the kg rate for the customer.
2. In the **Add debt** button, enter kilograms; the date field for the debt calculated by the rate is pre-filled with today. Choosing another date from the calendar and adding a note is optional. In the **Accept payment** button, enter the amount; the date is again pre-filled with today, changing from the calendar and writing a note is optional. Payment reduces the outstanding debt.
3. For curtain work, in the **Add curtain debt** button, enter the meter length and the meter rate suitable for that job; the amount is calculated as `meter × rate`. The rate is stored only for that operation and does not affect the customer's kg rate.
4. Record the expense in the **Expenses** section with the name and amount; the date is pre-filled with today and can be changed from the calendar. In the expenses list, select a date from the calendar and view expenses grouped by **Day** or **Month** and the total for the period.
5. After saving, the customer operation remains in the journal; any operation can be opened separately to edit or delete.
6. If the amount is calculated in the calculator, send the result to the customer. Loading an existing operation into the calculator does not create a duplicate; when the result is sent back, that operation is updated.

## Accessibility and interaction

- All text visible on screen must be in Azerbaijani.
- Use large touch targets, high contrast, readable text, and a keyboard suitable for entering numbers.
- AZN is shown with two qəpik digits and the `₼` sign; weight is shown in kilograms.
- In an empty list, provide a clear next step. Confirmation is required before deletion and backup restoration.
- Keep daily operations simple; place main buttons within thumb reach.
