"""SimulatedClock — the single source of time for the whole app.

``datetime.now()`` is never used anywhere: the demo moves this clock forward by realistic
amounts (travel to pickup, waiting, ride duration), so every run prints exactly the same day.
"""

from __future__ import annotations

from datetime import datetime, timedelta

from ridehailing.exceptions import TimeTravelError


class SimulatedClock:
    """A clock that only moves forward."""

    # Class attributes: Chennai traffic windows (start hour inclusive, end hour exclusive)
    PEAK_WINDOWS: tuple[tuple[int, int], ...] = ((8, 11), (17, 21))   # 8–11 AM and 5–9 PM
    NIGHT_START_HOUR = 23   # 11 PM
    NIGHT_END_HOUR = 5      # 5 AM

    def __init__(self, start: datetime) -> None:
        self._now = start

    @property
    def now(self) -> datetime:
        return self._now

    def advance(self, minutes: int = 0, seconds: int = 0) -> datetime:
        """Move forward by a duration. A negative duration would be time travel."""
        if minutes < 0 or seconds < 0:
            raise TimeTravelError(f"The clock cannot go backwards ({minutes} min, {seconds} s).")
        self._now = self._now + timedelta(minutes=minutes, seconds=seconds)
        return self._now

    def advance_to(self, moment: datetime) -> datetime:
        """Jump forward to ``moment``. Asking for an earlier time raises TimeTravelError."""
        if moment < self._now:
            raise TimeTravelError(f"The clock is at {self._now:%I:%M %p}; it cannot go back to "
                                  f"{moment:%I:%M %p}.")
        self._now = moment
        return self._now

    # ----- @staticmethod helpers: pure functions of a datetime -----
    @staticmethod
    def is_peak_hour(moment: datetime) -> bool:
        for start_hour, end_hour in SimulatedClock.PEAK_WINDOWS:
            if start_hour <= moment.hour < end_hour:
                return True
        return False

    @staticmethod
    def is_night(moment: datetime) -> bool:
        """11:00 PM up to (not including) 5:00 AM."""
        return moment.hour >= SimulatedClock.NIGHT_START_HOUR or moment.hour < SimulatedClock.NIGHT_END_HOUR

    @staticmethod
    def format_time(moment: datetime) -> str:
        return moment.strftime("%I:%M %p")

    def __str__(self) -> str:
        return SimulatedClock.format_time(self._now)

    def __repr__(self) -> str:
        return f"SimulatedClock({self._now.isoformat()})"
