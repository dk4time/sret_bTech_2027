"""CaptainStatus — where a captain is in their working lifecycle."""

from __future__ import annotations

from enum import Enum


class CaptainStatus(Enum):
    KYC_PENDING = "KYC pending"
    OFFLINE = "Offline"
    ONLINE = "Online"
    ON_RIDE = "On ride"

    def __init__(self, label: str) -> None:
        self.label = label

    def is_working(self) -> bool:
        """True while the captain is logged in (idle or on a ride)."""
        return self in (CaptainStatus.ONLINE, CaptainStatus.ON_RIDE)

    def __str__(self) -> str:
        return self.label
