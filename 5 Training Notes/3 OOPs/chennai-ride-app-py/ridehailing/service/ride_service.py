"""RideService — runs every ride flow from booking to payment, and notifies on every event."""

from __future__ import annotations

import random
from datetime import timedelta
from decimal import Decimal

from ridehailing.exceptions import (InsufficientWalletBalanceError, InvalidBookingError,
                                    InvalidOtpError, InvalidRideStatusError,
                                    NoCaptainAvailableError, PaymentFailedError)
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.common.service_area import ServiceArea
from ridehailing.model.payment.payable import Payable
from ridehailing.model.payment.wallet_payment import WalletPayment
from ridehailing.model.ride.cancellation_reason import CancellationReason
from ridehailing.model.ride.fare_estimate import FareEstimate
from ridehailing.model.ride.fare_receipt import FareReceipt
from ridehailing.model.ride.ride import Ride
from ridehailing.model.ride.ride_request import RideRequest
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.customer import Customer, PendingFee
from ridehailing.model.user.user import User
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.notification.notifier import Notifier
from ridehailing.service.fare_service import FareService
from ridehailing.service.matching_service import MatchingService
from ridehailing.service.payment_service import PaymentService
from ridehailing.time.simulated_clock import SimulatedClock


class RideService:
    """Coordinates Ride, Customer, Captain, FareService, MatchingService and PaymentService.

    Composition: RideService is built from the services it uses (they are passed in once).
    No teleporting: whenever someone travels, the SimulatedClock moves forward by the travel time
    first (captain to pickup, pickup to drop), and only then does the location change.
    """

    MIN_TRIP_KM = 0.5
    MAX_TRIP_KM = Decimal("60")
    CANCELLATION_GRACE = timedelta(minutes=2)
    NO_SHOW_WAIT_MINUTES = 5

    def __init__(self, clock: SimulatedClock, service_area: ServiceArea, fare_service: FareService,
                 matching_service: MatchingService, payment_service: PaymentService,
                 notifiers: list[Notifier], otp_rng: random.Random) -> None:
        self._clock = clock
        self._service_area = service_area
        self._fares = fare_service
        self._matching = matching_service
        self._payments = payment_service
        self._notifiers = notifiers
        self._otp_rng = otp_rng
        self._rides: list[Ride] = []
        self._manual_offers = False   # False: the offered captain answers at once (scripted demo)

    @property
    def rides(self) -> tuple[Ride, ...]:
        return tuple(self._rides)

    # ================================================================ captain mode
    @property
    def manual_offers(self) -> bool:
        return self._manual_offers

    def set_manual_offers(self, enabled: bool) -> None:
        """Manual: an offer waits on the captain's phone until accept_offer() / reject_offer().
        Automatic (default): the offered captain answers immediately."""
        self._manual_offers = enabled

    # ================================================================ queries
    def active_rides(self) -> list[Ride]:
        """Rides that are not finished yet: booked, on the way, in progress or waiting for payment."""
        return [ride for ride in self._rides
                if ride.status.is_active() or ride.status == RideStatus.PAYMENT_PENDING]

    def last_completed_ride_for_captain(self, captain: Captain) -> Ride | None:
        for ride in reversed(self._rides):
            if ride.captain is captain and ride.status == RideStatus.COMPLETED:
                return ride
        return None

    def surge_at(self, place: Location, vehicle_type: VehicleType) -> Decimal:
        """The surge a new request from ``place`` would see right now."""
        return self._surge_for(place, vehicle_type)

    def cancellation_fee_if_cancelled_now(self, ride: Ride) -> Money:
        """What cancel_by_customer() would charge at this moment (used to warn before cancelling)."""
        if ride.status == RideStatus.CAPTAIN_ASSIGNED and self._clock.now - ride.assigned_at > RideService.CANCELLATION_GRACE:
            return ride.vehicle_type.cancellation_fee
        if ride.status == RideStatus.CAPTAIN_ARRIVED:
            return ride.vehicle_type.cancellation_fee
        return Money.zero()

    def position_now(self, ride: Ride) -> Location:
        """Where an IN_PROGRESS ride is at this moment, part-way from its last waypoint to the drop."""
        self._require_in_progress(ride, "report its position")
        minutes_needed = self._fares.travel_minutes(ride.captain.vehicle, ride.last_waypoint, ride.drop,
                                                    ride.last_waypoint_at)
        minutes_driven = (self._clock.now - ride.last_waypoint_at).total_seconds() / 60
        fraction = min(minutes_driven / minutes_needed, 1.0)
        return ride.last_waypoint.point_towards(ride.drop, fraction,
                                                f"En route to {ride.drop} ({round(fraction * 100)}%)")

    # ================================================================ estimates
    def fare_estimates(self, customer: Customer, drop: Location,
                       pickup: Location | None = None) -> list[FareEstimate]:
        """Every vehicle type side by side, like the real app screen. Pickup defaults to where the
        customer is now (a quote from another place is allowed; booking there needs relocating)."""
        pickup = pickup or customer.location
        self._validate_trip(pickup, drop, VehicleType.BIKE, passengers=1)
        estimates = []
        for vehicle_type in VehicleType:
            surge = self._surge_for(pickup, vehicle_type)
            eta = self._matching.nearest_eta(pickup, vehicle_type, self._clock.now)
            estimates.append(self._fares.estimate(pickup, drop, vehicle_type, surge, self._clock.now, eta))
        return estimates

    def _surge_for(self, pickup: Location, vehicle_type: VehicleType) -> Decimal:
        open_requests = 1   # this request itself
        for ride in self._rides:
            if (ride.status == RideStatus.REQUESTED and ride.vehicle_type == vehicle_type
                    and ride.pickup.is_within_km(pickup, MatchingService.SEARCH_RADIUS_KM)):
                open_requests += 1
        free_captains = len(self._matching.free_captains_near(pickup, vehicle_type))
        return self._fares.surge_multiplier(pickup, open_requests, free_captains, self._clock.now)

    # ================================================================ booking
    def book_ride(self, customer: Customer, drop: Location, vehicle_type: VehicleType,
                  passengers: int = 1, payment_method: Payable | None = None,
                  coupon_code: str | None = None) -> Ride:
        """Book a ride from the customer's current location.

        Python has NO method overloading (a second ``def book_ride`` would simply replace the
        first). The Pythonic alternative is default / keyword arguments: callers may write
        ``book_ride(c, drop, VehicleType.BIKE)`` or
        ``book_ride(c, drop, VehicleType.AUTO, payment_method=CashPayment(), coupon_code="FIRSTRIDE")``.
        """
        now = self._clock.now
        customer.ensure_can_book()                         # one active ride; no unpaid ride
        pickup = customer.location
        self._validate_trip(pickup, drop, vehicle_type, passengers)
        self._fares.validate_coupon(customer, coupon_code)
        payable = payment_method or customer.preferred_payment or WalletPayment(customer)

        surge = self._surge_for(pickup, vehicle_type)       # shown BEFORE confirming
        eta = self._matching.nearest_eta(pickup, vehicle_type, now)
        estimate = self._fares.estimate(pickup, drop, vehicle_type, surge, now, eta)
        request = RideRequest(customer, pickup, drop, vehicle_type, passengers, now, coupon_code)
        otp = str(self._otp_rng.randint(1000, 9999))
        ride = Ride(request, otp, estimate, payable, now)
        customer.begin_ride(ride)
        self._rides.append(ride)

        surge_text = f" (surge {surge}x)" if surge > 1 else ""
        self._notify(customer, f"Ride {ride.ride_id} booked: {pickup} → {drop} by "
                               f"{vehicle_type.display_name}. Estimated fare {estimate.fare}{surge_text}, "
                               f"paying by {payable.method_name}. Finding your captain…")
        self._match(ride)
        return ride

    def _validate_trip(self, pickup: Location, drop: Location, vehicle_type: VehicleType,
                       passengers: int) -> None:
        self._service_area.ensure_contains(pickup)
        self._service_area.ensure_contains(drop)
        if pickup.distance_to(drop) < RideService.MIN_TRIP_KM:
            raise InvalidBookingError(f"Pickup {pickup} and drop {drop} are less than 500 m apart.")
        road_km = FareService.road_km(pickup, drop)
        if road_km > RideService.MAX_TRIP_KM:
            raise InvalidBookingError(f"{pickup} → {drop} is {road_km} km; the limit is {RideService.MAX_TRIP_KM} km.")
        if not vehicle_type.can_carry(passengers):
            raise InvalidBookingError(f"A {vehicle_type.display_name} carries at most "
                                      f"{vehicle_type.seats} passenger(s); {passengers} requested.")

    def _match(self, ride: Ride) -> Captain:
        """Find a captain. Returns the ASSIGNED captain, or (manual mode) the captain now holding the offer."""
        if self._manual_offers:
            return self._offer_to_next_captain(ride)
        try:
            captain = self._matching.find_captain(ride, self._clock.now)
        except NoCaptainAvailableError as error:
            self._give_up(ride, error)
            raise
        return self._assign(ride, captain)

    def _give_up(self, ride: Ride, error: NoCaptainAvailableError) -> None:
        ride.cancel(CancellationReason.NO_CAPTAIN_AVAILABLE, None, self._clock.now)
        ride.customer.ride_cancelled(ride)
        self._notify(ride.customer, f"Sorry, no captains available right now for ride {ride.ride_id} "
                                    f"({error.reason}). You have not been charged.")

    # ---------------------------------------------------------------- manual offers
    def _offer_to_next_captain(self, ride: Ride) -> Captain:
        try:
            captain = self._matching.next_candidate(ride, self._clock.now)
        except NoCaptainAvailableError as error:
            self._give_up(ride, error)
            raise
        captain.receive_offer(ride)
        distance = captain.location.distance_to(ride.pickup)
        self._notify(captain, f"New ride offer {ride.ride_id}: {ride.pickup} → {ride.drop}, "
                              f"{distance:.1f} km from you, est. fare {ride.estimate.fare}. Accept or reject?")
        return captain

    def accept_offer(self, captain: Captain) -> Ride:
        """Manual mode: the captain accepts the offer on their phone. Returns the assigned ride."""
        ride = captain.withdraw_offer()
        captain.record_offer_response(accepted=True)
        ride.record_offer(captain, captain.location.distance_to(ride.pickup), True, self._clock.now)
        self._assign(ride, captain)
        return ride

    def reject_offer(self, captain: Captain) -> Captain | None:
        """Manual mode: the captain rejects. The offer moves to the next-nearest eligible captain,
        who is returned. None means nobody is left and the ride was cancelled (no charge)."""
        ride = captain.withdraw_offer()
        captain.record_offer_response(accepted=False)
        ride.record_offer(captain, captain.location.distance_to(ride.pickup), False, self._clock.now)
        try:
            return self._offer_to_next_captain(ride)
        except NoCaptainAvailableError:
            return None

    def _withdraw_pending_offer(self, ride: Ride) -> None:
        for captain in self._matching.offered_captains():
            if captain.pending_offer is ride:
                captain.withdraw_offer()
                self._notify(captain, f"Offer {ride.ride_id} was withdrawn — the customer cancelled.")

    def _assign(self, ride: Ride, captain: Captain) -> Captain:
        now = self._clock.now
        eta = self._matching.eta_minutes(captain, ride.pickup, now)
        captain.start_assignment(ride, now)
        ride.assign_captain(captain, eta, now)
        vehicle = captain.vehicle
        self._notify(ride.customer, f"Your captain {captain.name} ({vehicle.registration_number}, "
                                    f"{vehicle.model_name}) is {eta} {'min' if eta == 1 else 'mins'} away near {captain.location}. "
                                    f"OTP: {ride.otp_for(ride.customer)}")
        self._notify(captain, f"New ride {ride.ride_id}: pick up {ride.customer.name} at {ride.pickup} "
                              f"→ {ride.drop}. {eta} min away.")
        return captain

    # ================================================================ pickup
    def captain_arrives(self, ride: Ride) -> None:
        """The captain drives to the pickup: the clock moves by the ETA, THEN the location changes."""
        if ride.status != RideStatus.CAPTAIN_ASSIGNED:
            raise InvalidRideStatusError(ride.ride_id, str(ride.status), "mark the captain arrived")
        if self._clock.now < ride.expected_arrival_at:
            self._clock.advance_to(ride.expected_arrival_at)
        ride.captain.arrive_at(ride.pickup)
        ride.mark_arrived(self._clock.now)
        self._notify(ride.customer, f"{ride.captain.name} has arrived at {ride.pickup}. "
                                    f"First 3 minutes of waiting are free.")

    def start_ride(self, ride: Ride, entered_otp: str) -> None:
        try:
            ride.start(entered_otp, self._clock.now)
        except InvalidOtpError as error:
            if error.ride_cancelled:
                ride.captain.release_after_cancellation(self._clock.now, cancelled_by_captain=False)
                ride.customer.ride_cancelled(ride)
                self._notify(ride.customer, f"Ride {ride.ride_id} cancelled: wrong OTP entered 3 times. No fee.")
                self._notify(ride.captain, f"Ride {ride.ride_id} cancelled (OTP_FAILED). You are free for new rides.")
            raise
        waited = f" after {ride.waiting_minutes} min wait" if ride.waiting_minutes else ""
        self._notify(ride.customer, f"Ride started{waited}. Heading to {ride.drop}. Have a safe trip!")

    # ================================================================ during the trip
    def change_destination(self, ride: Ride, change_point: Location, new_drop: Location) -> None:
        """Mid-ride change (allowed once). The fare will follow pickup → change point → new drop."""
        self._require_in_progress(ride, "change destination")
        self._service_area.ensure_contains(new_drop)
        self._drive(ride, change_point)
        ride.change_destination(change_point, new_drop, self._clock.now)
        self._notify(ride.customer, f"Destination changed to {new_drop}. Your fare will use the actual route.")
        self._notify(ride.captain, f"{ride.customer.name} changed the destination to {new_drop}.")

    def end_trip_early(self, ride: Ride, stop_point: Location,
                       payment_method: Payable | None = None) -> FareReceipt:
        """The customer gets off before the drop. Fare = actual distance and time only."""
        self._require_in_progress(ride, "end early")
        self._drive(ride, stop_point)
        ride.end_early(stop_point, self._clock.now)
        return self._settle_trip(ride, payment_method)

    def complete_trip(self, ride: Ride, payment_method: Payable | None = None) -> FareReceipt:
        """Drive to the drop, bill the ride and take payment.

        ``payment_method`` lets the customer switch method at the end of the ride. A failed payment
        leaves the ride PAYMENT_PENDING and re-raises the error (the receipt is still on ``ride.receipt``).
        """
        self._require_in_progress(ride, "complete the trip")
        self._drive(ride, ride.drop)
        ride.end_trip(self._clock.now)
        return self._settle_trip(ride, payment_method)

    def _drive(self, ride: Ride, destination: Location) -> None:
        minutes = self._fares.travel_minutes(ride.captain.vehicle, ride.last_waypoint, destination,
                                             ride.last_waypoint_at)
        arrival = ride.last_waypoint_at + timedelta(minutes=minutes)
        if self._clock.now < arrival:
            self._clock.advance_to(arrival)

    def _settle_trip(self, ride: Ride, payment_method: Payable | None) -> FareReceipt:
        now = self._clock.now
        customer = ride.customer
        captain = ride.captain
        carried_fees = customer.take_pending_fees()          # each old fee is billed exactly once
        receipt = self._fares.build_receipt(ride, Money.total([fee.amount for fee in carried_fees]), now)
        ride.attach_receipt(receipt, carried_fees)
        if not receipt.coupon_discount.is_zero():
            customer.mark_coupon_used(receipt.coupon_code)
        # Location continuity: both people are now exactly where the trip ended.
        captain.finish_ride(ride.drop, now)
        customer.finish_ride(ride, ride.drop, now)
        self._notify(customer, f"You have reached {ride.drop}. Fare for {ride.ride_id}: {receipt.total}.")
        self._notify(captain, f"Trip {ride.ride_id} ended at {ride.drop}. Fare {receipt.total}.")
        self.pay_for_ride(ride, payment_method or ride.payment_method)
        return receipt

    # ================================================================ payment
    def pay_for_ride(self, ride: Ride, payable: Payable) -> str:
        """Pay (or retry paying) a ride. Works from IN_PROGRESS (trip ended) or PAYMENT_PENDING."""
        amount = ride.receipt.total if ride.receipt else Money.zero()
        try:
            reference = self._payments.pay_ride(ride, payable, self._clock.now)
        except (PaymentFailedError, InsufficientWalletBalanceError) as error:
            self._notify(ride.customer, f"Payment of {amount} via {payable.method_name} failed ({error}). "
                                        f"Ride {ride.ride_id} is PAYMENT_PENDING — retry or pay cash.")
            raise
        self._notify(ride.customer, f"Payment of {amount} via {payable.method_name} successful. Thank you!")
        if payable.is_cash:
            self._notify(ride.captain, f"Collect {amount} in cash from {ride.customer.name}. "
                                       f"Dues owed to Rapido: {ride.captain.dues_owed}.")
        return reference

    # ================================================================ cancellations
    def cancel_by_customer(self, ride: Ride) -> Money:
        """Free before assignment and within 2 minutes of it; otherwise ₹20 bike / ₹30 auto-cab,
        added to the customer's NEXT ride. Returns the fee."""
        now = self._clock.now
        fee = self.cancellation_fee_if_cancelled_now(ride)
        captain = ride.captain
        ride.cancel(CancellationReason.CUSTOMER_CANCELLED, ride.customer, now, fee)   # guards status
        if captain is None:
            self._withdraw_pending_offer(ride)
        if captain is not None:
            captain.release_after_cancellation(now, cancelled_by_captain=False)
        ride.customer.ride_cancelled(ride)
        if not fee.is_zero():
            ride.customer.add_pending_fee(PendingFee(fee, captain, ride.ride_id))
            self._notify(ride.customer, f"Ride {ride.ride_id} cancelled. A cancellation fee of {fee} "
                                        f"will be added to your next ride.")
        else:
            self._notify(ride.customer, f"Ride {ride.ride_id} cancelled free of charge.")
        if captain is not None:
            self._notify(captain, f"{ride.customer.name} cancelled ride {ride.ride_id}. You are free for new rides.")
        return fee

    def cancel_by_captain(self, ride: Ride) -> Captain:
        """The captain backs out after accepting. The customer is NOT charged; the ride is re-matched
        to someone else. Returns the new captain (raises NoCaptainAvailableError if nobody is left)."""
        now = self._clock.now
        old_captain = ride.release_captain(now)             # guards status, excludes this captain
        old_captain.release_after_cancellation(now, cancelled_by_captain=True)
        self._notify(ride.customer, f"Captain {old_captain.name} cancelled. You will not be charged — "
                                    f"finding you another captain…")
        return self._match(ride)

    def cancel_no_show(self, ride: Ride) -> Money:
        """The captain waited 5+ minutes and the customer never came. Returns the no-show fee."""
        now = self._clock.now
        if ride.status != RideStatus.CAPTAIN_ARRIVED:
            raise InvalidRideStatusError(ride.ride_id, str(ride.status), "be cancelled as NO_SHOW",
                                         "The captain must have arrived first.")
        waited = ride.minutes_waited(now)
        if waited < RideService.NO_SHOW_WAIT_MINUTES:
            raise InvalidRideStatusError(ride.ride_id, str(ride.status), "be cancelled as NO_SHOW",
                                         f"Captain has waited only {waited} min; must wait "
                                         f"{RideService.NO_SHOW_WAIT_MINUTES}.")
        captain = ride.captain
        customer = ride.customer
        fee = ride.vehicle_type.cancellation_fee
        ride.cancel(CancellationReason.NO_SHOW, captain, now, fee)
        captain.release_after_cancellation(now, cancelled_by_captain=False)   # not the captain's fault
        customer.ride_cancelled(ride)
        charged_now = False
        if not ride.payment_method.is_cash:
            try:
                self._payments.charge_fee(customer, fee, captain, ride.payment_method, ride.ride_id, now)
                charged_now = True
            except (PaymentFailedError, InsufficientWalletBalanceError):
                charged_now = False
        if charged_now:
            self._notify(customer, f"You missed ride {ride.ride_id}. A no-show fee of {fee} was charged "
                                   f"via {ride.payment_method.method_name}.")
        else:
            customer.add_pending_fee(PendingFee(fee, captain, ride.ride_id))
            self._notify(customer, f"You missed ride {ride.ride_id}. A no-show fee of {fee} will be added "
                                   f"to your next ride.")
        self._notify(captain, f"Ride {ride.ride_id} cancelled as NO_SHOW. You earn the no-show fee.")
        return fee

    # ================================================================ helpers
    def _require_in_progress(self, ride: Ride, action: str) -> None:
        if ride.status != RideStatus.IN_PROGRESS or ride.trip_has_ended:
            raise InvalidRideStatusError(ride.ride_id, str(ride.status), action)

    def _notify(self, user: User, message: str) -> None:
        # Polymorphism: the same notify() call on every Notifier — SMS, push, or any future channel.
        for notifier in self._notifiers:
            notifier.notify(user, message, self._clock.now)
