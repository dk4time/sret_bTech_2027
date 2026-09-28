# Walkthrough 3 — Low wallet, UPI failure, PAYMENT_PENDING, then cash

```bash
python run.py --cli < walkthroughs/03_payment_pending.txt
```

**Goal:** show polymorphic payments (`Payable.pay()`), the all-or-nothing wallet, the
PAYMENT_PENDING state, and the rule that an unpaid ride blocks new bookings.

| Step | Input | What happens | What to point out |
|---|---|---|---|
| 1 | `4` `3` `19:00` `0` | Clock → 7:00 PM | Evening peak. |
| 2 | `1` `5` `1` | Customer app → Divya → profile | Wallet **₹200.00**, preferred payment Wallet. |
| 3 | `4` … `y` `0` | Book Cab Economy, Anna Nagar → Guindy | Estimate ₹329.96 — more than her wallet. The app still lets her book (payment happens at the end). |
| 4 | `2` `10` `3` `a` `4` `5` `2951` `6` | Rajesh accepts, arrives, starts, completes | The receipt prints, then ✘ **InsufficientWalletBalanceError: required ₹329.96, available ₹200.00**. The ride is now **PAYMENT_PENDING**. |
| 5 | `0` `1` `5` `1` | Divya's profile | *Payment pending: RD-1001 — ₹329.96*. Her wallet is still ₹200.00: **never partially deducted**. |
| 6 | `9` `2` | Pay pending ride → UPI | ✘ **PaymentFailedError** — her bank's UPI server is down (seeded: this account always fails tonight). Still PAYMENT_PENDING. |
| 7 | `4` | Try to book a new ride | ✘ **PaymentPendingError** — blocked by `Customer.ensure_can_book()`. |
| 8 | `9` `3` | Pay pending ride → Cash | ✔ *Paid ₹329.96 via Cash. RD-1001 is COMPLETED.* Rajesh's dues rise (cash). |
| 9 | `4` … `y` | Book a Bike, Guindy → Saidapet | ✔ The booking now works (RD-1002). |
| 10 | `1` `0` `0` | Profile, leave | Payment pending: No. |

**OOP point:** `PaymentService.pay_ride()` calls `payable.pay(amount)` three times with three
different classes — `WalletPayment`, `UpiPayment`, `CashPayment` — and never checks which one it has.
Try paying the same ride again from code: `DuplicatePaymentError`.
