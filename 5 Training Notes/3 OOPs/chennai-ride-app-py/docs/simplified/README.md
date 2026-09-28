# Simplified class diagram (for teaching)

A one-page, projector-friendly version of the class diagram: **type names and relationships only**,
no fields or methods. For the full detailed UML, see `../class_diagram.html`.

## How to open

Open `class_diagram_simplified.html` in any modern browser (double-click it). It loads Mermaid from
`cdn.jsdelivr.net`, so it needs an internet connection. Each diagram shows ✔ once it has rendered.
Print it with Ctrl+P / Cmd+P: every group starts on its own page.

The diagrams are also saved separately in `diagrams/*.mmd`, and each file carries its own colours, so
you can paste it into any Mermaid editor (e.g. mermaid.live).

## Legend

| Arrow | Label | Meaning |
|---|---|---|
| solid line, hollow triangle | is-a | inheritance |
| dashed line, hollow triangle | can-do | interface realisation |
| filled diamond ◆ | has-a, owns | composition |
| hollow diamond ◇ | has-a, uses | aggregation |
| dotted line, open arrow | uses | dependency (group 6 only) |

Colours: **amber** = abstract class, **purple** = interface, **green** = enum, **white** = concrete class,
dashed = module (not a class).

Rule of thumb: **abstract class = IS-A with shared data; interface = CAN-DO, no shared data.**

## Types

