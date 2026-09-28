"""PaymentService — takes money through any Payable and keeps a ledger of every attempt."""

from __future__ import annotations

from datetime import datetime

from ridehailing.exceptions import (DuplicatePaymentError, InsufficientWalletBalanceError,
                                    PaymentFailedError)
from ridehailing.model.common.money import Money
from ridehailing.model.payment.payable import Payable
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain
from ridehailing.model.user.customer import Customer
from ridehailing.service.earnings_service import EarningsService


class PaymentRecord:
    """One line in the payment ledger (successful or failed)."""

    def __init__(self, at: datetime, reference: str, customer: Customer, method: str,
                 amount: Money, succeeded: bool, note: str) -> None:
        self._at = at
        self._reference = reference
        self._customer = customer
        self._method = method
        self._amount = amount
        self._succeeded = succeeded
        self._note = note

    @property
    def amount(self) -> Money:
        return self._amount

    @property
    def succeeded(self) -> bool:
        return self._succeeded

    @property
    def method(self) -> str:
        return self._method

    @property
    def customer(self) -> Customer:
        return self._customer

    def __str__(self) -> str:
        outcome = "OK    " if self._succeeded else "FAILED"
        return (f"{self._at:%I:%M %p}  {outcome} {self._method:<6} {str(self._amount):>10}  "
                f"{self._customer.name:<8} {self._reference:<10} {self._note}")


class PaymentService:
    def __init__(self, earnings_service: EarningsService) -> None:
        self._earnings = earnings_service
        self._ledger: list[PaymentRecord] = []

    def pay_ride(self, ride: Ride, payable: Payable, at: datetime) -> str:
        """Charge the ride's receipt total. On failure the ride becomes PAYMENT_PENDING."""
        if ride.is_paid:
            raise DuplicatePaymentError(ride.ride_id)
        amount = ride.receipt.total
        try:
            reference = payable.pay(amount)       # dynamic dispatch: UPI / Cash / Wallet decide how
        except (PaymentFailedError, InsufficientWalletBalanceError) as error:
            ride.mark_payment_failed(str(error), at)
            ride.customer.mark_payment_pending(ride)
            self._ledger.append(PaymentRecord(at, "-", ride.customer, payable.method_name, amount,
                                              False, f"{ride.ride_id}: {error}"))
            raise
        ride.complete(payable, reference, at)
        ride.customer.clear_payment_pending(ride)
        self._earnings.record_ride_payment(ride, payable.is_cash, at)
        self._ledger.append(PaymentRecord(at, reference, ride.customer, payable.method_name, amount,
                                          True, f"{ride.ride_id} fare"))
        return reference

    def charge_fee(self, customer: Customer, fee: Money, captain: Captain, payable: Payable,
                   ride_id: str, at: datetime) -> str:
        """Charge a no-show fee straight away through a non-cash method."""
        try:
            reference = payable.pay(fee)
        except (PaymentFailedError, InsufficientWalletBalanceError) as error:
            self._ledger.append(PaymentRecord(at, "-", customer, payable.method_name, fee, False,
                                              f"{ride_id} no-show fee: {error}"))
            raise
        self._earnings.record_fee_payment(fee, captain, at)
        self._ledger.append(PaymentRecord(at, reference, customer, payable.method_name, fee, True,
                                          f"{ride_id} no-show fee"))
        return reference

    @property
    def ledger(self) -> tuple[PaymentRecord, ...]:
        return tuple(self._ledger)

    @property
    def total_collected(self) -> Money:
        """Sum of successful payments, counted independently of EarningsService."""
        return Money.total([record.amount for record in self._ledger if record.succeeded])
