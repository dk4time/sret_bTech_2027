"""MatchingService — offers a ride to the nearest eligible captain, then the next nearest..."""

from __future__ import annotations

from datetime import datetime

from ridehailing.exceptions import NoCaptainAvailableError
from ridehailing.model.common.location import Location
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.service.captain_service import CaptainService
from ridehailing.service.fare_service import FareService


class MatchingService:
    """Finds a captain for a ride.

    A captain is eligible only if ALL of these hold: online and free, the right vehicle type,
    within the search radius, has not rejected/cancelled THIS ride, is allowed cash rides (for a
    cash booking), has enough seats, and is not still busy with an earlier ride's time.
    """

    SEARCH_RADIUS_KM = 8.0
    MAX_REJECTIONS = 3

    def __init__(self, captain_service: CaptainService, fare_service: FareService) -> None:
        self._captain_service = captain_service
        self._fare_service = fare_service

    # ----- eligibility -----
    def ineligibility_reason(self, captain: Captain, ride: Ride, at: datetime) -> str | None:
        """None when the captain may be offered this ride, otherwise the reason why not."""
        return self._reason(captain, ride.pickup, ride.vehicle_type, at,
                            needs_cash=ride.payment_method.is_cash,
                            passengers=ride.request.passengers, ride=ride)

    def captains_near(self, place: Location, vehicle_type: VehicleType | None,
                      at: datetime) -> list[tuple[Captain, float, str | None]]:
        """Every KYC-verified captain, nearest first, with the reason they could NOT take a request
        from ``place`` right now (None = eligible). ``vehicle_type=None`` means any type."""
        rows = []
        for captain in self._captain_service.captains:
            reason = self._reason(captain, place, vehicle_type, at, needs_cash=False, passengers=1, ride=None)
            rows.append((captain, captain.location.distance_to(place), reason))
        rows.sort(key=lambda row: row[1])
        return rows

    def _reason(self, captain: Captain, pickup: Location, vehicle_type: VehicleType | None, at: datetime,
                needs_cash: bool, passengers: int, ride: Ride | None) -> str | None:
        """The single place where every eligibility rule is written, checked in a fixed order."""
        if not captain.is_available:
            return f"not available ({captain.status})"
        if captain.pending_offer is not None and captain.pending_offer is not ride:
            return f"deciding on another offer ({captain.pending_offer.ride_id})"
        if vehicle_type is not None and captain.vehicle_type != vehicle_type:
            return f"wrong vehicle ({captain.vehicle_type}; needs {vehicle_type})"
        if ride is not None and ride.is_excluded(captain):
            return "already rejected or cancelled this ride"
        distance = captain.location.distance_to(pickup)
        if distance > MatchingService.SEARCH_RADIUS_KM:
            return f"{distance:.1f} km away (radius {MatchingService.SEARCH_RADIUS_KM:.0f} km)"
        if needs_cash and not captain.accepts_cash_rides:
            return f"blocked from cash rides (dues {captain.dues_owed})"
        if not captain.vehicle.can_carry(passengers):
            return "not enough seats"
        if captain.free_since is not None and at < captain.free_since:
            return "still finishing an earlier ride"
        return None

    def eligible_captains(self, ride: Ride, at: datetime) -> list[Captain]:
        """Eligible captains, nearest first."""
        eligible = []
        for captain in self._captain_service.captains:
            if self.ineligibility_reason(captain, ride, at) is None:
                eligible.append(captain)
        eligible.sort(key=lambda captain: captain.location.distance_to(ride.pickup))
        return eligible

    def free_captains_near(self, pickup: Location, vehicle_type: VehicleType) -> list[Captain]:
        """Free captains of one type within the radius — the SUPPLY side of surge and estimates."""
        nearby = []
        for captain in self._captain_service.captains:
            if (captain.is_available and captain.vehicle_type == vehicle_type
                    and captain.location.distance_to(pickup) <= MatchingService.SEARCH_RADIUS_KM):
                nearby.append(captain)
        nearby.sort(key=lambda captain: captain.location.distance_to(pickup))
        return nearby

    def offered_captains(self) -> list[Captain]:
        """Captains who currently have a ride offer waiting for their answer (manual mode)."""
        return [captain for captain in self._captain_service.captains if captain.pending_offer is not None]

    def eta_minutes(self, captain: Captain, pickup: Location, at: datetime) -> int:
        return self._fare_service.travel_minutes(captain.vehicle, captain.location, pickup, at)

    def nearest_eta(self, pickup: Location, vehicle_type: VehicleType, at: datetime) -> int | None:
        nearby = self.free_captains_near(pickup, vehicle_type)
        if not nearby:
            return None
        return self.eta_minutes(nearby[0], pickup, at)

    # ----- the offer loop -----
    def next_candidate(self, ride: Ride, at: datetime) -> Captain:
        """The captain who should be offered this ride next (the nearest eligible one).

        Raises NoCaptainAvailableError after 3 rejections or when nobody eligible is left.
        """
        if ride.rejection_count >= MatchingService.MAX_REJECTIONS:
            raise NoCaptainAvailableError(ride.vehicle_type.display_name, ride.pickup.name,
                                          f"{MatchingService.MAX_REJECTIONS} offers rejected")
        candidates = self.eligible_captains(ride, at)
        if not candidates:
            raise NoCaptainAvailableError(ride.vehicle_type.display_name, ride.pickup.name,
                                          f"no eligible captain within {MatchingService.SEARCH_RADIUS_KM:.0f} km")
        return candidates[0]

    def find_captain(self, ride: Ride, at: datetime) -> Captain:
        """Offer to the nearest eligible captain; on rejection move to the next nearest.

        Stops with NoCaptainAvailableError after 3 rejections or when nobody eligible is left.
        """
        while True:
            captain = self.next_candidate(ride, at)
            accepted = captain.respond_to_offer(ride)
            ride.record_offer(captain, captain.location.distance_to(ride.pickup), accepted, at)
            if accepted:
                return captain
