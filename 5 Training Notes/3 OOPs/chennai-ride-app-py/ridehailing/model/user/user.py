"""User — the abstract parent of Customer and Captain."""

from __future__ import annotations

import re
from abc import ABC, abstractmethod
from decimal import Decimal, ROUND_HALF_UP

from ridehailing.exceptions import InvalidRatingError
from ridehailing.model.common.location import Location


class User(ABC):
    """Anyone with an account in the app.

    OOP concepts shown here:
    * Abstraction: ``User`` is abstract (``role`` and ``id_prefix`` are abstract), so
      ``User(...)`` raises TypeError.
    * Hierarchical inheritance: Customer and Captain both inherit from User.
    * Class attribute vs instance attribute: ``_id_counter`` is ONE number shared by all users;
      ``_name`` belongs to each object separately.
    * Entity equality: users are compared by their id, not by their name.
    """

    # Class attributes
    _id_counter = 0
    _PHONE_PATTERN = re.compile(r"^\+91[6-9]\d{9}$")

    def __init__(self, name: str, phone: str, location: Location) -> None:
        if not name or not name.strip():
            raise ValueError("A user needs a name.")
        compact_phone = phone.replace(" ", "")
        if not User._PHONE_PATTERN.match(compact_phone):
            raise ValueError(f"'{phone}' is not a valid Indian mobile number (+91 followed by 10 digits).")

        User._id_counter += 1              # class attribute: shared counter across ALL users
        self._user_id = f"{self.id_prefix}-{User._id_counter:03d}"   # instance attribute
        self._name = name.strip()
        self._phone = phone
        self._location = location          # _protected: subclasses may change it, outsiders may not
        self._ratings: list[int] = []

    # ----- abstract members -----
    @property
    @abstractmethod
    def role(self) -> str:
        """'Customer' or 'Captain'."""

    @property
    @abstractmethod
    def id_prefix(self) -> str:
        """Prefix for the generated id, e.g. 'CUS' or 'CAP'."""

    # ----- read-only properties (getters, no setters) -----
    @property
    def user_id(self) -> str:
        return self._user_id

    @property
    def name(self) -> str:
        return self._name

    @property
    def phone(self) -> str:
        return self._phone

    @property
    def location(self) -> Location:
        return self._location

    @property
    def rating_count(self) -> int:
        return len(self._ratings)

    @property
    def average_rating(self) -> Decimal:
        """Average of all ratings received, 2 decimals. 0.00 when there are none."""
        if not self._ratings:
            return Decimal("0.00")
        average = Decimal(sum(self._ratings)) / Decimal(len(self._ratings))
        return average.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    # ----- behaviour -----
    def receive_rating(self, score: int) -> None:
        """Record a 1–5 star rating from the other side of a completed ride."""
        if not isinstance(score, int) or isinstance(score, bool) or not 1 <= score <= 5:
            raise InvalidRatingError(f"Rating must be a whole number from 1 to 5, got {score!r}.")
        self._ratings.append(score)

    # ----- entity equality: by id -----
    def __eq__(self, other: object) -> bool:
        if not isinstance(other, User):
            return NotImplemented
        return self._user_id == other._user_id

    def __hash__(self) -> int:
        return hash(self._user_id)

    def __str__(self) -> str:
        return self._name

    def __repr__(self) -> str:
        return f"{type(self).__name__}(id='{self._user_id}', name='{self._name}')"
