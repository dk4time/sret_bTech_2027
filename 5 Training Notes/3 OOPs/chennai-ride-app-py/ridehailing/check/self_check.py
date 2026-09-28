"""Self-check — plain-Python verification of every business rule (no pytest).

Run with ``python run.py --check``. Prints PASS/FAIL per check and exits with code 1 if anything fails.
Each group builds a small, fresh "world" so that checks never depend on each other.
"""

from __future__ import annotations

import contextlib
import io
import random
from datetime import datetime, timedelta
from decimal import Decimal

from ridehailing.exceptions import (ActiveRideExistsError, CaptainBusyError,
                                    CaptainNotAtPickupError, CaptainNotEligibleError,
                                    DuplicatePaymentError, InsufficientWalletBalanceError,
                                    InvalidAmountError, InvalidBookingError, InvalidOtpError,
                                    InvalidRatingError, InvalidRideStatusError,
                                    NoCaptainAvailableError, OutOfServiceAreaError,
                                    PaymentFailedError, PaymentPendingError, RideHailingError,
                                    TimeTravelError, UnauthorizedAccessError)
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.common.service_area import ServiceArea
from ridehailing.model.payment.cash_payment import CashPayment
from ridehailing.model.payment.payable import Payable
from ridehailing.model.payment.upi_payment import UpiPayment
from ridehailing.model.payment.wallet_payment import WalletPayment
from ridehailing.model.ride.cancellation_reason import CancellationReason
from ridehailing.model.ride.ride import Ride
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.customer import Customer
from ridehailing.model.user.user import User
from ridehailing.model.vehicle.auto import Auto
from ridehailing.model.vehicle.bike import Bike
from ridehailing.model.vehicle.cab import Cab
from ridehailing.model.vehicle.cab_economy import CabEconomy
from ridehailing.model.vehicle.cab_premium import CabPremium
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.notification.notifier import Notifier
from ridehailing.notification.push_notifier import PushNotifier
from ridehailing.notification.sms_notifier import SmsNotifier
from ridehailing.service.captain_service import CaptainService
from ridehailing.service.customer_service import CustomerService
from ridehailing.service.earnings_service import EarningsService
from ridehailing.service.fare_service import FareService
from ridehailing.service.matching_service import MatchingService
from ridehailing.service.payment_service import PaymentService
from ridehailing.service.rating_service import RatingService
from ridehailing.service.ride_service import RideService
from ridehailing.time.simulated_clock import SimulatedClock

_results = {"passed": 0, "failed": 0}


def check(name: str, condition: bool) -> None:
    """Print PASS or FAIL for one check and count it."""
    if condition:
        _results["passed"] += 1
        print(f"  PASS  {name}")
    else:
        _results["failed"] += 1
        print(f"  FAIL  {name}")


def raises(exception_type: type, action) -> bool:
    """True if calling ``action()`` raises ``exception_type`` (and nothing else)."""
    try:
        action()
    except exception_type:
        return True
    except Exception:            # a different error is still a failure of the check
        return False
    return False


def place(name: str) -> Location:
    return Location.from_place_name(name)


NOON = datetime(2026, 9, 28, 12, 0)   # off-peak, daytime


class World:
    """A small, fresh copy of the app for one group of checks."""

    _plate_counter = 0

    def __init__(self, start: datetime = NOON) -> None:
        self.clock = SimulatedClock(start)
        self.captains = CaptainService(self.clock)
        self.customers = CustomerService(self.clock)
        self.fares = FareService()
        self.matching = MatchingService(self.captains, self.fares)
        self.earnings = EarningsService()
        self.payments = PaymentService(self.earnings)
        self.ratings = RatingService()
        self.notifiers: list[Notifier] = [SmsNotifier(), PushNotifier(echo=False)]
        self.rides = RideService(self.clock, ServiceArea.chennai_metro(), self.fares, self.matching,
                                 self.payments, self.notifiers, otp_rng=random.Random(7))

    def captain(self, name: str, home: str, vehicle_class: type, online: bool = True,
                opening_dues: Money | None = None) -> Captain:
        World._plate_counter += 1
        plate = f"TN-{World._plate_counter % 90 + 10:02d}-ZZ-{World._plate_counter:04d}"
        captain = self.captains.register_captain(name, "+91 98765 43210", home,
                                                 vehicle_class(plate, "Test vehicle"),
                                                 opening_dues=opening_dues)
        captain.verify_kyc()
        if online:
            captain.go_online()
        return captain

    def customer(self, name: str, home: str, wallet: str = "5000") -> Customer:
        customer = self.customers.register_customer(name, "+91 91234 56789", home, Money(wallet))
        return customer

    def arrive(self, customer: Customer, drop: str, vehicle_type: VehicleType, **options) -> Ride:
        ride = self.rides.book_ride(customer, place(drop), vehicle_type, **options)
        self.rides.captain_arrives(ride)
        return ride

    def start(self, customer: Customer, drop: str, vehicle_type: VehicleType, **options) -> Ride:
        ride = self.arrive(customer, drop, vehicle_type, **options)
        self.rides.start_ride(ride, ride.otp_for(customer))
        return ride

    def full_ride(self, customer: Customer, drop: str, vehicle_type: VehicleType, **options) -> Ride:
        ride = self.start(customer, drop, vehicle_type, **options)
        self.rides.complete_trip(ride)
        return ride


