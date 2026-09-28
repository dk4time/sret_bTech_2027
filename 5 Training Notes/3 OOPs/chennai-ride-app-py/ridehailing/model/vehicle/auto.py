"""Auto — the three-wheeler auto-rickshaw (Bajaj RE Auto, TVS King...)."""

from __future__ import annotations

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.model.vehicle.vehicle_type import VehicleType


class Auto(Vehicle):
    """An auto-rickshaw. Inheritance: Auto IS-A Vehicle."""

    _BASE_FARE = Money("25")
    _PER_KM = Money("12")
    _PER_MINUTE = Money("1.00")
    _MINIMUM_FARE = Money("40")

    def __init__(self, registration_number: str, model_name: str, colour: str = "Yellow-Green") -> None:
        super().__init__(registration_number, model_name, colour)

    @property
    def vehicle_type(self) -> VehicleType:
        return VehicleType.AUTO

    @property
    def base_fare(self) -> Money:
        return Auto._BASE_FARE

    @property
    def per_km_rate(self) -> Money:
        return Auto._PER_KM

    @property
    def per_minute_rate(self) -> Money:
        return Auto._PER_MINUTE

    @property
    def minimum_fare(self) -> Money:
        return Auto._MINIMUM_FARE

    @property
    def seat_capacity(self) -> int:
        return 3

    def average_speed_kmph(self, is_peak: bool) -> int:
        return 18 if is_peak else 26

    def __str__(self) -> str:
        return f"{self.model_name} ({self.registration_number})"
