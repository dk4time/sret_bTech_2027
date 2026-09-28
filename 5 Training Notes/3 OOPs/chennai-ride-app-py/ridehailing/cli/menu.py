"""Menu — the abstract base class of every screen in the interactive mode."""

from __future__ import annotations

from abc import ABC, abstractmethod

from ridehailing.cli.console_io import ConsoleIO, GoBack
from ridehailing.exceptions import RideHailingError


class Menu(ABC):
    """A titled list of options.

    OOP concepts shown here:
    * Abstraction: every menu MUST say which options it has (``options()``) and what each one does
      (``handle()``). ``Menu(...)`` itself raises TypeError.
    * Template method: ``run()`` is written once here — show, read, handle, repeat — and every
      subclass reuses it unchanged, filling in only the abstract steps.
    * Polymorphism: MainMenu keeps its sub-menus in a ``dict[str, Menu]`` and simply calls
      ``menu.run()``; it never knows whether it is the Customer app or the Clock controls.
    * Composition: every menu HAS-A shared ConsoleIO (one object passed to all menus).
    """

    def __init__(self, title: str, io: ConsoleIO) -> None:
        self._title = title
        self._io = io

    @property
    def title(self) -> str:
        return self._title

    # ----- abstract steps -----
    @abstractmethod
    def options(self) -> list[tuple[str, str]]:
        """(key, label) pairs shown to the user, e.g. ("1", "Book a ride")."""

    @abstractmethod
    def handle(self, choice: str) -> bool:
        """Carry out one chosen option. Return False to leave this menu."""

    # ----- hooks a subclass MAY override (concrete defaults) -----
    def on_enter(self) -> bool:
        """Runs once when the menu opens. Return False to not open it."""
        return True

    def status_line(self) -> str | None:
        """An optional line printed above the menu."""
        return None

    def heading(self) -> str:
        return self._title

    # ----- the concrete loop -----
    def run(self) -> None:
        try:
            if not self.on_enter():
                return
        except GoBack:
            return
        while True:
            status = self.status_line()
            if status:
                self._io.print_status(status)
            self._io.print_header(self.heading())
            self._io.print_options(self.options())
            keys = [key for key, _ in self.options()]
            try:
                choice = self._io.read_choice("Choose: ", keys)
            except GoBack:
                return
            try:
                keep_going = self.handle(choice)
            except GoBack:
                self._io.print_info("Cancelled — back to the menu.")
                continue
            except RideHailingError as error:
                # Business-rule errors come from the model/services and are shown, never crashed on.
                self._io.print_error(f"{type(error).__name__}: {error}")
                continue
            except (ValueError, KeyError, TypeError, AttributeError) as error:
                # Last resort: a live class must never end in a stack trace.
                self._io.print_error(f"Unexpected problem ({type(error).__name__}): {error}")
                continue
            if not keep_going:
                return