# ====================================================================== groups of checks
def check_fares() -> None:
    fares = FareService()
    five_km = Decimal("5")
    bike = Bike("TN-01-AA-0001", "Honda Activa")
    auto = Auto("TN-01-AA-0002", "Bajaj RE Auto")
    economy = CabEconomy("TN-01-AA-0003", "Maruti Dzire")
    premium = CabPremium("TN-01-AA-0004", "Toyota Innova Crysta")
    check("Bike fare 5 km / 15 min = ₹47.50", bike.calculate_base_fare(five_km, 15) == Money("47.50"))
    check("Auto fare 5 km / 15 min = ₹100.00", auto.calculate_base_fare(five_km, 15) == Money("100.00"))
    check("Cab Economy fare 5 km / 15 min = ₹152.50", economy.calculate_base_fare(five_km, 15) == Money("152.50"))
    check("Cab Premium fare 5 km / 15 min = ₹220.00", premium.calculate_base_fare(five_km, 15) == Money("220.00"))
    ordered = [v.calculate_base_fare(five_km, 15) for v in (bike, auto, economy, premium)]
    check("Bike < Auto < Cab Economy < Cab Premium", ordered[0] < ordered[1] < ordered[2] < ordered[3])
    check("Minimum fare applied (bike 0.6 km, 1 min → ₹25)",
          bike.calculate_base_fare(Decimal("0.6"), 1) == Money("25"))
    check("Seat capacity matches VehicleType (1/3/4/6)",
          [v.seat_capacity for v in (bike, auto, economy, premium)] == [1, 3, 4, 6]
          and all(v.seat_capacity == v.vehicle_type.seats for v in (bike, auto, economy, premium)))

    day = datetime(2026, 9, 28)
    check("10:59 PM is not night", not SimulatedClock.is_night(day.replace(hour=22, minute=59)))
    check("11:00 PM is night", SimulatedClock.is_night(day.replace(hour=23, minute=0)))
    check("4:59 AM is night, 5:00 AM is not",
          SimulatedClock.is_night(day.replace(hour=4, minute=59)) and not SimulatedClock.is_night(day.replace(hour=5)))
    pickup, drop = place("Chennai Airport"), place("Anna Nagar")
    before = fares.estimate(pickup, drop, VehicleType.CAB_PREMIUM, Decimal("1.0"), day.replace(hour=22, minute=59), 5)
    at_night = fares.estimate(pickup, drop, VehicleType.CAB_PREMIUM, Decimal("1.0"), day.replace(hour=23), 5)
    check("Night estimate is +20% (10:59 PM vs 11:00 PM)",
          abs(at_night.fare.amount - before.fare.amount * Decimal("1.2")) <= Decimal("0.02"))

    world = World(day.replace(hour=22, minute=50))
    world.captain("Night Captain", "Chennai Airport", CabPremium)
    rider = world.customer("Night Rider", "Chennai Airport")
    ride = world.arrive(rider, "Anna Nagar", VehicleType.CAB_PREMIUM)
    world.clock.advance_to(day.replace(hour=22, minute=59))
    world.rides.start_ride(ride, ride.otp_for(rider))
    world.rides.complete_trip(ride)
    check("Ride starting 10:59 PM has no night charge", ride.receipt.night_charge.is_zero())
    world.clock.advance_to(day.replace(hour=23, minute=30))
    second = world.customer("Late Rider", "Chennai Airport")
    world.captain("Night Captain 2", "Chennai Airport", CabPremium)
    ride2 = world.full_ride(second, "Anna Nagar", VehicleType.CAB_PREMIUM)
    check("Ride starting after 11:00 PM has a night charge", ride2.receipt.night_charge > Money.zero())

    receipt = ride2.receipt
    check("GST is 5% of the fare before GST", receipt.gst == receipt.fare_before_gst.percent(Decimal("0.05")))
    check("Total = fare before GST + GST", receipt.total == receipt.fare_before_gst + receipt.gst)

    peak = day.replace(hour=9)
    hotspot, normal = place("Chennai Central"), place("Adyar")
    check("Surge capped at 2.0x", fares.surge_multiplier(hotspot, 50, 1, peak) == Decimal("2.0"))
    check("No surge off-peak with enough captains", fares.surge_multiplier(normal, 1, 3, day.replace(hour=14)) == Decimal("1.0"))
    check("Peak surge is higher at a hotspot",
          fares.surge_multiplier(hotspot, 1, 3, peak) > fares.surge_multiplier(normal, 1, 3, peak))

    world = World()
    far_captain = world.captain("Far Captain", "Guindy", Bike)
    rider = world.customer("Waiter", "Velachery")
    ride = world.arrive(rider, "Adyar", VehicleType.BIKE)
    world.clock.advance(minutes=4)
    world.rides.start_ride(ride, ride.otp_for(rider))
    world.rides.complete_trip(ride)
    check("4 min wait → ₹1 waiting charge (3 min free)", ride.receipt.waiting_charge == Money("1"))
    check("Captain's trip to pickup is never billed",
          ride.receipt.distance_km == FareService.road_km(place("Velachery"), place("Adyar")))
    check("Final receipt shows the estimate too", ride.receipt.estimated_total == ride.estimate.fare)
    rider2 = world.customer("Waiter Two", "Adyar")
    ride = world.arrive(rider2, "Velachery", VehicleType.BIKE)
    world.clock.advance(minutes=3)
    world.rides.start_ride(ride, ride.otp_for(rider2))
    world.rides.complete_trip(ride)
    check("3 min wait → no waiting charge", ride.receipt.waiting_charge.is_zero())

    world = World()
    world.captain("Coupon Bike", "Adyar", Bike)
    world.captain("Coupon SUV", "Adyar", CabPremium)
    rider = world.customer("Coupon User", "Adyar")
    small = world.full_ride(rider, "Besant Nagar", VehicleType.BIKE, coupon_code="FIRSTRIDE")
    check("Coupon not applied below ₹100 minimum", small.receipt.coupon_discount.is_zero())
    big = world.full_ride(rider, "T. Nagar", VehicleType.CAB_PREMIUM, coupon_code="FIRSTRIDE")
    check("FIRSTRIDE takes ₹50 off", big.receipt.coupon_discount == Money("50"))
    check("Total never below the vehicle minimum fare", big.receipt.fare_before_gst >= Money("150"))
    check("FIRSTRIDE only once per customer",
          raises(InvalidBookingError, lambda: world.rides.book_ride(rider, place("Adyar"), VehicleType.BIKE,
                                                                    coupon_code="FIRSTRIDE")))


