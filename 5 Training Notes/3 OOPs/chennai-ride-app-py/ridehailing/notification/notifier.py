"""Notifier — the abstract idea of "a channel that can message a user"."""

from __future__ import annotations

from abc import ABC, abstractmethod
from datetime import datetime
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ridehailing.model.user.user import User


class Notifier(ABC):
    """Abstract notification channel.

    RideService holds a ``list[Notifier]`` and calls ``notify()`` on each one for every event —
    it never checks whether it is an SMS or a push notifier (polymorphism / duck typing).
    """

    _sequence = 0   # class attribute shared by ALL notifiers: a global send order across channels

    def __init__(self) -> None:
        self._sent: list[str] = []   # every message this channel delivered, in order
        self._log: list[tuple[int, datetime, str]] = []

    @property
    @abstractmethod
    def channel(self) -> str:
        """'SMS' or 'Push'."""

    @abstractmethod
    def notify(self, user: User, message: str, at: datetime) -> None:
        """Deliver ``message`` to ``user``."""

    def _record(self, at: datetime, line: str) -> None:
        """Called by every subclass's notify(): keeps the text and its global send order."""
        Notifier._sequence += 1
        self._sent.append(line)
        self._log.append((Notifier._sequence, at, line))

    @property
    def log(self) -> tuple[tuple[int, datetime, str], ...]:
        """(global sequence number, time, text) for every message sent on this channel."""
        return tuple(self._log)

    @property
    def sent_count(self) -> int:
        return len(self._sent)

    @property
    def sent_messages(self) -> tuple[str, ...]:
        return tuple(self._sent)
