# Walkthrough 01: Egmore → Tambaram (the canonical case)

**Replay:** `./build.sh --cli < walkthroughs/01_egmore_to_tambaram.txt` (Windows: `build.bat --cli < walkthroughs\01_egmore_to_tambaram.txt`)
**Mode:** Manual captain (default). You play both the customer and the captain.
**Lines starting with `#`** in the `.txt` file are narration. They are printed during replay and never answered.

| Step | What happens on screen | What to point out to students |
|---|---|---|
| 1. Clock → jump to 09:10 | Status line turns to `Peak hour` | All time comes from one `SimulatedClock`, and it only moves forward. |
| 2. Customer app → Arun → Book a ride | Pickup defaults to **Egmore** (Arun's current location); `tam` finds Tambaram | Location continuity: a booking starts where the customer really is (`RideService.validateBooking`). |
| 3. Estimate table | All 4 vehicle types side by side with fare, surge and nearest-captain ETA | Same `FareService.estimate` → `Vehicle.calculateBaseFare` code for every type. That's **polymorphism**. |
| 4. Confirm | `Booked RIDE-5001 · OTP 8050`, then *Offer sent to Captain Senthil* | In Manual mode, `RideService.bookRide` calls `offerToNextCaptain`. The ride stays `REQUESTED` until the captain answers. |
| 5. Captain app → Senthil (● marker) → View offer → Accept | Customer gets "Your captain Senthil (TN-02-AR-3345, Maruti Dzire) is 1 mins away… OTP: 8050" | `respondToOffer` → `Ride.assignCaptain`. Nobody calls `setStatus`, because none exists (**encapsulation**). |
| 6. Mark arrived | Clock moves by the ETA | `Ride.markArrived` refuses before the ETA and further than 100 m away. No teleporting. |
| 7. Start ride, OTP 8050 | "OTP verified - ride started" | The OTP is private to `Ride`; only `revealOtpTo(customer)` shows it. Try a wrong one live to see "2 attempt(s) left". |
| 8. Complete ride | Clock moves 45 min, receipt, wallet pays ₹523.74 | Senthil's location is now **Tambaram**: `Captain.finishRide(actualDrop)` is the only way it changes. |
| 9. Operations → Nearby captains → **Egmore**, Cab Economy | Senthil `✘ too far (24.0 km at Tambaram)`; Rajesh `✔ eligible` | A new Egmore request can **never** go to Senthil: `MatchingService.ineligibilityReason`. |
| 10. Nearby captains → **Chromepet**, Cab Economy | Senthil `✔ eligible` (5.8 km) | The same captain *is* matchable near his real position. |
| 11. Captain position board | Senthil at Tambaram, 1 ride, ₹399.04 earned | Earnings = fare before GST − 20% commission (`EarningsService`). |

**Try live:** repeat step 9 with *Any vehicle* to see "wrong vehicle" reasons, and look at Mani's `✘ KYC pending`.
