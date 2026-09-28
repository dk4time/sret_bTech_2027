"""WalletPayment — pays from the customer's in-app wallet."""

from __future__ import annotations

from typing import TYPE_CHECKING

from ridehailing.model.common.money import Money
from ridehailing.model.payment.payable import Payable

if TYPE_CHECKING:
    from ridehailing.model.user.customer import Customer


class WalletPayment(Payable):
    """Wallet. If the balance is short, InsufficientWalletBalanceError is raised by the customer's
    wallet and NOTHING is deducted (never a partial deduction)."""

    _reference_counter = 0

    def __init__(self, customer: Customer) -> None:
        self._customer = customer

    @property
    def method_name(self) -> str:
        return "Wallet"

    def pay(self, amount: Money) -> str:
        self._customer.pay_from_wallet(amount)   # all-or-nothing, enforced by Customer
        WalletPayment._reference_counter += 1
        return f"WAL{WalletPayment._reference_counter:06d}"

    def __repr__(self) -> str:
        return f"WalletPayment(customer='{self._customer.name}')"