def check_transitions() -> None:
    world = World()
    world.captain("T1", "Adyar", Bike)
    world.captain("T2", "Adyar", Bike)
    world.captain("T3", "Adyar", Bike)
    alice = world.customer("Alice", "Adyar")

    ride = world.rides.book_ride(alice, place("Guindy"), VehicleType.BIKE)
    check("Booking → CAPTAIN_ASSIGNED", ride.status == RideStatus.CAPTAIN_ASSIGNED)
    check("start() on CAPTAIN_ASSIGNED raises", raises(InvalidRideStatusError, lambda: ride.start("1234", world.clock.now)))
    check("end_trip() before start raises", raises(InvalidRideStatusError, lambda: ride.end_trip(world.clock.now)))
    check("change_destination() before start raises",
          raises(InvalidRideStatusError, lambda: ride.change_destination(place("Adyar"), place("Egmore"), world.clock.now)))
    world.rides.captain_arrives(ride)
    check("→ CAPTAIN_ARRIVED", ride.status == RideStatus.CAPTAIN_ARRIVED)
    check("mark_arrived() twice raises", raises(InvalidRideStatusError, lambda: ride.mark_arrived(world.clock.now)))
    check("Completing a ride that never started raises",
          raises(InvalidRideStatusError, lambda: world.rides.complete_trip(ride)))
    world.rides.start_ride(ride, ride.otp_for(alice))
    check("→ IN_PROGRESS", ride.status == RideStatus.IN_PROGRESS)
    check("cancel() while IN_PROGRESS raises",
          raises(InvalidRideStatusError, lambda: world.rides.cancel_by_customer(ride)))
    check("assign_captain() while IN_PROGRESS raises",
          raises(InvalidRideStatusError, lambda: ride.assign_captain(ride.captain, 1, world.clock.now)))
    world.rides.complete_trip(ride)
    check("→ COMPLETED after payment", ride.status == RideStatus.COMPLETED)
    check("end_early() on COMPLETED raises",
          raises(InvalidRideStatusError, lambda: ride.end_early(place("Adyar"), world.clock.now)))
    check("cancel() on COMPLETED raises",
          raises(InvalidRideStatusError, lambda: ride.cancel(CancellationReason.CUSTOMER_CANCELLED, alice, world.clock.now)))
    check("status has no setter", raises(AttributeError, lambda: setattr(ride, "status", RideStatus.REQUESTED)))
    times = [moment for moment, _ in ride.timeline]
    check("Every status change is timestamped, in order", len(times) >= 6 and times == sorted(times))

    cancelled = world.rides.book_ride(alice, place("Adyar"), VehicleType.BIKE)
    world.rides.cancel_by_customer(cancelled)
    check("CAPTAIN_ASSIGNED → CANCELLED", cancelled.status == RideStatus.CANCELLED)
    check("start() on a CANCELLED ride raises", raises(InvalidRideStatusError, lambda: cancelled.start("1234", world.clock.now)))
    arrived = world.arrive(alice, "Adyar", VehicleType.BIKE)
    world.rides.cancel_by_customer(arrived)
    check("CAPTAIN_ARRIVED → CANCELLED", arrived.status == RideStatus.CANCELLED)

    rematch = world.rides.book_ride(alice, place("Adyar"), VehicleType.BIKE)
    first = rematch.captain
    world.rides.cancel_by_captain(rematch)
    check("Captain cancel: CAPTAIN_ASSIGNED → REQUESTED → CAPTAIN_ASSIGNED (new captain)",
          rematch.status == RideStatus.CAPTAIN_ASSIGNED and rematch.captain != first)
    world.rides.captain_arrives(rematch)
    world.rides.start_ride(rematch, rematch.otp_for(alice))
    halfway = rematch.pickup.point_towards(rematch.drop, 0.5, "Halfway")
    world.rides.change_destination(rematch, halfway, place("Besant Nagar"))
    check("Second destination change raises",
          raises(InvalidRideStatusError, lambda: world.rides.change_destination(rematch, place("Besant Nagar"), place("Guindy"))))
    world.rides.complete_trip(rematch)
    check("end_trip() twice raises", raises(InvalidRideStatusError, lambda: rematch.end_trip(world.clock.now)))

    pending_world = World()
    pending_world.captain("P1", "Adyar", Bike)
    bob = pending_world.customer("Bob", "Adyar")
    failing_upi = UpiPayment("bob@oksbi", random.Random(1), failure_rate=1.0)
    ride = pending_world.start(bob, "Guindy", VehicleType.BIKE, payment_method=failing_upi)
    check("Failed payment raises PaymentFailedError", raises(PaymentFailedError, lambda: pending_world.rides.complete_trip(ride)))
    check("IN_PROGRESS → PAYMENT_PENDING", ride.status == RideStatus.PAYMENT_PENDING)
    pending_world.rides.pay_for_ride(ride, CashPayment())
    check("PAYMENT_PENDING → COMPLETED after cash", ride.status == RideStatus.COMPLETED)

    # a REQUESTED ride can be cancelled (before any captain) — built directly on the model
    request_world = World()
    carol = request_world.customer("Carol", "Adyar")
    check("No captain at all → NoCaptainAvailableError and ride CANCELLED",
          raises(NoCaptainAvailableError, lambda: request_world.rides.book_ride(carol, place("Guindy"), VehicleType.BIKE))
          and carol.ride_history[-1].status == RideStatus.CANCELLED
          and carol.ride_history[-1].cancellation_reason == CancellationReason.NO_CAPTAIN_AVAILABLE)


