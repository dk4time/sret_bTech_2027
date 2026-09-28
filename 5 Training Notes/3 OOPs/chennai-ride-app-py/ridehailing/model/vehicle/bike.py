"""Bike — the cheapest category (Honda Activa, TVS Jupiter...)."""

from __future__ import annotations

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.model.vehicle.vehicle_type import VehicleType


class Bike(Vehicle):
    """A bike taxi. Inheritance: Bike IS-A Vehicle and overrides every abstract rate."""

    # Class attributes: the rate card is the same for every bike.
    _BASE_FARE = Money("15")
    _PER_KM = Money("5")
    _PER_MINUTE = Money("0.50")
    _MINIMUM_FARE = Money("25")

    def __init__(self, registration_number: str, model_name: str, colour: str = "Black") -> None:
        super().__init__(registration_number, model_name, colour)   # super(): let Vehicle validate

    # Method overriding: each property below replaces an abstract one in Vehicle.
    @property
    def vehicle_type(self) -> VehicleType:
        return VehicleType.BIKE

    @property
    def base_fare(self) -> Money:
        return Bike._BASE_FARE

    @property
    def per_km_rate(self) -> Money:
        return Bike._PER_KM

    @property
    def per_minute_rate(self) -> Money:
        return Bike._PER_MINUTE

    @property
    def minimum_fare(self) -> Money:
        return Bike._MINIMUM_FARE

    @property
    def seat_capacity(self) -> int:
        return 1

    def average_speed_kmph(self, is_peak: bool) -> int:
        # Bikes weave through traffic, so they slow down the least at peak.
        return 24 if is_peak else 32

    def __str__(self) -> str:
        return f"{self.model_name} bike ({self.registration_number})"
