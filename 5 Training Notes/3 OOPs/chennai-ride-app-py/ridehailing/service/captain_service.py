"""CaptainService — registration and the captain's working day."""

from __future__ import annotations

from ridehailing.exceptions import CaptainNotEligibleError
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.user.captain import Captain
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.time.simulated_clock import SimulatedClock


class CaptainService:
    """Keeps every registered captain and the dues they settled."""

    def __init__(self, clock: SimulatedClock) -> None:
        self._clock = clock
        self._captains: list[Captain] = []
        self._total_dues_settled = Money.zero()

    def register_captain(self, name: str, phone: str, home_place: str, vehicle: Vehicle,
                         opening_dues: Money | None = None,
                         past_ratings: list[int] | None = None) -> Captain:
        """Register a captain with a freshly created vehicle. They start as KYC_PENDING."""
        for existing in self._captains:
            if existing.vehicle.registration_number == vehicle.registration_number:
                raise CaptainNotEligibleError(name, f"vehicle {vehicle.registration_number} "
                                                    f"already belongs to {existing.name}.")
        captain = Captain(name, phone, Location.from_place_name(home_place), vehicle,
                          opening_dues=opening_dues, past_ratings=past_ratings)
        self._captains.append(captain)
        return captain

    def verify_kyc(self, captain: Captain) -> None:
        captain.verify_kyc()

    def go_online(self, captain: Captain) -> None:
        captain.go_online()

    def go_offline(self, captain: Captain) -> None:
        captain.go_offline()

    def settle_dues(self, captain: Captain) -> Money:
        settled = captain.settle_dues()
        self._total_dues_settled = self._total_dues_settled + settled
        return settled

    @property
    def captains(self) -> tuple[Captain, ...]:
        return tuple(self._captains)

    @property
    def total_dues_settled(self) -> Money:
        return self._total_dues_settled

    def find(self, name: str) -> Captain:
        for captain in self._captains:
            if captain.name == name:
                return captain
        raise KeyError(f"No captain called {name}")