def check_otp() -> None:
    world = World()
    world.captain("O1", "Adyar", Bike)
    dan = world.customer("Dan", "Adyar")
    ride = world.arrive(dan, "Guindy", VehicleType.BIKE)
    first_error = None
    try:
        world.rides.start_ride(ride, "0000" if ride.otp_for(dan) != "0000" else "0001")
    except InvalidOtpError as error:
        first_error = error
    check("1st wrong OTP: 2 attempts left", first_error is not None and first_error.attempts_left == 2)
    check("Ride still waiting after 1 wrong OTP", ride.status == RideStatus.CAPTAIN_ARRIVED)
    wrong = "0000" if ride.otp_for(dan) != "0000" else "0001"
    check("2nd wrong OTP raises", raises(InvalidOtpError, lambda: world.rides.start_ride(ride, wrong)))
    check("3rd wrong OTP raises", raises(InvalidOtpError, lambda: world.rides.start_ride(ride, wrong)))
    check("3 wrong OTPs auto-cancel as OTP_FAILED",
          ride.status == RideStatus.CANCELLED and ride.cancellation_reason == CancellationReason.OTP_FAILED)
    check("OTP_FAILED cancellation has no fee", ride.cancellation_fee.is_zero() and dan.pending_fee_total.is_zero())
    check("ride.__otp is not accessible from outside", raises(AttributeError, lambda: ride.__otp))
    check("…because name mangling stored it as _Ride__otp", hasattr(ride, "_Ride__otp"))
    stranger = world.customer("Stranger", "Adyar")
    check("Only the ride's customer can see the OTP", raises(UnauthorizedAccessError, lambda: ride.otp_for(stranger)))

    world2 = World()
    world2.captain("O2", "Adyar", Bike)
    eve = world2.customer("Eve", "Adyar")
    ride = world2.arrive(eve, "Guindy", VehicleType.BIKE)
    wrong = "0000" if ride.otp_for(eve) != "0000" else "0001"
    raises(InvalidOtpError, lambda: world2.rides.start_ride(ride, wrong))
    world2.rides.start_ride(ride, ride.otp_for(eve))
    check("Wrong OTP then correct OTP starts the ride", ride.status == RideStatus.IN_PROGRESS)


def check_abstraction() -> None:
    check("User cannot be instantiated", raises(TypeError, lambda: User("X", "+91 98765 43210", place("Egmore"))))
    check("Vehicle cannot be instantiated", raises(TypeError, lambda: Vehicle("TN-01-AA-0001", "X", "Red")))
    check("Cab cannot be instantiated", raises(TypeError, lambda: Cab("TN-01-AA-0001", "X", "Red")))
    check("Payable cannot be instantiated", raises(TypeError, lambda: Payable()))
    check("Notifier cannot be instantiated", raises(TypeError, lambda: Notifier()))
    premium = CabPremium("TN-01-AA-0009", "Toyota Innova Crysta")
    check("Multilevel: CabPremium IS-A Cab IS-A Vehicle",
          isinstance(premium, Cab) and isinstance(premium, Vehicle) and issubclass(CabPremium, Cab))
    check("Hierarchical: Customer and Captain are both Users",
          issubclass(Customer, User) and issubclass(Captain, User) and not issubclass(Customer, Captain))


def check_value_objects() -> None:
    egmore = place("Egmore")
    check("Location attribute cannot be changed", raises(AttributeError, lambda: setattr(egmore, "latitude", 1.0)))
    check("Location cannot gain new attributes", raises(AttributeError, lambda: setattr(egmore, "nickname", "x")))
    check("Locations compare by value", place("Egmore") == Location("Egmore", 13.0732, 80.2609))
    check("Location works as a dict key", {place("Egmore"): "station"}[place("Egmore")] == "station")
    check("Location works in a set", len({place("Egmore"), place("Egmore"), place("Adyar")}) == 2)

    fare = Money("100")
    check("Money attribute cannot be changed", raises(AttributeError, lambda: setattr(fare, "amount", Decimal("1"))))
    check("Money compares by value", Money("100") == Money("100.00") and Money(100) == Money("100"))
    check("Money works as a dict key and in a set",
          {Money("5"): "five"}[Money("5.00")] == "five" and len({Money("5"), Money("5.00"), Money("6")}) == 2)
    check("Money + Money", Money("100") + Money("5.50") == Money("105.50"))
    check("Money - Money", Money("100") - Money("30.25") == Money("69.75"))
    check("Money * Decimal", Money("12") * Decimal("2.5") == Money("30"))
    check("int * Money", 3 * Money("1.10") == Money("3.30"))
    check("Money < and <=", Money("1") < Money("2") and Money("2") <= Money("2") and not Money("3") <= Money("2"))
    check("ROUND_HALF_UP to 2 places", Money("0.005") == Money("0.01") and Money("2.675") == Money("2.68"))
    check("Money rejects float", raises(TypeError, lambda: Money(0.1)))
    check("Money + int rejected", raises(TypeError, lambda: Money("1") + 1))
    check("Money prints as ₹1,234.50", str(Money("1234.5")) == "₹1,234.50")


