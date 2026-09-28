"""RideRequest — what the customer asked for, frozen at booking time."""

from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING

from ridehailing.model.common.location import Location
from ridehailing.model.vehicle.vehicle_type import VehicleType

if TYPE_CHECKING:
    from ridehailing.model.user.customer import Customer


class RideRequest:
    """The booking details. Read-only after creation (getters only)."""

    def __init__(self, customer: Customer, pickup: Location, drop: Location,
                 vehicle_type: VehicleType, passengers: int, requested_at: datetime,
                 coupon_code: str | None = None) -> None:
        self._customer = customer
        self._pickup = pickup
        self._drop = drop
        self._vehicle_type = vehicle_type
        self._passengers = passengers
        self._requested_at = requested_at
        self._coupon_code = coupon_code.upper() if coupon_code else None

    @property
    def customer(self) -> Customer:
        return self._customer

    @property
    def pickup(self) -> Location:
        return self._pickup

    @property
    def drop(self) -> Location:
        return self._drop

    @property
    def vehicle_type(self) -> VehicleType:
        return self._vehicle_type

    @property
    def passengers(self) -> int:
        return self._passengers

    @property
    def requested_at(self) -> datetime:
        return self._requested_at

    @property
    def coupon_code(self) -> str | None:
        return self._coupon_code

    def __str__(self) -> str:
        return (f"{self._customer.name}: {self._pickup} → {self._drop} "
                f"({self._vehicle_type.display_name}, {self._passengers} pax)")

    def __repr__(self) -> str:
        return (f"RideRequest(customer='{self._customer.user_id}', pickup='{self._pickup}', "
                f"drop='{self._drop}', vehicle_type={self._vehicle_type.name})")
