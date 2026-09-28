"""CaptainMenu — the captain's phone app."""

from __future__ import annotations

from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.cli.menu import Menu
from ridehailing.exceptions import NoCaptainAvailableError
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.captain_status import CaptainStatus


class CaptainMenu(Menu):
    """Inheritance: CaptainMenu IS-A Menu."""

    def __init__(self, io: ConsoleIO, session: CliSession) -> None:
        super().__init__("Captain app", io)
        self._session = session
        self._captain: Captain | None = None

    # ----- Menu hooks -----
    def on_enter(self) -> bool:
        self._captain = self._pick_captain()
        return True

    def status_line(self) -> str:
        return self._session.status_line()

    def heading(self) -> str:
        captain = self._captain
        offer = f" · ● offer {captain.pending_offer.ride_id} waiting" if captain.pending_offer else ""
        return f"CAPTAIN APP — {captain.name} ({captain.vehicle}) at {captain.location} · {captain.status}{offer}"

    def options(self) -> list[tuple[str, str]]:
        return [("1", "View profile"), ("2", "Go online / go offline"), ("3", "View incoming offer"),
                ("4", "Mark arrived at pickup"), ("5", "Start ride (enter OTP)"), ("6", "Complete ride"),
                ("7", "Cancel ride / mark customer no-show"), ("8", "Today's earnings"),
                ("9", "Settle cash dues"), ("10", "Rate customer for last completed ride"),
                ("11", "Switch captain"), ("0", "Back")]

    def handle(self, choice: str) -> bool:
        actions = {"1": self._profile, "2": self._toggle_online, "3": self._offer, "4": self._arrive,
                   "5": self._start, "6": self._complete, "7": self._cancel, "8": self._earnings,
                   "9": self._settle, "10": self._rate, "11": self._switch}
        if choice == "0":
            return False
        actions[choice]()
        return True

    # ----- helpers -----
    def _pick_captain(self) -> Captain:
        captains = self._session.captains
        rows = []
        for number, captain in enumerate(captains, start=1):
            rows.append([str(number), captain.name, f"{captain.vehicle.model_name} ({captain.vehicle_type})",
                         captain.location.name, str(captain.status), "●" if captain.pending_offer else ""])
        self._io.print_header("Choose a captain (● = ride offer waiting)")
        self._io.print_table(["#", "Captain", "Vehicle", "Location", "Status", "Offer"], rows, "rlllll")
        number = self._io.read_int("Captain number: ", 1, len(captains))
        return captains[number - 1]

    def _current_ride(self) -> Ride | None:
        ride = self._captain.current_ride
        if ride is None:
            self._io.print_info(f"{self._captain.name} has no active ride.")
        return ride

    # ----- 1 -----
    def _profile(self) -> None:
        captain = self._captain
        kyc = "Pending" if captain.status == CaptainStatus.KYC_PENDING else "Verified"
        rows = [
            ["Captain", f"{captain.name} ({captain.user_id}, {captain.phone})"],
            ["Vehicle", f"{captain.vehicle} — {captain.vehicle_type}, {captain.vehicle.seat_capacity} seat(s)"],
            ["Location", captain.location.name],
            ["Status", str(captain.status)],
            ["KYC", kyc],
            ["Rating", f"{captain.average_rating} from {captain.rating_count} rating(s)"],
            ["Acceptance rate", f"{captain.acceptance_rate}% of {captain.offers_received} offer(s)"],
            ["Cancellations", str(captain.cancellation_count)],
            ["Flagged (< 4.0)", "YES ⚑" if captain.is_flagged else "No"],
            ["Cash dues owed", f"{captain.dues_owed}" + ("" if captain.accepts_cash_rides else "  — BLOCKED from cash rides")],
            ["Current ride", str(captain.current_ride) if captain.current_ride else "None"],
            ["Pending offer", captain.pending_offer.ride_id if captain.pending_offer else "None"],
        ]
        self._io.print_table(["Field", "Value"], rows)

    # ----- 2 -----
    def _toggle_online(self) -> None:
        service = self._session.app.captain_service
        if self._captain.status.is_working():
            service.go_offline(self._captain)
        else:
            service.go_online(self._captain)
        self._io.print_success(f"{self._captain.name} is now {self._captain.status} at {self._captain.location}.")

    # ----- 3 -----
    def _offer(self) -> None:
        ride = self._captain.pending_offer
        if ride is None:
            self._io.print_info("No incoming offer right now.")
            return
        distance = self._captain.location.distance_to(ride.pickup)
        eta = self._session.app.matching_service.eta_minutes(self._captain, ride.pickup, self._session.clock.now)
        self._io.print_table(["Offer", "Details"], [
            ["Ride", ride.ride_id],
            ["Customer", ride.customer.name],
            ["Pickup", f"{ride.pickup} — {distance:.1f} km from you, about {eta} min"],
            ["Drop", ride.drop.name],
            ["Estimated fare", f"{ride.estimate.fare} ({ride.estimate.distance_km} km)"],
            ["Payment", ride.payment_method.method_name],
        ])
        choice = self._io.read_choice("  Accept (a) or reject (r)? ", ["a", "r"])
        if choice == "a":
            self._session.rides.accept_offer(self._captain)
            self._io.print_success(f"Accepted {ride.ride_id}. Head to {ride.pickup} — "
                                   f"expected at {ride.expected_arrival_at:%I:%M %p}.")
            return
        next_captain = self._session.rides.reject_offer(self._captain)
        if next_captain is None:
            self._io.print_info(f"Rejected. Nobody else was eligible — {ride.ride_id} is {ride.status} "
                                f"({ride.cancellation_reason}); the customer was not charged.")
        else:
            self._io.print_info(f"Rejected. The offer moved to {next_captain.name} "
                                f"({next_captain.location.distance_to(ride.pickup):.1f} km away).")

    # ----- 4 -----
    def _arrive(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        self._session.rides.captain_arrives(ride)
        self._io.print_success(f"Arrived at {ride.pickup} at {self._session.clock}. Ask {ride.customer.name} for the OTP.")

    # ----- 5 -----
    def _start(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        otp = self._io.read_text(f"  OTP told by {ride.customer.name}: ")
        self._session.rides.start_ride(ride, otp)
        self._io.print_success(f"OTP correct — {ride.ride_id} started. Heading to {ride.drop}.")

    # ----- 6 -----
    def _complete(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        self._session.finish_trip(ride)

    # ----- 7 -----
    def _cancel(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        self._io.print_info("1. Cancel this ride (the customer is re-matched and NOT charged)")
        self._io.print_info("2. Customer no-show (only after 5 minutes of waiting at the pickup)")
        choice = self._io.read_choice("  Choose: ", ["1", "2"])
        if not self._io.read_yes_no(f"  Confirm for {ride.ride_id}? [y/n]: "):
            return
        if choice == "2":
            fee = self._session.rides.cancel_no_show(ride)
            self._io.print_success(f"{ride.ride_id} cancelled as NO_SHOW. Fee {fee}; you earn it minus 20% commission.")
            return
        try:
            new_captain = self._session.rides.cancel_by_captain(ride)
        except NoCaptainAvailableError as error:
            self._io.print_success(f"You cancelled {ride.ride_id} (cancellations today: {self._captain.cancellation_count}).")
            self._io.print_info(f"No other captain could take it ({error.reason}) — the ride is {ride.status} "
                                f"and {ride.customer.name} was not charged.")
            return
        self._io.print_success(f"You cancelled {ride.ride_id} (cancellations today: {self._captain.cancellation_count}).")
        self._io.print_info(f"The ride is now {ride.status} — {self._session.describe_waiting(ride)}; "
                            f"next captain: {new_captain.name}.")

    # ----- 8 -----
    def _earnings(self) -> None:
        today = self._session.clock.now.date()
        entry = self._session.app.earnings_service.earnings_for(self._captain, today)
        self._io.print_table(["Today", "Amount"], [
            ["Rides", str(entry.rides)],
            ["Gross fare", str(entry.gross_fare)],
            ["Commission (20%)", str(entry.commission)],
            ["Net earnings", str(entry.net_earnings)],
            ["Cash collected", str(entry.cash_collected)],
            ["Dues added today", str(entry.dues_added)],
            ["Dues owed now", str(self._captain.dues_owed)],
        ], "lr")

    # ----- 9 -----
    def _settle(self) -> None:
        settled = self._session.app.captain_service.settle_dues(self._captain)
        self._io.print_success(f"Settled {settled}. Dues now {self._captain.dues_owed}; "
                               f"cash rides {'allowed' if self._captain.accepts_cash_rides else 'blocked'}.")

    # ----- 10 -----
    def _rate(self) -> None:
        ride = self._session.rides.last_completed_ride_for_captain(self._captain)
        if ride is None:
            self._io.print_info("No completed ride to rate yet.")
            return
        self._io.print_info(f"Last completed ride: {ride} with {ride.customer.name}")
        score = self._io.read_int("  Stars (1–5): ", 1, 5)
        self._session.app.rating_service.rate_customer(ride, score)
        self._io.print_success(f"Rated {ride.customer.name} {score}★.")

    # ----- 11 -----
    def _switch(self) -> None:
        self._captain = self._pick_captain()
