# Flows — which class and method enforces each rule

The core principle is **no teleporting, no double-booking, no time travel, and no money appearing or
disappearing.** Paths are relative to `ridehailing/`. The last column names the self-check group
(`check/self_check.py`) that verifies the rule.

## A. Captain lifecycle

| # | Rule | Enforced by | Verified in |
|---|---|---|---|
| 1 | Registers as KYC_PENDING; cannot go online until verified | `Captain.__init__` sets `KYC_PENDING`; `Captain.go_online()` raises `CaptainNotEligibleError`; `Captain.verify_kyc()` | `check_matching` |
| 2 | Online at a real location; no going offline mid-ride; offline captains never matched | `Captain.go_offline()` raises `CaptainBusyError`; `MatchingService.ineligibility_reason()` ("not available") | `check_matching` |
| 3 | Location changes only through events | `Captain.arrive_at()` (on arrival), `Captain.finish_ride(drop)` (on trip end). `RideService.captain_arrives()` and `_drive()` first advance the clock by the travel time | `check_location_continuity`, `check_canonical` |
| 3 | Canonical Egmore → Tambaram | Radius check in `MatchingService.ineligibility_reason()` (8 km): the captain at Tambaram is 24 km from Egmore but 5.8 km from Chromepet | `check_canonical` |
| 4 | One ride at a time | `Captain.start_assignment()` raises `CaptainBusyError`; `Captain.is_available` | `check_matching` |
| 5 | Nearest first, then next-nearest; a rejector is never re-offered; stop after 3 rejections; acceptance rate tracked | `MatchingService.find_captain()` / `next_candidate()`, `Ride.record_offer()`, `Ride.is_excluded()`, `Captain.respond_to_offer()`, `Captain.acceptance_rate`. In the interactive **Manual** mode, the offer waits on the captain (`Captain.receive_offer()`), who answers with `RideService.accept_offer()` / `reject_offer()`; a captain holding one offer is not offered another | `check_matching`; walkthroughs 1–3 |
| 6 | `mark_arrived()` only within ~100 m | `Ride.mark_arrived()` raises `CaptainNotAtPickupError` | `check_location_continuity` |
| 7 | 3 minutes of waiting free, then ₹1/min | `Ride.start()` records `waiting_minutes`; `FareService._price()` charges `max(wait − 3, 0)` | `check_fares` |
| 8 | No-show after 5 minutes: fee charged, captain earns it minus commission, captain free at the pickup | `RideService.cancel_no_show()`, `PaymentService.charge_fee()`, `EarningsService.record_fee_payment()` | `check_cancellations` |
| 9 | Captain cancels after accepting: re-match excluding them, customer not charged, cancel count increases, captain stays put | `RideService.cancel_by_captain()`, `Ride.release_captain()`, `Captain.release_after_cancellation(cancelled_by_captain=True)` | `check_transitions`, `check_location_continuity` |
| 10 | Cash dues over ₹500 block cash rides until `settle_dues()` | `EarningsService.record_ride_payment()` → `Captain.add_dues()`; `Captain.accepts_cash_rides`; `MatchingService.ineligibility_reason()`; `CaptainService.settle_dues()` | `check_cash_block` |
| 11 | Average below 4.0 after at least 5 ratings → flagged | `Captain.is_flagged`; `RatingService.flagged_captains()` | `check_cancellations`, `check_full_day` |

## B. Customer lifecycle

| # | Rule | Enforced by | Verified in |
|---|---|---|---|
| 1 | Wallet top-up must be positive and at most ₹10,000 | `Customer.top_up()` raises `InvalidAmountError` | `check_booking_rules` |
| 2 | Estimates for all vehicle types, with fare, ETA and availability | `RideService.fare_estimates()` → `FareService.estimate()`, `MatchingService.nearest_eta()` | `check_booking_rules` |
| 3 | Booking validation: pickup = drop or < 500 m, outside area, > 60 km, too many passengers, active ride, payment pending | `RideService._validate_trip()`, `ServiceArea.ensure_contains()`, `Customer.ensure_can_book()` | `check_booking_rules`, `check_matching` |
| 4 | Customer's location becomes the drop point; moving elsewhere needs realistic time | `Customer.finish_ride()`; `Customer.relocate_to()` (20 km/h) | `check_location_continuity` |
| 5 | Cancellation: free before assignment and within 2 min; ₹20 bike / ₹30 auto-cab after the grace period or after arrival; fee added to the NEXT ride exactly once; no cancelling once IN_PROGRESS | `RideService.cancel_by_customer()`, `Customer.add_pending_fee()` / `take_pending_fees()`, `Ride.cancel()` | `check_cancellations`, `check_transitions` |
| 6 | End trip early: fare on actual distance and time; both locations = stop point | `RideService.end_trip_early()` → `Ride.end_early()` → `FareService.build_receipt()` (uses `Ride.actual_distance_km`, `trip_minutes`) | `check_location_continuity` |
| 7 | Change destination once; fare on pickup → change point → new drop | `Ride.change_destination()` (once), `Ride.route` | `check_transitions`, `check_location_continuity` |
| 8 | One active ride only | `Customer.ensure_can_book()` raises `ActiveRideExistsError` | `check_booking_rules` |
| 9 | Only COMPLETED rides can be rated, once per side, 1–5 | `Ride.rate_captain()` / `rate_customer()`, `RatingService._validate_score()` | `check_cancellations` |
| 10 | Chronological history, cancelled rides included | `Customer.begin_ride()` appends at booking time; `Customer.ride_history` | `check_cancellations` |

