"""RideStatus — every state a ride can be in."""

from __future__ import annotations

from enum import Enum


class RideStatus(Enum):
    """REQUESTED -> CAPTAIN_ASSIGNED -> CAPTAIN_ARRIVED -> IN_PROGRESS -> COMPLETED,
    plus CANCELLED and PAYMENT_PENDING."""

    REQUESTED = "Requested"
    CAPTAIN_ASSIGNED = "Captain assigned"
    CAPTAIN_ARRIVED = "Captain arrived"
    IN_PROGRESS = "In progress"
    PAYMENT_PENDING = "Payment pending"
    COMPLETED = "Completed"
    CANCELLED = "Cancelled"

    def __init__(self, label: str) -> None:
        self.label = label

    def is_active(self) -> bool:
        """Statuses during which the customer and captain are tied to this ride."""
        return self in (RideStatus.REQUESTED, RideStatus.CAPTAIN_ASSIGNED,
                        RideStatus.CAPTAIN_ARRIVED, RideStatus.IN_PROGRESS)

    def is_cancellable(self) -> bool:
        return self in (RideStatus.REQUESTED, RideStatus.CAPTAIN_ASSIGNED, RideStatus.CAPTAIN_ARRIVED)

    def is_final(self) -> bool:
        return self in (RideStatus.COMPLETED, RideStatus.CANCELLED)

    def __str__(self) -> str:
        return self.name