| Group | Type | Kind | Why (one line) |
|---|---|---|---|
| User and vehicles | User | abstract class | Abstract: no "plain" user exists. It holds shared data (id, name, phone, rating), so it is an abstract class, not an interface. |
| User and vehicles | Customer | concrete class | Concrete: a real person who books, with their own data (wallet, ride history). |
| User and vehicles | Captain | concrete class | Concrete: a real person who drives, with their own data (status, dues, vehicle). |
| User and vehicles | Vehicle | abstract class | Abstract: the fare formula is written ONCE here; each child overrides its own rates (polymorphism). |
| User and vehicles | Bike | concrete class | Concrete: overrides the rates with bike values. |
| User and vehicles | Auto | concrete class | Concrete: overrides the rates with auto values. |
| User and vehicles | Cab | abstract class | Abstract, in the middle: groups Economy and Premium, giving multilevel inheritance. |
| User and vehicles | CabEconomy | concrete class | Concrete: Vehicle → Cab → CabEconomy (three levels). |
| User and vehicles | CabPremium | concrete class | Concrete: Vehicle → Cab → CabPremium (three levels). |
| User and vehicles | CaptainStatus | enum | Enum: a fixed set of values (KYC_PENDING, OFFLINE, ONLINE, ON_RIDE), so an invalid value is impossible. |
| User and vehicles | VehicleType | enum | Enum: BIKE, AUTO, CAB_ECONOMY, CAB_PREMIUM, each with a seat count and a cancellation fee. |
| Location and money | Location | concrete class | Immutable: a place never changes, so it is safe to share. Compared by value (same coordinates = equal). |
| Location and money | Money | concrete class | Immutable: ₹ amounts never change. Decimal inside, so no floating-point errors (0.1 + 0.2 = 0.3 exactly). |
| Location and money | chennai_places | module (not a class) | Not a class: a module holding a dict of real Chennai places and helper functions. Location.from_place_name() reads it. |
| Location and money | ServiceArea | concrete class | Concrete: the Chennai Metro box. A pickup or drop outside it is rejected. |
| Ride | Ride | concrete class | Guards its own status: there is no set_status(), only assign_captain(), start(otp), complete(), cancel(). The OTP is private. The best encapsulation example. |
| Ride | Customer | concrete class | Aggregation: the customer exists before and after the ride. |
| Ride | Captain | concrete class | Aggregation: the captain exists before and after the ride. |
| Ride | RideRequest | concrete class | Composition: what was booked (pickup, drop, vehicle type), frozen at booking. |
| Ride | FareEstimate | concrete class | Composition: the price shown before confirming. |
| Ride | FareReceipt | concrete class | Composition: the itemised bill, created for this ride only. |
| Ride | RideStatus | enum | Enum: REQUESTED → … → COMPLETED / CANCELLED. Only these values can exist. |
| Ride | CancellationReason | enum | Enum: CUSTOMER_CANCELLED, NO_SHOW, OTP_FAILED, NO_CAPTAIN_AVAILABLE. |
| Payments | Payable | interface | Interface: shares no data, only a CAN-DO promise (pay). A new payment method = one new class, no changes to existing code. |
| Payments | UpiPayment | concrete class | Can-do pay(): UPI, which can fail (seeded, so every run is identical). |
| Payments | CashPayment | concrete class | Can-do pay(): cash, which always succeeds; the captain then owes the platform its share. |
| Payments | WalletPayment | concrete class | Can-do pay(): wallet, which deducts all or nothing, never partially. |
| Payments | PaymentStatus | enum | Enum: NOT_DUE, FAILED, PAID, so a ride's payment can only be in one of these states. |
| Notifications | Notifier | abstract class | Abstract class in the code (not an interface): it keeps shared data, the log of sent messages. By the rule of thumb (shared data), that makes it abstract. |
| Notifications | SmsNotifier | concrete class | Is-a Notifier: sends by SMS to the user's +91 number. |
| Notifications | PushNotifier | concrete class | Is-a Notifier: in-app push (printed in the demo). |
| Services | CustomerService | concrete class | Registers customers, tops up wallets, moves customers around. |
| Services | CaptainService | concrete class | Registers captains with their vehicles; online/offline; settles dues. |
| Services | MatchingService | concrete class | Finds the nearest eligible captain for a ride. |
| Services | RideService | concrete class | Runs every ride flow, from booking to payment, and notifies on each event. |
| Services | FareService | concrete class | All fare maths: rates, surge, night charge, coupon, receipt. |
| Services | PaymentService | concrete class | Takes payment through any Payable and keeps a ledger. |
| Services | RatingService | concrete class | Ratings after COMPLETED rides only. |
| Services | EarningsService | concrete class | Splits money into captain earnings, commission and GST; proves the books balance. |
| Clock and exceptions | SimulatedClock | concrete class | Controlled time: repeatable demos, and night rides on demand. It can never go backwards. |
| Clock and exceptions | Exception | concrete class | Python's built-in base class for all errors. |
| Clock and exceptions | RideHailingError | concrete class | Inheritance again: catch the parent to handle ALL app errors… |
| Clock and exceptions | 17 specific errors | concrete class | …or catch one child for one specific error, e.g. InsufficientWalletBalanceError. |
| CLI | Menu | abstract class | Abstract: the show/read/handle loop is written ONCE; each menu handles its choices differently. |
| CLI | StartupMenu | concrete class | Is-a Menu: demo / interactive mode / self-check. |
| CLI | MainMenu | concrete class | Is-a Menu. It also owns its four sub-menus and holds them as Menu (polymorphism). |
| CLI | CustomerMenu | concrete class | Is-a Menu: the customer's phone app. |
| CLI | CaptainMenu | concrete class | Is-a Menu: the captain's phone app. |
| CLI | OperationsMenu | concrete class | Is-a Menu: the operations dashboard. |
| CLI | ClockMenu | concrete class | Is-a Menu: move the clock forward. |
| CLI | ConsoleIO | concrete class | One shared object for all input and output (aggregation: every menu uses the same one). |
| Clock and exceptions | InvalidRideStatusError, InvalidOtpError, NoCaptainAvailableError, ActiveRideExistsError, PaymentPendingError, InsufficientWalletBalanceError, OutOfServiceAreaError, InvalidBookingError, CaptainNotEligibleError, CaptainBusyError, CaptainNotAtPickupError, PaymentFailedError, DuplicatePaymentError, InvalidAmountError, InvalidRatingError, TimeTravelError, UnauthorizedAccessError | concrete classes | Each is-a RideHailingError; catch one for one specific error. |

## Where the brief's names differ from the code

- **ChennaiPlaces** → there is no class; it is the module `chennai_places` (a dict of places plus helper functions).
- **Notifier** → an *abstract class* in the code (it stores the sent-message log), not an interface.
- **Payable** → an ABC used as an interface (both members abstract, plus one default `is_cash`).
- **setStatus() / assignCaptain()** → Python names: `assign_captain()`, `start(otp)`, `complete()`, `cancel()`; no `set_status()` exists.
- **Base exception** → `RideHailingError`, which extends Python's `Exception`.
- Also shown because they are in the code: RideRequest, FareEstimate, ServiceArea, StartupMenu, ConsoleIO.
