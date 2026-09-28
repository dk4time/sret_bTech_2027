# OOP Concept Map — the teaching script

Each row maps one OOP feature to where it lives in the code, and why it is used there. The paths are
relative to `ridehailing/`. Every place listed here also has a short comment in the code itself.

## 1. Classes & objects

| Feature | Where | Why it is there |
|---|---|---|
| `__init__` and `self` | `model/ride/ride.py` → `Ride.__init__` | Sets up every piece of state a new ride needs, on *this* object. |
| Instance vs **class attribute** (id counter) | `model/user/user.py` → `User._id_counter`; `model/ride/ride.py` → `Ride._next_number` | One counter is shared by all objects; each object stores its own `_user_id` / `_ride_id`. |
| Class attribute (constant) | `model/ride/fare_receipt.py` → `FareReceipt.GST_RATE`; `service/earnings_service.py` → `EarningsService.COMMISSION_RATE`; `model/user/captain.py` → `Captain.CASH_DUES_LIMIT` | A business rule defined once, the same for every object. |
| Class attribute (rate card) | `model/vehicle/bike.py` → `Bike._BASE_FARE`, `_PER_KM` … | Every bike shares one rate card, so it isn't copied into each bike. |
| `super().__init__()` | `model/user/captain.py` → `Captain.__init__`; `model/vehicle/cab.py` → `Cab.__init__`; `model/vehicle/cab_premium.py` → `CabPremium.__init__` | The parent validates and sets its own fields; the child adds only what is new. |

## 2. Encapsulation

| Feature | Where | Why it is there |
|---|---|---|
| `_protected` attributes | `model/user/user.py` → `self._location`, `self._name` | By convention, only the class and its subclasses touch them. |
| `__private` + name mangling | `model/ride/ride.py` → `self.__otp` (stored as `_Ride__otp`) | The OTP must not be readable from outside. `ride.__otp` raises `AttributeError` (see the comment in `Ride.__init__`, and `check_otp` in the self-check). |
| Controlled access to a secret | `model/ride/ride.py` → `Ride.otp_for(user)` | Only the ride's own customer can see the OTP. Anyone else gets `UnauthorizedAccessError`. |
| `@property` getters with **no setter** | `model/ride/ride.py` → `Ride.status`; `model/user/customer.py` → `Customer.wallet_balance`; `model/user/captain.py` → `Captain.dues_owed` | Outsiders can read the state but can never assign it. `ride.status = …` raises `AttributeError`. |
| Intent-revealing state changes | `Captain.go_online()`, `Captain.go_offline()`, `Ride.start(otp)`, `Ride.cancel(reason, by)`, `Customer.top_up()`, `Captain.settle_dues()` | Each change goes through one method that checks the rules before touching the state. |
| Returning copies | `Customer.ride_history`, `Ride.timeline`, `Ride.route` return tuples | Callers cannot append to or reorder the real internal lists. |
| Validation inside methods | `Customer.top_up` (positive, ≤ ₹10,000); `Customer.pay_from_wallet` (all-or-nothing); `Ride.mark_arrived` (within 100 m) | The object protects its own invariants, so no service can put it into a broken state. |

## 3. Abstraction

| Abstract class | Where | Abstract members | Why |
|---|---|---|---|
| `User(ABC)` | `model/user/user.py` | `role`, `id_prefix` | Nobody is "just a user"; everyone is a Customer or a Captain. |
| `Vehicle(ABC)` | `model/vehicle/vehicle.py` | `vehicle_type`, `base_fare`, `per_km_rate`, `per_minute_rate`, `minimum_fare`, `seat_capacity`, `average_speed_kmph()` | Rates are unknown until we know the kind of vehicle. |
| `Cab(Vehicle, ABC)` | `model/vehicle/cab.py` | `luggage_bags` (plus the inherited rates) | A middle layer that is still abstract. |
| `Payable(ABC)` | `model/payment/payable.py` | `method_name`, `pay(amount)` | Services depend on "something that can pay", not on UPI specifically. |
| `Notifier(ABC)` | `notification/notifier.py` | `channel`, `notify(user, message, at)` | New channels can be added without changing `RideService`. |

Instantiating any of them raises `TypeError`. This is verified in `check/self_check.py` →
`check_abstraction()`.

## 4. Inheritance

| Kind | Where | Why |
|---|---|---|
| **Multilevel** | `Vehicle → Cab → CabPremium` (`model/vehicle/`) | `Cab` adds air-conditioning and a shared cab speed; `CabPremium` adds its rates and extends `__str__` twice up the chain. |
| **Hierarchical** | `User → Customer`, `User → Captain` (`model/user/`) | Both share an id, name, phone, location and ratings; each adds its own behaviour. |
| Hierarchical | `Payable → UpiPayment / CashPayment / WalletPayment`; `Notifier → SmsNotifier / PushNotifier` | One interface with several implementations. |
| `super()` in methods | `model/vehicle/cab.py` → `Cab.__str__`; `model/vehicle/cab_premium.py` → `CabPremium.__str__` | Reuses the parent's text and adds detail. |
| `isinstance()` | `model/user/captain.py` → `Captain.__init__` checks `isinstance(vehicle, Vehicle)`; `Money._require_money` | Guards a boundary with a meaningful check. |
| `issubclass()` | `check/self_check.py` → `check_abstraction()` | Proves the hierarchy (for example, `issubclass(CabPremium, Cab)`). |

