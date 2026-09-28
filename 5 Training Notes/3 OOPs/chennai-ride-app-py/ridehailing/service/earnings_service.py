"""EarningsService — who earned what, and proof that the books balance."""

from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal

from ridehailing.model.common.money import Money
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain


class DailyEarnings:
    """One captain's numbers for one day."""

    def __init__(self, captain: Captain, day: date) -> None:
        self._captain = captain
        self._day = day
        self._rides = 0
        self._gross_fare = Money.zero()
        self._commission = Money.zero()
        self._net_earnings = Money.zero()
        self._cash_collected = Money.zero()
        self._dues_added = Money.zero()

    def add_earning(self, gross: Money, commission: Money, net: Money, counts_as_ride: bool) -> None:
        if counts_as_ride:
            self._rides += 1
        self._gross_fare = self._gross_fare + gross
        self._commission = self._commission + commission
        self._net_earnings = self._net_earnings + net

    def add_cash(self, collected: Money, owed_to_platform: Money) -> None:
        self._cash_collected = self._cash_collected + collected
        self._dues_added = self._dues_added + owed_to_platform

    @property
    def captain(self) -> Captain:
        return self._captain

    @property
    def day(self) -> date:
        return self._day

    @property
    def rides(self) -> int:
        return self._rides

    @property
    def gross_fare(self) -> Money:
        return self._gross_fare

    @property
    def commission(self) -> Money:
        return self._commission

    @property
    def net_earnings(self) -> Money:
        return self._net_earnings

    @property
    def cash_collected(self) -> Money:
        return self._cash_collected

    @property
    def dues_added(self) -> Money:
        """Cash the captain collected that belongs to others (commission + GST + others' fees)."""
        return self._dues_added


class EarningsService:
    """Splits every payment into captain earnings, platform commission and GST.

    Invariant checked by the self-check:
        total paid by customers == captain earnings + platform commission + GST collected
    """

    COMMISSION_RATE = Decimal("0.20")   # 20% of the fare before GST

    def __init__(self) -> None:
        self._daily: dict[tuple[str, date], DailyEarnings] = {}
        self._total_paid = Money.zero()
        self._total_commission = Money.zero()
        self._total_gst = Money.zero()
        self._total_captain_earnings = Money.zero()

    def record_ride_payment(self, ride: Ride, is_cash: bool, at: datetime) -> None:
        receipt = ride.receipt
        captain = ride.captain
        day = at.date()
        # 1. This ride's own fare: 20% commission, the rest to the captain.
        ride_net = self._split(receipt.ride_fare_before_gst, captain, day, counts_as_ride=True)
        # 2. Old cancellation fees carried into this bill go to the captain who was cancelled on.
        for fee in ride.carried_fees:
            self._split(fee.amount, fee.captain, day, counts_as_ride=False)
        # 3. GST belongs to the government, collected by the platform.
        self._total_gst = self._total_gst + receipt.gst
        self._total_paid = self._total_paid + receipt.total
        # 4. Cash: the captain holds all of it, so they owe everything that is not their own earning.
        if is_cash:
            owed = receipt.total - ride_net
            captain.add_dues(owed)
            self._entry(captain, day).add_cash(receipt.total, owed)

    def record_fee_payment(self, fee: Money, captain: Captain | None, at: datetime) -> None:
        """A no-show / cancellation fee paid directly (not inside a ride bill). No GST on fees."""
        self._split(fee, captain, at.date(), counts_as_ride=False)
        self._total_paid = self._total_paid + fee

    def _split(self, amount: Money, captain: Captain | None, day: date, counts_as_ride: bool) -> Money:
        if captain is None:                      # nobody to pay: all of it stays with the platform
            self._total_commission = self._total_commission + amount
            return Money.zero()
        commission = amount.percent(EarningsService.COMMISSION_RATE)
        net = amount - commission                # subtraction, so commission + net == amount exactly
        self._entry(captain, day).add_earning(amount, commission, net, counts_as_ride)
        self._total_commission = self._total_commission + commission
        self._total_captain_earnings = self._total_captain_earnings + net
        return net

    def _entry(self, captain: Captain, day: date) -> DailyEarnings:
        key = (captain.user_id, day)
        if key not in self._daily:
            self._daily[key] = DailyEarnings(captain, day)
        return self._daily[key]

    # ----- queries -----
    def earnings_for(self, captain: Captain, day: date) -> DailyEarnings:
        return self._entry(captain, day)

    @property
    def total_paid_by_customers(self) -> Money:
        return self._total_paid

    @property
    def total_commission(self) -> Money:
        return self._total_commission

    @property
    def total_gst(self) -> Money:
        return self._total_gst

    @property
    def total_captain_earnings(self) -> Money:
        return self._total_captain_earnings

    def books_balance(self) -> bool:
        return self._total_paid == (self._total_captain_earnings + self._total_commission + self._total_gst)
