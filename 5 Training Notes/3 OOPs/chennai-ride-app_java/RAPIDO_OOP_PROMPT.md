# Build Prompt: Chennai Ride-Hailing App (Rapido-style) — Pure OOP in Java

Build a Rapido-style bike/auto/cab ride-hailing application in plain Java. It will be used to teach OOP to final-year engineering students who already know the theory. It must behave like a REAL app running a REAL day in Chennai: every object's state must stay consistent across the whole day.

---

## 1. Strict scope

- **Core OOP only**: classes/objects, encapsulation, abstraction, inheritance, polymorphism, composition. Do not use or mention design patterns or SOLID.
- **Strictly no frameworks or libraries**: no Spring, Maven, Gradle, JUnit, Lombok — nothing outside the JDK. Java 17 standard library only.
- Must compile and run with plain `javac` and `java`. Provide `build.sh` and `build.bat` that compile into `out/` and run the demo (with an option to run the self-check).
- Production-looking code: proper packages, validation, custom exceptions, meaningful names, realistic data. But readable enough to walk through in a classroom — no reflection, no clever stream one-liners, explicit logic students can trace.
- Use `BigDecimal` for money (INR, 2 decimals, HALF_UP), `java.time` for time.
- **Deterministic**: any randomness (OTP, UPI failure) uses a `java.util.Random` with a fixed seed, so the demo prints the same output every run. This matters for teaching.

**Terminology (same as the real Rapido app):** the person who books is the **Customer**. The person who drives is the **Captain**.

---

## 2. Package structure

```
com.ridehailing
  ├── model/
  │   ├── common/     Location, Money (final, immutable), ServiceArea, ChennaiPlaces (named locations)
  │   ├── user/       User (abstract), Customer, Captain, CaptainStatus enum
  │   ├── vehicle/    Vehicle (abstract), Bike, Auto, Cab (abstract), CabEconomy, CabPremium, VehicleType enum
  │   ├── ride/       Ride, RideStatus enum, RideRequest, FareEstimate, FareReceipt, CancellationReason enum
  │   └── payment/    Payable (interface), UpiPayment, CashPayment, WalletPayment, PaymentStatus enum
  ├── notification/   Notifier (interface), SmsNotifier, PushNotifier
  ├── service/        CustomerService, CaptainService, MatchingService, RideService, FareService,
  │                   PaymentService, RatingService, EarningsService
  ├── time/           SimulatedClock
  ├── exception/      RideHailingException (base) + specific exceptions (see section 7)
  ├── check/          SelfCheck
  └── app/            ChennaiRideApp (main — the simulated day)
```

---

## 3. Chennai setting (realistic)

- **Named locations with accurate lat/long:** T. Nagar, Anna Nagar, Adyar, Velachery, Guindy, Mylapore, Egmore, Chennai Central, Chennai Airport (Meenambakkam), Sholinganallur (OMR), Porur, Vadapalani, Besant Nagar, Koyambedu, Tambaram, Chromepet, Pallavaram, Guduvanchery, Medavakkam, Perungudi, Thiruvanmiyur, Ashok Nagar, KK Nagar, Kilpauk, Royapettah, Washermanpet.
- **Service area:** Chennai metro bounding box. Pickup or drop outside it is rejected.
- **TN registration numbers:** TN-09-AB-4521, TN-22-CK-7813, TN-07-BX-1190, etc.
- **Vehicles:** Honda Activa, TVS Jupiter, Bajaj RE Auto, TVS King, Maruti Dzire, Hyundai Aura, Toyota Innova Crysta.
- **People:** Tamil names (Murugan, Karthik, Selvi, Priya, Arun, Lakshmi, Senthil, Divya…) with +91 numbers.
- **Chennai-flavoured messages:** e.g. "Your captain Murugan (TN-09-AB-4521, Bajaj RE Auto) is 4 mins away near Kathipara junction. OTP: 4821".
- **Chennai traffic:** peak hours are 8–11 AM and 5–9 PM. During peak, average speed drops, especially on OMR, T. Nagar and Koyambedu. High-demand hotspots are Chennai Central, Koyambedu and the Airport.

---

## 4. OOP features that must appear naturally

Add a short comment wherever each one is demonstrated:

- Classes & objects, constructor overloading with `this()` chaining, `super()` calls
- **Encapsulation:** private fields, no public setters on model classes. State changes only through intent-revealing methods (`captain.goOnline()`, `ride.start(otp)`), with validation inside those methods.
- **Abstraction:** abstract classes (User, Vehicle, Cab) and interfaces (Payable, Notifier)
- **Inheritance:** multilevel (Vehicle → Cab → CabPremium) and hierarchical (User → Customer / Captain)
- **Polymorphism:**
  - overriding: fare rates, `toString`
  - overloading: e.g. `bookRide(...)` with and without a payment method
  - upcasting: `Vehicle v = new Auto(...)`
  - dynamic dispatch through `Payable` and `Notifier` references
