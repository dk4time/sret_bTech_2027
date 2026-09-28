"""ClockMenu — move the simulated clock forward by hand."""

from __future__ import annotations

from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.cli.menu import Menu
from ridehailing.time.simulated_clock import SimulatedClock


class ClockMenu(Menu):
    """Inheritance: ClockMenu IS-A Menu. The "never backwards" rule lives in SimulatedClock, not here."""

    def __init__(self, io: ConsoleIO, session: CliSession) -> None:
        super().__init__("Clock controls", io)
        self._session = session

    def status_line(self) -> str:
        return self._session.status_line()

    def heading(self) -> str:
        return f"CLOCK CONTROLS — it is {self._session.clock}"

    def options(self) -> list[tuple[str, str]]:
        return [("1", "Show current time"), ("2", "Advance by N minutes"),
                ("3", "Jump forward to a time (HH:MM)"), ("0", "Back")]

    def handle(self, choice: str) -> bool:
        clock = self._session.clock
        if choice == "0":
            return False
        if choice == "1":
            now = clock.now
            self._io.print_info(f"{now:%A %d %B %Y, %I:%M %p} — peak hour: "
                                f"{'yes' if SimulatedClock.is_peak_hour(now) else 'no'}, night (+20%): "
                                f"{'yes' if SimulatedClock.is_night(now) else 'no'}")
        elif choice == "2":
            minutes = self._io.read_int("  Minutes to advance (1–1440): ", 1, 1440)
            clock.advance(minutes=minutes)
            self._io.print_success(f"Clock advanced {minutes} min → {clock}.")
        elif choice == "3":
            hour, minute = self._io.read_time("  Jump to (HH:MM, 24-hour): ")
            target = clock.now.replace(hour=hour, minute=minute, second=0, microsecond=0)
            clock.advance_to(target)       # raises TimeTravelError if it would go backwards
            self._io.print_success(f"Clock is now {clock}.")
        return True
