"""ServiceArea — the bounding box inside which the app operates."""

from __future__ import annotations

from ridehailing.exceptions import OutOfServiceAreaError
from ridehailing.model.common.location import Location


class ServiceArea:
    """A rectangular (lat/long bounding box) service area."""

    def __init__(self, name: str, south: float, north: float, west: float, east: float) -> None:
        if south >= north or west >= east:
            raise ValueError("A service area needs south < north and west < east.")
        self._name = name
        self._south = south
        self._north = north
        self._west = west
        self._east = east

    @classmethod
    def chennai_metro(cls) -> ServiceArea:
        """@classmethod alternative constructor for the Chennai metropolitan area."""
        return cls("Chennai Metro", south=12.80, north=13.25, west=79.95, east=80.35)

    @property
    def name(self) -> str:
        return self._name

    def contains(self, location: Location) -> bool:
        return (self._south <= location.latitude <= self._north
                and self._west <= location.longitude <= self._east)

    def ensure_contains(self, location: Location) -> None:
        """Raise OutOfServiceAreaError if the location lies outside the area."""
        if not self.contains(location):
            raise OutOfServiceAreaError(location.name, self._name)

    def __str__(self) -> str:
        return f"{self._name} ({self._south}–{self._north}°N, {self._west}–{self._east}°E)"

    def __repr__(self) -> str:
        return (f"ServiceArea('{self._name}', south={self._south}, north={self._north}, "
                f"west={self._west}, east={self._east})")