def check_location_continuity() -> None:
    world = World()
    world.captain("L1", "Mylapore", Auto)
    fay = world.customer("Fay", "Mylapore")
    ride = world.full_ride(fay, "Adyar", VehicleType.AUTO)
    check("Normal drop: captain location == drop", ride.captain.location == place("Adyar"))
    check("Normal drop: customer location == drop", fay.location == place("Adyar"))

    ride = world.start(fay, "Tambaram", VehicleType.AUTO)
    world.rides.end_trip_early(ride, place("Guindy"))
    check("End early: captain location == stop point", ride.captain.location == place("Guindy"))
    check("End early: customer location == stop point", fay.location == place("Guindy"))
    check("End early: fare on actual distance only",
          ride.receipt.distance_km == FareService.road_km(place("Adyar"), place("Guindy")))

    ride = world.start(fay, "Adyar", VehicleType.AUTO)
    change_point = ride.pickup.point_towards(ride.drop, 0.5, "Midway")
    world.rides.change_destination(ride, change_point, place("Besant Nagar"))
    world.rides.complete_trip(ride)
    check("Change destination: route is pickup → change point → new drop",
          ride.route == (place("Guindy"), change_point, place("Besant Nagar")))
    check("Change destination: captain and customer at new drop",
          ride.captain.location == place("Besant Nagar") and fay.location == place("Besant Nagar"))
    expected_km = (Decimal(str(place("Guindy").road_distance_to(change_point) + change_point.road_distance_to(place("Besant Nagar"))))
                   .quantize(Decimal("0.01")))
    check("Change destination: fare distance uses the actual route", ride.receipt.distance_km == expected_km)

    world2 = World()
    captain = world2.captain("L2", "Guindy", Bike)
    gia = world2.customer("Gia", "Velachery")
    ride = world2.rides.book_ride(gia, place("Adyar"), VehicleType.BIKE)
    world2.rides.cancel_by_customer(ride)
    check("Customer cancels before arrival: captain did not move", captain.location == place("Guindy"))
    ride = world2.rides.book_ride(gia, place("Adyar"), VehicleType.BIKE)
    check("mark_arrived() far from pickup raises", raises(CaptainNotAtPickupError, lambda: ride.mark_arrived(world2.clock.now)))
    world2.captain("L3", "Velachery", Bike, online=True)
    world2.rides.cancel_by_captain(ride)
    check("Captain cancels: they stay where they were", captain.location == place("Guindy"))
    check("Customer relocating too fast is refused (no teleporting)",
          raises(InvalidBookingError, lambda: Customer("Hop", "+91 91234 56789", place("Velachery"), Money("0"), NOON)
                 .relocate_to(place("Washermanpet"), NOON + timedelta(minutes=5))))


def check_matching() -> None:
    world = World()
    near_rejector = world.captain("Rejector", "Adyar", Bike)                 # nearest, will reject
    eligible = world.captain("Eligible", "Besant Nagar", Bike)
    offline = world.captain("Offline", "Adyar", Bike, online=False)
    far = world.captain("Far", "Washermanpet", Bike)
    wrong_type = world.captain("Autowala", "Adyar", Auto)
    busy = world.captain("Busy", "Adyar", Bike)
    busy_customer = world.customer("BusyCust", "Adyar")
    busy_ride = world.rides.book_ride(busy_customer, place("Guindy"), VehicleType.BIKE)
    check("Busy captain setup", busy_ride.captain in (near_rejector, busy))
    # Make sure "Busy" is the one on a ride
    if busy_ride.captain is not busy:
        world.rides.cancel_by_customer(busy_ride)
        near_rejector.plan_to_reject_next_offer()
        busy_ride = world.rides.book_ride(busy_customer, place("Guindy"), VehicleType.BIKE)
    near_rejector.plan_to_reject_next_offer()
    rider = world.customer("Rider", "Adyar")
    ride = world.rides.book_ride(rider, place("Guindy"), VehicleType.BIKE)
    offered = [line.split("→ ")[1].split(" (")[0] for line in ride.offer_log]
    check("Nearest captain offered first, then next-nearest", offered[:2] == ["Rejector", "Eligible"])
    check("Rejected → next eligible captain assigned", ride.captain is eligible)
    check("Offline captain never offered", "Offline" not in offered)
    check("Captain outside radius never offered", "Far" not in offered)
    check("Wrong vehicle type never offered", "Autowala" not in offered)
    check("Busy captain never offered", "Busy" not in offered)
    check("Captain who rejected THIS ride is excluded",
          world.matching.ineligibility_reason(near_rejector, ride, world.clock.now) is not None
          and ride.is_excluded(near_rejector))
    check("Acceptance rate tracked", near_rejector.acceptance_rate < Decimal("100"))

    world2 = World()
    for name in ("R1", "R2", "R3"):
        world2.captain(name, "Adyar", Bike).plan_to_reject_next_offer()
    world2.captain("R4", "Adyar", Bike)
    rider = world2.customer("Unlucky", "Adyar")
    check("3 rejections → NoCaptainAvailableError even if a 4th exists",
          raises(NoCaptainAvailableError, lambda: world2.rides.book_ride(rider, place("Guindy"), VehicleType.BIKE)))

    world3 = World()
    pending = world3.captain("Kyc", "Adyar", Bike, online=False)
    new = Captain("Newbie", "+91 98765 43210", place("Adyar"), Bike("TN-55-QQ-5555", "TVS Jupiter"))
    check("KYC-pending captain cannot go online", raises(CaptainNotEligibleError, new.go_online))
    captain = world3.captain("Driver", "Adyar", Bike)
    rider = world3.customer("Mid", "Adyar")
    ride = world3.start(rider, "Guindy", VehicleType.BIKE)
    check("Captain cannot go offline mid-ride", raises(CaptainBusyError, captain.go_offline))
    world3.rides.complete_trip(ride)
    captain.go_offline()
    check("Captain can go offline after the ride", str(captain.status) == "Offline")
    check("A bike captain never receives an auto request",
          raises(NoCaptainAvailableError, lambda: world3.rides.book_ride(rider, place("Adyar"), VehicleType.AUTO)))
    check("Too many passengers for a bike is rejected",
          raises(InvalidBookingError, lambda: world3.rides.book_ride(rider, place("Adyar"), VehicleType.BIKE, passengers=2)))
    _ = pending


