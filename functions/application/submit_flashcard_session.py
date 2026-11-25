"""Use case and persistence port for awarding session XP."""

from dataclasses import dataclass
from typing import Protocol

from domain.flashcard_session import FlashcardSession, calculate_flashcard_xp, parse_session


@dataclass(frozen=True, slots=True)
class XpReceipt:
    earned_xp: int
    total_xp: int
    already_awarded: bool

    def to_payload(self) -> dict:
        return {"earnedXp": self.earned_xp, "totalXp": self.total_xp, "alreadyAwarded": self.already_awarded}


class XpLedger(Protocol):
    def award(self, uid: str, display_name: str, session: FlashcardSession, earned_xp: int) -> XpReceipt:
        ...


def submit_flashcard_session(ledger: XpLedger, uid: str, display_name: str, data: object, now: int) -> XpReceipt:
    session = parse_session(data, now)
    return ledger.award(uid, display_name.strip()[:80] or "Người học", session,
                        calculate_flashcard_xp(len(session.reviews)))
