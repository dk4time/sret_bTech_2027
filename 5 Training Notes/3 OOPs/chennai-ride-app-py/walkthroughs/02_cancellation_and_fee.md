# Walkthrough 2 — Cancellation grace period and the fee on the next ride

```bash
python run.py --cli < walkthroughs/02_cancellation_and_fee.txt
```

**Goal:** show that time decides the fee, and that a fee is carried to the next ride **exactly once**.

| Step | Input | What happens | What to point out |
|---|---|---|---|
| 1 | `4` `3` `11:00` `0` | Clock → 11:00 AM | Off-peak, so no surge. |
| 2 | `1` `1` `4` … `y` `0` | Priya books a Bike, Velachery → Guindy | OTP 2951. The offer waits for Arun (Manual mode). |
| 3 | `2` `3` `3` `a` `0` | Arun accepts at 11:00 | The 2-minute grace period starts at **assignment**, not at booking. |
| 4 | `1` `1` `6` `y` | Priya cancels straight away | *"Cancelling now is FREE."* The warning comes from `RideService.cancellation_fee_if_cancelled_now()`, the same method `cancel_by_customer()` uses. |
| 5 | `4` … `y` `0` / `2` `3` `3` `a` `0` | Book again (OTP 6234); Arun accepts | |
| 6 | `4` `2` `3` `0` | Clock → advance **3 minutes** | Manual clock control is how we show time-based rules. |
| 7 | `1` `1` `6` `y` | Cancel again | ✘ *"The 2-minute grace period is over: a ₹20.00 fee will be added to your next ride."* Bike = ₹20 (it comes from `VehicleType.BIKE.cancellation_fee`). |
| 8 | `1` | Profile | **Pending cancellation fee ₹20.00**. Nothing has been charged yet. |
| 9 | `4` … `y` `0` / `2` `3` `3` `a` `4` `5` `9233` `6` | Book a third time; Arun accepts, arrives, starts with OTP 9233, completes | The receipt line **"7. Previous cancellation fee ₹20.00"**; total ₹58.91. |
| 10 | `0` `1` `1` `11` | Priya's ride history | Both cancelled rides are listed in order, one with `fee ₹20.00`. Book again and the fee is gone — it is billed **exactly once** (`Customer.take_pending_fees()` empties the list). |

**Money question:** who receives the ₹20? Arun — the captain who was cancelled on — gets it minus 20%
commission, but only once Priya actually pays it (`EarningsService.record_ride_payment()` →
`ride.carried_fees`). Check it in Operations → 6 (Earnings summary).
