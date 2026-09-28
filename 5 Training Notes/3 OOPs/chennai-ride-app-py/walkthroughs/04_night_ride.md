# Walkthrough 4 — Night Cab Premium from the Airport with FIRSTRIDE (Auto captain mode)

```bash
python run.py --cli < walkthroughs/04_night_ride.txt
```

**Goal:** show the night charge, the coupon, and Auto captain mode (you act only as the customer).

| Step | Input | What happens | What to point out |
|---|---|---|---|
| 1 | `4` `3` `23:30` `1` `0` | Clock → 11:30 PM, show time | *night (+20%): yes*. The night window is 11:00 PM–5:00 AM (`SimulatedClock.is_night()`). |
| 2 | `5` `2` | Settings → Auto | Status line: `Mode: Auto captain`. |
| 3 | `1` `4` `4` | Customer app → Lakshmi → Book | |
| 4 | `airport` | Pickup = Chennai Airport (typed part of the name) | Lakshmi was at Chromepet since 6 AM, so `Customer.relocate_to()` allows the move (enough time has passed). Try this at 6:05 AM and it is refused — no teleporting. |
| 5 | `anna nagar` `4` Enter `1` `FIRSTRIDE` `y` | Drop Anna Nagar, Cab Premium, 1 passenger, Wallet, coupon, confirm | The estimate already includes the night charge. |
| 6 | (automatic) | Vignesh accepts, drives over, starts with the correct OTP | Auto mode only **chains the same service calls** the Captain app makes by hand. |
| 7 | `5` `y` | Track → ride on to Anna Nagar | Receipt: **6. Night charge +₹86.68**, **8. Coupon FIRSTRIDE −₹50.00**, total ₹493.58, paid from the wallet. |
| 8 | `1` `0` `0` | Profile, leave | Book again with FIRSTRIDE → *"Lakshmi has already used coupon FIRSTRIDE."* |

**Boundary exercise:** repeat with the clock at `22:59` — the night charge disappears (the self-check
tests exactly this boundary).
