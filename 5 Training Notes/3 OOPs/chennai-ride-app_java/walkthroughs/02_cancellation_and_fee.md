# Walkthrough 02: Cancellation grace period and the carried-forward fee

**Replay:** `./build.sh --cli < walkthroughs/02_cancellation_and_fee.txt`
**Mode:** Manual captain. Priya books bikes Velachery → Guindy; Karthik is the captain.

| Step | What happens | What to point out |
|---|---|---|
| 1. Clock → 08:15, Priya books a bike (UPI) | OTP 8050, offer to Karthik | — |
| 2. Karthik accepts | Ride `CAPTAIN_ASSIGNED` at 08:15 | The 2-minute grace period starts at **assignment** (`Ride.getAssignedAt`). |
| 3. Priya → Cancel ride | "Cancelling now is free." → `Fee: ₹0.00` | The menu asks `RideService.cancellationFeeIfCancelledNow`. The CLI never calculates fees itself. |
| 4. Book again, Karthik accepts | New ride RIDE-5002, OTP 1626 | Each ride has its own OTP from a seeded `Random`, so replays always match. |
| 5. Clock → advance 3 minutes | 08:18 AM | Moving time by hand is how the rule is demonstrated. |
| 6. Priya → Cancel ride | ✘ warning: "grace period is over… ₹20.00 fee will be added to your next ride", then `Fee: ₹20.00` | Bike fee ₹20, auto/cab ₹30: `VehicleType.getCancellationFee()` (an **enum with fields**). |
| 7. View profile | `Pending fee : ₹20.00` | The fee is an `OutstandingFee` owed to **Karthik**, the captain who lost his time. |
| 8. Book the next ride | The confirmation line mentions "plus ₹20.00 previous cancellation fee" | — |
| 9. Karthik accepts, arrives, starts (OTP 7186), completes | Receipt line **`7. Previous cancellation fee (RIDE-5002) ₹20.00`**, total ₹58.63 | `Customer.takeOutstandingFees()` hands the fee over **exactly once** and empties the list. |
| 10. Profile + Ride history | Pending fee back to ₹0.00; history shows both CANCELLED rides and the COMPLETED one | History is chronological and keeps cancelled rides. |

**Try live:** Karthik → Mark arrived, then have Priya cancel *inside* 2 minutes: she still pays, because the captain has arrived.
