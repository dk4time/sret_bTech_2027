"""ChennaiRideApp — one full simulated day of the ride-hailing app in Chennai.

Every scene runs on the SAME objects, so state carries across the whole day: where a captain
drops a customer is where their next ride starts, money owed at 8:40 AM is still owed at 8 PM,
and the clock never goes backwards.
"""

from __future__ import annotations

import random
from datetime import datetime

from ridehailing.exceptions import RideHailingError
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.common.service_area import ServiceArea
from ridehailing.model.payment.cash_payment import CashPayment
from ridehailing.model.payment.upi_payment import UpiPayment
from ridehailing.model.payment.wallet_payment import WalletPayment
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.customer import Customer
from ridehailing.model.vehicle.auto import Auto
from ridehailing.model.vehicle.bike import Bike
from ridehailing.model.vehicle.cab_economy import CabEconomy
from ridehailing.model.vehicle.cab_premium import CabPremium
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.notification.notifier import Notifier
from ridehailing.notification.push_notifier import PushNotifier
from ridehailing.notification.sms_notifier import SmsNotifier
from ridehailing.service.captain_service import CaptainService
from ridehailing.service.customer_service import CustomerService
from ridehailing.service.earnings_service import EarningsService
from ridehailing.service.fare_service import FareService
from ridehailing.service.matching_service import MatchingService
from ridehailing.service.payment_service import PaymentService
from ridehailing.service.rating_service import RatingService
from ridehailing.service.ride_service import RideService
from ridehailing.time.simulated_clock import SimulatedClock

DAY = datetime(2026, 9, 28)   # a Monday in Chennai
SEED = 2026                    # fixed seed: OTPs and UPI outcomes are identical on every run


def place(name: str) -> Location:
    return Location.from_place_name(name)