- **Composition vs aggregation:** Captain HAS-A Vehicle; Ride references a Customer and a Captain
- `static` members (id counters, GST constant), `final` fields and `final` classes
- `equals()` / `hashCode()` / `toString()` overridden properly (entities by id, value objects by value)
- Custom exceptions with a base class, with a clear checked vs unchecked choice
- Enums with fields, constructors and methods (e.g. `VehicleType` with display name and seat count)

---

## 5. Vehicles & fares

- The abstract `Vehicle` declares abstract methods: base fare, per-km rate, per-minute rate, minimum fare, seat capacity, average speed (normal and peak).
- Each subclass overrides these with realistic Chennai INR values. Bike is cheapest, then Auto, CabEconomy and CabPremium.
- `Vehicle` has a concrete `calculateBaseFare(distanceKm, minutes)` method that uses the overridden rates. This is the central polymorphism example.
- Seats: Bike 1, Auto 3, CabEconomy 4, CabPremium 6. A booking for more passengers than the seat count is rejected.
- A captain's vehicle type never changes. A bike captain NEVER receives an auto or cab request.

---

## 6. Complete real-world flows (VERY IMPORTANT)

Every flow below must work exactly like the real app. **Core principle:** no teleporting, no double-booking, no time travel, and no money appearing or disappearing.

### A. Captain lifecycle

1. **Registration:** A captain registers with a vehicle and starts as KYC_PENDING. They cannot go online until verified.
2. **Online/offline:** A verified captain goes online at a real location. A captain CANNOT go offline during an active ride, only after it ends. Offline captains are never matched.
3. **Location continuity:** A captain's location changes ONLY through events:
   - When assigned, they travel from their current location to the pickup.
   - When the ride completes, their location becomes the actual drop point.
   - **Canonical example:** Captain drops a customer at Tambaram after an Egmore → Tambaram ride. Their location is now Tambaram. A new request from Egmore must NEVER go to them. A request from Chromepet or Pallavaram (within radius) can.
4. **One ride at a time:** A captain is not matchable from assignment until completion or cancellation.
5. **Accept/reject:** A request is offered to the nearest eligible captain first. The captain may accept or reject (simulated deterministically).
   - On a reject, the offer moves to the next-nearest eligible captain.
   - A captain who rejected THIS ride is never re-offered it.
   - After 3 offers are rejected, or no eligible captain is left, the result is "No captains available".
   - Track each captain's acceptance rate.
6. **Arrival:** `markArrived()` is only allowed when the captain's location is within about 100 m of the pickup. The simulation moves them there first.
7. **Waiting:** The first 3 minutes after arrival are free. After that, a waiting charge of ₹1 per minute is added to the fare.
8. **Customer no-show:** If the customer doesn't show after 5 minutes of waiting, the captain may cancel as NO_SHOW. The customer is charged a no-show fee, and the captain gets that fee (minus commission). The captain becomes free at the pickup location.
9. **Captain cancels after accepting:** The ride goes back to matching (excluding this captain), and the customer is NOT charged. The captain's cancellation count increases and they stay at their current location.
10. **Cash commission limit:** For cash rides, the captain collects the cash and owes the platform 20% commission. If the total owed exceeds ₹500, the captain is blocked from receiving CASH rides until they settle. Include a `settleDues()` method.
11. **Rating flag:** A captain whose average rating falls below 4.0 (after at least 5 ratings) is flagged.

### B. Customer lifecycle

1. **Registration and wallet:** A customer registers with a wallet balance and can top up. Top-up amounts must be positive and at most ₹10,000 per transaction.
2. **Fare estimates:** Given pickup and drop, the app returns estimates for ALL vehicle types side by side, like the real app screen. Each shows the fare, the ETA of the nearest captain, and whether any captain is available at all.
3. **Booking validation:** Reject the booking if:
   - pickup equals drop (or is under 500 m away)
   - either point is outside the service area
   - the trip is over 60 km
   - there are too many passengers for the vehicle
   - the customer already has an active ride
   - the customer has a PAYMENT_PENDING ride
4. **Location continuity:** After a ride completes, the customer's location becomes the drop point. Their next booking in the demo starts from there, or from a realistic place after enough simulated time has passed.
5. **Cancellation rules:**
   - Before a captain is assigned: free.
   - Within 2 minutes of assignment: free (grace period).
   - After the grace period, or after the captain has arrived: ₹20 (bike) / ₹30 (auto/cab) fee, added to the customer's NEXT ride as "Previous cancellation fee".
   - Once IN_PROGRESS, the ride cannot be cancelled. The customer can only end it early (see 6).