## 5. Polymorphism

| Feature | Where | Why |
|---|---|---|
| **Method overriding** (fare rates) | `Bike.base_fare`, `Auto.base_fare`, `CabEconomy.base_fare`, `CabPremium.base_fare` … | Each vehicle answers with its own rates. |
| **The central example** | `model/vehicle/vehicle.py` → `Vehicle.calculate_base_fare()` | Written once and never overridden, yet it returns different fares for a Bike and a CabPremium because `self.base_fare` etc. are dispatched at runtime. |
| Polymorphism over a rate card | `service/fare_service.py` → `FareService._rate_card` + `FareService._price()` | Estimates for all four types use the same calls on four different vehicle objects. |
| Overriding `__str__` | `Bike.__str__`, `Cab.__str__`, `CabPremium.__str__`, `Captain.__str__`, `UpiPayment.__str__` | Each class prints itself appropriately. |
| Overriding a concrete default | `model/payment/payable.py` → `Payable.is_cash` (False); `CashPayment.is_cash` (True) | The matching and earnings code asks `payable.is_cash`, never "is this a CashPayment?". |
| **Duck typing / dynamic dispatch** — notifiers | `service/ride_service.py` → `RideService._notify()` loops `for notifier in self._notifiers: notifier.notify(...)` | SMS and push are treated identically; adding a WhatsApp notifier needs no change here. |
| Dynamic dispatch — payables | `service/payment_service.py` → `PaymentService.pay_ride()` calls `payable.pay(amount)`; `check_payments()` loops over three Payables | UPI, cash and wallet each decide *how* to pay. |
| **Operator overloading** | `model/common/money.py` → `Money.__add__`, `__sub__`, `__mul__`, `__rmul__`, `__neg__`, `__lt__`, `__le__`, `__gt__`, `__ge__`, `__eq__` | `fare + gst`, `rate * km` and `total < minimum` read like the business rule. See `FareReceipt.__init__`: `self._fare_before_gst + self._gst`. |
| **No method overloading → default/keyword args** | `service/ride_service.py` → `RideService.book_ride(customer, drop, vehicle_type, passengers=1, payment_method=None, coupon_code=None)` (see its docstring) | A second `def book_ride` would just replace the first, so optional parameters give one flexible method. The same idea appears in `complete_trip(ride, payment_method=None)`. |

## 6. Composition vs aggregation

| Relationship | Where | Why |
|---|---|---|
| **Composition**: Captain HAS-A Vehicle | `model/user/captain.py` → `self._vehicle`; created at registration in `app/chennai_ride_app.py` → `_seed_captains()` | The vehicle is made for this captain and never changes owner or type. `CaptainService.register_captain` refuses a vehicle that already belongs to someone. |
| Composition: Ride owns its RideRequest and FareReceipt | `model/ride/ride.py` | They exist only as part of this ride. |
| Composition: services built from services | `service/ride_service.py` → `RideService.__init__`; `app/chennai_ride_app.py` → `ChennaiRideApp.__init__` | A service is assembled from the smaller services it needs. |
| **Aggregation**: Ride references a Customer and a Captain | `model/ride/ride.py` → `self._request.customer`, `self._captain` | Both existed before the ride and live on after it; many rides reference the same captain. |

## 7. Class-level members

| Feature | Where | Why |
|---|---|---|
| `@classmethod` alternative constructor | `model/common/location.py` → `Location.from_place_name("Egmore")`; `model/common/money.py` → `Money.zero()`; `model/common/service_area.py` → `ServiceArea.chennai_metro()` | These are clearer ways to build common objects than a raw constructor call. |
| `@staticmethod` helper | `model/common/location.py` → `Location.haversine_km(...)`; `time/simulated_clock.py` → `SimulatedClock.is_peak_hour()`, `is_night()`; `Money.total()`; `FareService.road_km()` | Pure functions that belong with the class but need no object. |

## 8. Immutability

| Where | How | Why |
|---|---|---|
| `model/common/money.py` → `Money` | Read-only `amount` property; `__setattr__` / `__delattr__` raise `AttributeError`; the constructor uses `object.__setattr__` exactly once | A ₹100 note never becomes ₹90. Every operation returns a **new** Money. |
| `model/common/location.py` → `Location` | Same technique; `point_towards()` returns a new Location | A place never moves. Only *who is at* a place changes. |

## 9. Dunder methods