class ChennaiRideApp:
    """Builds the city (services, 12 captains, 5 customers) and plays the day scene by scene."""

    def __init__(self) -> None:
        self.clock = SimulatedClock(DAY.replace(hour=6, minute=0))
        self.rng = random.Random(SEED)
        self.sms = SmsNotifier(echo=False)     # SMS are logged, not printed (keeps the story short)
        self.push = PushNotifier(echo=True)
        notifiers: list[Notifier] = [self.sms, self.push]

        # Composition: the app is built from its services.
        self.captain_service = CaptainService(self.clock)
        self.customer_service = CustomerService(self.clock)
        self.fare_service = FareService()
        self.matching_service = MatchingService(self.captain_service, self.fare_service)
        self.earnings_service = EarningsService()
        self.payment_service = PaymentService(self.earnings_service)
        self.rating_service = RatingService()
        self.ride_service = RideService(self.clock, ServiceArea.chennai_metro(), self.fare_service,
                                        self.matching_service, self.payment_service, notifiers,
                                        otp_rng=self.rng)
        self._seed_captains()
        self._seed_customers()

    # ================================================================== seed data
    def _seed_captains(self) -> None:
        register = self.captain_service.register_captain
        self.murugan = register("Murugan", "+91 98401 23456", "Chennai Central",
                                Auto("TN-09-AB-4521", "Bajaj RE Auto"), opening_dues=Money("470"))
        self.senthil = register("Senthil", "+91 98402 34567", "Egmore",
                                CabEconomy("TN-22-CK-7813", "Maruti Dzire"))
        self.arun = register("Arun", "+91 98403 45678", "Velachery",
                             Bike("TN-07-BX-1190", "Honda Activa"))
        self.ramesh = register("Ramesh", "+91 98404 56789", "Guindy",
                               Auto("TN-10-AJ-3345", "TVS King"), past_ratings=[4, 3, 4, 4])
        self.balaji = register("Balaji", "+91 98405 67890", "Royapettah",
                               CabEconomy("TN-02-BM-6620", "Hyundai Aura"))
        self.vignesh = register("Vignesh", "+91 98406 78901", "Chennai Airport",
                                CabPremium("TN-12-AZ-9001", "Toyota Innova Crysta"))
        self.suresh = register("Suresh", "+91 98407 89012", "Adyar",
                               Bike("TN-06-CD-2210", "TVS Jupiter"))
        self.ganesh = register("Ganesh", "+91 98408 90123", "Mylapore",
                               Bike("TN-05-EF-7788", "Honda Activa"))
        self.manikandan = register("Manikandan", "+91 98409 01234", "T. Nagar",
                                   Auto("TN-04-GH-5561", "Bajaj RE Auto"))
        self.rajesh = register("Rajesh", "+91 98410 12345", "Anna Nagar",
                               CabEconomy("TN-11-JK-4402", "Hyundai Aura"))
        self.saravanan = register("Saravanan", "+91 98411 23456", "Koyambedu",
                                  Bike("TN-14-LM-8830", "TVS Jupiter"))
        self.kumaravel = register("Kumaravel", "+91 98412 34567", "Porur",
                                  Auto("TN-13-NP-1276", "TVS King"))   # KYC still pending
        for captain in self.captain_service.captains:
            if captain is not self.kumaravel:
                self.captain_service.verify_kyc(captain)

    def _seed_customers(self) -> None:
        register = self.customer_service.register_customer
        self.priya = register("Priya", "+91 94440 11223", "Velachery", Money("800"))
        self.karthik = register("Karthik", "+91 94441 22334", "Chennai Central", Money("600"))
        self.selvi = register("Selvi", "+91 94442 33445", "Egmore", Money("1500"))
        self.lakshmi = register("Lakshmi", "+91 94443 44556", "Chromepet", Money("2000"))
        self.divya = register("Divya", "+91 94444 55667", "Anna Nagar", Money("200"))
        # Each UPI account gets its own seeded Random, so one account's outcomes never shift another's.
        self.priya.choose_preferred_payment(UpiPayment("priya@okicici", random.Random(SEED + 3), failure_rate=0.02))
        self.karthik.choose_preferred_payment(UpiPayment("karthik@okhdfcbank", random.Random(SEED + 4), failure_rate=0.02))
        self.selvi.choose_preferred_payment(WalletPayment(self.selvi))
        self.lakshmi.choose_preferred_payment(WalletPayment(self.lakshmi))
        self.divya.choose_preferred_payment(WalletPayment(self.divya))
        # Divya's bank is having an outage tonight: every UPI attempt on this account fails.
        self.divya_upi = UpiPayment("divya@oksbi", random.Random(SEED + 5), failure_rate=1.0)
        # One UPI account per customer (used by the interactive mode; the scripted day never touches
        # the two new accounts, so its output is unchanged).
        self.upi_accounts: dict[Customer, UpiPayment] = {
            self.priya: self.priya.preferred_payment,
            self.karthik: self.karthik.preferred_payment,
            self.selvi: UpiPayment("selvi@okaxis", random.Random(SEED + 6), failure_rate=0.02),
            self.lakshmi: UpiPayment("lakshmi@okicici", random.Random(SEED + 7), failure_rate=0.02),
            self.divya: self.divya_upi,
        }

    def bring_captains_online(self) -> list[tuple[Captain, str | None]]:
        """Start of day without printing: every captain tries to go online.
        Returns (captain, error message or None) so the caller decides how to show it."""
        results = []
        for captain in self.captain_service.captains:
            try:
                self.captain_service.go_online(captain)
                results.append((captain, None))
            except RideHailingError as error:
                results.append((captain, str(error)))
        return results

    # ================================================================== console helpers
    def say(self, message: str) -> None:
        print(f"[{self.clock}] {message}")

    def fail(self, error: RideHailingError) -> None:
        print(f"[{self.clock}] ✘ {type(error).__name__}: {error}")

    def scene(self, number: int, hour: int, minute: int, title: str) -> None:
        target = DAY.replace(hour=hour, minute=minute)
        if self.clock.now < target:
            self.clock.advance_to(target)
        print()
        print("=" * 100)
        print(f" SCENE {number} · {self.clock} · {title}")
        print("=" * 100)

    def board(self, heading: str) -> None:
        print()
        print(f"  CAPTAIN POSITION BOARD — {heading} ({self.clock})")
        print(f"  {'Captain':<11}{'Vehicle':<23}{'Plate':<15}{'Location':<22}{'Status':<13}"
              f"{'Rides':>5}{'Earnings':>12}{'Dues':>10}")
        print("  " + "-" * 111)
        for captain in self.captain_service.captains:
            day = self.earnings_service.earnings_for(captain, DAY.date())
            vehicle = f"{captain.vehicle.model_name} ({captain.vehicle_type.display_name})"
            print(f"  {captain.name:<11}{vehicle[:22]:<23}{captain.vehicle.registration_number:<15}"
                  f"{captain.location.name[:21]:<22}{str(captain.status):<13}{day.rides:>5}"
                  f"{str(day.net_earnings):>12}{str(captain.dues_owed):>10}")
        print()

    def show_estimates(self, customer: Customer, drop: Location) -> None:
        print(f"  Fare estimates for {customer.name}: {customer.location} → {drop}")
        print(f"  {'Vehicle':<12} {'Fare':>10}  {'Trip':>7}       {'Surge':<11}Nearest captain")
        for estimate in self.ride_service.fare_estimates(customer, drop):
            print(f"  {estimate}")

    def run_simple_trip(self, ride: Ride, wait_minutes: int = 0) -> None:
        """Captain drives to pickup, (optionally) waits, the customer gives the OTP, trip completes."""
        self.ride_service.captain_arrives(ride)
        if wait_minutes:
            self.clock.advance(minutes=wait_minutes)
        self.ride_service.start_ride(ride, ride.otp_for(ride.customer))
        self.ride_service.complete_trip(ride)

    # ================================================================== the day
    def run(self) -> None:
        print("#" * 100)
        print(f"#  RAPIDO CHENNAI — A SIMULATED DAY · {DAY:%A, %d %B %Y}")
        print(f"#  {len(self.captain_service.captains)} captains · {len(self.customer_service.customers)} "
              f"customers · seed {SEED}")
        print("#" * 100)
        self.scene_01_captains_online()
        self.scene_02_priya_compares_and_books_bike()
        self.scene_03_central_to_egmore_cash_auto()
        canonical = self.scene_04a_egmore_to_tambaram_starts()
        self.scene_05_reject_then_accept()
        self.scene_06_second_booking_blocked()
        self.scene_04b_canonical_follow_up(canonical)
        self.scene_07_cancellations()
        self.scene_08_waiting_charge()
        self.scene_09_no_show()
        self.scene_10_otp()
        self.scene_11_captain_cancels_after_accepting()
        self.scene_12_change_destination()
        self.scene_13_end_trip_early()
        self.scene_14_payment_pending()
        self.scene_15_cash_dues_block()
        self.scene_16_no_premium_near_omr()
        self.scene_17_airport_night_premium()
        self.scene_18_outside_service_area()
        self.scene_20_ratings()
        self.scene_21_end_of_day()

    # ---------------------------------------------------------------- 06:00
    def scene_01_captains_online(self) -> None:
        self.scene(1, 6, 0, "Captains come online at their home areas")
        for captain in self.captain_service.captains:
            try:
                self.captain_service.go_online(captain)
                self.say(f"{captain.name} is online at {captain.location} — {captain.vehicle}")
            except RideHailingError as error:
                self.fail(error)
        self.board("start of day")

    # ---------------------------------------------------------------- 08:15
    def scene_02_priya_compares_and_books_bike(self) -> None:
        self.scene(2, 8, 15, "Priya compares all vehicle types, Velachery → Guindy (peak hour)")
        guindy = place("Guindy")
        self.show_estimates(self.priya, guindy)
        self.say("Priya picks the Bike and pays by UPI.")
        ride = self.ride_service.book_ride(self.priya, guindy, VehicleType.BIKE)
        self.run_simple_trip(ride)
        print(ride.receipt)
        self.say(f"Priya is now at {self.priya.location}; Arun is now at {self.arun.location}.")

    # ---------------------------------------------------------------- 08:40
    def scene_03_central_to_egmore_cash_auto(self) -> None:
        self.scene(3, 8, 40, "Karthik books an Auto, Chennai Central → Egmore (hotspot surge), pays cash")
        dues_before = self.murugan.dues_owed
        ride = self.ride_service.book_ride(self.karthik, place("Egmore"), VehicleType.AUTO,
                                           payment_method=CashPayment())
        self.say(f"Surge shown before confirming: {ride.estimate.surge_multiplier}x "
                 f"(Chennai Central is a peak-hour hotspot).")
        self.run_simple_trip(ride)
        print(ride.receipt)
        self.say(f"Murugan collected {ride.receipt.total} cash. Dues owed to the platform: "
                 f"{dues_before} → {self.murugan.dues_owed}.")

    # ---------------------------------------------------------------- 09:10
    def scene_04a_egmore_to_tambaram_starts(self) -> Ride:
        self.scene(4, 9, 10, "CANONICAL FLOW — Cab Economy, Egmore → Tambaram (part 1)")
        self.say(f"Senthil's location BEFORE the ride: {self.senthil.location}")
        ride = self.ride_service.book_ride(self.selvi, place("Tambaram"), VehicleType.CAB_ECONOMY)
        self.ride_service.captain_arrives(ride)
        self.ride_service.start_ride(ride, ride.otp_for(self.selvi))
        self.say(f"{ride.ride_id} is on its way down GST Road — about {ride.estimate.trip_minutes} min in "
                 f"peak traffic. The city keeps moving meanwhile…")
        return ride

    # ---------------------------------------------------------------- 10:00
    def scene_05_reject_then_accept(self) -> None:
        self.scene(5, 10, 0, "Nearest captain rejects, the next-nearest accepts")
        self.arun.plan_to_reject_next_offer()          # Arun is heading home for breakfast
        ride = self.ride_service.book_ride(self.priya, place("Ashok Nagar"), VehicleType.BIKE)
        self.say("Offer sequence:")
        for line in ride.offer_log:
            print(f"      {line}")
        self.say(f"Arun will never be offered {ride.ride_id} again. "
                 f"Arun's acceptance rate: {self.arun.acceptance_rate}%")
        self.run_simple_trip(ride)
        self.say(f"Priya reached {self.priya.location}. Fare {ride.receipt.total} via {ride.paid_with}.")

    # ---------------------------------------------------------------- 10:30
    def scene_06_second_booking_blocked(self) -> None:
        self.scene(6, 10, 30, "A customer with an active ride tries to book again")
        self.say("Selvi, still in Senthil's cab, tries to book an Auto for her return trip.")
        try:
            self.ride_service.book_ride(self.selvi, place("Egmore"), VehicleType.AUTO)
        except RideHailingError as error:
            self.fail(error)

    def scene_04b_canonical_follow_up(self, ride: Ride) -> None:
        print()
        print("-" * 100)
        print(f" SCENE 4 (continued) · {self.clock} · The Egmore → Tambaram ride finishes")
        print("-" * 100)
        self.ride_service.complete_trip(ride)
        print(ride.receipt)
        self.say(f"Senthil's location AFTER the ride: {self.senthil.location} (the actual drop point).")
        print(ride.timeline_text())

        self.say("Karthik, waiting at Egmore, requests a Cab Economy.")
        egmore_ride = self.ride_service.book_ride(self.karthik, place("Anna Nagar"), VehicleType.CAB_ECONOMY)
        reason = self.matching_service.ineligibility_reason(self.senthil, egmore_ride, self.clock.now)
        self.say(f"→ Assigned to {egmore_ride.captain.name} (near Egmore). Senthil was skipped: {reason}.")

        self.say("Lakshmi, at Chromepet, requests a Cab Economy.")
        chromepet_ride = self.ride_service.book_ride(self.lakshmi, place("Guindy"), VehicleType.CAB_ECONOMY)
        distance = self.senthil.location.distance_to(place("Chromepet"))
        self.say(f"→ Assigned to {chromepet_ride.captain.name}, who is now at Tambaram, {distance:.1f} km away.")

        # Both rides run at the same time; events are processed in time order.
        self.ride_service.captain_arrives(egmore_ride)
        self.ride_service.start_ride(egmore_ride, egmore_ride.otp_for(self.karthik))
        self.ride_service.captain_arrives(chromepet_ride)
        self.ride_service.start_ride(chromepet_ride, chromepet_ride.otp_for(self.lakshmi))
        self.rides_still_running = [egmore_ride, chromepet_ride]   # they finish during scene 7
        self.board("after the canonical flow")

    # ---------------------------------------------------------------- 11:00
    def scene_07_cancellations(self) -> None:
        self.scene(7, 11, 0, "Cancellations: inside the 2-minute grace period vs after it")
        ride = self.ride_service.book_ride(self.priya, place("Mylapore"), VehicleType.AUTO)
        self.clock.advance(minutes=1)
        fee = self.ride_service.cancel_by_customer(ride)
        self.say(f"Priya cancelled 1 min after assignment → fee {fee} (grace period).")

        self.divya_cancelled_ride = self.ride_service.book_ride(self.divya, place("Vadapalani"), VehicleType.BIKE)
        self.clock.advance(minutes=3)
        fee = self.ride_service.cancel_by_customer(self.divya_cancelled_ride)
        self.say(f"Divya cancelled 3 min after assignment → fee {fee}, carried to her NEXT ride. "
                 f"Pending: {self.divya.pending_fee_total}")

        self.say("Meanwhile, the two cab rides booked at 10:33 AM reach their drops:")
        for ride in self.rides_still_running:
            self.ride_service.complete_trip(ride)
            self.say(f"{ride.ride_id}: {ride.customer.name} and {ride.captain.name} are both at {ride.drop}.")
        self.board("late morning")

    # ---------------------------------------------------------------- 12:30
    def scene_08_waiting_charge(self) -> None:
        self.scene(8, 12, 30, "Captain waits 4 minutes (1 minute charged); previous fee appears")
        ride = self.ride_service.book_ride(self.divya, place("Vadapalani"), VehicleType.BIKE)
        self.ride_service.captain_arrives(ride)
        self.say("Divya is still coming down from her flat…")
        self.clock.advance(minutes=4)
        self.ride_service.start_ride(ride, ride.otp_for(self.divya))
        self.ride_service.complete_trip(ride)
        print(ride.receipt)
        self.say(f"Pending fees after this ride: {self.divya.pending_fee_total} (charged exactly once).")

    # ---------------------------------------------------------------- 13:00
    def scene_09_no_show(self) -> None:
        self.scene(9, 13, 0, "Customer no-show")
        ride = self.ride_service.book_ride(self.karthik, place("Kilpauk"), VehicleType.AUTO)
        self.ride_service.captain_arrives(ride)
        captain = ride.captain
        self.clock.advance(minutes=3)
        try:
            self.ride_service.cancel_no_show(ride)
        except RideHailingError as error:
            self.fail(error)
        self.clock.advance(minutes=2)
        fee = self.ride_service.cancel_no_show(ride)
        self.say(f"{captain.name} cancelled as NO_SHOW after 5 min. Fee {fee}; {captain.name} is free "
                 f"at the pickup ({captain.location}).")

    # ---------------------------------------------------------------- 14:00
    def scene_10_otp(self) -> None:
        self.scene(10, 14, 0, "OTP checks")
        ride = self.ride_service.book_ride(self.priya, place("Mylapore"), VehicleType.AUTO)
        self.ramesh_ride = ride
        self.ride_service.captain_arrives(ride)
        try:
            self.ride_service.start_ride(ride, "0000")
        except RideHailingError as error:
            self.fail(error)
        self.ride_service.start_ride(ride, ride.otp_for(self.priya))
        self.say(f"Second attempt correct — {ride.ride_id} is {ride.status}.")
        self.ride_service.complete_trip(ride)

        self.say("Separate ride: Lakshmi's captain enters three wrong OTPs.")
        ride = self.ride_service.book_ride(self.lakshmi, place("Saidapet"), VehicleType.BIKE)
        self.otp_failed_ride = ride
        self.ride_service.captain_arrives(ride)
        for wrong_otp in ("1111", "2222", "3333"):
            try:
                self.ride_service.start_ride(ride, wrong_otp)
            except RideHailingError as error:
                self.fail(error)
        self.say(f"{ride.ride_id} status: {ride.status} ({ride.cancellation_reason}), fee {ride.cancellation_fee}.")

    # ---------------------------------------------------------------- 15:30
    def scene_11_captain_cancels_after_accepting(self) -> None:
        self.scene(11, 15, 30, "Captain accepts, then cancels — ride is re-matched, customer not charged")
        ride = self.ride_service.book_ride(self.divya, place("Anna Nagar"), VehicleType.BIKE)
        first = ride.captain
        self.clock.advance(minutes=2)
        new_captain = self.ride_service.cancel_by_captain(ride)
        self.say(f"{first.name} cancelled (cancellations today: {first.cancellation_count}) and stays at "
                 f"{first.location}. Re-matched to {new_captain.name}.")
        for line in ride.offer_log:
            print(f"      {line}")
        self.run_simple_trip(ride)
        self.say(f"Divya paid {ride.receipt.total}; previous cancellation fee on this bill: "
                 f"{ride.receipt.previous_cancellation_fee}.")

    # ---------------------------------------------------------------- 17:45
    def scene_12_change_destination(self) -> None:
        self.scene(12, 17, 45, "Customer changes destination mid-ride")
        ride = self.ride_service.book_ride(self.priya, place("Adyar"), VehicleType.AUTO)
        self.ride_service.captain_arrives(ride)
        self.ride_service.start_ride(ride, ride.otp_for(self.priya))
        change_point = ride.pickup.point_towards(ride.drop, 0.5, "Mandaveli (en route)")
        self.ride_service.change_destination(ride, change_point, place("Thiruvanmiyur"))
        self.ride_service.complete_trip(ride)
        print(ride.receipt)
        self.say(f"Estimated {ride.estimate.fare} for Mylapore → Adyar; charged {ride.receipt.total} for the "
                 f"actual route. Priya and {ride.captain.name} are both at {self.priya.location}.")

    # ---------------------------------------------------------------- 18:30
    def scene_13_end_trip_early(self) -> None:
        self.scene(13, 18, 30, "Customer ends the trip early")
        ride = self.ride_service.book_ride(self.karthik, place("Tambaram"), VehicleType.CAB_ECONOMY)
        self.ride_service.captain_arrives(ride)
        self.ride_service.start_ride(ride, ride.otp_for(self.karthik))
        self.say("Karthik gets a call from his office and asks to be dropped at Ashok Nagar.")
        self.ride_service.end_trip_early(ride, place("Ashok Nagar"))
        print(ride.receipt)
        self.say(f"Booked to Tambaram (est. {ride.estimate.fare}) but paid {ride.receipt.total} for "
                 f"{ride.receipt.distance_km} km. Karthik: {self.karthik.location}; "
                 f"{ride.captain.name}: {ride.captain.location}.")

    # ---------------------------------------------------------------- 19:00
    def scene_14_payment_pending(self) -> None:
        self.scene(14, 19, 0, "Wallet fails, UPI fails → PAYMENT_PENDING → cash")
        self.say(f"Divya's wallet balance: {self.divya.wallet_balance}")
        ride = self.ride_service.book_ride(self.divya, place("Vadapalani"), VehicleType.AUTO)
        self.ride_service.captain_arrives(ride)
        self.ride_service.start_ride(ride, ride.otp_for(self.divya))
        try:
            self.ride_service.complete_trip(ride)
        except RideHailingError as error:
            self.fail(error)
        self.say(f"Wallet still {self.divya.wallet_balance} (never partially deducted). {ride.ride_id} is {ride.status}.")
        try:
            self.ride_service.pay_for_ride(ride, self.divya_upi)
        except RideHailingError as error:
            self.fail(error)
        try:
            self.ride_service.book_ride(self.divya, place("T. Nagar"), VehicleType.BIKE)
        except RideHailingError as error:
            self.fail(error)
        self.ride_service.pay_for_ride(ride, CashPayment())
        self.say(f"{ride.ride_id} is now {ride.status}. Divya taps 'Pay' once more by mistake…")
        try:
            self.ride_service.pay_for_ride(ride, CashPayment())
        except RideHailingError as error:
            self.fail(error)
        self.say("Divya tries to book again now that nothing is pending…")
        next_ride = self.ride_service.book_ride(self.divya, place("T. Nagar"), VehicleType.BIKE,
                                                payment_method=CashPayment())
        self.say(f"Booking succeeded: {next_ride}.")
        self.run_simple_trip(next_ride)

    # ---------------------------------------------------------------- 20:00
    def scene_15_cash_dues_block(self) -> None:
        self.scene(15, 20, 0, "Cash dues cross ₹500 → blocked from cash rides → settle → unblocked")
        self.customer_service.relocate(self.selvi, "Egmore")
        self.say(f"Selvi is back home at {self.selvi.location}. Murugan's dues: {self.murugan.dues_owed}.")
        ride = self.ride_service.book_ride(self.selvi, place("Washermanpet"), VehicleType.AUTO,
                                           payment_method=CashPayment())
        self.run_simple_trip(ride)
        self.say(f"Murugan's dues are now {self.murugan.dues_owed} — "
                 f"{'BLOCKED from cash rides' if not self.murugan.accepts_cash_rides else 'still allowed cash'}.")
        try:
            self.ride_service.book_ride(self.selvi, place("Chennai Central"), VehicleType.AUTO,
                                        payment_method=CashPayment())
        except RideHailingError as error:
            self.fail(error)
        reason = self.matching_service.ineligibility_reason(self.murugan, self.selvi.ride_history[-1], self.clock.now)
        self.say(f"Murugan was standing right there but was skipped: {reason}.")
        settled = self.captain_service.settle_dues(self.murugan)
        self.say(f"Murugan settles {settled} via the captain app. Dues now {self.murugan.dues_owed} — "
                 f"cash rides {'allowed' if self.murugan.accepts_cash_rides else 'blocked'}.")
        ride = self.ride_service.book_ride(self.selvi, place("Chennai Central"), VehicleType.AUTO,
                                           payment_method=CashPayment())
        self.say(f"Cash ride {ride.ride_id} matched to {ride.captain.name}.")
        self.run_simple_trip(ride)

    # ---------------------------------------------------------------- 21:00
    def scene_16_no_premium_near_omr(self) -> None:
        self.scene(16, 21, 0, "No Cab Premium near Sholinganallur (OMR)")
        self.customer_service.relocate(self.priya, "Sholinganallur")
        self.say("Priya finished dinner with friends at Sholinganallur.")
        try:
            self.ride_service.book_ride(self.priya, place("Velachery"), VehicleType.CAB_PREMIUM)
        except RideHailingError as error:
            self.fail(error)
        reason = self.matching_service.ineligibility_reason(self.vignesh, self.priya.ride_history[-1], self.clock.now)
        self.say(f"The only Cab Premium captain, Vignesh, is at {self.vignesh.location}: {reason}.")

    # ---------------------------------------------------------------- 23:30
    def scene_17_airport_night_premium(self) -> None:
        self.scene(17, 23, 30, "Night Cab Premium, Chennai Airport → Anna Nagar, FIRSTRIDE coupon, wallet")
        self.customer_service.relocate(self.lakshmi, "Chennai Airport")
        balance = self.customer_service.top_up(self.lakshmi, Money("1000"))
        self.say(f"Lakshmi, flying back late, tops up her wallet with ₹1,000 → {balance}.")
        try:
            self.customer_service.top_up(self.lakshmi, Money("15000"))
        except RideHailingError as error:
            self.fail(error)
        ride = self.ride_service.book_ride(self.lakshmi, place("Anna Nagar"), VehicleType.CAB_PREMIUM,
                                           coupon_code="FIRSTRIDE")
        self.ride_service.captain_arrives(ride)
        self.ride_service.start_ride(ride, ride.otp_for(self.lakshmi))
        # Scene 19: a captain cannot go offline during a ride
        print(f"  --- SCENE 19 · a captain tries to go offline mid-ride ---")
        try:
            self.captain_service.go_offline(self.vignesh)
        except RideHailingError as error:
            self.fail(error)
        self.ride_service.complete_trip(ride)
        print(ride.receipt)
        self.captain_service.go_offline(self.vignesh)
        self.say(f"Ride over — Vignesh goes offline at {self.vignesh.location}. Status: {self.vignesh.status}.")

    # ---------------------------------------------------------------- 23:5x
    def scene_18_outside_service_area(self) -> None:
        self.scene(18, 23, 30, "A booking outside the service area")
        try:
            self.ride_service.book_ride(self.lakshmi, place("Mahabalipuram"), VehicleType.CAB_ECONOMY)
        except RideHailingError as error:
            self.fail(error)

    # ---------------------------------------------------------------- ratings
    def scene_20_ratings(self) -> None:
        self.scene(20, 23, 30, "Ratings")
        scores = {self.priya: 5, self.karthik: 5, self.selvi: 4, self.lakshmi: 5, self.divya: 4}
        for ride in self.ride_service.rides:
            if ride.status.name != "COMPLETED":
                continue
            score = scores[ride.customer]
            if ride is self.ramesh_ride:
                score = 2    # Priya was unhappy: Ramesh took a long detour and was rude
            self.rating_service.rate_captain(ride, score)
            self.rating_service.rate_customer(ride, 5)
        self.say(f"Rated every completed ride ({sum(1 for r in self.ride_service.rides if r.status.name == 'COMPLETED')} rides).")
        try:
            self.rating_service.rate_captain(self.divya_cancelled_ride, 1)
        except RideHailingError as error:
            self.fail(error)
        try:
            self.rating_service.rate_captain(self.ramesh_ride, 1)
        except RideHailingError as error:
            self.fail(error)
        for captain in self.rating_service.flagged_captains(self.captain_service.captains):
            self.say(f"⚑ FLAGGED: {captain.name} — average {captain.average_rating} over "
                     f"{captain.rating_count} ratings (below {Captain.RATING_FLAG_THRESHOLD}).")

    # ---------------------------------------------------------------- end of day
    def scene_21_end_of_day(self) -> None:
        self.scene(21, 23, 59, "End of day")
        day = DAY.date()

        print("\n  CAPTAIN EARNINGS")
        print(f"  {'Captain':<11}{'Rides':>6}{'Gross fare':>13}{'Commission':>13}{'Net earned':>13}"
              f"{'Cash coll.':>13}{'Dues added':>13}{'Dues now':>11}{'Accept %':>10}{'Cancels':>9}")
        print("  " + "-" * 112)
        for captain in self.captain_service.captains:
            entry = self.earnings_service.earnings_for(captain, day)
            print(f"  {captain.name:<11}{entry.rides:>6}{str(entry.gross_fare):>13}{str(entry.commission):>13}"
                  f"{str(entry.net_earnings):>13}{str(entry.cash_collected):>13}{str(entry.dues_added):>13}"
                  f"{str(captain.dues_owed):>11}{str(captain.acceptance_rate):>10}{captain.cancellation_count:>9}")

        print("\n  RATINGS LEADERBOARD")
        print(f"  {'#':<4}{'Captain':<12}{'Average':>8}{'Ratings':>9}  Flag")
        for rank, captain in enumerate(self.rating_service.leaderboard(self.captain_service.captains), start=1):
            flag = "⚑ below 4.0" if captain.is_flagged else ""
            print(f"  {rank:<4}{captain.name:<12}{str(captain.average_rating):>8}{captain.rating_count:>9}  {flag}")

        print("\n  CUSTOMER RIDE HISTORY (chronological, cancelled rides included)")
        for customer in self.customer_service.customers:
            print(f"  {customer.name} — now at {customer.location}, wallet {customer.wallet_balance}")
            for ride in customer.ride_history:
                amount = str(ride.receipt.total) if ride.receipt else (
                    f"fee {ride.cancellation_fee}" if not ride.cancellation_fee.is_zero() else "—")
                route = " → ".join(point.name for point in ride.route) if ride.trip_has_ended else \
                    f"{ride.pickup} → {ride.request.drop}"
                reason = f" ({ride.cancellation_reason})" if ride.cancellation_reason else ""
                print(f"      {ride.request.requested_at:%I:%M %p}  {ride.ride_id}  "
                      f"{ride.vehicle_type.display_name:<12}{route:<56}{ride.status.name + reason:<32}{amount:>10}")

        self.board("final")
        self.print_reconciliation()

    def print_reconciliation(self) -> None:
        earnings = self.earnings_service
        collected = self.payment_service.total_collected
        right_side = earnings.total_captain_earnings + earnings.total_commission + earnings.total_gst
        print("  MONEY RECONCILIATION")
        print(f"  Total paid by customers (payment ledger) {str(collected):>14}")
        print(f"    = Captain earnings                     {str(earnings.total_captain_earnings):>14}")
        print(f"    + Platform commission (20%)            {str(earnings.total_commission):>14}")
        print(f"    + GST collected (5%)                   {str(earnings.total_gst):>14}")
        print(f"                                           {'-' * 14}")
        print(f"                                           {str(right_side):>14}")
        wallet_ok = self.wallets_balance()
        print(f"  Wallets: opening + top-ups − spent = balance for every customer: {'yes' if wallet_ok else 'NO'}")
        print(f"  Dues settled by captains today: {self.captain_service.total_dues_settled}")
        print(f"  Notifications sent: {self.push.sent_count} push, {self.sms.sent_count} SMS")
        if collected == right_side and earnings.books_balance() and wallet_ok:
            print("\n  Books Balanced ✔  (to the paisa)")
        else:
            print("\n  BOOKS DO NOT BALANCE ✘")

    def wallets_balance(self) -> bool:
        for customer in self.customer_service.customers:
            expected = customer.opening_wallet_balance + customer.total_topped_up - customer.total_wallet_spent
            if expected != customer.wallet_balance:
                return False
        wallet_paid = Money.total([record.amount for record in self.payment_service.ledger
                                   if record.succeeded and record.method == "Wallet"])
        spent = Money.total([customer.total_wallet_spent for customer in self.customer_service.customers])
        return wallet_paid == spent
