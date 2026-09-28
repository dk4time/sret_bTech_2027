"""Captain — the person who drives."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import TYPE_CHECKING

from ridehailing.exceptions import (CaptainBusyError, CaptainNotEligibleError,
                                    InvalidAmountError, TimeTravelError)
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.user.captain_status import CaptainStatus
from ridehailing.model.user.user import User
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.model.vehicle.vehicle_type import VehicleType

if TYPE_CHECKING:   # imported only for type hints, avoids a circular import at runtime
    from ridehailing.model.ride.ride import Ride


class Captain(User):
    """A driver with exactly one vehicle.

    OOP concepts shown here:
    * Inheritance: Captain IS-A User (``super().__init__`` sets name, phone, id, location).
    * Composition: Captain HAS-A Vehicle. The vehicle is created for this captain at
      registration and lives and dies with them; the vehicle type can never change.
    * Encapsulation: status, location and dues change ONLY through intent-revealing methods
      (``go_online()``, ``start_assignment()``, ``finish_ride()``, ``settle_dues()``...), and each
      method checks the rules first.
    """

    # Class attributes: business rules shared by every captain
    CASH_DUES_LIMIT = Money("500")
    RATING_FLAG_THRESHOLD = Decimal("4.0")
    MIN_RATINGS_BEFORE_FLAG = 5

    def __init__(self, name: str, phone: str, home_location: Location, vehicle: Vehicle,
                 opening_dues: Money | None = None, past_ratings: list[int] | None = None) -> None:
        super().__init__(name, phone, home_location)
        if not isinstance(vehicle, Vehicle):
            raise TypeError("A captain must be registered with a Vehicle.")
        self._vehicle = vehicle                          # composition: HAS-A Vehicle
        self._status = CaptainStatus.KYC_PENDING         # everyone starts unverified
        self._current_ride: Ride | None = None
        self._offers_received = 0
        self._offers_accepted = 0
        self._planned_rejections = 0
        self._pending_offer: Ride | None = None          # an offer waiting on the captain's phone
        self._cancellation_count = 0
        # Dues carried from earlier days (commission the captain still owes on old cash rides).
        self._dues_owed = opening_dues if opening_dues is not None else Money.zero()
        self._free_since: datetime | None = None         # when the last ride ended
        # Lifetime ratings from earlier weeks, received the normal way.
        for score in past_ratings or []:
            self.receive_rating(score)

    # ----- abstract members from User, implemented (method overriding) -----
    @property
    def role(self) -> str:
        return "Captain"

    @property
    def id_prefix(self) -> str:
        return "CAP"

    # ----- read-only properties -----
    @property
    def vehicle(self) -> Vehicle:
        return self._vehicle

    @property
    def vehicle_type(self) -> VehicleType:
        return self._vehicle.vehicle_type

    @property
    def status(self) -> CaptainStatus:
        return self._status

    @property
    def current_ride(self) -> Ride | None:
        return self._current_ride

    @property
    def pending_offer(self) -> Ride | None:
        """A ride offered to this captain that they have not accepted or rejected yet."""
        return self._pending_offer

    @property
    def is_available(self) -> bool:
        """Online, verified and not on a ride — the only state in which a captain can be matched."""
        return self._status == CaptainStatus.ONLINE and self._current_ride is None

    @property
    def free_since(self) -> datetime | None:
        return self._free_since

    @property
    def dues_owed(self) -> Money:
        return self._dues_owed

    @property
    def accepts_cash_rides(self) -> bool:
        """Blocked from CASH rides once dues exceed ₹500 until settle_dues() is called."""
        return self._dues_owed <= Captain.CASH_DUES_LIMIT

    @property
    def cancellation_count(self) -> int:
        return self._cancellation_count

    @property
    def offers_received(self) -> int:
        return self._offers_received

    @property
    def acceptance_rate(self) -> Decimal:
        """Percentage of offers accepted (100.0 when no offers yet)."""
        if self._offers_received == 0:
            return Decimal("100.0")
        rate = Decimal(self._offers_accepted * 100) / Decimal(self._offers_received)
        return rate.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP)

    @property
    def is_flagged(self) -> bool:
        """Flagged when the average falls below 4.0 after at least 5 ratings."""
        return (self.rating_count >= Captain.MIN_RATINGS_BEFORE_FLAG
                and self.average_rating < Captain.RATING_FLAG_THRESHOLD)

    # ----- lifecycle: KYC, online, offline -----
    def verify_kyc(self) -> None:
        if self._status != CaptainStatus.KYC_PENDING:
            raise CaptainNotEligibleError(self.name, "KYC is already verified.")
        self._status = CaptainStatus.OFFLINE

    def go_online(self) -> None:
        if self._status == CaptainStatus.KYC_PENDING:
            raise CaptainNotEligibleError(self.name, "KYC verification pending — cannot go online.")
        if self._status == CaptainStatus.OFFLINE:
            self._status = CaptainStatus.ONLINE

    def go_offline(self) -> None:
        if self._current_ride is not None:
            raise CaptainBusyError(self.name, self._current_ride.ride_id, "go offline")
        if self._pending_offer is not None:
            raise CaptainNotEligibleError(self.name, f"must accept or reject the offer for ride "
                                                     f"{self._pending_offer.ride_id} before going offline.")
        if self._status == CaptainStatus.KYC_PENDING:
            raise CaptainNotEligibleError(self.name, "KYC verification pending.")
        self._status = CaptainStatus.OFFLINE

    # ----- offers (accept / reject, simulated deterministically) -----
    def plan_to_reject_next_offer(self) -> None:
        """Demo scripting: the next offer this captain sees will be rejected (e.g. going for lunch)."""
        self._planned_rejections += 1

    def respond_to_offer(self, ride: Ride) -> bool:
        """Automatic decision (used when offers are answered instantly). True = accept."""
        if self._planned_rejections > 0:
            self._planned_rejections -= 1
            self.record_offer_response(accepted=False)
            return False
        self.record_offer_response(accepted=True)
        return True

    def record_offer_response(self, accepted: bool) -> None:
        """Keep the acceptance-rate statistics for one answered offer."""
        self._offers_received += 1
        if accepted:
            self._offers_accepted += 1

    def receive_offer(self, ride: Ride) -> None:
        """Manual mode: the offer appears on the captain's phone and waits for an answer."""
        if not self.is_available:
            raise CaptainNotEligibleError(self.name, f"is {self._status} and cannot receive an offer.")
        if self._pending_offer is not None:
            raise CaptainNotEligibleError(self.name, f"already has a pending offer ({self._pending_offer.ride_id}).")
        self._pending_offer = ride

    def withdraw_offer(self) -> Ride:
        """The offer is answered or withdrawn. Returns the ride it was for."""
        if self._pending_offer is None:
            raise CaptainNotEligibleError(self.name, "has no pending ride offer.")
        ride = self._pending_offer
        self._pending_offer = None
        return ride

    # ----- ride events: the ONLY ways a captain's state and location change -----
    def start_assignment(self, ride: Ride, at: datetime) -> None:
        """Called when the captain is assigned a ride. One ride at a time, no time travel."""
        if self._current_ride is not None:
            raise CaptainBusyError(self.name, self._current_ride.ride_id, "take another ride")
        if self._status != CaptainStatus.ONLINE:
            raise CaptainNotEligibleError(self.name, f"is {self._status}, not online.")
        if self._free_since is not None and at < self._free_since:
            raise TimeTravelError(f"Captain {self.name} is free only from "
                                  f"{self._free_since:%I:%M %p}; cannot start a ride at {at:%I:%M %p}.")
        self._current_ride = ride
        self._status = CaptainStatus.ON_RIDE

    def arrive_at(self, pickup: Location) -> None:
        """Event: the captain has driven from their current location to the pickup point."""
        self._require_ride("arrive at a pickup")
        self._location = pickup

    def finish_ride(self, drop: Location, at: datetime) -> None:
        """Event: the trip ended. The captain is now exactly where the customer got off."""
        self._require_ride("finish a ride")
        self._location = drop
        self._release(at)

    def release_after_cancellation(self, at: datetime, cancelled_by_captain: bool) -> None:
        """Event: the ride was cancelled. The captain stays where they currently are."""
        self._require_ride("be released")
        if cancelled_by_captain:
            self._cancellation_count += 1
        self._release(at)

    # ----- money -----
    def add_dues(self, amount: Money) -> None:
        """Cash the captain collected that belongs to the platform (commission, GST...)."""
        if amount.is_negative():
            raise InvalidAmountError("Dues cannot be negative.")
        self._dues_owed = self._dues_owed + amount

    def settle_dues(self) -> Money:
        """Pay everything owed to the platform. Returns the amount settled."""
        settled = self._dues_owed
        self._dues_owed = Money.zero()
        return settled

    # ----- private helpers -----
    def _release(self, at: datetime) -> None:
        self._current_ride = None
        self._free_since = at
        if self._status == CaptainStatus.ON_RIDE:
            self._status = CaptainStatus.ONLINE

    def _require_ride(self, action: str) -> None:
        if self._current_ride is None:
            raise CaptainNotEligibleError(self.name, f"has no active ride, so cannot {action}.")

    def __str__(self) -> str:
        return f"{self.name} ({self._vehicle.vehicle_type.display_name}, {self._vehicle.registration_number})"
