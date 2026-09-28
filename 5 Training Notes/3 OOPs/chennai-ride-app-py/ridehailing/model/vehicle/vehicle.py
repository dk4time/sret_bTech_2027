"""Vehicle — the abstract base class of every vehicle a captain can drive."""

from __future__ import annotations

import re
from abc import ABC, abstractmethod
from decimal import Decimal

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.vehicle_type import VehicleType


class Vehicle(ABC):
    """Abstract vehicle.

    OOP concepts shown here:
    * Abstraction: ``abc.ABC`` + ``@abstractmethod``. ``Vehicle(...)`` raises TypeError because
      the rates are unknown until a subclass (Bike, Auto, CabEconomy, CabPremium) supplies them.
    * Polymorphism: ``calculate_base_fare()`` is written ONCE here, but uses ``self.base_fare``,
      ``self.per_km_rate`` ... which each subclass overrides. The same call gives a different
      answer for a Bike and for a CabPremium.
    """

    # Class attribute: Tamil Nadu registration plate format, e.g. TN-09-AB-4521
    _PLATE_PATTERN = re.compile(r"^TN-\d{2}-[A-Z]{1,2}-\d{4}$")

    def __init__(self, registration_number: str, model_name: str, colour: str) -> None:
        if not Vehicle._PLATE_PATTERN.match(registration_number):
            raise ValueError(f"'{registration_number}' is not a valid TN plate (e.g. TN-09-AB-4521).")
        if not model_name.strip():
            raise ValueError("A vehicle needs a model name.")
        self._registration_number = registration_number   # _protected: internal state
        self._model_name = model_name
        self._colour = colour

    # ----- read-only properties -----
    @property
    def registration_number(self) -> str:
        return self._registration_number

    @property
    def model_name(self) -> str:
        return self._model_name

    @property
    def colour(self) -> str:
        return self._colour

    # ----- abstract members: every concrete vehicle MUST override these -----
    @property
    @abstractmethod
    def vehicle_type(self) -> VehicleType:
        """Which booking category this vehicle serves."""

    @property
    @abstractmethod
    def base_fare(self) -> Money:
        """Flat amount charged when the trip starts."""

    @property
    @abstractmethod
    def per_km_rate(self) -> Money:
        """Charge per kilometre travelled."""

    @property
    @abstractmethod
    def per_minute_rate(self) -> Money:
        """Charge per minute of trip time."""

    @property
    @abstractmethod
    def minimum_fare(self) -> Money:
        """The trip fare never goes below this."""

    @property
    @abstractmethod
    def seat_capacity(self) -> int:
        """Maximum passengers."""

    @abstractmethod
    def average_speed_kmph(self, is_peak: bool) -> int:
        """Typical Chennai city speed, lower during peak hours."""

    # ----- concrete method that relies on the abstract ones (central polymorphism example) -----
    def distance_charge(self, distance_km: Decimal) -> Money:
        return self.per_km_rate * distance_km

    def time_charge(self, minutes: int) -> Money:
        return self.per_minute_rate * minutes

    def calculate_base_fare(self, distance_km: Decimal, minutes: int) -> Money:
        """base fare + distance charge + time charge, never below the minimum fare.

        This method is NOT overridden anywhere — yet it returns different values for a Bike and a
        CabPremium, because ``self.base_fare`` etc. are dispatched to the subclass at runtime.
        """
        fare = self.base_fare + self.distance_charge(distance_km) + self.time_charge(minutes)
        if fare < self.minimum_fare:
            return self.minimum_fare
        return fare

    def can_carry(self, passengers: int) -> bool:
        return 1 <= passengers <= self.seat_capacity

    # ----- entity equality: a vehicle is identified by its registration number -----
    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Vehicle):
            return NotImplemented
        return self._registration_number == other._registration_number

    def __hash__(self) -> int:
        return hash(self._registration_number)

    def __str__(self) -> str:
        return f"{self._model_name} ({self._registration_number})"

    def __repr__(self) -> str:
        return f"{type(self).__name__}('{self._registration_number}', '{self._model_name}')"
