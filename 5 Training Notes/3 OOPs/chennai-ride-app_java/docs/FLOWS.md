# Real-world flows and where they are enforced

Core principle: **no teleporting, no double-booking, no time travel, no money appearing or disappearing.**
Paths are relative to `src/com/ridehailing/`. "SelfCheck §n" is the section of `check/SelfCheck.java` that proves the rule.

## A. Captain lifecycle

| # | Flow | Enforced by | Proof |
|---|------|-------------|-------|
| 1 | Registers with a vehicle, starts KYC_PENDING, can't go online until verified | `Captain` constructor sets `KYC_PENDING`; `Captain.verifyKyc()`; `Captain.goOnline()` throws `CaptainNotEligibleException` | Scene 1 (Mani); SelfCheck §5, §12 |
| 2 | Online at a real location; can't go offline mid-ride; offline captains never matched | `Captain.goOnline(at, now)`, `Captain.goOffline()` throws `CaptainBusyException`; `MatchingService.ineligibilityReason()` returns "offline" | Scene 19; SelfCheck §5, §12 |
| 3 | Location changes only through events: to the pickup on arrival, to the actual drop on completion | `User.moveTo()` is `protected final`, called only by `Captain.reachPickup()` (from `RideService.captainArrived`) and `Captain.finishRide()` (from `RideService.finishTrip`) | Scene 4 before/after; SelfCheck §4, §6, §13 |
| 3a | **Canonical**: after Egmore → Tambaram the captain is at Tambaram; an Egmore request never goes to him, a Chromepet request can | `MatchingService.ineligibilityReason()` "too far" (radius `MATCH_RADIUS_KM = 7.5`), nearest-first sort | Scene 4; SelfCheck §6 |
| 4 | One ride at a time | `Captain.acceptRide()` requires `AVAILABLE` and `now ≥ availableFrom`; matching skips `ON_RIDE` | SelfCheck §5, §11, §13 |
| 5 | Offer to nearest first; reject → next-nearest; a rejecter is never re-offered; after 3 rejections → "No captains available"; acceptance rate tracked | `MatchingService.dispatch()`, `Captain.considerOffer()` (declines trips longer than `maxPreferredTripKm`), `Ride.recordOffer()` adds to `excludedCaptains`, `MAX_REJECTED_OFFERS = 3`, `Captain.getAcceptanceRate()` | Scene 5; SelfCheck §5 |
| 5a | (Interactive, Manual captain) the offer waits on the captain's phone; Accept/Reject; a captain with a pending offer gets no second offer and can't go offline | `RideService.setDispatchMode(CAPTAIN_APP)`, `offerToNextCaptain()`, `respondToOffer()`, `Ride.offerTo()`, `Captain.receiveOffer()` / `respondToOffer()`, `MatchingService.nextCandidate()` / "deciding on the offer" reason | `walkthroughs/01` |
| 6 | `markArrived()` only within 100 m of the pickup, and not before the ETA | `Ride.markArrived()` (`ARRIVAL_TOLERANCE_METERS`, `expectedArrivalAt`); `RideService.captainArrived()` moves the captain first | SelfCheck §2, §4 |
| 7 | First 3 minutes of waiting free, then ₹1/min | `FareService.generateReceipt()` (`FREE_WAITING_MINUTES`, `WAITING_CHARGE_PER_MINUTE`), `Ride.waitingMinutes()` | Scene 8; SelfCheck §7 |
| 8 | No-show after 5 min: customer charged, captain gets the fee minus commission, captain free at the pickup | `RideService.cancelForNoShow()` (`NO_SHOW_WAIT_MINUTES`), `VehicleType.getNoShowFee()`, `OutstandingFee` owed to that captain, `EarningsService.recordPaidRide()` step 2 | Scenes 9 and 17; SelfCheck §7 |
| 9 | Captain cancels after accepting: back to matching without him, customer not charged, his count goes up, he stays put | `RideService.captainCancelsAfterAccepting()`, `Ride.unassignCaptain()` (→ `REQUESTED`, excluded), `Captain.cancelAcceptedRide()` | Scene 11; SelfCheck §2 |
| 10 | Cash rides: captain owes the platform's share; over ₹500 → blocked from cash rides; `settleDues()` | `EarningsService.recordPaidRide()` step 4 → `Captain.addDues()`; `Captain.isBlockedForCash()` (`CASH_DUES_LIMIT`); `MatchingService` skips for CASH only; `CaptainService.settleDues()` | Scenes 3 and 15; SelfCheck §10 |
| 11 | Average below 4.0 after at least 5 ratings → flagged | `Captain.isFlagged()`, `RatingService.flaggedCaptains()` | Scene 20 (Ramesh); SelfCheck §12 |

