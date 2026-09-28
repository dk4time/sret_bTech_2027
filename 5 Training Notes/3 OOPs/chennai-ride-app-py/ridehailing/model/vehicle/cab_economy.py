"""CabEconomy — a 4-seater sedan (Maruti Dzire, Hyundai Aura)."""

from __future__ import annotations

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.cab import Cab
from ridehailing.model.vehicle.vehicle_type import VehicleType


class CabEconomy(Cab):
    """Economy sedan. Inheritance chain: Vehicle -> Cab -> CabEconomy."""

    _BASE_FARE = Money("50")
    _PER_KM = Money("16")
    _PER_MINUTE = Money("1.50")
    _MINIMUM_FARE = Money("80")

    def __init__(self, registration_number: str, model_name: str, colour: str = "White") -> None:
        super().__init__(registration_number, model_name, colour)

    @property
    def vehicle_type(self) -> VehicleType:
        return VehicleType.CAB_ECONOMY

    @property
    def base_fare(self) -> Money:
        return CabEconomy._BASE_FARE

    @property
    def per_km_rate(self) -> Money:
        return CabEconomy._PER_KM

    @property
    def per_minute_rate(self) -> Money:
        return CabEconomy._PER_MINUTE

    @property
    def minimum_fare(self) -> Money:
        return CabEconomy._MINIMUM_FARE

    @property
    def seat_capacity(self) -> int:
        return 4

    @property
    def luggage_bags(self) -> int:
        return 2
