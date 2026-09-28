"""PaymentStatus — the payment side of a ride."""

from __future__ import annotations

from enum import Enum


class PaymentStatus(Enum):
    NOT_DUE = "Not due yet"       # the trip has not ended (or the ride was cancelled)
    FAILED = "Failed — pending"   # an attempt failed; the ride is PAYMENT_PENDING
    PAID = "Paid"

    def __init__(self, label: str) -> None:
        self.label = label

    def __str__(self) -> str:
        return self.label
