"""Location — an immutable value object: a named point on the map."""

from __future__ import annotations

import math

from ridehailing.model.common import chennai_places


class Location:
    """A named latitude/longitude point.

    OOP concepts shown here:
    * Immutability: read-only properties and a ``__setattr__`` that refuses changes.
    * @classmethod alternative constructor: ``Location.from_place_name("Egmore")``.
    * @staticmethod helper: ``Location.haversine_km(...)`` needs no object at all.
    * Value equality: two Locations with the same coordinates are equal and hash the same,
      so they work as dict keys and in sets.
    """

    # Class attributes (constants shared by all Location objects)
    EARTH_RADIUS_KM = 6371.0
    ROAD_FACTOR = 1.25          # roads are never a straight line: road km ≈ 1.25 × straight km
    _COORDINATE_DECIMALS = 5    # ~1 metre precision for equality

    def __init__(self, name: str, latitude: float, longitude: float) -> None:
        if not name or not name.strip():
            raise ValueError("A location needs a name.")
        if not -90.0 <= latitude <= 90.0:
            raise ValueError(f"Latitude {latitude} is out of range.")
        if not -180.0 <= longitude <= 180.0:
            raise ValueError(f"Longitude {longitude} is out of range.")
        # Immutability: set the fields once, bypassing our own __setattr__.
        object.__setattr__(self, "_name", name.strip())
        object.__setattr__(self, "_latitude", float(latitude))
        object.__setattr__(self, "_longitude", float(longitude))

    # ----- @classmethod: alternative constructor -----
    @classmethod
    def from_place_name(cls, place_name: str) -> Location:
        """Build a Location from a known Chennai place: ``Location.from_place_name("Egmore")``."""
        latitude, longitude = chennai_places.find_coordinates(place_name)
        return cls(chennai_places.canonical_name(place_name), latitude, longitude)

    # ----- @staticmethod: a pure helper that needs neither self nor cls -----
    @staticmethod
    def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
        """Great-circle distance in km between two lat/long points (the Haversine formula)."""
        phi1 = math.radians(lat1)
        phi2 = math.radians(lat2)
        delta_phi = math.radians(lat2 - lat1)
        delta_lambda = math.radians(lon2 - lon1)
        a = (math.sin(delta_phi / 2) ** 2
             + math.cos(phi1) * math.cos(phi2) * math.sin(delta_lambda / 2) ** 2)
        return 2 * Location.EARTH_RADIUS_KM * math.asin(math.sqrt(a))

    # ----- immutability -----
    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"Location is immutable — cannot set '{name}'.")

    def __delattr__(self, name: str) -> None:
        raise AttributeError(f"Location is immutable — cannot delete '{name}'.")

    # ----- read-only properties -----
    @property
    def name(self) -> str:
        return self._name

    @property
    def latitude(self) -> float:
        return self._latitude

    @property
    def longitude(self) -> float:
        return self._longitude

    # ----- behaviour -----
    def distance_to(self, other: Location) -> float:
        """Straight-line (as the crow flies) distance in km."""
        return Location.haversine_km(self._latitude, self._longitude,
                                     other.latitude, other.longitude)

    def road_distance_to(self, other: Location) -> float:
        """Approximate driving distance in km."""
        return self.distance_to(other) * Location.ROAD_FACTOR

    def is_within_km(self, other: Location, radius_km: float) -> bool:
        return self.distance_to(other) <= radius_km

    def is_near_place(self, place_name: str, radius_km: float) -> bool:
        return self.is_within_km(Location.from_place_name(place_name), radius_km)

    def point_towards(self, destination: Location, fraction: float, name: str) -> Location:
        """A new point part-way along the line to ``destination`` (0.0 = here, 1.0 = there).

        Returns a NEW Location — the existing objects never change (immutability).
        """
        if not 0.0 <= fraction <= 1.0:
            raise ValueError("fraction must be between 0 and 1.")
        latitude = self._latitude + (destination.latitude - self._latitude) * fraction
        longitude = self._longitude + (destination.longitude - self._longitude) * fraction
        return Location(name, latitude, longitude)

    # ----- value equality -----
    def _key(self) -> tuple[float, float]:
        return (round(self._latitude, Location._COORDINATE_DECIMALS),
                round(self._longitude, Location._COORDINATE_DECIMALS))

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Location):
            return NotImplemented
        return self._key() == other._key()

    def __hash__(self) -> int:
        return hash(self._key())

    # ----- string forms -----
    def __str__(self) -> str:
        return self._name

    def __repr__(self) -> str:
        return f"Location('{self._name}', {self._latitude:.4f}, {self._longitude:.4f})"
