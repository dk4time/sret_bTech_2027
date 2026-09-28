"""CancellationReason — why a ride ended up CANCELLED."""

from __future__ import annotations

from enum import Enum


class CancellationReason(Enum):
    #                        description                               fee may apply?
    CUSTOMER_CANCELLED = ("Customer cancelled the ride", True)
    NO_SHOW = ("Customer did not show up at the pickup", True)
    OTP_FAILED = ("Wrong OTP entered 3 times", False)
    NO_CAPTAIN_AVAILABLE = ("No captain accepted the request", False)

    def __init__(self, description: str, fee_may_apply: bool) -> None:
        self.description = description
        self.fee_may_apply = fee_may_apply

    def __str__(self) -> str:
        return self.name
