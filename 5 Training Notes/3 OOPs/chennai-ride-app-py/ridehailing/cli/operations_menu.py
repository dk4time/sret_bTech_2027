"""OperationsMenu — the operations team's dashboard."""

from __future__ import annotations

from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.cli.menu import Menu
from ridehailing.model.common import chennai_places
from ridehailing.model.common.location import Location
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.service.matching_service import MatchingService


class OperationsMenu(Menu):
    """Inheritance: OperationsMenu IS-A Menu. Read-only views of the live world."""

    def __init__(self, io: ConsoleIO, session: CliSession) -> None:
        super().__init__("Operations dashboard", io)
        self._session = session

    def status_line(self) -> str:
        return self._session.status_line()

    def heading(self) -> str:
        return "OPERATIONS DASHBOARD"

    def options(self) -> list[tuple[str, str]]:
        return [("1", "Captain position board"), ("2", "Live rides"),
                ("3", "Nearby captains for a place"), ("4", "Surge status at hotspots"),
                ("5", "Notification log"), ("6", "Earnings summary and books reconciliation"),
                ("7", "Flagged captains / captains blocked from cash rides"), ("0", "Back")]

    def handle(self, choice: str) -> bool:
        actions = {"1": self._board, "2": self._live_rides, "3": self._nearby, "4": self._surge,
                   "5": self._notifications, "6": self._earnings, "7": self._flagged}
        if choice == "0":
            return False
        actions[choice]()
        return True

    # ----- 1 -----
    def _board(self) -> None:
        today = self._session.clock.now.date()
        rows = []
        for captain in self._session.captains:
            entry = self._session.app.earnings_service.earnings_for(captain, today)
            rows.append([captain.name, f"{captain.vehicle.model_name} ({captain.vehicle_type})",
                         captain.location.name, str(captain.status) + (" ●" if captain.pending_offer else ""),
                         str(entry.rides), str(entry.net_earnings), str(captain.dues_owed)])
        self._io.print_info(f"Captain position board at {self._session.clock} (● = offer waiting)")
        self._io.print_table(["Captain", "Vehicle", "Location", "Status", "Rides", "Earnings", "Dues"],
                             rows, "llllrrr")

    # ----- 2 -----
    def _live_rides(self) -> None:
        rows = []
        for ride in self._session.rides.active_rides():
            since = ride.timeline[-1][0]
            rows.append([ride.ride_id, ride.customer.name, self._session.describe_waiting(ride, hint=False),
                         f"{ride.pickup} → {ride.drop}", ride.status.name, f"{since:%I:%M %p}"])
        self._io.print_table(["Ride", "Customer", "Captain", "Route", "Status", "Since"], rows)

    # ----- 3 -----
    def _nearby(self) -> None:
        place = self._session.read_place("Place: ")
        vehicle_types = list(VehicleType)
        self._io.print_info("0. Any vehicle   " + "   ".join(f"{number}. {vehicle_type.display_name}"
                                                            for number, vehicle_type in enumerate(vehicle_types, start=1)))
        number = self._io.read_int("  Vehicle type [Enter = any]: ", 0, len(vehicle_types), default=0)
        vehicle_type = vehicle_types[number - 1] if number else None
        rows = []
        for captain, distance, reason in self._session.app.matching_service.captains_near(
                place, vehicle_type, self._session.clock.now):
            verdict = "✔ eligible" if reason is None else f"✘ {reason}"
            rows.append([captain.name, f"{captain.vehicle_type}", captain.location.name,
                         f"{distance:.1f} km", verdict])
        kind = vehicle_type.display_name if vehicle_type else "any vehicle"
        self._io.print_info(f"Captains for a request from {place} ({kind}), nearest first — "
                            f"search radius {MatchingService.SEARCH_RADIUS_KM:.0f} km:")
        self._io.print_table(["Captain", "Vehicle", "Location", "Distance", "Eligible?"], rows, "lllrl")

    # ----- 4 -----
    def _surge(self) -> None:
        places = [Location.from_place_name(name) for name in chennai_places.HIGH_DEMAND_HOTSPOTS]
        if self._io.read_yes_no("Add another place to the hotspots? [y/n]: "):
            places.append(self._session.read_place("Place: "))
        rows = []
        for place in places:
            row = [place.name]
            for vehicle_type in VehicleType:
                surge = self._session.rides.surge_at(place, vehicle_type)
                eta = self._session.app.matching_service.nearest_eta(place, vehicle_type, self._session.clock.now)
                row.append(f"{surge}x / {eta} min" if eta is not None else f"{surge}x / none")
            rows.append(row)
        self._io.print_info(f"Surge and nearest-captain ETA at {self._session.clock}:")
        self._io.print_table(["Place"] + [vehicle_type.display_name for vehicle_type in VehicleType], rows, "lrrrr")

    # ----- 5 -----
    def _notifications(self) -> None:
        entries = list(self._session.app.sms.log) + list(self._session.app.push.log)
        entries.sort(key=lambda entry: entry[0], reverse=True)      # latest first
        self._io.print_info(f"{len(entries)} notification(s) so far, latest first:")
        for _, _, line in entries:
            print(f"  {line.strip()}")

    # ----- 6 -----
    def _earnings(self) -> None:
        today = self._session.clock.now.date()
        rows = []
        for captain in self._session.captains:
            entry = self._session.app.earnings_service.earnings_for(captain, today)
            if entry.rides or not entry.gross_fare.is_zero():
                rows.append([captain.name, str(entry.rides), str(entry.gross_fare), str(entry.commission),
                             str(entry.net_earnings), str(entry.cash_collected), str(captain.dues_owed)])
        self._io.print_table(["Captain", "Rides", "Gross", "Commission", "Net", "Cash coll.", "Dues now"],
                             rows, "lrrrrrr")
        print()
        self._session.app.print_reconciliation()

    # ----- 7 -----
    def _flagged(self) -> None:
        captains = self._session.captains
        flagged = self._session.app.rating_service.flagged_captains(captains)
        self._io.print_info("Flagged captains (average below 4.0 after at least 5 ratings):")
        self._io.print_table(["Captain", "Average", "Ratings"],
                             [[c.name, str(c.average_rating), str(c.rating_count)] for c in flagged], "lrr")
        blocked = [captain for captain in captains if not captain.accepts_cash_rides]
        self._io.print_info("Captains blocked from cash rides (dues above ₹500):")
        self._io.print_table(["Captain", "Dues owed"], [[c.name, str(c.dues_owed)] for c in blocked], "lr")
