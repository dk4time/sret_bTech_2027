"""StartupMenu — the first screen of ``python run.py``, plus the entry point for ``--cli``."""

from __future__ import annotations

import subprocess
import sys
from pathlib import Path

from ridehailing.cli.cli_session import CliSession
from ridehailing.cli.console_io import ConsoleIO, EndOfInput, QuitRequested
from ridehailing.cli.main_menu import MainMenu
from ridehailing.cli.menu import Menu

RUN_PY = Path(__file__).resolve().parents[2] / "run.py"


class StartupMenu(Menu):
    """Demo, interactive mode or self-check.

    The demo and the self-check run as separate processes, so every run starts from a clean slate
    and prints exactly what ``python run.py --demo`` / ``--check`` print.
    """

    def __init__(self, io: ConsoleIO) -> None:
        super().__init__("Start-up menu", io)

    def heading(self) -> str:
        return "RAPIDO CHENNAI — START-UP MENU"

    def options(self) -> list[tuple[str, str]]:
        return [("1", "Run full simulated day (demo)"), ("2", "Interactive mode"),
                ("3", "Run self-check"), ("0", "Exit")]

    def handle(self, choice: str) -> bool:
        if choice == "0":
            return False
        if choice == "1":
            self._run_separately("--demo")
        elif choice == "2":
            run_interactive_session(self._io)
        elif choice == "3":
            self._run_separately("--check")
        return True

    def _run_separately(self, flag: str) -> None:
        sys.stdout.flush()
        subprocess.run([sys.executable, str(RUN_PY), flag], check=False, stdin=subprocess.DEVNULL)


def run_interactive_session(io: ConsoleIO) -> None:
    """A fresh seeded world (6:00 AM) and the main menu. Quitting inside it ends the program."""
    session = CliSession(io)
    session.open_for_business()
    MainMenu(io, session).run()


def launch(start_with_menu: bool) -> int:
    """Entry point used by run.py. Never lets 'q', end of input or Ctrl-C become a traceback."""
    io = ConsoleIO()
    try:
        if start_with_menu:
            StartupMenu(io).run()
        else:
            run_interactive_session(io)
            print("Interactive mode closed — goodbye!")
    except EndOfInput:
        print("End of input — goodbye!")
    except QuitRequested:
        print("Goodbye!")
    except KeyboardInterrupt:
        print("\nInterrupted — goodbye!")
    return 0
