"""FareService — prices, trip times, surge, coupons and receipts."""

from __future__ import annotations

import math
from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import TYPE_CHECKING

from ridehailing.exceptions import InvalidBookingError
from ridehailing.model.common import chennai_places
from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money
from ridehailing.model.ride.fare_estimate import FareEstimate
from ridehailing.model.ride.fare_receipt import FareReceipt
from ridehailing.model.vehicle.auto import Auto
from ridehailing.model.vehicle.bike import Bike
from ridehailing.model.vehicle.cab_economy import CabEconomy
from ridehailing.model.vehicle.cab_premium import CabPremium
from ridehailing.model.vehicle.vehicle import Vehicle
from ridehailing.model.vehicle.vehicle_type import VehicleType
from ridehailing.time.simulated_clock import SimulatedClock

if TYPE_CHECKING:
    from ridehailing.model.ride.ride import Ride
    from ridehailing.model.user.customer import Customer


class FareService:
    """All fare maths lives here, so the rules are in one place."""

    NIGHT_CHARGE_RATE = Decimal("0.20")
    WAITING_CHARGE_PER_MINUTE = Money("1")
    MAX_SURGE = Decimal("2.0")
    HOTSPOT_RADIUS_KM = 1.5
    SLOW_ZONE_RADIUS_KM = 3.0
    SLOW_ZONE_SPEED_FACTOR = 0.75        # OMR / T. Nagar / Koyambedu crawl at peak hours
    # One simple flat coupon: code -> (discount, minimum fare needed to use it)
    COUPONS: dict[str, tuple[Money, Money]] = {"FIRSTRIDE": (Money("50"), Money("100"))}

    def __init__(self) -> None:
        # A "rate card" vehicle per type, used to price estimates BEFORE a captain is chosen.
        # Polymorphism: we only ever call Vehicle methods on these; each answers with its own rates.
        self._rate_card: dict[VehicleType, Vehicle] = {
            VehicleType.BIKE: Bike("TN-01-RC-0001", "Rate card bike"),
            VehicleType.AUTO: Auto("TN-01-RC-0002", "Rate card auto"),
            VehicleType.CAB_ECONOMY: CabEconomy("TN-01-RC-0003", "Rate card sedan"),
            VehicleType.CAB_PREMIUM: CabPremium("TN-01-RC-0004", "Rate card SUV"),
        }

    def rate_card_vehicle(self, vehicle_type: VehicleType) -> Vehicle:
        return self._rate_card[vehicle_type]

    # ----------------------------------------------------------------- distance & time
    @staticmethod
    def road_km(start: Location, end: Location) -> Decimal:
        """Road distance as a Decimal with 2 places (money is multiplied by it)."""
        return Decimal(str(start.road_distance_to(end))).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    def travel_minutes(self, vehicle: Vehicle, start: Location, end: Location, at: datetime) -> int:
        """Driving time in whole minutes, slower at peak hours and slower still in the slow zones."""
        is_peak = SimulatedClock.is_peak_hour(at)
        speed = float(vehicle.average_speed_kmph(is_peak))
        if is_peak and (self._in_slow_zone(start) or self._in_slow_zone(end)):
            speed = speed * FareService.SLOW_ZONE_SPEED_FACTOR
        minutes = math.ceil(start.road_distance_to(end) / speed * 60)
        return max(minutes, 1)

    def _in_slow_zone(self, location: Location) -> bool:
        for place in chennai_places.SLOW_TRAFFIC_ZONES:
            if location.is_near_place(place, FareService.SLOW_ZONE_RADIUS_KM):
                return True
        return False

    @staticmethod
    def is_hotspot(location: Location) -> bool:
        for place in chennai_places.HIGH_DEMAND_HOTSPOTS:
            if location.is_near_place(place, FareService.HOTSPOT_RADIUS_KM):
                return True
        return False

    # ----------------------------------------------------------------- surge
    def surge_multiplier(self, pickup: Location, open_requests: int, free_captains: int,
                         at: datetime) -> Decimal:
        """Demand vs supply near the pickup, plus a peak-hour bump (bigger at hotspots). Max 2.0x."""
        surge = Decimal("1.0")
        if free_captains > 0 and open_requests > free_captains:
            surge += Decimal("0.5") * Decimal(open_requests - free_captains) / Decimal(free_captains)
        if SimulatedClock.is_peak_hour(at):
            if FareService.is_hotspot(pickup):
                surge += Decimal("0.4")
            else:
                surge += Decimal("0.1")
        if surge > FareService.MAX_SURGE:
            surge = FareService.MAX_SURGE
        return surge.quantize(Decimal("0.1"), rounding=ROUND_HALF_UP)

    # ----------------------------------------------------------------- coupons
    def validate_coupon(self, customer: Customer, code: str | None) -> None:
        if code is None:
            return
        if code.upper() not in FareService.COUPONS:
            raise InvalidBookingError(f"Coupon '{code}' does not exist.")
        if customer.has_used_coupon(code):
            raise InvalidBookingError(f"{customer.name} has already used coupon {code.upper()}.")

    # ----------------------------------------------------------------- estimate & receipt
    def estimate(self, pickup: Location, drop: Location, vehicle_type: VehicleType,
                 surge: Decimal, at: datetime, nearest_captain_eta: int | None) -> FareEstimate:
        vehicle = self._rate_card[vehicle_type]
        distance = FareService.road_km(pickup, drop)
        minutes = self.travel_minutes(vehicle, pickup, drop, at)
        priced = self._price(vehicle=vehicle, distance_km=distance, trip_minutes=minutes,
                             waiting_minutes=0, surge=surge, is_night=SimulatedClock.is_night(at),
                             previous_fee=Money.zero(), coupon_code=None, ride_id="ESTIMATE",
                             route_text=f"{pickup} → {drop}", estimated_total=None, at=at)
        return FareEstimate(vehicle_type, distance, minutes, surge, priced.total, nearest_captain_eta)

    def build_receipt(self, ride: Ride, previous_fee: Money, at: datetime) -> FareReceipt:
        """Final fare on the ACTUAL route and time. The captain's trip to the pickup is never billed."""
        route_text = " → ".join(str(point) for point in ride.route)
        return self._price(vehicle=ride.captain.vehicle, distance_km=ride.actual_distance_km,
                           trip_minutes=ride.trip_minutes, waiting_minutes=ride.waiting_minutes,
                           surge=ride.estimate.surge_multiplier,
                           is_night=SimulatedClock.is_night(ride.started_at),
                           previous_fee=previous_fee, coupon_code=ride.request.coupon_code,
                           ride_id=ride.ride_id, route_text=route_text,
                           estimated_total=ride.estimate.fare, at=at)

    def _price(self, *, vehicle: Vehicle, distance_km: Decimal, trip_minutes: int,
               waiting_minutes: int, surge: Decimal, is_night: bool, previous_fee: Money,
               coupon_code: str | None, ride_id: str, route_text: str,
               estimated_total: Money | None, at: datetime) -> FareReceipt:
        # 1–3: polymorphic calls — the vehicle decides its own rates.
        base = vehicle.base_fare
        distance_charge = vehicle.distance_charge(distance_km)
        time_charge = vehicle.time_charge(trip_minutes)
        trip_fare = vehicle.calculate_base_fare(distance_km, trip_minutes)   # applies minimum fare
        top_up = trip_fare - (base + distance_charge + time_charge)
        # 4: waiting — first 3 minutes free, then ₹1 per minute
        chargeable_wait = max(waiting_minutes - 3, 0)
        waiting_charge = FareService.WAITING_CHARGE_PER_MINUTE * chargeable_wait
        # 5: surge on the trip fare
        surge_charge = trip_fare * (surge - Decimal("1"))
        # 6: night charge, +20% between 11 PM and 5 AM
        night_charge = Money.zero()
        if is_night:
            night_charge = (trip_fare + waiting_charge + surge_charge) * FareService.NIGHT_CHARGE_RATE
        # 8: coupon — flat off, never pushing the fare below the vehicle's minimum fare
        before_coupon = trip_fare + waiting_charge + surge_charge + night_charge + previous_fee
        discount = Money.zero()
        if coupon_code is not None:
            flat_off, minimum_needed = FareService.COUPONS[coupon_code]
            if before_coupon >= minimum_needed:
                room = before_coupon - vehicle.minimum_fare
                discount = flat_off if flat_off <= room else room
        return FareReceipt(ride_id=ride_id, vehicle_type=vehicle.vehicle_type, route_text=route_text,
                           distance_km=distance_km, trip_minutes=trip_minutes, base_fare=base,
                           distance_charge=distance_charge, time_charge=time_charge,
                           minimum_fare_top_up=top_up, waiting_minutes=waiting_minutes,
                           waiting_charge=waiting_charge, surge_multiplier=surge,
                           surge_charge=surge_charge, night_charge=night_charge,
                           previous_cancellation_fee=previous_fee, coupon_code=coupon_code,
                           coupon_discount=discount, estimated_total=estimated_total, issued_at=at)
