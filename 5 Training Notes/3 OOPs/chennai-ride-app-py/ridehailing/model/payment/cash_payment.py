"""CashPayment — the customer hands cash to the captain. Always succeeds."""

from __future__ import annotations

from ridehailing.model.common.money import Money
from ridehailing.model.payment.payable import Payable


class CashPayment(Payable):
    """Cash. The captain keeps the cash and owes the platform its share (see EarningsService)."""

    _reference_counter = 0

    @property
    def method_name(self) -> str:
        return "Cash"

    @property
    def is_cash(self) -> bool:
        # Overrides Payable.is_cash — the only payment method that returns True.
        return True

    def pay(self, amount: Money) -> str:
        CashPayment._reference_counter += 1
        return f"CASH{CashPayment._reference_counter:06d}"
