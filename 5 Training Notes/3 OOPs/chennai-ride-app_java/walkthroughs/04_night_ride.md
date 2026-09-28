# Walkthrough 04: Night ride with the FIRSTRIDE coupon

**Replay:** `./build.sh --cli < walkthroughs/04_night_ride.txt`
**Mode:** Auto captain.

| Step | What happens | What to point out |
|---|---|---|
| 1. Clock → jump to 23:30 | Status line `Night (+20%)` | Night = 11:00 PM to before 5:00 AM (`FareService.isNightTime`). Try jumping to 22:59 first to see "Off-peak". |
| 2. Settings → Auto captain | — | — |
| 3. Priya → Travel on your own → `airport` | "Priya is now at Chennai Airport (Meenambakkam)" | Customers move by events only: a ride drop, or travelling on their own (`Customer.travelOnOwnTo`, blocked during a ride). |
| 4. See fare estimates → Anna Nagar | Every row shows `+20%` in the Night column | — |
| 5. Book Cab Premium for **5** passengers, Wallet, coupon `FIRSTRIDE` | Balaji comes from Pallavaram and starts the trip | 5 people only fit a Cab Premium (6 seats). Try Cab Economy to see "5 passengers cannot ride a Cab Economy (max 4)". |
| 6. Clock → Advance to the next ride event | Receipt: **`6. Night charge ₹66.25`**, **`8. Coupon FIRSTRIDE − ₹50.00`**, total ₹364.90 | The coupon is applied only when the fare is at least ₹100, and never takes it below the ₹150 minimum. |
| 7. View profile | `FIRSTRIDE : already used` | Booking with FIRSTRIDE again is rejected: once per customer. |

**Try live:** Operations → Surge status at hotspots at 23:30 vs 09:00 (the hotspot peak surge disappears at night).
