"""Firestore adapter. Receipt creation and balance update are atomic."""

import hashlib
import json

from google.cloud import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from application.submit_flashcard_session import XpReceipt
from domain.flashcard_session import (
    FlashcardSession, InvalidXpAccount, OverlappingSession, SessionAlreadySubmitted, is_safe_integer,
)


class FirestoreXpLedger:
    def __init__(self, database: firestore.Client):
        self.database = database

    def award(self, uid: str, display_name: str, session: FlashcardSession, earned_xp: int) -> XpReceipt:
        ranking = self.database.collection("leaderboard").document(uid)
        receipts = self.database.collection("xpAccounts").document(uid).collection("sessions")
        receipt = receipts.document(session.session_id)
        canonical_payload = json.dumps(session.to_payload(), ensure_ascii=False, sort_keys=True, separators=(",", ":"))
        payload_hash = hashlib.sha256(canonical_payload.encode("utf-8")).hexdigest()

        @firestore.transactional
        def award_once(transaction: firestore.Transaction) -> XpReceipt:
            previous = receipt.get(transaction=transaction)
            profile = ranking.get(transaction=transaction)
            total_xp = profile.to_dict().get("totalXp") if profile.exists else 0
            if not is_safe_integer(total_xp) or total_xp < 0 or not is_safe_integer(total_xp + earned_xp):
                raise InvalidXpAccount("Invalid XP account")
            if previous.exists:
                if previous.get("payloadHash") != payload_hash:
                    raise SessionAlreadySubmitted("Session ID was already used with different data")
                return XpReceipt(previous.get("earnedXp"), int(total_xp), True)

            # Accepted intervals do not overlap; the closest earlier start is sufficient.
            nearest = list(receipts.where(filter=FieldFilter("startedAt", "<", session.ended_at))
                           .order_by("startedAt", direction=firestore.Query.DESCENDING)
                           .limit(1).stream(transaction=transaction))
            if nearest and nearest[0].get("endedAt") > session.started_at:
                raise OverlappingSession("Study sessions cannot overlap")
            transaction.create(receipt, {
                "payloadHash": payload_hash, "earnedXp": earned_xp, "reviewedCardCount": len(session.reviews),
                "startedAt": session.started_at, "endedAt": session.ended_at,
                "acceptedAt": firestore.SERVER_TIMESTAMP,
            })
            transaction.set(ranking, {
                "userId": uid, "displayName": display_name, "totalXp": int(total_xp) + earned_xp,
                "updatedAt": firestore.SERVER_TIMESTAMP,
            })
            return XpReceipt(earned_xp, int(total_xp) + earned_xp, False)

        return award_once(self.database.transaction())
