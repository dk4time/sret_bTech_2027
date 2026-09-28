"""Money — an immutable value object for Indian Rupees."""

from __future__ import annotations

from decimal import Decimal, ROUND_HALF_UP


class Money:
    """An amount of INR, always stored as a Decimal with exactly 2 decimal places (paisa).

    OOP concepts shown here:
    * Immutability: there are no setters, and ``__setattr__`` refuses every change after creation.
    * Operator overloading: ``fare + gst``, ``fare - discount``, ``rate * km``, ``a < b`` ...
    * Value equality: two Money objects are equal when their amounts are equal, and they hash the
      same, so Money works as a dict key and inside a set.
    """

    # Class attributes: shared by every Money object, not copied into each instance.
    CURRENCY_SYMBOL = "₹"
    _PAISA = Decimal("0.01")

    def __init__(self, amount: Decimal | int | str) -> None:
        # Floats are rejected on purpose: 0.1 + 0.2 != 0.3 in binary floating point.
        if isinstance(amount, float):
            raise TypeError("Money never accepts float. Use Decimal, int or a string like '12.50'.")
        if isinstance(amount, bool) or not isinstance(amount, (Decimal, int, str)):
            raise TypeError(f"Money needs Decimal, int or str, got {type(amount).__name__}.")
        rounded = Decimal(amount).quantize(Money._PAISA, rounding=ROUND_HALF_UP)
        # Immutability: we bypass our own __setattr__ exactly once, inside the constructor.
        object.__setattr__(self, "_amount", rounded)

    # ----- alternative constructor (class-level member) -----
    @classmethod
    def zero(cls) -> Money:
        """@classmethod alternative constructor: ``Money.zero()`` reads better than ``Money(0)``."""
        return cls(0)

    # ----- immutability -----
    def __setattr__(self, name: str, value: object) -> None:
        raise AttributeError(f"Money is immutable — cannot set '{name}'.")

    def __delattr__(self, name: str) -> None:
        raise AttributeError(f"Money is immutable — cannot delete '{name}'.")

    # ----- read-only property (getter, no setter) -----
    @property
    def amount(self) -> Decimal:
        return self._amount

    # ----- operator overloading -----
    def __add__(self, other: Money) -> Money:
        self._require_money(other, "+")
        return Money(self._amount + other._amount)

    def __sub__(self, other: Money) -> Money:
        self._require_money(other, "-")
        return Money(self._amount - other._amount)

    def __mul__(self, factor: Decimal | int) -> Money:
        """Money * number (for example rate-per-km * distance). Money * Money makes no sense."""
        if isinstance(factor, (float, bool)) or not isinstance(factor, (Decimal, int)):
            raise TypeError("Money can only be multiplied by a Decimal or an int.")
        return Money(self._amount * factor)

    def __rmul__(self, factor: Decimal | int) -> Money:
        return self.__mul__(factor)

    def __neg__(self) -> Money:
        return Money(-self._amount)

    def __lt__(self, other: Money) -> bool:
        self._require_money(other, "<")
        return self._amount < other._amount

    def __le__(self, other: Money) -> bool:
        self._require_money(other, "<=")
        return self._amount <= other._amount

    def __gt__(self, other: Money) -> bool:
        self._require_money(other, ">")
        return self._amount > other._amount

    def __ge__(self, other: Money) -> bool:
        self._require_money(other, ">=")
        return self._amount >= other._amount

    # ----- value equality: compare by VALUE, hash by VALUE -----
    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Money):
            return NotImplemented
        return self._amount == other._amount

    def __hash__(self) -> int:
        return hash(self._amount)

    # ----- helpers -----
    def percent(self, rate: Decimal) -> Money:
        """Return ``rate`` fraction of this amount, e.g. ``fare.percent(Decimal('0.05'))`` for 5% GST."""
        return self * rate

    def is_zero(self) -> bool:
        return self._amount == 0

    def is_negative(self) -> bool:
        return self._amount < 0

    @staticmethod
    def total(amounts: list[Money]) -> Money:
        """Add up a list of Money objects (sum() would start from the int 0)."""
        result = Money.zero()
        for amount in amounts:
            result = result + amount
        return result

    @staticmethod
    def _require_money(other: object, operator: str) -> None:
        if not isinstance(other, Money):
            raise TypeError(f"Cannot use '{operator}' between Money and {type(other).__name__}.")

    # ----- dunder string methods -----
    def __str__(self) -> str:
        """User-friendly: ₹1,234.50"""
        sign = "-" if self._amount < 0 else ""
        return f"{sign}{Money.CURRENCY_SYMBOL}{abs(self._amount):,.2f}"

    def __repr__(self) -> str:
        """Developer-friendly: Money('1234.50')"""
        return f"Money('{self._amount}')"
