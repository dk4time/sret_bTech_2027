"""Ride — one trip, which guards its own status."""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from typing import TYPE_CHECKING

from ridehailing.exceptions import (CaptainNotAtPickupError, CaptainNotEligibleError,
                                    DuplicatePaymentError, InvalidOtpError, InvalidRatingError,
                                    InvalidRideStatusError, UnauthorizedAccessError)
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.payment.payment_status import PaymentStatus
from ridehailing.model.ride.cancellation_reason import CancellationReason
from ridehailing.model.ride.fare_estimate import FareEstimate
from ridehailing.model.ride.fare_receipt import FareReceipt
from ridehailing.model.ride.ride_request import RideRequest
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.vehicle.vehicle_type import VehicleType

if TYPE_CHECKING:
    from ridehailing.model.payment.payable import Payable
    from ridehailing.model.user.captain import Captain
    from ridehailing.model.user.customer import Customer, PendingFee
    from ridehailing.model.user.user import User


class Ride:
    """A single ride from booking to payment.

    OOP concepts shown here:
    * Encapsulation: ``status`` is a read-only property with NO setter. It changes only through
      intent-revealing methods — ``assign_captain()``, ``mark_arrived()``, ``start(otp)``,
      ``change_destination()``, ``end_early()``, ``end_trip()``, ``complete()``, ``cancel()`` —
      and each one first checks that the current status allows it.
    * Name mangling: the OTP is stored in ``self.__otp``. Python renames it to ``_Ride__otp``, so
      ``ride.__otp`` from outside the class raises AttributeError.
    * Aggregation: a Ride REFERENCES an existing Customer and Captain. They existed before the
      ride and keep existing after it — the ride does not own them.
    """

    # Class attributes
    _next_number = 1001
    MAX_OTP_ATTEMPTS = 3
    ARRIVAL_RADIUS_KM = 0.1          # "within about 100 m of the pickup"
    FREE_WAITING_MINUTES = 3

    def __init__(self, request: RideRequest, otp: str, estimate: FareEstimate,
                 payment_method: Payable, booked_at: datetime) -> None:
        if len(otp) != 4 or not otp.isdigit():
            raise ValueError("OTP must be exactly 4 digits.")
        self._ride_id = f"RD-{Ride._next_number}"
        Ride._next_number += 1

        self._request = request
        self.__otp = otp                     # __private: stored as _Ride__otp (name mangling)
        # Outside the class:  ride.__otp  -> AttributeError: 'Ride' object has no attribute '__otp'
        self._otp_attempts = 0
        self._estimate = estimate
        self._payment_method = payment_method

        self._status = RideStatus.REQUESTED
        self._timeline: list[tuple[datetime, str]] = []

        self._captain: Captain | None = None           # aggregation: set later, never owned
        self._offer_log: list[str] = []
        self._excluded_captain_ids: set[str] = set()   # rejected or cancelled THIS ride
        self._rejection_count = 0

        self._assigned_at: datetime | None = None
        self._expected_arrival_at: datetime | None = None
        self._arrived_at: datetime | None = None
        self._started_at: datetime | None = None
        self._trip_ended_at: datetime | None = None
        self._waiting_minutes = 0

        self._route: list[Location] = [request.pickup]   # every point actually travelled through
        self._route_times: list[datetime] = []
        self._drop = request.drop
        self._destination_changed = False
        self._ended_early = False

        self._receipt: FareReceipt | None = None
        self._carried_fees: list[PendingFee] = []
        self._payment_status = PaymentStatus.NOT_DUE
        self._paid_with: Payable | None = None
        self._payment_reference: str | None = None

        self._cancellation_reason: CancellationReason | None = None
        self._cancelled_by: User | None = None
        self._cancellation_fee = Money.zero()

        self._rating_by_customer: int | None = None     # stars the customer gave the captain
        self._rating_by_captain: int | None = None      # stars the captain gave the customer

        self._record(booked_at, f"{'REQUESTED':<17} {request.pickup} → {request.drop} "
                                f"({request.vehicle_type.display_name}), est. {estimate.fare}")

    # ================= read-only properties =================
    @property
    def ride_id(self) -> str:
        return self._ride_id

    @property
    def status(self) -> RideStatus:
        """Read-only. There is deliberately no setter: ``ride.status = ...`` raises AttributeError."""
        return self._status

    @property
    def request(self) -> RideRequest:
        return self._request

    @property
    def customer(self) -> Customer:
        return self._request.customer

    @property
    def captain(self) -> Captain | None:
        return self._captain

    @property
    def vehicle_type(self) -> VehicleType:
        return self._request.vehicle_type

    @property
    def pickup(self) -> Location:
        return self._request.pickup

    @property
    def drop(self) -> Location:
        """The CURRENT destination (changes after change_destination / end_early)."""
        return self._drop

    @property
    def estimate(self) -> FareEstimate:
        return self._estimate

    @property
    def payment_method(self) -> Payable:
        return self._payment_method

    @property
    def payment_status(self) -> PaymentStatus:
        return self._payment_status

    @property
    def paid_with(self) -> Payable | None:
        return self._paid_with

    @property
    def is_paid(self) -> bool:
        return self._payment_status == PaymentStatus.PAID

    @property
    def receipt(self) -> FareReceipt | None:
        return self._receipt

    @property
    def carried_fees(self) -> tuple[PendingFee, ...]:
        return tuple(self._carried_fees)

    @property
    def offer_log(self) -> tuple[str, ...]:
        return tuple(self._offer_log)

    @property
    def rejection_count(self) -> int:
        return self._rejection_count

    @property
    def otp_attempts(self) -> int:
        return self._otp_attempts

    @property
    def assigned_at(self) -> datetime | None:
        return self._assigned_at

    @property
    def expected_arrival_at(self) -> datetime | None:
        return self._expected_arrival_at

    @property
    def arrived_at(self) -> datetime | None:
        return self._arrived_at

    @property
    def started_at(self) -> datetime | None:
        return self._started_at

    @property
    def trip_ended_at(self) -> datetime | None:
        return self._trip_ended_at

    @property
    def trip_has_ended(self) -> bool:
        return self._trip_ended_at is not None

    @property
    def waiting_minutes(self) -> int:
        return self._waiting_minutes

    @property
    def route(self) -> tuple[Location, ...]:
        return tuple(self._route)

    @property
    def last_waypoint(self) -> Location:
        return self._route[-1]

    @property
    def last_waypoint_at(self) -> datetime | None:
        return self._route_times[-1] if self._route_times else None

    @property
    def destination_changed(self) -> bool:
        return self._destination_changed

    @property
    def ended_early(self) -> bool:
        return self._ended_early

    @property
    def cancellation_reason(self) -> CancellationReason | None:
        return self._cancellation_reason

    @property
    def cancelled_by(self) -> User | None:
        return self._cancelled_by

    @property
    def cancellation_fee(self) -> Money:
        return self._cancellation_fee

    @property
    def rating_by_customer(self) -> int | None:
        return self._rating_by_customer

    @property
    def rating_by_captain(self) -> int | None:
        return self._rating_by_captain

    @property
    def actual_distance_km(self) -> Decimal:
        """Road distance along the route actually travelled (pickup → ... → current point)."""
        total = 0.0
        for index in range(1, len(self._route)):
            total += self._route[index - 1].road_distance_to(self._route[index])
        return Decimal(str(total)).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    @property
    def trip_minutes(self) -> int:
        if self._started_at is None or self._trip_ended_at is None:
            return 0
        seconds = (self._trip_ended_at - self._started_at).total_seconds()
        return math.ceil(seconds / 60)

    def is_excluded(self, captain: Captain) -> bool:
        """True if this captain rejected or cancelled THIS ride — they are never offered it again."""
        return captain.user_id in self._excluded_captain_ids

    def otp_for(self, user: User) -> str:
        """Only the ride's own customer may see the OTP (it is shown in their app)."""
        if user != self.customer:
            raise UnauthorizedAccessError(f"{user.name} is not allowed to see the OTP of ride {self._ride_id}.")
        return self.__otp

    # ================= state transitions =================
    def record_offer(self, captain: Captain, distance_km: float, accepted: bool, at: datetime) -> None:
        """Log an offer made to a captain. A rejection excludes that captain from THIS ride."""
        self._require_status("offer to a captain", RideStatus.REQUESTED)
        if self.is_excluded(captain):
            raise CaptainNotEligibleError(captain.name, f"already rejected/cancelled ride {self._ride_id}.")
        outcome = "ACCEPTED" if accepted else "REJECTED"
        self._offer_log.append(f"Offer #{len(self._offer_log) + 1} → {captain.name} "
                               f"({distance_km:.1f} km away): {outcome}")
        if not accepted:
            self._excluded_captain_ids.add(captain.user_id)
            self._rejection_count += 1
            self._record(at, f"Offer rejected by {captain.name}")

    def assign_captain(self, captain: Captain, eta_minutes: int, at: datetime) -> None:
        self._require_status("assign a captain", RideStatus.REQUESTED)
        if self.is_excluded(captain):
            raise CaptainNotEligibleError(captain.name, f"is excluded from ride {self._ride_id}.")
        if captain.vehicle_type != self.vehicle_type:
            raise CaptainNotEligibleError(captain.name, f"wrong vehicle ({captain.vehicle_type}); "
                                                        f"ride needs {self.vehicle_type}.")
        self._captain = captain
        self._assigned_at = at
        self._expected_arrival_at = at + timedelta(minutes=eta_minutes)
        self._transition(RideStatus.CAPTAIN_ASSIGNED, at,
                         f"{captain.name} ({captain.vehicle}) assigned, ETA {eta_minutes} min")

    def release_captain(self, at: datetime) -> Captain:
        """The captain cancelled after accepting: the ride goes back to REQUESTED for re-matching."""
        self._require_status("release its captain", RideStatus.CAPTAIN_ASSIGNED, RideStatus.CAPTAIN_ARRIVED)
        captain = self._captain
        self._excluded_captain_ids.add(captain.user_id)
        self._captain = None
        self._assigned_at = None
        self._expected_arrival_at = None
        self._arrived_at = None
        self._transition(RideStatus.REQUESTED, at, f"{captain.name} cancelled — back to matching")
        return captain

    def mark_arrived(self, at: datetime) -> None:
        """Allowed only when the captain is really at the pickup (within ~100 m)."""
        self._require_status("mark arrived", RideStatus.CAPTAIN_ASSIGNED)
        distance_km = self._captain.location.distance_to(self.pickup)
        if distance_km > Ride.ARRIVAL_RADIUS_KM:
            raise CaptainNotAtPickupError(self._captain.name, round(distance_km * 1000))
        self._arrived_at = at
        self._transition(RideStatus.CAPTAIN_ARRIVED, at, f"{self._captain.name} arrived at {self.pickup}")

    def minutes_waited(self, at: datetime) -> int:
        if self._arrived_at is None:
            return 0
        return int((at - self._arrived_at).total_seconds() // 60)

    def start(self, entered_otp: str, at: datetime) -> None:
        """Start the trip if the OTP matches. The 3rd wrong OTP auto-cancels the ride (no fee)."""
        self._require_status("start", RideStatus.CAPTAIN_ARRIVED)
        if entered_otp != self.__otp:                      # inside the class, __otp works normally
            self._otp_attempts += 1
            attempts_left = Ride.MAX_OTP_ATTEMPTS - self._otp_attempts
            self._record(at, f"Wrong OTP entered ({self._otp_attempts}/{Ride.MAX_OTP_ATTEMPTS})")
            if attempts_left == 0:
                self._mark_cancelled(CancellationReason.OTP_FAILED, None, Money.zero(), at)
                raise InvalidOtpError(self._ride_id, 0, ride_cancelled=True)
            raise InvalidOtpError(self._ride_id, attempts_left, ride_cancelled=False)
        self._waiting_minutes = self.minutes_waited(at)
        self._started_at = at
        self._route_times.append(at)
        self._transition(RideStatus.IN_PROGRESS, at, f"OTP verified — trip started from {self.pickup}")

    def change_destination(self, change_point: Location, new_drop: Location, at: datetime) -> None:
        """Allowed once, mid-ride. The route becomes pickup → change point → new drop."""
        self._require_status("change destination", RideStatus.IN_PROGRESS)
        self._require_trip_running("change destination")
        if self._destination_changed:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "change destination",
                                         "The destination can be changed only once.")
        if new_drop == self._drop:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "change destination",
                                         "The new drop is the same as the current drop.")
        old_drop = self._drop
        self._route.append(change_point)
        self._route_times.append(at)
        self._drop = new_drop
        self._destination_changed = True
        self._record(at, f"Destination changed at {change_point}: {old_drop} → {new_drop}")

    def end_early(self, stop_point: Location, at: datetime) -> None:
        """The customer ends the trip before the drop. The stop point becomes the real drop."""
        self._require_status("end early", RideStatus.IN_PROGRESS)
        self._require_trip_running("end early")
        self._drop = stop_point
        self._ended_early = True
        self._finish_trip(at, f"Trip ended EARLY at {stop_point}")

    def end_trip(self, at: datetime) -> None:
        """The captain reached the (current) drop point."""
        self._require_status("end the trip", RideStatus.IN_PROGRESS)
        self._require_trip_running("end the trip")
        self._finish_trip(at, f"Trip ended at {self._drop}")

    def attach_receipt(self, receipt: FareReceipt, carried_fees: list[PendingFee]) -> None:
        self._require_status("attach a receipt", RideStatus.IN_PROGRESS)
        if not self.trip_has_ended:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "attach a receipt",
                                         "The trip has not ended yet.")
        if self._receipt is not None:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "attach a second receipt")
        self._receipt = receipt
        self._carried_fees = list(carried_fees)

    def mark_payment_failed(self, reason: str, at: datetime) -> None:
        """A payment attempt failed: IN_PROGRESS (trip ended) → PAYMENT_PENDING."""
        self._require_status("record a failed payment", RideStatus.IN_PROGRESS, RideStatus.PAYMENT_PENDING)
        self._require_receipt("record a failed payment")
        self._payment_status = PaymentStatus.FAILED
        if self._status == RideStatus.IN_PROGRESS:
            self._transition(RideStatus.PAYMENT_PENDING, at, f"Payment failed: {reason}")
        else:
            self._record(at, f"Payment retry failed: {reason}")

    def complete(self, paid_with: Payable, payment_reference: str, at: datetime) -> None:
        """Payment succeeded: IN_PROGRESS (trip ended) or PAYMENT_PENDING → COMPLETED."""
        if self.is_paid:
            raise DuplicatePaymentError(self._ride_id)
        self._require_status("complete", RideStatus.IN_PROGRESS, RideStatus.PAYMENT_PENDING)
        self._require_receipt("complete")
        self._payment_status = PaymentStatus.PAID
        self._paid_with = paid_with
        self._payment_reference = payment_reference
        self._transition(RideStatus.COMPLETED, at,
                         f"Paid {self._receipt.total} via {paid_with.method_name} (ref {payment_reference})")

    def cancel(self, reason: CancellationReason, by: User | None, at: datetime,
               fee: Money | None = None) -> None:
        """Cancel before the trip starts. Once IN_PROGRESS the customer can only end early."""
        if self._status == RideStatus.IN_PROGRESS:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "be cancelled",
                                         "The trip has started — use end_early() instead.")
        self._require_status("be cancelled", RideStatus.REQUESTED, RideStatus.CAPTAIN_ASSIGNED,
                             RideStatus.CAPTAIN_ARRIVED)
        self._mark_cancelled(reason, by, fee if fee is not None else Money.zero(), at)

    def rate_captain(self, score: int) -> None:
        """The customer's rating of the captain: COMPLETED rides only, once."""
        self._require_rateable("rate the captain")
        if self._rating_by_customer is not None:
            raise InvalidRatingError(f"The customer has already rated ride {self._ride_id}.")
        self._rating_by_customer = score

    def rate_customer(self, score: int) -> None:
        """The captain's rating of the customer: COMPLETED rides only, once."""
        self._require_rateable("rate the customer")
        if self._rating_by_captain is not None:
            raise InvalidRatingError(f"The captain has already rated ride {self._ride_id}.")
        self._rating_by_captain = score

    # ================= timeline =================
    @property
    def timeline(self) -> tuple[tuple[datetime, str], ...]:
        return tuple(self._timeline)

    def timeline_text(self) -> str:
        lines = [f"Timeline of {self._ride_id}:"]
        for moment, note in self._timeline:
            lines.append(f"    {moment:%I:%M %p}  {note}")
        return "\n".join(lines)

    # ================= private helpers =================
    def _finish_trip(self, at: datetime, note: str) -> None:
        self._route.append(self._drop)
        self._route_times.append(at)
        self._trip_ended_at = at
        self._record(at, note)

    def _mark_cancelled(self, reason: CancellationReason, by: User | None, fee: Money,
                        at: datetime) -> None:
        self._cancellation_reason = reason
        self._cancelled_by = by
        self._cancellation_fee = fee
        who = by.name if by is not None else "system"
        fee_text = f", fee {fee}" if not fee.is_zero() else ", no fee"
        self._transition(RideStatus.CANCELLED, at, f"CANCELLED by {who}: {reason.description}{fee_text}")

    def _transition(self, new_status: RideStatus, at: datetime, note: str) -> None:
        self._status = new_status
        self._record(at, f"{new_status.name:<17} {note}")

    def _record(self, at: datetime, note: str) -> None:
        if self._timeline and at < self._timeline[-1][0]:
            raise InvalidRideStatusError(self._ride_id, str(self._status), "record an event in the past")
        self._timeline.append((at, note))

    def _require_status(self, action: str, *allowed: RideStatus) -> None:
        if self._status not in allowed:
            names = ", ".join(status.name for status in allowed)
            raise InvalidRideStatusError(self._ride_id, str(self._status), action, f"Allowed only when {names}.")

    def _require_trip_running(self, action: str) -> None:
        if self.trip_has_ended:
            raise InvalidRideStatusError(self._ride_id, str(self._status), action, "The trip has already ended.")

    def _require_receipt(self, action: str) -> None:
        if self._receipt is None:
            raise InvalidRideStatusError(self._ride_id, str(self._status), action,
                                         "The trip has not ended and been billed yet.")

    def _require_rateable(self, action: str) -> None:
        if self._status != RideStatus.COMPLETED:
            raise InvalidRatingError(f"Ride {self._ride_id} is {self._status}; only COMPLETED rides can be rated.")

    # ================= dunder methods =================
    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Ride):
            return NotImplemented
        return self._ride_id == other._ride_id

    def __hash__(self) -> int:
        return hash(self._ride_id)

    def __str__(self) -> str:
        return f"{self._ride_id} {self.pickup} → {self._drop} [{self._status.name}]"

    def __repr__(self) -> str:
        return f"Ride(id='{self._ride_id}', status={self._status.name}, customer='{self.customer.name}')"