| Method | Where | Rule |
|---|---|---|
| `__str__` (user-friendly) | `Money.__str__` → `₹1,234.50`; `Ride.__str__`; `FareReceipt.__str__` (full receipt) | What a customer would see. |
| `__repr__` (developer-friendly) | `Money.__repr__` → `Money('1234.50')`; `Location.__repr__`; `User.__repr__`; `Ride.__repr__` | What a debugger should show. |
| `__eq__` / `__hash__` — **entities by id** | `User` (by `user_id`), `Ride` (by `ride_id`), `Vehicle` (by registration number) | Two users with the same name are still different people. |
| `__eq__` / `__hash__` — **values by value** | `Money` (by amount), `Location` (by coordinates) | Equal values are interchangeable, so they work as dict keys and in sets (`check_value_objects()`). |

## 10. Custom exception hierarchy

`exceptions.py`: `RideHailingError(Exception)` is the base class, with these subclasses:
`InvalidRideStatusError`, `InvalidOtpError(attempts_left, ride_cancelled)`,
`NoCaptainAvailableError(reason)`, `ActiveRideExistsError`, `PaymentPendingError`,
`InsufficientWalletBalanceError(required, available)`, `OutOfServiceAreaError`,
`InvalidBookingError`, `CaptainNotEligibleError`, `CaptainBusyError`, plus `CaptainNotAtPickupError`,
`PaymentFailedError`, `DuplicatePaymentError`, `InvalidAmountError`, `InvalidRatingError`,
`TimeTravelError` and `UnauthorizedAccessError`.

The demo catches them one by one and prints them with `ChennaiRideApp.fail()`, so no raw tracebacks
appear. `run.py` also catches the base class as a last resort.

## 11. CLI layer (`cli/`)

The interactive mode is itself written as OOP, and it contains **no business rules**. Every rule
comes from the same services the scripted demo uses.

| Feature | Where | Why |
|---|---|---|
| **Abstract class** | `cli/menu.py` → `Menu(ABC)` with abstract `options()` and `handle(choice)` | Every screen must say what it offers and what each option does. `Menu(...)` raises TypeError. |
| **Template method** (concrete loop in the base class) | `Menu.run()`: status line → header → options → read → `handle()` → repeat | The loop, back/quit handling and error display are written once. Subclasses fill in only the abstract steps. |
| Hook methods with defaults | `Menu.on_enter()`, `Menu.status_line()`, `Menu.heading()` | Optional steps a subclass *may* override (e.g. `CustomerMenu.on_enter()` picks the customer). |
| **Inheritance** | `StartupMenu`, `MainMenu`, `CustomerMenu`, `CaptainMenu`, `OperationsMenu`, `ClockMenu` all extend `Menu` | Six screens, one loop. |
| **Polymorphism / dynamic dispatch** | `cli/main_menu.py` → `self._submenus: dict[str, Menu]` and `menu.run()` in `MainMenu.handle()` | The main menu holds every sub-menu through a `Menu` reference and never knows which kind it is. |
| **Aggregation** vs **composition** | Every menu has a *shared* `ConsoleIO` (aggregation ◇, one object passed to all menus); `CliSession` *creates and owns* its `ChennaiRideApp` (composition ◆, the same world as the demo); `MainMenu` creates its sub-menus (composition ◆) | Shared parts are aggregated, parts a class creates for itself are composed. See `docs/class_diagram.html`, diagram (d). |
| **Encapsulation** of I/O | `cli/console_io.py` → `ConsoleIO.read_int`, `read_choice`, `read_yes_no`, `read_place`, `read_money`, `read_time`, `print_table`, `print_header`, `print_error`, `print_success` | Menus never call `input()` or format tables themselves. Validation and re-prompting live in one class. |
| Exceptions for control flow | `cli/console_io.py` → `GoBack`, `QuitRequested`, `EndOfInput(QuitRequested)` | `b`, `q` and end of input unwind cleanly from any depth. `EndOfInput` IS-A `QuitRequested`, so one `except` handles both. |
| Catching the base exception | `Menu.run()` → `except RideHailingError` | Every business error from any service prints as a clean ✘ message, and the session keeps running. |
| Class attribute across subclasses | `notification/notifier.py` → `Notifier._sequence` | One counter shared by SMS and push gives a global "latest first" notification log. |

The services gained a few small, clean methods for the CLI; the menus never reach into private
state:
- **Manual offers:** `RideService.set_manual_offers()`, `accept_offer()`, `reject_offer()`, and
  `Captain.receive_offer()` / `withdraw_offer()` / `pending_offer`.
- **Queries:** `RideService.active_rides()`, `position_now()`, `surge_at()`,
  `cancellation_fee_if_cancelled_now()`, `last_completed_ride_for_captain()`, and
  `MatchingService.captains_near()` / `offered_captains()`.

## 12. Enums with data and behaviour

| Enum | Where | Extra data / methods |
|---|---|---|
| `VehicleType` | `model/vehicle/vehicle_type.py` | `display_name`, `seats`, `cancellation_fee`, `can_carry()`, `is_cab()` |
| `RideStatus` | `model/ride/ride_status.py` | `label`, `is_active()`, `is_cancellable()`, `is_final()` |
| `CaptainStatus` | `model/user/captain_status.py` | `label`, `is_working()` |
| `CancellationReason` | `model/ride/cancellation_reason.py` | `description`, `fee_may_apply` |
| `PaymentStatus` | `model/payment/payment_status.py` | `label` |