6. **End trip early:** The customer ends the ride mid-way. The fare is based on the ACTUAL distance and time travelled, and the drop location becomes the actual stop point. Both captain and customer locations update to that point.
7. **Change destination mid-ride:** Allowed once. The final fare uses the actual route (pickup → change point → new drop), and the new drop becomes the location for both.
8. **One active ride only:** A second booking while a ride is active throws an exception.
9. **Rating:** Only COMPLETED rides can be rated, once per side, with a score of 1–5. Cancelled rides cannot be rated.
10. **History:** Each customer keeps their ride history in chronological order, cancelled rides included.

### C. Ride state rules

The `RideStatus` values are: REQUESTED → CAPTAIN_ASSIGNED → CAPTAIN_ARRIVED → IN_PROGRESS → COMPLETED, plus CANCELLED and PAYMENT_PENDING.

- `Ride` guards its own status. There is no `setStatus()`, only methods like `assignCaptain()`, `markArrived()`, `start(otp)`, `endEarly()`, `changeDestination()`, `complete()`, `cancel(reason, by)`.
- Every method validates the current status and throws `InvalidRideStatusException` for any illegal call (e.g. starting a cancelled ride, completing a ride that never started).
- **OTP:** a 4-digit OTP is generated at booking and stored privately. `start(enteredOtp)` checks it and allows at most 3 wrong attempts. After the third wrong attempt, the ride is auto-cancelled as OTP_FAILED with no fee.
- Every status change records a timestamp from the SimulatedClock, and prints the full timeline on request.

### D. Fare & receipt

The `FareReceipt` itemises:

1. Base fare
2. Distance charge
3. Time charge
4. Waiting charge
5. Surge (1.0x–2.0x)
6. Night charge (+20% between 11 PM and 5 AM)
7. Previous cancellation fee
8. Coupon discount
9. GST 5%
10. Total

Rules:
- **Surge** is based on demand vs supply near the pickup: open requests vs free captains within radius. It is higher at the hotspots during peak hours, capped at 2.0x, and shown to the customer BEFORE confirming.
- **Coupon:** one simple flat coupon (e.g. FIRSTRIDE ₹50 off, minimum fare ₹100, once per customer). The total can never go below the minimum fare.
- **Final vs estimate:** The final fare is recalculated on actual distance and time, so it may differ from the estimate. Show both on the receipt.
- The customer pays only for pickup → drop. The captain's travel to the pickup is never billed.
- `toString()` prints a clean, realistic receipt.

### E. Payments

- `Payable` has `pay(Money)`, implemented by UpiPayment, CashPayment and WalletPayment.
- The customer chooses a method at booking and may switch at the end of the ride.
- **Wallet:** if the balance is insufficient, throw `InsufficientWalletBalanceException`. The balance is never deducted partially.
- **UPI:** can fail (deterministic simulation). On failure, the ride becomes PAYMENT_PENDING. The customer can then retry UPI or switch to cash, and once paid the ride moves to COMPLETED.
- **Cash:** always succeeds; the captain's commission owed increases.
- A ride's payment can never succeed twice.

### F. Earnings & money reconciliation

- Platform commission is 20% of the fare before GST.
- `EarningsService` tracks, per captain per day: rides, gross fare, commission, net earnings, cash collected and commission owed.
- **End of day, the books MUST balance:** the total paid by customers = captain earnings + platform commission + GST collected. SelfCheck verifies this.

### G. SimulatedClock

- All time comes from `SimulatedClock`. `LocalDateTime.now()` is never used.
- The demo advances the clock realistically: captain travel to pickup, waiting, and ride duration.
- A captain who finishes at 9:40 AM cannot start another ride before 9:40 AM.
- The clock never goes backward.

### H. Notifications

- `Notifier` is an interface implemented by SmsNotifier and PushNotifier. `RideService` holds a `List<Notifier>` and notifies on every event.
- Events: booked, captain assigned (with name, vehicle, plate, ETA, OTP), captain arrived, ride started, destination changed, ride completed (with fare), payment success/failure, cancelled (with reason and any fee).

---

## 7. Exceptions

- `RideHailingException` is the base class.
- Specific exceptions: `InvalidRideStatusException`, `InvalidOtpException`, `NoCaptainAvailableException`, `ActiveRideExistsException`, `PaymentPendingException`, `InsufficientWalletBalanceException`, `OutOfServiceAreaException`, `InvalidBookingException`, `CaptainNotEligibleException`, `CaptainBusyException`.
- Choose checked vs unchecked deliberately, and explain the choice in a comment.

---

## 8. Demo — a full simulated day (ChennaiRideApp.main)

Seed 5 customers and 12 captains spread realistically across Chennai: a mix of bikes, autos and cabs, with one captain still KYC-pending.

