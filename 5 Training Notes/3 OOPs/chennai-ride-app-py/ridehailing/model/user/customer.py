"""Customer — the person who books."""

from __future__ import annotations

import math
from datetime import datetime, timedelta
from typing import TYPE_CHECKING

from ridehailing.exceptions import (ActiveRideExistsError, InsufficientWalletBalanceError,
                                    InvalidAmountError, InvalidBookingError, PaymentPendingError)
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.user.user import User

if TYPE_CHECKING:
    from ridehailing.model.payment.payable import Payable
    from ridehailing.model.ride.ride import Ride
    from ridehailing.model.user.captain import Captain


class PendingFee:
    """A cancellation/no-show fee the customer will pay on their NEXT ride.

    ``captain`` is the captain who was cancelled on — they receive the fee (minus commission)
    once it is actually paid, so no money appears before it is collected.
    """

    def __init__(self, amount: Money, captain: Captain | None, ride_id: str) -> None:
        self._amount = amount
        self._captain = captain
        self._ride_id = ride_id

    @property
    def amount(self) -> Money:
        return self._amount

    @property
    def captain(self) -> Captain | None:
        return self._captain

    @property
    def ride_id(self) -> str:
        return self._ride_id

    def __repr__(self) -> str:
        return f"PendingFee({self._amount!r}, ride='{self._ride_id}')"