def check_canonical() -> None:
    world = World(datetime(2026, 9, 28, 9, 10))
    at_egmore = world.captain("Senthil", "Egmore", CabEconomy)
    near_egmore = world.captain("Balaji", "Royapettah", CabEconomy)
    selvi = world.customer("Selvi", "Egmore")
    ride = world.full_ride(selvi, "Tambaram", VehicleType.CAB_ECONOMY)
    check("Canonical: Egmore captain took the Egmore → Tambaram ride", ride.captain is at_egmore)
    check("Canonical: after the ride the captain is at Tambaram", at_egmore.location == place("Tambaram"))
    karthik = world.customer("Karthik", "Egmore")
    egmore_ride = world.rides.book_ride(karthik, place("Anna Nagar"), VehicleType.CAB_ECONOMY)
    check("Canonical: next Egmore request goes to a DIFFERENT captain near Egmore", egmore_ride.captain is near_egmore)
    lakshmi = world.customer("Lakshmi", "Chromepet")
    chromepet_ride = world.rides.book_ride(lakshmi, place("Guindy"), VehicleType.CAB_ECONOMY)
    check("Canonical: Chromepet request goes to the captain now at Tambaram", chromepet_ride.captain is at_egmore)
    arun = world.customer("Arun", "Pallavaram")
    world.captain("Third", "Tambaram", CabEconomy)
    pallavaram_ride = world.rides.book_ride(arun, place("Guindy"), VehicleType.CAB_ECONOMY)
    check("Canonical: a Pallavaram request can go to a Tambaram captain",
          pallavaram_ride.captain.location == place("Tambaram"))


def check_cancellations() -> None:
    world = World()
    captain = world.captain("C1", "Guindy", Bike)
    world.captain("C2", "Guindy", Auto)
    hema = world.customer("Hema", "Adyar")
    ride = world.rides.book_ride(hema, place("Guindy"), VehicleType.BIKE)
    world.clock.advance(minutes=2)
    check("Cancel exactly 2 min after assignment is free", world.rides.cancel_by_customer(ride).is_zero())
    ride = world.rides.book_ride(hema, place("Guindy"), VehicleType.BIKE)
    world.clock.advance(minutes=3)
    check("Bike cancel after the grace period costs ₹20", world.rides.cancel_by_customer(ride) == Money("20"))
    check("Fee is pending, not charged yet", hema.pending_fee_total == Money("20") and hema.wallet_balance == Money("5000"))
    ride = world.arrive(hema, "Guindy", VehicleType.AUTO)
    check("Auto cancel after arrival costs ₹30", world.rides.cancel_by_customer(ride) == Money("30"))
    check("Pending fees add up (₹50)", hema.pending_fee_total == Money("50"))
    ride = world.full_ride(hema, "Guindy", VehicleType.BIKE)
    check("Next ride's receipt carries the previous cancellation fee", ride.receipt.previous_cancellation_fee == Money("50"))
    check("The fee is cleared after being billed", hema.pending_fee_total.is_zero())
    ride = world.full_ride(hema, "Adyar", VehicleType.BIKE)
    check("The fee is billed exactly once", ride.receipt.previous_cancellation_fee.is_zero())
    check("History keeps cancelled rides, in order",
          [r.status for r in hema.ride_history][:3] == [RideStatus.CANCELLED] * 3
          and [r.request.requested_at for r in hema.ride_history] == sorted(r.request.requested_at for r in hema.ride_history))
    check("Cancelled ride cannot be rated",
          raises(InvalidRatingError, lambda: world.ratings.rate_captain(hema.ride_history[0], 5)))
    check("Rating out of range rejected", raises(InvalidRatingError, lambda: world.ratings.rate_captain(ride, 6)))
    world.ratings.rate_captain(ride, 4)
    check("Rating twice rejected", raises(InvalidRatingError, lambda: world.ratings.rate_captain(ride, 5)))
    world.ratings.rate_customer(ride, 5)
    check("Each side rates once", ride.rating_by_customer == 4 and ride.rating_by_captain == 5)

    world2 = World()
    world2.captain("N1", "Adyar", Auto)
    ivan = world2.customer("Ivan", "Adyar")
    ride = world2.arrive(ivan, "Guindy", VehicleType.AUTO)
    world2.clock.advance(minutes=4)
    check("No-show before 5 min is refused", raises(InvalidRideStatusError, lambda: world2.rides.cancel_no_show(ride)))
    world2.clock.advance(minutes=1)
    no_show_captain = ride.captain
    fee = world2.rides.cancel_no_show(ride)
    check("No-show fee ₹30 charged to the wallet", fee == Money("30") and ivan.wallet_balance == Money("4970"))
    check("Captain earns the no-show fee minus 20%",
          world2.earnings.earnings_for(no_show_captain, NOON.date()).net_earnings == Money("24"))
    check("Captain is free at the pickup after a no-show",
          no_show_captain.is_available and no_show_captain.location == place("Adyar"))

    flag_captain = Captain("Flaggy", "+91 98765 43210", place("Adyar"), Bike("TN-66-FF-6666", "Honda Activa"),
                           past_ratings=[4, 4, 3, 4])
    check("4 ratings: not flagged yet", not flag_captain.is_flagged)
    flag_captain.receive_rating(3)
    check("Average below 4.0 after 5 ratings → flagged", flag_captain.is_flagged)


