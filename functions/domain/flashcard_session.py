"""Flashcard session validation and XP rules, independent of Firebase."""

from dataclasses import dataclass
import math
import re

MAX_SAFE_INTEGER = 2**53 - 1


class InvalidSession(ValueError):
    pass


class SessionAlreadySubmitted(Exception):
    pass


class OverlappingSession(Exception):
    pass


class InvalidXpAccount(Exception):
    pass


@dataclass(frozen=True, slots=True)
class FlashcardReview:
    word_id: str
    rating: str
    reviewed_at: int

    def to_payload(self) -> dict:
        return {"wordId": self.word_id, "rating": self.rating, "reviewedAt": self.reviewed_at}


@dataclass(frozen=True, slots=True)
class FlashcardSession:
    session_id: str
    started_at: int
    ended_at: int
    active_duration_ms: int
    reviews: tuple[FlashcardReview, ...]

    def to_payload(self) -> dict:
        return {
            "sessionId": self.session_id,
            "startedAt": self.started_at,
            "endedAt": self.ended_at,
            "activeDurationMs": self.active_duration_ms,
            "reviews": [review.to_payload() for review in self.reviews],
        }


def is_safe_integer(value: object) -> bool:
    # bool subclasses int in Python but is not a valid JSON numeric field.
    return (
        type(value) in (int, float)
        and abs(value) <= MAX_SAFE_INTEGER
        and math.isfinite(value)
        and int(value) == value
    )


def parse_session(value: object, now: int) -> FlashcardSession:
    if not isinstance(value, dict):
        raise InvalidSession("Missing session")
    if value.keys() - {"sessionId", "startedAt", "endedAt", "activeDurationMs", "reviews"}:
        raise InvalidSession("Unexpected session fields")
    session_id = value.get("sessionId")
    if not isinstance(session_id, str) or re.fullmatch(r"[a-zA-Z0-9-]{1,128}", session_id) is None:
        raise InvalidSession("Invalid session ID")
    started_at, ended_at, active_ms = (value.get(key) for key in ("startedAt", "endedAt", "activeDurationMs"))
    if not all(is_safe_integer(item) for item in (started_at, ended_at, active_ms)):
        raise InvalidSession("Invalid session timing")
    if (started_at <= 0 or ended_at <= started_at or ended_at > now + 300_000
            or ended_at - started_at > 86_400_000 or active_ms < 0 or active_ms > ended_at - started_at):
        raise InvalidSession("Invalid session timing")
    items = value.get("reviews")
    if not isinstance(items, list) or not 1 <= len(items) <= 5000:
        raise InvalidSession("Expected between 1 and 5000 reviewed cards")
    ids = set()
    reviews = []
    for item in items:
        if not isinstance(item, dict):
            raise InvalidSession("Invalid review")
        word_id, rating, reviewed_at = (item.get(key) for key in ("wordId", "rating", "reviewedAt"))
        if (item.keys() - {"wordId", "rating", "reviewedAt"}
                or not isinstance(word_id, str) or not word_id.strip() or len(word_id) > 256
                or not isinstance(rating, str) or rating not in {"AGAIN", "HARD", "GOOD", "EASY"}
                or not is_safe_integer(reviewed_at) or not started_at <= reviewed_at <= ended_at
                or word_id in ids):
            raise InvalidSession("Invalid or duplicate review")
        ids.add(word_id)
        reviews.append(FlashcardReview(word_id, rating, int(reviewed_at)))
    # Normalize both review order and integer-like JSON numbers for retry fingerprints.
    return FlashcardSession(session_id, int(started_at), int(ended_at), int(active_ms),
                            tuple(sorted(reviews, key=lambda review: review.word_id)))


def calculate_flashcard_xp(reviewed_card_count: int) -> int:
    """Keep aligned with Android domain/usecase/CalculateFlashcardXp."""
    if not is_safe_integer(reviewed_card_count) or not 1 <= reviewed_card_count <= 5000:
        raise InvalidSession("Invalid reviewed card count")
    return int(reviewed_card_count) * 5
