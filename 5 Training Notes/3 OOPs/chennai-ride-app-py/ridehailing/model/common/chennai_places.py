"""Real Chennai places with their latitude/longitude, plus traffic knowledge about the city.

Only plain data lives here (tuples of floats), so that ``location.py`` can import this module
without creating a circular import.
"""

from __future__ import annotations

# name -> (latitude, longitude)
CHENNAI_PLACES: dict[str, tuple[float, float]] = {
    "T. Nagar": (13.0418, 80.2341),
    "Anna Nagar": (13.0850, 80.2101),
    "Adyar": (13.0012, 80.2565),
    "Velachery": (12.9815, 80.2180),
    "Guindy": (13.0067, 80.2206),
    "Mylapore": (13.0368, 80.2676),
    "Egmore": (13.0732, 80.2609),
    "Chennai Central": (13.0827, 80.2757),
    "Chennai Airport": (12.9941, 80.1709),  # Meenambakkam
    "Sholinganallur": (12.9010, 80.2279),   # OMR
    "Porur": (13.0382, 80.1565),
    "Vadapalani": (13.0500, 80.2121),
    "Besant Nagar": (13.0003, 80.2667),
    "Koyambedu": (13.0694, 80.1948),
    "Tambaram": (12.9249, 80.1000),
    "Chromepet": (12.9516, 80.1462),
    "Pallavaram": (12.9675, 80.1491),
    "Guduvanchery": (12.8452, 80.0606),
    "Medavakkam": (12.9171, 80.1923),
    "Perungudi": (12.9654, 80.2461),       # OMR
    "Thiruvanmiyur": (12.9830, 80.2594),
    "Ashok Nagar": (13.0373, 80.2123),
    "KK Nagar": (13.0410, 80.1990),
    "Kilpauk": (13.0843, 80.2422),
    "Royapettah": (13.0540, 80.2640),
    "Washermanpet": (13.1148, 80.2872),
    # Landmarks used in messages and mid-ride stops
    "Kathipara Junction": (13.0098, 80.2050),
    "Saidapet": (13.0213, 80.2231),
    # Outside the service area — used to show a rejected booking
    "Mahabalipuram": (12.6208, 80.1945),
    "Kanchipuram": (12.8342, 79.7036),
}

# Places where demand spikes during peak hours (surge is higher here).
HIGH_DEMAND_HOTSPOTS: tuple[str, ...] = ("Chennai Central", "Koyambedu", "Chennai Airport")

# Places where peak-hour traffic crawls even more (OMR, T. Nagar and Koyambedu).
SLOW_TRAFFIC_ZONES: tuple[str, ...] = ("Sholinganallur", "Perungudi", "T. Nagar", "Koyambedu")


def all_place_names() -> list[str]:
    """Every known place name, in the order listed above."""
    return list(CHENNAI_PLACES)


def find_coordinates(place_name: str) -> tuple[float, float]:
    """Return (lat, lng) for a place, ignoring case. Raises KeyError for an unknown place."""
    for name, coordinates in CHENNAI_PLACES.items():
        if name.lower() == place_name.strip().lower():
            return coordinates
    raise KeyError(f"Unknown Chennai place: '{place_name}'")


def canonical_name(place_name: str) -> str:
    """Return the correctly spelt name, e.g. 'egmore' -> 'Egmore'."""
    for name in CHENNAI_PLACES:
        if name.lower() == place_name.strip().lower():
            return name
    raise KeyError(f"Unknown Chennai place: '{place_name}'")