> Dues on a cash ride = cash collected − the captain's own net share. That is the 20% commission plus the GST the captain is holding (and any carried-forward fee that belongs to another captain).

## B. Customer lifecycle

| # | Flow | Enforced by | Proof |
|---|------|-------------|-------|
| 1 | Wallet balance; top-up positive and ≤ ₹10,000 | `Customer.topUpWallet()` (`MAX_TOP_UP`), `CustomerService.topUpWallet()` | SelfCheck §9 |
| 2 | Estimates for ALL vehicle types with fare, nearest-captain ETA and availability | `RideService.getFareEstimates()` → `FareService.estimate()` + `MatchingService.nearestEtaMinutes()` → `FareEstimate` | Scenes 2 and 16; SelfCheck §1 |
| 3 | Booking rejected when: pickup = drop or < 500 m apart · outside the service area · > 60 km · too many passengers · active ride · PAYMENT_PENDING ride | `RideService.validateBooking()` / `validateRoute()`, `ServiceArea.requireInside()`, `VehicleType.canCarry()` | Scenes 6, 14, 17, 18; SelfCheck §8 |
| 3a | (Extra) pickup must be where the customer actually is (within 1 km) | `RideService.validateBooking()` (`PICKUP_GPS_TOLERANCE_KM`) | SelfCheck §8 |
| 4 | After a ride the customer is at the drop; later bookings start there, or where they travelled on their own | `Customer.arriveAt()` (from `RideService.finishTrip`), `Customer.travelOnOwnTo()` (blocked during a ride) | Scenes 11, 15, 16, 17; SelfCheck §4 |
| 5 | Cancel: free before assignment · free within 2 min of assignment · after that or after arrival ₹20 bike / ₹30 auto/cab, added to the NEXT ride · no cancel once IN_PROGRESS | `RideService.cancelByCustomer()` (`CANCELLATION_GRACE_MINUTES`), `VehicleType.getCancellationFee()`, `Customer.addOutstandingFee()`, `Ride.cancel()` throws for IN_PROGRESS | Scenes 7 and 8; SelfCheck §2, §7 |
| 5a | Fee carried to the next ride exactly once | `Customer.takeOutstandingFees()` empties the list as it hands the fees over; printed as "Previous cancellation fee" on `FareReceipt` | SelfCheck §7 |
| 6 | End trip early: fare on actual distance and time; both locations move to the stop point | `RideService.endTripEarly()` → `Ride.endEarly()` (stop point = `Ride.positionAt(now)` from time elapsed on the leg) → `finishTrip()` | Scene 13; SelfCheck §4 |
| 7 | Change destination once; fare on pickup → change point → new drop; new drop is where both end up | `RideService.changeDestination()` → `Ride.changeDestination()` (once), `Ride.actualDistanceKm()` sums the route | Scene 12; SelfCheck §4 |
| 8 | One active ride only | `ActiveRideExistsException` from `RideService.validateBooking()` via `Customer.getActiveRide()` | Scene 6; SelfCheck §8 |
| 9 | Rate only COMPLETED rides, once per side, 1–5 stars | `RatingService.rateCaptain/rateCustomer()`, `Ride.markCaptainRated/markCustomerRated()`, `User.receiveRating()` | Scene 20; SelfCheck §2, §12 |
| 10 | Chronological history including cancelled rides | `Customer.recordRide()` refuses out-of-order rides; every ride (even "no captain") is recorded at booking | Scene 21; SelfCheck §8, §13 |

## C. Ride state rules

| Rule | Enforced by |
|------|-------------|
| No `setStatus()`; only intent methods | `Ride`: `recordOffer`, `assignCaptain`, `unassignCaptain`, `markArrived`, `start`, `changeDestination`, `endEarly`, `reachDestination`, `attachReceipt`, `complete`, `markPaymentPending`, `cancel` |
| Every illegal call throws `InvalidRideStatusException` | `Ride.requireStatus()` and the explicit checks in each method (SelfCheck §2 tests about 20 illegal moves) |
| 4-digit OTP from seeded `Random`, private, at most 3 wrong attempts, then auto-cancel as OTP_FAILED with no fee | `RideService.bookRide()` (`OTP_SEED`), `Ride.start()` (`MAX_OTP_ATTEMPTS`), `RideService.startRide()` frees the captain (scene 10; SelfCheck §3) |
| Every status change timestamped from the SimulatedClock; timeline printable | `Ride.record()` → `TimelineEntry`; `Ride.timelineAsText()` (scenes 4 and 10) |

