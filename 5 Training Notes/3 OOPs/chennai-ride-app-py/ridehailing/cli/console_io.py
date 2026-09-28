"""ConsoleIO — every keyboard read and screen write of the interactive mode lives here.

The menus never call ``input()`` or format a table themselves. Every read re-prompts on bad input,
so the app never crashes on letters-for-numbers, out-of-range choices, negative money or empty
input. Two words work at ANY prompt: ``b`` goes back and ``q`` quits (after a confirmation).
"""

from __future__ import annotations

import sys
from decimal import Decimal, InvalidOperation

from ridehailing.model.common.location import Location
from ridehailing.model.common.money import Money


class GoBack(Exception):
    """The user typed 'b': abandon the current action and return to the menu above."""


class QuitRequested(Exception):
    """The user typed 'q' and confirmed: leave the interactive mode."""


class EndOfInput(QuitRequested):
    """Input ended (Ctrl-D / Ctrl-Z, or the end of a replayed session file): leave gracefully."""


class ConsoleIO:
    """Reads validated input and prints aligned output."""

    WIDTH = 100

    def __init__(self, echo_input: bool | None = None) -> None:
        # When input is redirected from a file, the typed text is not shown on screen;
        # echoing it makes a replayed session read like a real one.
        self._echo_input = (not sys.stdin.isatty()) if echo_input is None else echo_input

    # ================================================================ reading
    def read_line(self, prompt: str, allow_empty: bool = False) -> str:
        """One line of text. Handles 'b', 'q' and end of input for every other read method."""
        while True:
            try:
                text = input(prompt)
            except EOFError:
                print()
                raise EndOfInput() from None
            if self._echo_input:
                print(text)
            text = text.strip()
            if text.lower() == "b":
                raise GoBack()
            if text.lower() == "q":
                if self._confirm_quit():
                    raise QuitRequested()
                continue
            if not text and not allow_empty:
                self.print_error("Please type something (b = back, q = quit).")
                continue
            return text

    def read_text(self, prompt: str, allow_empty: bool = False) -> str:
        return self.read_line(prompt, allow_empty)

    def read_int(self, prompt: str, low: int, high: int, default: int | None = None) -> int:
        """A whole number from ``low`` to ``high``. Enter accepts ``default`` when one is given."""
        while True:
            text = self.read_line(prompt, allow_empty=default is not None)
            if not text and default is not None:
                return default
            if not text.lstrip("-").isdigit():
                self.print_error(f"'{text}' is not a number. Enter a number from {low} to {high}.")
                continue
            value = int(text)
            if low <= value <= high:
                return value
            self.print_error(f"{value} is out of range. Enter a number from {low} to {high}.")

    def read_choice(self, prompt: str, choices: list[str]) -> str:
        """One of the given option keys (case-insensitive)."""
        lowered = [choice.lower() for choice in choices]
        while True:
            text = self.read_line(prompt).lower()
            if text in lowered:
                return choices[lowered.index(text)]
            self.print_error(f"'{text}' is not an option. Choose one of: {', '.join(choices)}.")

    def read_yes_no(self, prompt: str) -> bool:
        while True:
            text = self.read_line(prompt).lower()
            if text in ("y", "yes"):
                return True
            if text in ("n", "no"):
                return False
            self.print_error("Please answer y or n.")

    def read_money(self, prompt: str) -> Money:
        """A positive rupee amount with at most 2 decimal places, e.g. 500 or 250.50."""
        while True:
            text = self.read_line(prompt).replace("₹", "").replace(",", "")
            try:
                amount = Decimal(text)
            except InvalidOperation:
                self.print_error(f"'{text}' is not an amount. Try something like 500 or 250.50.")
                continue
            if not amount.is_finite() or amount <= 0:
                self.print_error("The amount must be more than ₹0.")
                continue
            if amount.as_tuple().exponent < -2:
                self.print_error("Use at most 2 decimal places (paisa).")
                continue
            return Money(amount)

    def read_time(self, prompt: str) -> tuple[int, int]:
        """A 24-hour time like 23:30. Returns (hour, minute)."""
        while True:
            text = self.read_line(prompt)
            parts = text.split(":")
            if len(parts) == 2 and parts[0].isdigit() and parts[1].isdigit():
                hour, minute = int(parts[0]), int(parts[1])
                if 0 <= hour <= 23 and 0 <= minute <= 59:
                    return hour, minute
            self.print_error(f"'{text}' is not a time. Use 24-hour HH:MM, e.g. 23:30.")

    def read_place(self, prompt: str, places: list[tuple[Location, str]],
                   default: Location | None = None, show_list: bool = True) -> Location:
        """A place, typed as its list number or as part of its name ('tam' → Tambaram).

        ``places`` is a list of (location, note) — the note marks e.g. "(outside service area)".
        If several places match, the matches are listed and the user picks one.
        """
        if show_list:
            self._print_place_list(places)
        while True:
            text = self.read_line(prompt, allow_empty=default is not None)
            if not text and default is not None:
                return default
            if text.isdigit():
                number = int(text)
                if 1 <= number <= len(places):
                    return places[number - 1][0]
                self.print_error(f"There is no place number {number}. Use 1 to {len(places)}.")
                continue
            matches = [entry for entry in places if text.lower() in entry[0].name.lower()]
            exact = [entry for entry in matches if entry[0].name.lower() == text.lower()]
            if exact:
                return exact[0][0]
            if len(matches) == 1:
                return matches[0][0]
            if not matches:
                self.print_error(f"No place matches '{text}'. Type part of a name or a number from the list.")
                continue
            self.print_info(f"'{text}' matches {len(matches)} places:")
            for index, (location, note) in enumerate(matches, start=1):
                print(f"      {index}. {location.name} {note}".rstrip())
            chosen = self.read_int("    Which one? ", 1, len(matches))
            return matches[chosen - 1][0]

    def _print_place_list(self, places: list[tuple[Location, str]]) -> None:
        """Plain places in 3 columns; places with a note (e.g. outside the area) on their own line."""
        plain = [f"{index:>2}. {location.name}" for index, (location, note) in enumerate(places, start=1) if not note]
        noted = [f"{index:>2}. {location.name} {note}" for index, (location, note) in enumerate(places, start=1) if note]
        per_row = 3
        width = max(len(cell) for cell in plain) + 3
        for start in range(0, len(plain), per_row):
            print("    " + "".join(cell.ljust(width) for cell in plain[start:start + per_row]).rstrip())
        if noted:
            print("    " + "   ".join(noted))

    def _confirm_quit(self) -> bool:
        while True:
            try:
                text = input("    Quit the interactive mode? [y/n]: ")
            except EOFError:
                print()
                raise EndOfInput() from None
            if self._echo_input:
                print(text)
            if text.strip().lower() in ("y", "yes"):
                return True
            if text.strip().lower() in ("n", "no"):
                return False

    # ================================================================ printing
    def print_header(self, title: str) -> None:
        print()
        print("=" * ConsoleIO.WIDTH)
        print(f" {title}")
        print("=" * ConsoleIO.WIDTH)

    def print_status(self, line: str) -> None:
        print()
        print(line)

    def print_options(self, options: list[tuple[str, str]]) -> None:
        for key, label in options:
            print(f"  {key:>2}. {label}")

    def print_table(self, headers: list[str], rows: list[list[str]], align: str | None = None) -> None:
        """Print an aligned table. ``align`` has one letter per column: 'l' left, 'r' right."""
        if not rows:
            self.print_info("(nothing to show)")
            return
        align = align or "l" * len(headers)
        widths = [len(header) for header in headers]
        for row in rows:
            for index, cell in enumerate(row):
                widths[index] = max(widths[index], len(str(cell)))

        def format_row(cells: list[str]) -> str:
            parts = []
            for index, cell in enumerate(cells):
                text = str(cell)
                parts.append(text.rjust(widths[index]) if align[index] == "r" else text.ljust(widths[index]))
            return "  " + "  ".join(parts).rstrip()

        print(format_row(headers))
        print("  " + "-" * (sum(widths) + 2 * (len(widths) - 1)))
        for row in rows:
            print(format_row(row))

    def print_error(self, message: str) -> None:
        print(f"  ✘ {message}")

    def print_success(self, message: str) -> None:
        print(f"  ✔ {message}")

    def print_info(self, message: str) -> None:
        print(f"    {message}")
