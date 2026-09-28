"""SmsNotifier — texts the user's +91 mobile number."""

from __future__ import annotations

from datetime import datetime

from ridehailing.model.user.user import User
from ridehailing.notification.notifier import Notifier


class SmsNotifier(Notifier):
    """SMS channel. By default it only stores messages (SMS logs are long); ``echo=True`` prints them."""

    def __init__(self, echo: bool = False) -> None:
        super().__init__()
        self._echo = echo

    @property
    def channel(self) -> str:
        return "SMS"

    def notify(self, user: User, message: str, at: datetime) -> None:
        line = f"[{at:%I:%M %p}]   ✉ SMS to {user.phone}: {message}"
        self._record(at, line)
        if self._echo:
            print(line)
