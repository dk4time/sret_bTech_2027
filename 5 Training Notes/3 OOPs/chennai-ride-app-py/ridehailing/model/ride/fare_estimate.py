"""FareEstimate — one row of the "choose your ride" screen."""

from __future__ import annotations

from decimal import Decimal

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.vehicle_type import VehicleType


class FareEstimate:
    """Estimated fare, trip time and nearest-captain ETA for one vehicle type."""

    def __init__(self, vehicle_type: VehicleType, distance_km: Decimal, trip_minutes: int,
                 surge_multiplier: Decimal, fare: Money, nearest_captain_eta: int | None) -> None:
        self._vehicle_type = vehicle_type
        self._distance_km = distance_km
        self._trip_minutes = trip_minutes
        self._surge_multiplier = surge_multiplier
        self._fare = fare
        self._nearest_captain_eta = nearest_captain_eta   # None = no captain nearby

    @property
    def vehicle_type(self) -> VehicleType:
        return self._vehicle_type

    @property
    def distance_km(self) -> Decimal:
        return self._distance_km

    @property
    def trip_minutes(self) -> int:
        return self._trip_minutes

    @property
    def surge_multiplier(self) -> Decimal:
        return self._surge_multiplier

    @property
    def fare(self) -> Money:
        return self._fare

    @property
    def nearest_captain_eta(self) -> int | None:
        return self._nearest_captain_eta

    @property
    def is_available(self) -> bool:
        return self._nearest_captain_eta is not None

    def __str__(self) -> str:
        eta = f"{self._nearest_captain_eta} min away" if self.is_available else "No captains nearby"
        surge = f"{self._surge_multiplier}x" if self._surge_multiplier > 1 else "—"
        return (f"{self._vehicle_type.display_name:<12} {str(self._fare):>10}  "
                f"{self._trip_minutes:>3} min trip  surge {surge:<5} {eta}")

    def __repr__(self) -> str:
        return f"FareEstimate({self._vehicle_type.name}, fare={self._fare!r}, eta={self._nearest_captain_eta})"
