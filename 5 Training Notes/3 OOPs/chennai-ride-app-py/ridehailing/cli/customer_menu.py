"""CustomerMenu — the customer's phone app."""

from __future__ import annotations

from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.cli.menu import Menu
from ridehailing.model.ride.ride import Ride
from ridehailing.model.ride.ride_status import RideStatus
from ridehailing.model.user.customer import Customer
from ridehailing.model.vehicle.vehicle_type import VehicleType


class CustomerMenu(Menu):
    """Inheritance: CustomerMenu IS-A Menu. It fills in options() and handle(); run() is inherited."""

    def __init__(self, io: ConsoleIO, session: CliSession) -> None:
        super().__init__("Customer app", io)
        self._session = session
        self._customer: Customer | None = None

    # ----- Menu hooks -----
    def on_enter(self) -> bool:
        self._customer = self._pick_customer()
        return True

    def status_line(self) -> str:
        return self._session.status_line()

    def heading(self) -> str:
        return f"CUSTOMER APP — {self._customer.name} at {self._customer.location}"

    def options(self) -> list[tuple[str, str]]:
        return [("1", "View profile"), ("2", "Top up wallet"), ("3", "See fare estimates"),
                ("4", "Book a ride"), ("5", "Track active ride"), ("6", "Cancel ride"),
                ("7", "Change destination"), ("8", "End trip early"), ("9", "Pay pending ride"),
                ("10", "Rate last completed ride"), ("11", "Ride history"), ("12", "Switch customer"),
                ("0", "Back")]

    def handle(self, choice: str) -> bool:
        actions = {"1": self._profile, "2": self._top_up, "3": self._estimates, "4": self._book,
                   "5": self._track, "6": self._cancel, "7": self._change_destination,
                   "8": self._end_early, "9": self._pay_pending, "10": self._rate,
                   "11": self._history, "12": self._switch}
        if choice == "0":
            return False
        actions[choice]()
        return True

    # ----- helpers -----
    def _pick_customer(self) -> Customer:
        customers = self._session.customers
        rows = []
        for number, customer in enumerate(customers, start=1):
            ride = customer.active_ride or customer.unpaid_ride
            rows.append([str(number), customer.name, customer.location.name, str(customer.wallet_balance),
                         f"{ride.ride_id} ({ride.status})" if ride else "—"])
        self._io.print_header("Choose a customer")
        self._io.print_table(["#", "Customer", "Location", "Wallet", "Active ride"], rows, "rllrl")
        number = self._io.read_int("Customer number: ", 1, len(customers))
        return customers[number - 1]

    def _current_ride(self) -> Ride | None:
        ride = self._customer.active_ride or self._customer.unpaid_ride
        if ride is None:
            self._io.print_info(f"{self._customer.name} has no active ride.")
        return ride

    # ----- 1 -----
    def _profile(self) -> None:
        customer = self._customer
        unpaid = customer.unpaid_ride
        rows = [
            ["Customer", f"{customer.name} ({customer.user_id}, {customer.phone})"],
            ["Location", customer.location.name],
            ["Wallet", str(customer.wallet_balance)],
            ["Preferred payment", str(customer.preferred_payment)],
            ["Rating", f"{customer.average_rating} from {customer.rating_count} rating(s)"],
            ["Pending cancellation fee", str(customer.pending_fee_total)],
            ["Payment pending", f"{unpaid.ride_id} — {unpaid.receipt.total}" if unpaid else "No"],
            ["Active ride", str(customer.active_ride) if customer.active_ride else "None"],
        ]
        self._io.print_table(["Field", "Value"], rows)

    # ----- 2 -----
    def _top_up(self) -> None:
        amount = self._io.read_money("Top-up amount (₹, max 10,000 per transaction): ")
        balance = self._session.app.customer_service.top_up(self._customer, amount)
        self._io.print_success(f"Added {amount}. New wallet balance: {balance}.")

    # ----- 3 -----
    def _estimates(self) -> None:
        current = self._customer.location
        pickup = self._session.read_place(f"Pickup [Enter = current location, {current}]: ", default=current)
        drop = self._session.read_place("Drop: ", show_list=False)
        estimates = self._session.rides.fare_estimates(self._customer, drop, pickup=pickup)
        self._print_estimates(estimates, f"{pickup} → {drop}")

    def _print_estimates(self, estimates, route: str) -> None:
        self._io.print_info(f"Fare estimates {route} at {self._session.clock}:")
        rows = []
        for estimate in estimates:
            eta = f"{estimate.nearest_captain_eta} min" if estimate.is_available else "—"
            rows.append([estimate.vehicle_type.display_name, str(estimate.fare), f"{estimate.surge_multiplier}x",
                         f"{estimate.distance_km} km", f"{estimate.trip_minutes} min", eta,
                         "✔ available" if estimate.is_available else "✘ no captains nearby"])
        self._io.print_table(["Vehicle", "Fare", "Surge", "Distance", "Trip", "Nearest captain", "Availability"],
                             rows, "lrrrrrl")

    # ----- 4 -----
    def _book(self) -> None:
        customer = self._customer
        customer.ensure_can_book()              # fail fast: active ride / payment pending
        current = customer.location
        pickup = self._session.read_place(f"Pickup [Enter = current location, {current}]: ", default=current)
        if pickup != current:
            self._session.app.customer_service.relocate(customer, pickup.name)
            self._io.print_success(f"{customer.name} made their own way to {pickup}.")
        drop = self._session.read_place("Drop: ", show_list=False)
        vehicle_types = list(VehicleType)
        for number, vehicle_type in enumerate(vehicle_types, start=1):
            self._io.print_info(f"{number}. {vehicle_type.display_name} (up to {vehicle_type.seats} passenger(s))")
        vehicle_type = vehicle_types[self._io.read_int("  Vehicle: ", 1, len(vehicle_types)) - 1]
        passengers = self._io.read_int("  Passengers [Enter = 1]: ", 1, 10, default=1)
        payment = self._session.read_payment(customer, allow_default=True)
        coupon = self._io.read_text("  Coupon code [Enter = none]: ", allow_empty=True) or None

        estimates = self._session.rides.fare_estimates(customer, drop)
        chosen = [estimate for estimate in estimates if estimate.vehicle_type == vehicle_type][0]
        self._print_estimates([chosen], f"{pickup} → {drop}")
        if not chosen.is_available:
            self._io.print_error("No captain is nearby right now — the booking will probably find nobody.")
        if not self._io.read_yes_no(f"  Confirm {vehicle_type.display_name} for about {chosen.fare}? [y/n]: "):
            self._io.print_info("Booking not confirmed.")
            return

        ride = self._session.rides.book_ride(customer, drop, vehicle_type, passengers=passengers,
                                             payment_method=payment, coupon_code=coupon)
        self._io.print_success(f"Booked {ride.ride_id}: {ride.pickup} → {ride.drop}. "
                               f"Your OTP is {ride.otp_for(customer)} — tell it to the captain.")
        self._io.print_info(f"Status: {ride.status} — {self._session.describe_waiting(ride)}.")
        if self._session.auto_captains and ride.status == RideStatus.CAPTAIN_ASSIGNED:
            self._session.rides.captain_arrives(ride)
            self._session.rides.start_ride(ride, ride.otp_for(customer))
            self._io.print_success(f"Auto captain {ride.captain.name} arrived and started the ride with the "
                                   f"correct OTP. Use 'Track active ride' to ride on to {ride.drop}.")

    # ----- 5 -----
    def _track(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        rows = [["Ride", str(ride)], ["Status", f"{ride.status} — {self._session.describe_waiting(ride)}"],
                ["OTP", ride.otp_for(self._customer)],
                ["Estimated fare", str(ride.estimate.fare)]]
        if ride.captain is not None:
            captain = ride.captain
            rows.append(["Captain", f"{captain.name}, {captain.vehicle} — rating {captain.average_rating}"])
            rows.append(["Captain location", captain.location.name])
        if ride.status == RideStatus.CAPTAIN_ASSIGNED:
            rows.append(["Expected arrival", f"{ride.expected_arrival_at:%I:%M %p}"])
        if ride.status == RideStatus.IN_PROGRESS:
            rows.append(["Position now", self._session.rides.position_now(ride).name])
        if ride.receipt is not None:
            rows.append(["Amount due", str(ride.receipt.total)])
        self._io.print_table(["Field", "Value"], rows)
        print(ride.timeline_text())
        if self._session.auto_captains and ride.status == RideStatus.IN_PROGRESS:
            if self._io.read_yes_no(f"  Ride on to {ride.drop} now? [y/n]: "):
                self._session.finish_trip(ride)

    # ----- 6 -----
    def _cancel(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        if ride.status.is_cancellable():
            fee = self._session.rides.cancellation_fee_if_cancelled_now(ride)
            if fee.is_zero():
                self._io.print_info("Cancelling now is FREE.")
            else:
                self._io.print_error(f"The 2-minute grace period is over: a {fee} fee will be added to your next ride.")
            if not self._io.read_yes_no(f"  Cancel {ride.ride_id}? [y/n]: "):
                return
        fee = self._session.rides.cancel_by_customer(ride)       # raises for IN_PROGRESS etc.
        self._io.print_success(f"{ride.ride_id} cancelled. Fee: {fee}.")

    # ----- 7 -----
    def _change_destination(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        position = self._session.rides.position_now(ride)       # raises unless IN_PROGRESS
        self._io.print_info(f"You are now at: {position.name}")
        new_drop = self._session.read_place("New destination: ")
        self._session.rides.change_destination(ride, position, new_drop)
        self._io.print_success(f"Destination changed to {new_drop}. Route so far: "
                               f"{' → '.join(point.name for point in ride.route)} → {new_drop}")

    # ----- 8 -----
    def _end_early(self) -> None:
        ride = self._current_ride()
        if ride is None:
            return
        position = self._session.rides.position_now(ride)       # raises unless IN_PROGRESS
        stop = self._session.read_place(f"Stop where? [Enter = right here, {position.name}]: ", default=position)
        self._session.finish_trip(ride, stop=stop)

    # ----- 9 -----
    def _pay_pending(self) -> None:
        ride = self._customer.unpaid_ride
        if ride is None:
            self._io.print_info(f"{self._customer.name} has no payment pending.")
            return
        self._io.print_info(f"{ride.ride_id} is {ride.status}: {ride.receipt.total} due.")
        payment = self._session.read_payment(self._customer, allow_default=False)
        self._session.rides.pay_for_ride(ride, payment)
        self._io.print_success(f"Paid {ride.receipt.total} via {payment.method_name}. {ride.ride_id} is {ride.status}.")

    # ----- 10 -----
    def _rate(self) -> None:
        ride = self._customer.last_completed_ride
        if ride is None:
            self._io.print_info("No completed ride to rate yet.")
            return
        self._io.print_info(f"Last completed ride: {ride} with captain {ride.captain.name}")
        score = self._io.read_int("  Stars (1–5): ", 1, 5)
        self._session.app.rating_service.rate_captain(ride, score)
        self._io.print_success(f"Rated {ride.captain.name} {score}★. Their average is now {ride.captain.average_rating}.")

    # ----- 11 -----
    def _history(self) -> None:
        rows = []
        for ride in self._customer.ride_history:
            if ride.receipt is not None:
                amount = str(ride.receipt.total)
            elif not ride.cancellation_fee.is_zero():
                amount = f"fee {ride.cancellation_fee}"
            else:
                amount = "—"
            route = " → ".join(point.name for point in ride.route) if ride.trip_has_ended \
                else f"{ride.pickup} → {ride.request.drop}"
            reason = f" ({ride.cancellation_reason})" if ride.cancellation_reason else ""
            rows.append([f"{ride.request.requested_at:%I:%M %p}", ride.ride_id, ride.vehicle_type.display_name,
                         route, ride.status.name + reason, amount])
        self._io.print_table(["Booked", "Ride", "Vehicle", "Route", "Status", "Amount"], rows, "lllllr")

    # ----- 12 -----
    def _switch(self) -> None:
        self._customer = self._pick_customer()
