"""MainMenu — the home screen of the interactive mode."""

from __future__ import annotations

from ridehailing.cli.captain_menu import CaptainMenu
from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.clock_menu import ClockMenu
from ridehailing.cli.console_io import ConsoleIO
from ridehailing.cli.customer_menu import CustomerMenu
from ridehailing.cli.menu import Menu
from ridehailing.cli.operations_menu import OperationsMenu


class MainMenu(Menu):
    """Holds its sub-menus through the Menu type and just calls ``run()`` on them (polymorphism)."""

    def __init__(self, io: ConsoleIO, session: CliSession) -> None:
        super().__init__("Main menu", io)
        self._session = session
        self._submenus: dict[str, Menu] = {
            "1": CustomerMenu(io, session),
            "2": CaptainMenu(io, session),
            "3": OperationsMenu(io, session),
            "4": ClockMenu(io, session),
        }

    def status_line(self) -> str:
        return self._session.status_line()

    def heading(self) -> str:
        return "MAIN MENU — Rapido Chennai (interactive)"

    def options(self) -> list[tuple[str, str]]:
        mode = "Auto" if self._session.auto_captains else "Manual"
        return [("1", "Customer app"), ("2", "Captain app"), ("3", "Operations dashboard"),
                ("4", "Clock controls"), ("5", f"Settings (captain mode: {mode})"),
                ("0", "Back to start-up menu")]

    def handle(self, choice: str) -> bool:
        if choice == "0":
            return False
        if choice == "5":
            self._settings()
            return True
        menu: Menu = self._submenus[choice]
        menu.run()          # dynamic dispatch: CustomerMenu / CaptainMenu / ... all answer run()
        return True

    def _settings(self) -> None:
        self._io.print_info("1. Manual — a booked ride's offer waits for the captain in the Captain app")
        self._io.print_info("2. Auto   — the captain accepts, arrives and starts at once; you act as the customer")
        choice = self._io.read_choice("  Captain mode: ", ["1", "2"])
        self._session.set_auto_captains(choice == "2")
        self._io.print_success(f"Captain mode is now {'Auto' if choice == '2' else 'Manual'}.")
