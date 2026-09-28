"""FareReceipt — the itemised bill for one ride."""

from __future__ import annotations

import textwrap
from datetime import datetime
from decimal import Decimal

from ridehailing.model.common.money import Money
from ridehailing.model.vehicle.vehicle_type import VehicleType


class FareReceipt:
    """An itemised, read-only receipt.

    The receipt computes GST and the total ITSELF from its line items, so the numbers printed on
    the receipt can never disagree with each other.
    """

    GST_RATE = Decimal("0.05")   # class attribute: 5% GST on every ride
    _WIDTH = 50

    def __init__(self, *, ride_id: str, vehicle_type: VehicleType, route_text: str,
                 distance_km: Decimal, trip_minutes: int, base_fare: Money, distance_charge: Money,
                 time_charge: Money, minimum_fare_top_up: Money, waiting_minutes: int,
                 waiting_charge: Money, surge_multiplier: Decimal, surge_charge: Money,
                 night_charge: Money, previous_cancellation_fee: Money, coupon_code: str | None,
                 coupon_discount: Money, estimated_total: Money | None, issued_at: datetime) -> None:
        # The ``*`` above forces keyword arguments: FareReceipt(ride_id=..., base_fare=...).
        self._ride_id = ride_id
        self._vehicle_type = vehicle_type
        self._route_text = route_text
        self._distance_km = distance_km
        self._trip_minutes = trip_minutes
        self._base_fare = base_fare
        self._distance_charge = distance_charge
        self._time_charge = time_charge
        self._minimum_fare_top_up = minimum_fare_top_up
        self._waiting_minutes = waiting_minutes
        self._waiting_charge = waiting_charge
        self._surge_multiplier = surge_multiplier
        self._surge_charge = surge_charge
        self._night_charge = night_charge
        self._previous_cancellation_fee = previous_cancellation_fee
        self._coupon_code = coupon_code
        self._coupon_discount = coupon_discount
        self._estimated_total = estimated_total
        self._issued_at = issued_at
        # Derived amounts, computed once.
        self._fare_before_gst = (base_fare + distance_charge + time_charge + minimum_fare_top_up
                                 + waiting_charge + surge_charge + night_charge
                                 + previous_cancellation_fee - coupon_discount)
        self._gst = self._fare_before_gst.percent(FareReceipt.GST_RATE)
        self._total = self._fare_before_gst + self._gst   # operator overloading: Money + Money

    # ----- read-only properties -----
    @property
    def ride_id(self) -> str:
        return self._ride_id

    @property
    def distance_km(self) -> Decimal:
        return self._distance_km

    @property
    def trip_minutes(self) -> int:
        return self._trip_minutes

    @property
    def base_fare(self) -> Money:
        return self._base_fare

    @property
    def distance_charge(self) -> Money:
        return self._distance_charge

    @property
    def time_charge(self) -> Money:
        return self._time_charge

    @property
    def minimum_fare_top_up(self) -> Money:
        return self._minimum_fare_top_up

    @property
    def waiting_charge(self) -> Money:
        return self._waiting_charge

    @property
    def surge_multiplier(self) -> Decimal:
        return self._surge_multiplier

    @property
    def surge_charge(self) -> Money:
        return self._surge_charge

    @property
    def night_charge(self) -> Money:
        return self._night_charge

    @property
    def previous_cancellation_fee(self) -> Money:
        return self._previous_cancellation_fee

    @property
    def coupon_code(self) -> str | None:
        return self._coupon_code

    @property
    def coupon_discount(self) -> Money:
        return self._coupon_discount

    @property
    def estimated_total(self) -> Money | None:
        return self._estimated_total

    @property
    def fare_before_gst(self) -> Money:
        return self._fare_before_gst

    @property
    def ride_fare_before_gst(self) -> Money:
        """The part of the fare earned by THIS ride's captain (excludes an older cancellation fee)."""
        return self._fare_before_gst - self._previous_cancellation_fee

    @property
    def gst(self) -> Money:
        return self._gst

    @property
    def total(self) -> Money:
        return self._total

    # ----- printing -----
    def _line(self, label: str, amount: Money) -> str:
        return f"| {label:<32}{str(amount):>14} |"

    def __str__(self) -> str:
        border = "+" + "-" * (FareReceipt._WIDTH - 2) + "+"
        lines = [
            border,
            f"| {'RAPIDO CHENNAI — RIDE RECEIPT':^46} |",
            f"| {self._ride_id + '  ' + self._vehicle_type.display_name:<46} |",
        ]
        for route_line in textwrap.wrap(self._route_text, 46):   # long routes wrap onto extra lines
            lines.append(f"| {route_line:<46} |")
        lines += [
            f"| {f'{self._distance_km} km  ·  {self._trip_minutes} min  ·  {self._issued_at:%d %b %Y %I:%M %p}':<46} |",
            border,
            self._line("1. Base fare", self._base_fare),
            self._line(f"2. Distance charge ({self._distance_km} km)", self._distance_charge),
            self._line(f"3. Time charge ({self._trip_minutes} min)", self._time_charge),
        ]
        if not self._minimum_fare_top_up.is_zero():
            lines.append(self._line("   Minimum fare top-up", self._minimum_fare_top_up))
        lines += [
            self._line(f"4. Waiting charge ({self._waiting_minutes} min)", self._waiting_charge),
            self._line(f"5. Surge ({self._surge_multiplier}x)", self._surge_charge),
            self._line("6. Night charge (+20%)", self._night_charge),
            self._line("7. Previous cancellation fee", self._previous_cancellation_fee),
            self._line(f"8. Coupon {self._coupon_code or ''}".rstrip(), -self._coupon_discount),
            self._line("   Fare before GST", self._fare_before_gst),
            self._line("9. GST 5%", self._gst),
            border,
            self._line("10. TOTAL", self._total),
        ]
        if self._estimated_total is not None:
            lines.append(self._line("    (Estimated at booking)", self._estimated_total))
        lines.append(border)
        return "\n".join(lines)

    def __repr__(self) -> str:
        return f"FareReceipt(ride_id='{self._ride_id}', total={self._total!r})"
