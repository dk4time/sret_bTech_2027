"""UpiPayment — pays through a UPI id; the bank can fail (deterministically simulated)."""

from __future__ import annotations

import random

from ridehailing.exceptions import PaymentFailedError
from ridehailing.model.common.money import Money
from ridehailing.model.payment.payable import Payable


class UpiPayment(Payable):
    """UPI payment. Failures come from a seeded ``random.Random``, so every run is identical."""

    _reference_counter = 0   # class attribute shared by all UPI payments

    def __init__(self, upi_id: str, rng: random.Random, failure_rate: float = 0.0) -> None:
        if "@" not in upi_id:
            raise ValueError(f"'{upi_id}' is not a valid UPI id (expected name@bank).")
        if not 0.0 <= failure_rate <= 1.0:
            raise ValueError("failure_rate must be between 0 and 1.")
        self._upi_id = upi_id
        self._rng = rng
        self._failure_rate = failure_rate

    @property
    def upi_id(self) -> str:
        return self._upi_id

    @property
    def method_name(self) -> str:
        return "UPI"

    def pay(self, amount: Money) -> str:
        """Method overriding: the UPI way to pay."""
        if self._rng.random() < self._failure_rate:
            raise PaymentFailedError("UPI", f"bank server for {self._upi_id} did not respond")
        UpiPayment._reference_counter += 1
        return f"UPI{UpiPayment._reference_counter:06d}"

    def __str__(self) -> str:
        return f"UPI ({self._upi_id})"

    def __repr__(self) -> str:
        return f"UpiPayment('{self._upi_id}')"
