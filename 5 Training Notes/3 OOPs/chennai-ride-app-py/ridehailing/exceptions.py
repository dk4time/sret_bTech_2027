"""Custom exception hierarchy for the ride-hailing app.

OOP: every error inherits from ONE base class, RideHailingError. Callers can catch a specific
problem (``except InsufficientWalletBalanceError``) or every app problem at once
(``except RideHailingError``) — that is inheritance used for error handling.

The exceptions only receive plain values (names, ids, amounts) so this module never needs to
import the model classes.
"""

from __future__ import annotations


class RideHailingError(Exception):
    """Base class for every business-rule violation in the app."""


class InvalidRideStatusError(RideHailingError):
    """An action was attempted that the ride's current status does not allow."""

    def __init__(self, ride_id: str, current_status: str, action: str, hint: str = "") -> None:
        self.ride_id = ride_id
        self.current_status = current_status
        self.action = action
        message = f"Ride {ride_id} cannot {action} while it is {current_status}."
        if hint:
            message += f" {hint}"
        super().__init__(message)


class InvalidOtpError(RideHailingError):
    """The OTP entered by the captain did not match the customer's OTP."""

    def __init__(self, ride_id: str, attempts_left: int, ride_cancelled: bool) -> None:
        self.ride_id = ride_id
        self.attempts_left = attempts_left
        self.ride_cancelled = ride_cancelled
        if ride_cancelled:
            message = f"Wrong OTP for ride {ride_id}. No attempts left — ride auto-cancelled (OTP_FAILED)."
        else:
            message = f"Wrong OTP for ride {ride_id}. {attempts_left} attempt(s) left."
        super().__init__(message)


class NoCaptainAvailableError(RideHailingError):
    """No eligible captain accepted the request."""

    def __init__(self, vehicle_type: str, pickup: str, reason: str) -> None:
        self.vehicle_type = vehicle_type
        self.pickup = pickup
        self.reason = reason
        super().__init__(f"No captains available for {vehicle_type} near {pickup} ({reason}).")


class ActiveRideExistsError(RideHailingError):
    """The customer tried to book while another ride is still active."""

    def __init__(self, customer_name: str, ride_id: str, status: str) -> None:
        self.customer_name = customer_name
        self.ride_id = ride_id
        super().__init__(f"{customer_name} already has an active ride {ride_id} ({status}). "
                         "Only one ride at a time.")


class PaymentPendingError(RideHailingError):
    """The customer has an unpaid ride and must clear it before booking again."""

    def __init__(self, customer_name: str, ride_id: str, amount: str) -> None:
        self.customer_name = customer_name
        self.ride_id = ride_id
        self.amount = amount
        super().__init__(f"{customer_name} has a PAYMENT_PENDING ride {ride_id} of {amount}. "
                         "Please pay it before booking again.")


class InsufficientWalletBalanceError(RideHailingError):
    """The wallet does not hold enough money. Nothing is deducted."""

    def __init__(self, required: str, available: str) -> None:
        self.required = required
        self.available = available
        super().__init__(f"Insufficient wallet balance: required {required}, available {available}.")


class OutOfServiceAreaError(RideHailingError):
    """A pickup or drop point lies outside the Chennai service area."""

    def __init__(self, location_name: str, area_name: str) -> None:
        self.location_name = location_name
        self.area_name = area_name
        super().__init__(f"{location_name} is outside our {area_name} service area.")


class InvalidBookingError(RideHailingError):
    """The booking request itself is invalid (too short, too long, too many passengers...)."""


class CaptainNotEligibleError(RideHailingError):
    """The captain may not perform this action (KYC pending, wrong vehicle, blocked...)."""

    def __init__(self, captain_name: str, reason: str) -> None:
        self.captain_name = captain_name
        self.reason = reason
        super().__init__(f"Captain {captain_name}: {reason}")


class CaptainBusyError(RideHailingError):
    """The captain is on an active ride."""

    def __init__(self, captain_name: str, ride_id: str, action: str) -> None:
        self.captain_name = captain_name
        self.ride_id = ride_id
        super().__init__(f"Captain {captain_name} cannot {action} during active ride {ride_id}.")


class CaptainNotAtPickupError(RideHailingError):
    """mark_arrived() was called while the captain was still away from the pickup point."""

    def __init__(self, captain_name: str, distance_m: int) -> None:
        self.captain_name = captain_name
        self.distance_m = distance_m
        super().__init__(f"Captain {captain_name} is {distance_m} m from the pickup point "
                         "(must be within 100 m to mark arrived).")


class PaymentFailedError(RideHailingError):
    """A payment attempt failed (for example, the UPI bank server did not respond)."""

    def __init__(self, method: str, reason: str) -> None:
        self.method = method
        self.reason = reason
        super().__init__(f"{method} payment failed: {reason}")


class DuplicatePaymentError(RideHailingError):
    """A ride that is already paid can never be paid again."""

    def __init__(self, ride_id: str) -> None:
        self.ride_id = ride_id
        super().__init__(f"Ride {ride_id} is already paid. A payment can never succeed twice.")


class InvalidAmountError(RideHailingError):
    """A money amount is not acceptable (negative top-up, above the per-transaction limit...)."""


class InvalidRatingError(RideHailingError):
    """A rating is out of range, duplicated, or given for a ride that did not complete."""


class TimeTravelError(RideHailingError):
    """Someone asked the simulated clock (or a captain) to go back in time."""


class UnauthorizedAccessError(RideHailingError):
    """Someone other than the owner asked for protected data (such as a ride's OTP)."""
