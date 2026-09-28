"""VehicleType — an Enum whose members carry extra data and behaviour."""

from __future__ import annotations

from enum import Enum

from ridehailing.model.common.money import Money


class VehicleType(Enum):
    """The four categories shown on the booking screen.

    OOP: an Enum member is an object. Each member here carries a display name, a seat count and a
    cancellation fee, and the Enum has methods like any other class.
    """

    #              display name    seats  cancellation fee (₹)
    BIKE = ("Bike", 1, "20")
    AUTO = ("Auto", 3, "30")
    CAB_ECONOMY = ("Cab Economy", 4, "30")
    CAB_PREMIUM = ("Cab Premium", 6, "30")

    def __init__(self, display_name: str, seats: int, cancellation_fee: str) -> None:
        # Enum calls __init__ once per member with the tuple unpacked.
        self.display_name = display_name
        self.seats = seats
        self._cancellation_fee_rupees = cancellation_fee

    @property
    def cancellation_fee(self) -> Money:
        """Fee for a late cancellation or a customer no-show: ₹20 bike, ₹30 auto/cab."""
        return Money(self._cancellation_fee_rupees)

    def can_carry(self, passengers: int) -> bool:
        return 1 <= passengers <= self.seats

    def is_cab(self) -> bool:
        return self in (VehicleType.CAB_ECONOMY, VehicleType.CAB_PREMIUM)

    def __str__(self) -> str:
        return self.display_name
