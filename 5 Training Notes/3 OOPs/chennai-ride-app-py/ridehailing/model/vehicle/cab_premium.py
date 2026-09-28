"""CabPremium — a 6-seater SUV (Toyota Innova Crysta)."""

from __future__ import annotations

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.cab import Cab
from ridehailing.model.vehicle.vehicle_type import VehicleType


class CabPremium(Cab):
    """Premium SUV. Deepest level of the multilevel chain: Vehicle -> Cab -> CabPremium."""

    _BASE_FARE = Money("80")
    _PER_KM = Money("22")
    _PER_MINUTE = Money("2.00")
    _MINIMUM_FARE = Money("150")

    def __init__(self, registration_number: str, model_name: str, colour: str = "Silver") -> None:
        super().__init__(registration_number, model_name, colour)

    @property
    def vehicle_type(self) -> VehicleType:
        return VehicleType.CAB_PREMIUM

    @property
    def base_fare(self) -> Money:
        return CabPremium._BASE_FARE

    @property
    def per_km_rate(self) -> Money:
        return CabPremium._PER_KM

    @property
    def per_minute_rate(self) -> Money:
        return CabPremium._PER_MINUTE

    @property
    def minimum_fare(self) -> Money:
        return CabPremium._MINIMUM_FARE

    @property
    def seat_capacity(self) -> int:
        return 6

    @property
    def luggage_bags(self) -> int:
        return 4

    def __str__(self) -> str:
        # Overrides Cab.__str__, which itself overrides Vehicle.__str__.
        return f"{super().__str__()} Premium SUV"
