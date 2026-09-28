# Walkthrough 03: Payment pending blocks booking

**Replay:** `./build.sh --cli < walkthroughs/03_payment_pending.txt`
**Mode:** switched to **Auto captain** (Settings → 2). Harish has only ₹60 in his wallet, and his bank (HDFC) has a UPI outage from 6:45 to 7:45 PM.

| Step | What happens | What to point out |
|---|---|---|
| 1. Clock → 19:00, Settings → Auto captain | Status line `Mode: Auto captain` | Auto mode = `DispatchMode.AUTOMATIC` + `RideAutopilot`. The same objects as the scripted demo. |
| 2. Harish → View profile | Wallet ₹60.00 | — |
| 3. Book Cab Premium Chromepet → Tambaram, **Wallet** | Balaji accepts, drives over and starts on his own | Only Cab Premium has a captain nearby (the estimate table shows ✘ for the rest). |
| 4. Clock → Advance to the next ride event | Trip ends; "Wallet payment failed: balance ₹60.00 is less than ₹252.27"; receipt followed by ✘ `PAYMENT_PENDING` | `WalletPayment.pay` throws `InsufficientWalletBalanceException` (**checked**). The wallet is never partly debited. |
| 5. Pay pending ride → Retry UPI | "UPI to harish.m@okhdfcbank failed: bank server not responding" → still PAYMENT_PENDING | `UpiPayment` fails deterministically during the outage window (`PaymentService.reportUpiOutage`). |
| 6. Book another ride | ✘ "Harish must first pay ₹252.27 for RIDE-5001" | `PaymentPendingException` IS-A `InvalidBookingException` (exception **inheritance**). |
| 7. Pay pending ride → Cash | "RIDE-5001 is now COMPLETED" | `CashPayment` always succeeds; Balaji's cash dues go up. |
| 8. Book Tambaram → Chromepet (cash) and advance the clock | Booking now works; second receipt, paid in cash | The same `Payable.pay(...)` call ran three different classes: **dynamic dispatch**. |
| 9. Ride history | RIDE-5001 COMPLETED, RIDE-5002 COMPLETED | — |

**Try live:** Operations → Earnings summary → **Books Balanced ✔**, then Captain app → Balaji → Today's earnings to see "Cash collected" and "Dues added".
