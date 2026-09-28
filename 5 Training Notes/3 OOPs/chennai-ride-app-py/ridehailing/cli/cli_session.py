"""CliSession — the live world behind the interactive mode, shared by every menu.

It builds the SAME seeded world as the scripted demo (same captains, customers, homes, seeds),
with the clock at 6:00 AM, and exposes it to the menus. It contains no business rules: every
action is a call to an existing service, and every rule is enforced there.
"""

from __future__ import annotations

from ridehailing.app.chennai_ride_app import ChennaiRideApp
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.exceptions import InsufficientWalletBalanceError, PaymentFailedError
from ridehailing.model.common import chennai_places
from ridehailing.model.common.location import Location
from ridehailing.model.common.service_area import ServiceArea
from ridehailing.model.payment.cash_payment import CashPayment
from ridehailing.model.payment.payable import Payable
from ridehailing.model.payment.wallet_payment import WalletPayment
from ridehailing.model.ride.ride import Ride
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.customer import Customer
from ridehailing.time.simulated_clock import SimulatedClock


class CliSession:
    """Holds the world, the chosen captain mode, and small display helpers used by several menus."""

    def __init__(self, io: ConsoleIO) -> None:
        self.io = io
        self.app = ChennaiRideApp()                 # the same objects that power the scripted day
        self.app.ride_service.set_manual_offers(True)
        self._auto_captains = False
        self._service_area = ServiceArea.chennai_metro()

    # ----- short names for the services the menus call -----
    @property
    def clock(self) -> SimulatedClock:
        return self.app.clock

    @property
    def rides(self):
        return self.app.ride_service

    @property
    def customers(self) -> tuple[Customer, ...]:
        return self.app.customer_service.customers

    @property
    def captains(self) -> tuple[Captain, ...]:
        return self.app.captain_service.captains

    # ----- start of day -----
    def open_for_business(self) -> None:
        self.io.print_header(f"INTERACTIVE MODE · {self.clock.now:%A, %d %B %Y} · {self.clock}")
        for captain, error in self.app.bring_captains_online():
            if error:
                self.io.print_error(error)
        online = sum(1 for captain in self.captains if captain.status.is_working())
        self.io.print_success(f"{online} captains are online at their home areas; "
                              f"{len(self.customers)} customers are registered.")
        self.io.print_info("Captain mode: Manual — offers wait for the captain (change it in Settings).")
        self.io.print_info("At any prompt: b = back, q = quit.")

    # ----- captain mode (Settings) -----
    @property
    def auto_captains(self) -> bool:
        return self._auto_captains

    def set_auto_captains(self, enabled: bool) -> None:
        """Auto: captains accept at once, drive over and start with the right OTP (the CLI only
        chains the existing service calls). Manual: offers wait in the Captain app."""
        self._auto_captains = enabled
        self.rides.set_manual_offers(not enabled)

    # ----- the status line shown above every menu -----
    def status_line(self) -> str:
        now = self.clock.now
        if SimulatedClock.is_night(now):
            traffic = "Night"
        elif SimulatedClock.is_peak_hour(now):
            traffic = "Peak hour"
        else:
            traffic = "Off-peak"
        online = sum(1 for captain in self.captains if captain.status.is_working())
        mode = "Auto captain" if self._auto_captains else "Manual captain"
        return (f"[ {self.clock} | {traffic} | Active rides: {len(self.rides.active_rides())} | "
                f"Online captains: {online} | Mode: {mode} ]")

    # ----- places -----
    def place_choices(self) -> list[tuple[Location, str]]:
        """Every known place: inside the service area first, outside ones last and marked."""
        inside, outside = [], []
        for name in chennai_places.all_place_names():
            location = Location.from_place_name(name)
            if self._service_area.contains(location):
                inside.append((location, ""))
            else:
                outside.append((location, "(outside service area)"))
        return inside + outside

    def read_place(self, prompt: str, default: Location | None = None, show_list: bool = True) -> Location:
        return self.io.read_place(prompt, self.place_choices(), default=default, show_list=show_list)

    # ----- payment methods -----
    def read_payment(self, customer: Customer, allow_default: bool) -> Payable | None:
        """Ask for a payment method. None (only when allowed) means "my preferred method"."""
        upi = self.app.upi_accounts[customer]
        options = []
        if allow_default:
            preferred = customer.preferred_payment
            options.append(("1", f"Preferred ({preferred.method_name if preferred else 'Wallet'})", None))
        options.append((str(len(options) + 1), f"Wallet (balance {customer.wallet_balance})", WalletPayment(customer)))
        options.append((str(len(options) + 1), f"UPI ({upi.upi_id})", upi))
        options.append((str(len(options) + 1), "Cash", CashPayment()))
        for key, label, _ in options:
            self.io.print_info(f"{key}. {label}")
        choice = self.io.read_choice("  Payment method: ", [key for key, _, _ in options])
        for key, _, payable in options:
            if key == choice:
                return payable
        return None

    # ----- shared ride output -----
    def finish_trip(self, ride: Ride, stop: Location | None = None) -> None:
        """Drive to the drop (or stop early), then show the receipt and the payment result."""
        try:
            if stop is None:
                self.rides.complete_trip(ride)
            else:
                self.rides.end_trip_early(ride, stop)
        except (PaymentFailedError, InsufficientWalletBalanceError) as error:
            if ride.receipt is not None:
                print(ride.receipt)
            self.io.print_error(f"{type(error).__name__}: {error}")
            self.io.print_info(f"{ride.ride_id} is {ride.status} — pay it from the Customer app (option 9).")
            return
        print(ride.receipt)
        self.io.print_success(f"{ride.ride_id} {ride.status}: paid {ride.receipt.total} via {ride.paid_with}. "
                              f"{ride.customer.name} and {ride.captain.name} are at {ride.drop}.")

    def offer_holder(self, ride: Ride) -> Captain | None:
        for captain in self.app.matching_service.offered_captains():
            if captain.pending_offer is ride:
                return captain
        return None

    def describe_waiting(self, ride: Ride, hint: bool = True) -> str:
        """One line saying who the ride is waiting for."""
        if ride.status == RideStatus.REQUESTED:
            holder = self.offer_holder(ride)
            if holder is not None:
                return f"offer waiting for captain {holder.name}" + (" (switch to the Captain app)" if hint else "")
            return "searching for a captain"
        if ride.captain is not None:
            return f"captain {ride.captain.name}"
        return "—"
