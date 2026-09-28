"""Payable — the abstract idea of "something that can pay"."""

from __future__ import annotations

from abc import ABC, abstractmethod

from ridehailing.model.common.money import Money


class Payable(ABC):
    """Abstract payment method: UPI, Cash and Wallet all implement ``pay()``.

    Polymorphism / dynamic dispatch: PaymentService calls ``payable.pay(amount)`` without knowing
    or caring which concrete class it holds — Python finds the right ``pay`` at runtime.
    """

    @property
    @abstractmethod
    def method_name(self) -> str:
        """Short name shown on receipts, e.g. 'UPI'."""

    @abstractmethod
    def pay(self, amount: Money) -> str:
        """Take ``amount`` and return a transaction reference. Raise a RideHailingError on failure."""

    @property
    def is_cash(self) -> bool:
        """Concrete default: most methods are not cash. CashPayment overrides this."""
        return False

    def __str__(self) -> str:
        return self.method_name

    def __repr__(self) -> str:
        return f"{type(self).__name__}()"
