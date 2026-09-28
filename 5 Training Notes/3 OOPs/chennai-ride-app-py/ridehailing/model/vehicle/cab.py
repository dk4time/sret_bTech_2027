"""Cab — an abstract middle layer between Vehicle and the concrete cab categories."""

from __future__ import annotations

from abc import ABC, abstractmethod

from ridehailing.model.vehicle.vehicle import Vehicle


class Cab(Vehicle, ABC):
    """Abstract cab.

    Multilevel inheritance: Vehicle -> Cab -> CabEconomy / CabPremium.
    Cab fills in what ALL cabs share (speed, AC) but still leaves the rates abstract, and adds a
    new abstract member of its own (``luggage_bags``). So ``Cab(...)`` also raises TypeError.
    """

    def __init__(self, registration_number: str, model_name: str, colour: str) -> None:
        super().__init__(registration_number, model_name, colour)
        self._air_conditioned = True

    @property
    def is_air_conditioned(self) -> bool:
        return self._air_conditioned

    @property
    @abstractmethod
    def luggage_bags(self) -> int:
        """How many suitcases fit in the boot."""

    def average_speed_kmph(self, is_peak: bool) -> int:
        # Implemented once for all cabs; CabEconomy and CabPremium inherit it unchanged.
        return 22 if is_peak else 32

    def __str__(self) -> str:
        # Method overriding + super(): reuse Vehicle's text, then add cab-specific detail.
        return f"{super().__str__()} AC"
