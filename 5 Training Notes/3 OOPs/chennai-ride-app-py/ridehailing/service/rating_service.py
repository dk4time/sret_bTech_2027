"""RatingService — both sides rate each other after a COMPLETED ride."""

from __future__ import annotations

from ridehailing.exceptions import InvalidRatingError
from ridehailing.model.ride.ride import Ride
from ridehailing.model.user.captain import Captain


class RatingService:
    def rate_captain(self, ride: Ride, score: int) -> None:
        """The customer rates the captain. Completed rides only, once, 1–5 stars."""
        RatingService._validate_score(score)
        ride.rate_captain(score)          # the ride guards status and "only once"
        ride.captain.receive_rating(score)

    def rate_customer(self, ride: Ride, score: int) -> None:
        """The captain rates the customer."""
        RatingService._validate_score(score)
        ride.rate_customer(score)
        ride.customer.receive_rating(score)

    def leaderboard(self, captains: tuple[Captain, ...]) -> list[Captain]:
        """Rated captains, best average first (ties: more ratings first)."""
        rated = [captain for captain in captains if captain.rating_count > 0]
        rated.sort(key=lambda captain: (-captain.average_rating, -captain.rating_count, captain.name))
        return rated

    def flagged_captains(self, captains: tuple[Captain, ...]) -> list[Captain]:
        return [captain for captain in captains if captain.is_flagged]

    @staticmethod
    def _validate_score(score: int) -> None:
        if not isinstance(score, int) or isinstance(score, bool) or not 1 <= score <= 5:
            raise InvalidRatingError(f"Rating must be a whole number from 1 to 5, got {score!r}.")
