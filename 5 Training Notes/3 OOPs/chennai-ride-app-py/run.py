"""Entry point.

    python run.py           -> start-up menu (demo / interactive mode / self-check)
    python run.py --demo    -> the simulated day in Chennai
    python run.py --check   -> the self-check (exit code 1 if anything fails)
    python run.py --cli     -> interactive mode (also works with redirected input: --cli < session.txt)
"""

from __future__ import annotations

import sys

from ridehailing.exceptions import RideHailingError


def run_demo() -> int:
    from ridehailing.app.chennai_ride_app import ChennaiRideApp
    try:
        ChennaiRideApp().run()
    except RideHailingError as error:
        print(f"\nThe demo stopped on an unexpected business-rule error: {type(error).__name__}: {error}")
        return 1
    return 0


def main() -> int:
    # Windows consoles default to a legacy code page; force UTF-8 so ₹, ✔ and ✘ print correctly.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    if hasattr(sys.stdin, "reconfigure"):
        sys.stdin.reconfigure(encoding="utf-8")
    arguments = sys.argv[1:]
    if "--check" in arguments:
        from ridehailing.check.self_check import run_checks
        return run_checks()
    if "--demo" in arguments:
        return run_demo()
    from ridehailing.cli.startup_menu import launch
    return launch(start_with_menu="--cli" not in arguments)


if __name__ == "__main__":
    sys.exit(main())