## D. Fare & receipt

| Rule | Enforced by |
|------|-------------|
| Itemised receipt: base, distance, time, waiting, surge, night, previous cancellation fee, coupon, GST 5%, total | `FareService.generateReceipt()` → `FareReceipt` (derives subtotal, GST and total itself) |
| Minimum fare | `Vehicle.calculateBaseFare()` / "Minimum fare top-up" line in `FareService.generateReceipt()` |
| Surge from demand vs supply near the pickup, higher at hotspots in peak hours, capped at 2.0x, shown before confirming and locked at booking | `FareService.calculateSurge()`, `RideService.surgeFor()` / `countOpenRequestsNear()`, `ServiceArea.isHotspot()` / `isPeakHour()`; stored in `Ride.surgeMultiplier` |
| Night +20% from 11:00 PM to before 5:00 AM (by trip start) | `FareService.isNightTime()` (SelfCheck §1 checks 10:59 vs 11:00) |
| FIRSTRIDE: ₹50 off, fare ≥ ₹100, once per customer, never below the minimum fare | `RideService.validateBooking()`, `FareService.generateReceipt()`, `Customer.markCouponUsed()` |
| Final fare on actual distance and time; estimate shown too | `Ride.actualDistanceKm()` / `actualTripMinutes()`; `FareReceipt` "Estimated at booking" |
| Captain's drive to the pickup is never billed | The route starts at the pickup in `Ride.start()` (SelfCheck §1) |

## E. Payments

| Rule | Enforced by |
|------|-------------|
| `Payable.pay(Money)` implemented by UPI, Cash, Wallet | `model/payment/*`, created by `PaymentService.createPayment()` |
| Method chosen at booking, can switch at the end | `RideRequest.paymentMethod`; `RideService.completeTrip(ride, method)`; `RideService.retryPayment(ride, method)` |
| Wallet insufficient → `InsufficientWalletBalanceException`, never a partial debit | `Customer.debitWallet()` (all-or-nothing), `WalletPayment.pay()` |
| UPI can fail (deterministic bank outage) → PAYMENT_PENDING → retry UPI or cash → COMPLETED | `PaymentService.reportUpiOutage()` / `isBankReachable()`, `UpiPayment.pay()`, `Ride.markPaymentPending()`, `Ride.complete()` (scene 14) |
| Cash always succeeds; captain's dues increase | `CashPayment.pay()`, `EarningsService.recordPaidRide()` step 4 |
| Payment can never succeed twice | `PaymentService.collect()` refuses COMPLETED rides; `Ride.complete()` throws if already COMPLETED; each `Payable` object can be used once (SelfCheck §9) |

## F. Earnings & reconciliation

| Rule | Enforced by |
|------|-------------|
| Commission is 20% of the fare before GST | `EarningsService.COMMISSION_PERCENT`; commission = `percent(20)`, net = amount − commission (so no paisa is lost to rounding) |
| Per captain per day: rides, gross, commission, net, cash collected, dues | `EarningsService.DayEarnings` (a trip counts on the day it was booked) |
| **Books balance**: paid by customers = captain earnings + commission + GST; cash + digital = paid | `EarningsService.isBalanced()`; printed "Books Balanced ✔" in scene 21; SelfCheck §13 re-runs the whole day silently and checks it to the paisa |

## G. SimulatedClock

| Rule | Enforced by |
|------|-------------|
| All time from `SimulatedClock`; `LocalDateTime.now()` never used | `time/SimulatedClock.java` is passed into every service |
| Demo advances the clock realistically (travel to the pickup, waiting, trip) | `ChennaiRideApp.advanceTo()` / autopilot; `Ride.markArrived()` and `Ride.reachDestination()` refuse to happen early |
| A captain free at 9:40 can't start before 9:40 | `Captain.availableFrom`, `Captain.isAvailableAt()`, `Captain.acceptRide()` (SelfCheck §11, §13) |
| The clock never goes backward | `SimulatedClock.advanceTo()` / `advanceMinutes()` throw (SelfCheck §11) |

## H. Notifications

| Rule | Enforced by |
|------|-------------|
| `Notifier` implemented by SMS and Push; RideService holds a `List<Notifier>` | `notification/*`, `RideService.notify()` |
| Events: booked, assigned (name, vehicle, plate, ETA, OTP), arrived, started, destination changed, completed (fare), payment success/failure, cancelled (reason, fee) | `RideEvent` enum; each call site in `RideService`. SMS sends only the events marked important (`SmsNotifier.handles()`). |