def check_booking_rules() -> None:
    world = World()
    world.captain("B1", "Adyar", Bike)
    world.captain("B2", "Adyar", Bike)
    jaya = world.customer("Jaya", "Adyar")
    check("Pickup equals drop rejected", raises(InvalidBookingError, lambda: world.rides.book_ride(jaya, place("Adyar"), VehicleType.BIKE)))
    near = Location("Adyar signal", 13.0030, 80.2565)
    check("Drop under 500 m rejected", raises(InvalidBookingError, lambda: world.rides.book_ride(jaya, near, VehicleType.BIKE)))
    check("Drop outside the service area rejected",
          raises(OutOfServiceAreaError, lambda: world.rides.book_ride(jaya, place("Mahabalipuram"), VehicleType.BIKE)))
    far_north = Location("Ponneri side", 13.24, 80.34)
    far_west = Location("Sriperumbudur side", 12.81, 79.96)
    edge = Customer("Edge", "+91 91234 56789", far_north, Money("0"), NOON)
    check("Trip over 60 km rejected", raises(InvalidBookingError, lambda: world.rides.book_ride(edge, far_west, VehicleType.BIKE)))
    estimates = world.rides.fare_estimates(jaya, place("Guindy"))
    check("Estimates cover all 4 vehicle types with availability",
          len(estimates) == 4 and estimates[0].is_available and not estimates[3].is_available)

    ride = world.rides.book_ride(jaya, place("Guindy"), VehicleType.BIKE)
    check("Second booking while active raises ActiveRideExistsError",
          raises(ActiveRideExistsError, lambda: world.rides.book_ride(jaya, place("Egmore"), VehicleType.BIKE)))
    world.rides.captain_arrives(ride)
    world.rides.start_ride(ride, ride.otp_for(jaya))
    failing = UpiPayment("jaya@oksbi", random.Random(3), failure_rate=1.0)
    raises(PaymentFailedError, lambda: world.rides.complete_trip(ride, payment_method=failing))
    check("Payment pending blocks booking",
          raises(PaymentPendingError, lambda: world.rides.book_ride(jaya, place("Adyar"), VehicleType.BIKE)))
    world.rides.pay_for_ride(ride, CashPayment())
    check("After paying, booking works again",
          world.rides.book_ride(jaya, place("Adyar"), VehicleType.BIKE).status == RideStatus.CAPTAIN_ASSIGNED)

    check("Top-up must be positive", raises(InvalidAmountError, lambda: jaya.top_up(Money("0"))))
    check("Top-up above ₹10,000 rejected", raises(InvalidAmountError, lambda: jaya.top_up(Money("10000.01"))))
    jaya.top_up(Money("10000"))
    check("Top-up of exactly ₹10,000 allowed", jaya.wallet_balance == Money("15000"))


def check_payments() -> None:
    world = World()
    world.captain("W1", "Adyar", CabPremium)
    kavi = world.customer("Kavi", "Adyar", wallet="100")
    ride = world.start(kavi, "Egmore", VehicleType.CAB_PREMIUM, payment_method=WalletPayment(kavi))
    check("Low wallet raises InsufficientWalletBalanceError",
          raises(InsufficientWalletBalanceError, lambda: world.rides.complete_trip(ride)))
    check("No partial wallet deduction", kavi.wallet_balance == Money("100"))
    try:
        world.rides.pay_for_ride(ride, WalletPayment(kavi))
    except InsufficientWalletBalanceError as error:
        check("Wallet error carries required and available", error.required == str(ride.receipt.total)
              and error.available == "₹100.00")
    kavi.top_up(Money("2000"))
    world.rides.pay_for_ride(ride, WalletPayment(kavi))
    check("Wallet pays in full once topped up", kavi.wallet_balance == Money("2100") - ride.receipt.total)
    check("Paying the same ride again raises DuplicatePaymentError",
          raises(DuplicatePaymentError, lambda: world.rides.pay_for_ride(ride, CashPayment())))
    successes = [r for r in world.payments.ledger if r.succeeded]
    check("Exactly one successful payment for the ride", len(successes) == 1)

    payables: list[Payable] = [UpiPayment("x@okaxis", random.Random(1)), CashPayment(), WalletPayment(kavi)]
    references = [payable.pay(Money("1")) for payable in payables]      # polymorphism
    check("Polymorphism: every Payable pays via the same call", len(set(references)) == 3)


def check_cash_block() -> None:
    world = World()
    murugan = world.captain("Murugan", "Chennai Central", Auto, opening_dues=Money("490"))
    lata = world.customer("Lata", "Chennai Central")
    ride = world.full_ride(lata, "Egmore", VehicleType.AUTO, payment_method=CashPayment())
    check("Cash ride increases dues", murugan.dues_owed > Money("490"))
    check("Dues above ₹500 block cash rides", not murugan.accepts_cash_rides)
    check("Blocked captain is skipped for a cash ride",
          raises(NoCaptainAvailableError, lambda: world.rides.book_ride(lata, place("Chennai Central"), VehicleType.AUTO,
                                                                        payment_method=CashPayment())))
    upi_ride = world.rides.book_ride(lata, place("Chennai Central"), VehicleType.AUTO,
                                     payment_method=UpiPayment("lata@okaxis", random.Random(9)))
    check("Blocked captain still gets non-cash rides", upi_ride.captain is murugan)
    world.rides.cancel_by_customer(upi_ride)
    settled = world.captains.settle_dues(murugan)
    check("settle_dues() clears dues and unblocks", settled > Money("500") and murugan.dues_owed.is_zero()
          and murugan.accepts_cash_rides)
    cash_ride = world.rides.book_ride(lata, place("Chennai Central"), VehicleType.AUTO, payment_method=CashPayment())
    check("After settling, cash rides are matched again", cash_ride.captain is murugan)
    _ = ride