Print a clean, story-like console output with a timestamp on every line, and a **Captain Position Board** (captain, vehicle, location, status, rides done, earnings) at key moments.

Scenes, in chronological order:

1. **6:00 AM:** Captains come online at their home areas. The KYC-pending captain tries to go online and is rejected.
2. **8:15 AM:** Priya compares estimates for all vehicle types, Velachery → Guindy (peak hour), then books a bike and pays by UPI. Show the full receipt.
3. **8:40 AM:** A customer books an auto, Chennai Central → Egmore, with peak surge and cash payment. The captain's commission owed increases.
4. **9:10 AM (canonical flow):** A cab ride, Egmore → Tambaram.
   - Print the captain's location before and after.
   - Next, a request from Egmore goes to a DIFFERENT captain near Egmore.
   - Then a request from Chromepet goes to the captain now at Tambaram.
5. **10:00 AM:** The nearest captain rejects a request, the next-nearest accepts. Show the offer sequence.
6. **10:30 AM:** A customer with an active ride tries to book again and is rejected.
7. **11:00 AM:** A customer cancels within the 2-minute grace period (free). Another customer cancels after the grace period, and the fee appears on their next ride's receipt.
8. **12:30 PM:** A captain arrives, waits 4 minutes (1 minute charged), then starts the ride.
9. **1:00 PM:** Customer no-show. The captain cancels after 5 minutes and the fee is applied.
10. **2:00 PM:** One wrong OTP, then the correct one. In a separate ride, 3 wrong OTPs auto-cancel the ride.
11. **3:30 PM:** The captain accepts and then cancels. The ride is re-matched to another captain, and the customer is not charged.
12. **5:45 PM:** A customer changes destination mid-ride and the fare uses the actual route.
13. **6:30 PM:** A customer ends the trip early. The partial fare applies, and both locations update to the stop point.
14. **7:00 PM:** A wallet payment fails on low balance. UPI then fails and the ride is PAYMENT_PENDING. The customer's new booking is blocked, then they pay cash, the ride completes, and the booking succeeds.
15. **8:00 PM:** A captain's cash commission owed crosses ₹500, so they are blocked from cash rides. They settle dues and are unblocked.
16. **9:00 PM:** No captain is available near Sholinganallur for a CabPremium request.
17. **11:30 PM:** A CabPremium ride, Chennai Airport → Anna Nagar, with night charge and the FIRSTRIDE coupon, paid by wallet.
18. **A booking outside the service area** (e.g. Mahabalipuram) is rejected.
19. **A captain tries to go offline mid-ride** and is rejected, then goes offline after the ride.
20. **Ratings:** ratings for completed rides, a rejected attempt to rate a cancelled ride, and a captain flagged below 4.0.
21. **End of day:**
    - Captain earnings table
    - Ratings leaderboard
    - Each customer's ride history
    - The final Captain Position Board
    - A **Books Balanced ✔** reconciliation summary

---

## 9. SelfCheck (plain Java, no JUnit)

Write `com.ridehailing.check.SelfCheck` with a `main` method and a private `check(String name, boolean condition)` helper that prints PASS/FAIL and a final count. Verify:

- Fare per vehicle type; minimum fare applied; night charge window boundaries (10:59 PM vs 11:00 PM)
- Every valid status transition works, and every invalid one throws
- The OTP 3-attempt rule
- The captain's location equals the last drop point (including end-early and destination change)
- A captain outside the radius, busy, offline, of the wrong vehicle type, or who rejected THIS ride is never matched
- The Egmore → Tambaram canonical case
- The cancellation grace period vs fee; the fee carries to the next ride exactly once
- One active ride per customer; payment pending blocks booking
- No partial wallet deduction; no double payment
- The cash commission ₹500 block and unblock
- The clock never goes backward, and a captain's next ride never starts before their previous one ended
- End-of-day money reconciliation balances to the paisa

---

## 10. Deliverables

- `build.sh` / `build.bat` — compile and run with only the JDK
- `README.md` — setup, a Mermaid class diagram (inheritance + composition), and a Mermaid state diagram of the ride lifecycle
- `docs/OOP_CONCEPT_MAP.md` — a table mapping each OOP concept in section 4 to the exact class/method where it appears, with one line on why. This is the teaching script.
- `docs/FLOWS.md` — each flow in section 6, with the class and method that enforces it

---

## 11. How to work

1. Build in this order, compiling after every step:
   common → vehicles → users → clock → ride → fare → matching → payments → notifications → services → demo → self-check → docs.
2. After finishing, run the demo and SelfCheck, and fix anything until SelfCheck is 100% PASS and the books balance.
3. **Do NOT build Rapido Parcel.** It will be used in class as a live exercise, where students add it by extending existing classes.
