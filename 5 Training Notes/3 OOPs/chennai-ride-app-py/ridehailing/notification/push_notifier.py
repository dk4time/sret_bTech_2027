"""PushNotifier — an in-app push notification."""

from __future__ import annotations

from datetime import datetime

from ridehailing.model.user.user import User
from ridehailing.notification.notifier import Notifier


class PushNotifier(Notifier):
    """Push channel. Prints each notification to the console (the demo's story line)."""

    def __init__(self, echo: bool = True) -> None:
        super().__init__()
        self._echo = echo

    @property
    def channel(self) -> str:
        return "Push"

    def notify(self, user: User, message: str, at: datetime) -> None:
        line = f"[{at:%I:%M %p}]   » Push to {user.name}: {message}"
        self._record(at, line)
        if self._echo:
            print(line)