## C. Ride state rules

| Rule | Enforced by |
|---|---|
| `status` is read-only | `Ride.status` property with no setter |
| Every method validates the current status | `Ride._require_status()`, which raises `InvalidRideStatusError` |
| 4-digit OTP in private `__otp`; 3 wrong attempts auto-cancel as OTP_FAILED with no fee | `Ride.__init__`, `Ride.start()`; `RideService.start_ride()` frees the captain and customer |
| Timestamp on every change, plus a printable timeline | `Ride._record()` (refuses events in the past), `Ride.timeline_text()` |

Verified in `check_transitions` and `check_otp`.

## D. Fare & receipt

| Rule | Enforced by |
|---|---|
| Itemised: base, distance, time, waiting, surge, night, previous fee, coupon, GST 5%, total | `FareService._price()` builds a `FareReceipt`; `FareReceipt.__init__` computes GST and total itself |
| Surge from demand vs supply near the pickup, higher at hotspots in peak, capped at 2.0x, shown before confirming | `RideService._surge_for()`, `FareService.surge_multiplier()`; the surge is locked in the `FareEstimate` at booking |
| Night +20% from 11 PM to 5 AM (based on trip start) | `SimulatedClock.is_night()`, `FareService.build_receipt()` |
| FIRSTRIDE ₹50 off, minimum fare ₹100, once per customer, never below the vehicle minimum | `FareService.validate_coupon()`, `FareService._price()`, `Customer.mark_coupon_used()` |
| Final fare vs estimate both shown | `FareReceipt.estimated_total` |
| Only pickup → drop is billed | `FareService.build_receipt()` uses `Ride.route`, which starts at the pickup |

Verified in `check_fares`.

## E. Payments

| Rule | Enforced by |
|---|---|
| `Payable.pay(amount)` implemented by UPI, Cash and Wallet | `model/payment/*` |
| Choose at booking, switch at the end | `RideService.book_ride(payment_method=…)`, `RideService.complete_trip(ride, payment_method=…)` |
| Wallet never partially deducted | `Customer.pay_from_wallet()` raises `InsufficientWalletBalanceError` before changing anything |
| UPI can fail (seeded) → PAYMENT_PENDING → retry UPI or cash → COMPLETED | `UpiPayment.pay()`, `PaymentService.pay_ride()`, `Ride.mark_payment_failed()`, `Ride.complete()`, `RideService.pay_for_ride()` |
| Cash always succeeds; captain's dues increase | `CashPayment.pay()`, `EarningsService.record_ride_payment(is_cash=True)` |
| A payment never succeeds twice | `PaymentService.pay_ride()` and `Ride.complete()` raise `DuplicatePaymentError` |

Verified in `check_payments` and `check_booking_rules`.

## F. Earnings & reconciliation

| Rule | Enforced by |
|---|---|
| 20% commission on the fare before GST | `EarningsService._split()` (net = amount − commission, so the parts add up to the paisa) |
| Per captain per day: rides, gross, commission, net, cash collected, dues | `DailyEarnings` in `service/earnings_service.py` |
| A carried-over cancellation fee goes to the captain who was cancelled on, minus commission | `EarningsService.record_ride_payment()` loops over `ride.carried_fees` |
| Cash: the captain owes everything that isn't their own earning (commission + GST + others' fees) | `EarningsService.record_ride_payment()` |
| **Books balance**: paid by customers = captain earnings + commission + GST | `EarningsService.books_balance()`, cross-checked against the independent ledger in `PaymentService.total_collected`; wallets checked by `ChennaiRideApp.wallets_balance()` |

Verified in `check_full_day`.

## G. SimulatedClock

| Rule | Enforced by |
|---|---|
| All time comes from the clock; `datetime.now()` is never used | `time/simulated_clock.py`; every service receives the clock |
| Realistic advances for travel to pickup, waiting and trip duration | `RideService.captain_arrives()`, `RideService._drive()`, `FareService.travel_minutes()` (slower at peak and in OMR/T. Nagar/Koyambedu) |
| A captain's next ride never starts before the previous one ended | `Captain.free_since`; `Captain.start_assignment()` raises `TimeTravelError`; matching skips them |
| The clock never goes backwards | `SimulatedClock.advance()` / `advance_to()` raise `TimeTravelError` |

Verified in `check_clock` and `check_full_day`.

## H. Notifications

`RideService._notify()` sends every event to each `Notifier` in its list (SMS and push). The events
are: booked; captain assigned (name, plate, vehicle, ETA, OTP); arrived; started; destination changed;
completed (with fare); payment success or failure; cancelled (with reason and any fee).