class Customer(User):
    """A rider with a wallet, a ride history and at most one active ride.

    Encapsulation: the wallet balance has a getter but no setter. Money enters only through
    ``top_up()`` and leaves only through ``pay_from_wallet()``, which never deducts partially.
    """

    MAX_TOP_UP = Money("10000")
    SELF_TRAVEL_SPEED_KMPH = 20   # bus / metro / walking, used when a customer moves on their own

    def __init__(self, name: str, phone: str, location: Location, wallet_balance: Money,
                 joined_at: datetime) -> None:
        super().__init__(name, phone, location)
        if wallet_balance.is_negative():
            raise InvalidAmountError("Opening wallet balance cannot be negative.")
        self._wallet_balance = wallet_balance
        self._total_topped_up = Money.zero()
        self._total_wallet_spent = Money.zero()
        self._opening_wallet_balance = wallet_balance
        self._preferred_payment: Payable | None = None
        self._active_ride: Ride | None = None
        self._unpaid_ride: Ride | None = None
        self._ride_history: list[Ride] = []
        self._pending_fees: list[PendingFee] = []
        self._used_coupons: set[str] = set()
        self._location_since = joined_at

    # ----- abstract members from User -----
    @property
    def role(self) -> str:
        return "Customer"

    @property
    def id_prefix(self) -> str:
        return "CUS"

    # ----- read-only properties -----
    @property
    def wallet_balance(self) -> Money:
        return self._wallet_balance

    @property
    def opening_wallet_balance(self) -> Money:
        return self._opening_wallet_balance

    @property
    def total_topped_up(self) -> Money:
        return self._total_topped_up

    @property
    def total_wallet_spent(self) -> Money:
        return self._total_wallet_spent

    @property
    def preferred_payment(self) -> Payable | None:
        return self._preferred_payment

    @property
    def active_ride(self) -> Ride | None:
        return self._active_ride

    @property
    def unpaid_ride(self) -> Ride | None:
        return self._unpaid_ride

    @property
    def ride_history(self) -> tuple[Ride, ...]:
        """A tuple, so callers cannot append to or reorder the real list."""
        return tuple(self._ride_history)

    @property
    def last_completed_ride(self) -> Ride | None:
        """The most recent COMPLETED ride in the history, if any."""
        for ride in reversed(self._ride_history):
            if ride.status == RideStatus.COMPLETED:
                return ride
        return None

    @property
    def pending_fee_total(self) -> Money:
        return Money.total([fee.amount for fee in self._pending_fees])

    # ----- wallet -----
    def top_up(self, amount: Money) -> None:
        if amount <= Money.zero():
            raise InvalidAmountError(f"Top-up must be positive, got {amount}.")
        if amount > Customer.MAX_TOP_UP:
            raise InvalidAmountError(f"Top-up of {amount} exceeds the {Customer.MAX_TOP_UP} per-transaction limit.")
        self._wallet_balance = self._wallet_balance + amount
        self._total_topped_up = self._total_topped_up + amount

    def pay_from_wallet(self, amount: Money) -> None:
        """Deduct the FULL amount or nothing at all."""
        if amount > self._wallet_balance:
            raise InsufficientWalletBalanceError(str(amount), str(self._wallet_balance))
        self._wallet_balance = self._wallet_balance - amount
        self._total_wallet_spent = self._total_wallet_spent + amount

    def choose_preferred_payment(self, payable: Payable) -> None:
        self._preferred_payment = payable

    # ----- booking rules -----
    def ensure_can_book(self) -> None:
        """One active ride at a time, and no booking while a ride is unpaid."""
        if self._active_ride is not None:
            raise ActiveRideExistsError(self.name, self._active_ride.ride_id, str(self._active_ride.status))
        if self._unpaid_ride is not None:
            amount = self._unpaid_ride.receipt.total if self._unpaid_ride.receipt else Money.zero()
            raise PaymentPendingError(self.name, self._unpaid_ride.ride_id, str(amount))

    def begin_ride(self, ride: Ride) -> None:
        self.ensure_can_book()
        self._active_ride = ride
        self._ride_history.append(ride)   # chronological: appended at booking time

    def finish_ride(self, ride: Ride, drop: Location, at: datetime) -> None:
        """Event: the trip ended. The customer is exactly where they got off."""
        self._require_active(ride)
        self._active_ride = None
        self._location = drop
        self._location_since = at

    def ride_cancelled(self, ride: Ride) -> None:
        """Event: the ride was cancelled before the trip. The customer did not move."""
        self._require_active(ride)
        self._active_ride = None

    def mark_payment_pending(self, ride: Ride) -> None:
        self._unpaid_ride = ride

    def clear_payment_pending(self, ride: Ride) -> None:
        if self._unpaid_ride is ride:
            self._unpaid_ride = None

    def relocate_to(self, place: Location, at: datetime) -> None:
        """The customer moved by themselves (bus, metro, walking) — only if enough time has passed.

        No teleporting: at 20 km/h, reaching a place 10 km away needs at least 30 minutes.
        """
        if self._active_ride is not None:
            raise ActiveRideExistsError(self.name, self._active_ride.ride_id, str(self._active_ride.status))
        needed_minutes = math.ceil(self._location.road_distance_to(place) / Customer.SELF_TRAVEL_SPEED_KMPH * 60)
        earliest = self._location_since + timedelta(minutes=needed_minutes)
        if at < earliest:
            raise InvalidBookingError(f"{self.name} cannot be at {place} yet — travelling from "
                                      f"{self._location} takes about {needed_minutes} min "
                                      f"(earliest {earliest:%I:%M %p}).")
        self._location = place
        self._location_since = at

    # ----- cancellation fees -----
    def add_pending_fee(self, fee: PendingFee) -> None:
        self._pending_fees.append(fee)

    def take_pending_fees(self) -> list[PendingFee]:
        """Hand over all pending fees and clear them, so each fee is charged exactly once."""
        fees = list(self._pending_fees)
        self._pending_fees.clear()
        return fees

    # ----- coupons -----
    def has_used_coupon(self, code: str) -> bool:
        return code.upper() in self._used_coupons

    def mark_coupon_used(self, code: str) -> None:
        self._used_coupons.add(code.upper())

    def _require_active(self, ride: Ride) -> None:
        if self._active_ride is not ride:
            raise InvalidBookingError(f"Ride {ride.ride_id} is not {self.name}'s active ride.")