def check_clock() -> None:
    clock = SimulatedClock(NOON)
    check("advance() with negative minutes raises", raises(TimeTravelError, lambda: clock.advance(minutes=-1)))
    check("advance_to() an earlier time raises", raises(TimeTravelError, lambda: clock.advance_to(NOON - timedelta(minutes=1))))
    clock.advance(minutes=10)
    check("Clock moves forward", clock.now == NOON + timedelta(minutes=10))

    world = World()
    captain = world.captain("Clocky", "Adyar", Bike)
    rider = world.customer("Tick", "Adyar")
    first = world.full_ride(rider, "Guindy", VehicleType.BIKE)
    ended = first.trip_ended_at
    check("Captain free_since == time the last ride ended", captain.free_since == ended)
    other = world.customer("Tock", "Guindy")
    second = world.rides.book_ride(other, place("Adyar"), VehicleType.BIKE)
    check("Next ride is assigned no earlier than the previous one ended", second.assigned_at >= ended)
    world.rides.cancel_by_customer(second)
    check("A captain cannot start a ride before their previous ride ended",
          raises(TimeTravelError, lambda: captain.start_assignment(second, captain.free_since - timedelta(minutes=1))))


def check_full_day() -> None:
    from ridehailing.app.chennai_ride_app import ChennaiRideApp
    app = ChennaiRideApp()
    with contextlib.redirect_stdout(io.StringIO()):
        app.run()
    earnings = app.earnings_service
    check("Full day: books balance to the paisa (paid = earnings + commission + GST)",
          earnings.books_balance()
          and app.payment_service.total_collected == earnings.total_paid_by_customers)
    check("Full day: every wallet reconciles (opening + top-ups − spent = balance)", app.wallets_balance())
    ordered = True
    for captain in app.captain_service.captains:
        trips = sorted((ride for ride in app.ride_service.rides if ride.captain is captain and ride.started_at),
                       key=lambda ride: ride.started_at)
        for earlier, later in zip(trips, trips[1:]):
            if later.assigned_at < earlier.trip_ended_at:
                ordered = False
    check("Full day: no captain started a ride before their previous ride ended", ordered)
    timelines_ok = all([moment for moment, _ in ride.timeline] == sorted(moment for moment, _ in ride.timeline)
                       for ride in app.ride_service.rides)
    check("Full day: every ride timeline moves forward in time", timelines_ok)
    continuity = all(ride.customer.location is not None for ride in app.ride_service.rides)
    last_drop_ok = True
    for captain in app.captain_service.captains:
        finished = [ride for ride in app.ride_service.rides if ride.captain is captain and ride.trip_has_ended]
        if finished:
            last = max(finished, key=lambda ride: ride.trip_ended_at)
            later_moves = [ride for ride in app.ride_service.rides
                           if ride.captain is captain and ride.arrived_at and ride.arrived_at > last.trip_ended_at]
            if not later_moves and captain.location != last.drop:
                last_drop_ok = False
    check("Full day: each captain's location equals their last drop point", continuity and last_drop_ok)
    check("Full day: at least one captain was flagged", len(app.rating_service.flagged_captains(app.captain_service.captains)) >= 1)
    check("Full day: demo output is deterministic", _demo_output() == _demo_output())


def _demo_output() -> str:
    from ridehailing.app.chennai_ride_app import ChennaiRideApp
    buffer = io.StringIO()
    # Ids come from class-level counters, so compare the text with ids removed.
    with contextlib.redirect_stdout(buffer):
        ChennaiRideApp().run()
    lines = []
    for line in buffer.getvalue().splitlines():
        words = [word for word in line.split() if not word.startswith(("RD-", "CAP-", "CUS-", "UPI0", "CASH0", "WAL0"))]
        lines.append(" ".join(words))
    return "\n".join(lines)


GROUPS = [
    ("Fares, surge, night charge, waiting, coupon", check_fares),
    ("Ride status transitions", check_transitions),
    ("OTP rules and name mangling", check_otp),
    ("Abstraction and inheritance", check_abstraction),
    ("Immutable, hashable value objects; Money operators", check_value_objects),
    ("Location continuity", check_location_continuity),
    ("Matching eligibility", check_matching),
    ("Canonical Egmore → Tambaram case", check_canonical),
    ("Cancellations, no-show, ratings", check_cancellations),
    ("Booking rules: validation, one active ride, payment pending", check_booking_rules),
    ("Payments: wallet, duplicates, polymorphism", check_payments),
    ("Cash commission ₹500 block", check_cash_block),
    ("Simulated clock", check_clock),
    ("End-of-day reconciliation (full demo day)", check_full_day),
]


def run_checks() -> int:
    """Run every check group. Returns the process exit code (0 = all passed)."""
    print("RAPIDO CHENNAI — SELF-CHECK")
    for title, group in GROUPS:
        print(f"\n[{title}]")
        try:
            group()
        except (RideHailingError, Exception) as error:   # an unexpected error fails the group, not the run
            check(f"{title} ran without an unexpected {type(error).__name__}: {error}", False)
    total = _results["passed"] + _results["failed"]
    print(f"\n{_results['passed']}/{total} checks passed, {_results['failed']} failed.")
    if _results["failed"] == 0:
        print("SelfCheck: 100% PASS ✔")
        return 0
    print("SelfCheck: FAILURES ✘")
    return 1
