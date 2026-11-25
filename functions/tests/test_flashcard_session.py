from copy import deepcopy
import unittest
from unittest.mock import Mock

from application.submit_flashcard_session import XpReceipt, submit_flashcard_session
from domain.flashcard_session import InvalidSession, calculate_flashcard_xp, is_safe_integer, parse_session


def session() -> dict:
    return {
        "sessionId": "session-1", "startedAt": 1000, "endedAt": 2000, "activeDurationMs": 1000,
        "reviews": [
            {"wordId": "book", "rating": "AGAIN", "reviewedAt": 1500},
            {"wordId": "phone", "rating": "HARD", "reviewedAt": 2000},
        ],
    }


class FlashcardSessionTest(unittest.TestCase):
    def test_server_calculates_xp_independently_of_ratings(self):
        ledger = Mock()
        ledger.award.return_value = XpReceipt(10, 10, False)
        result = submit_flashcard_session(ledger, "A", "  An  ", session(), 3000)
        uid, name, parsed, earned_xp = ledger.award.call_args.args
        self.assertEqual((uid, name, earned_xp), ("A", "An", 10))
        self.assertEqual([review.rating for review in parsed.reviews], ["AGAIN", "HARD"])
        self.assertEqual(result.to_payload(), {"earnedXp": 10, "totalXp": 10, "alreadyAwarded": False})

    def test_rejects_caller_supplied_xp_and_user_id(self):
        for extra in ({"earnedXp": 9999}, {"userId": "other"}, {"completed": True}):
            with self.subTest(extra=extra), self.assertRaises(InvalidSession):
                parse_session(session() | extra, 3000)

    def test_duplicate_words_and_malformed_ratings_do_not_inflate_xp(self):
        duplicate = session()
        duplicate["reviews"][1]["wordId"] = "book"
        invalid_rating = session()
        invalid_rating["reviews"][0]["rating"] = "PERFECT"
        for data in (duplicate, invalid_rating, session() | {"reviews": []}):
            with self.subTest(data=data), self.assertRaises(InvalidSession):
                parse_session(data, 3000)

    def test_invalid_timing_is_rejected(self):
        for patch in (
            {"endedAt": 999}, {"endedAt": 999999}, {"activeDurationMs": -1},
            {"activeDurationMs": 1001}, {"startedAt": 1.5}, {"startedAt": True},
            {"reviews": [{"wordId": "book", "rating": "GOOD", "reviewedAt": 999}]},
        ):
            with self.subTest(patch=patch), self.assertRaises(InvalidSession):
                parse_session(session() | patch, 3000)

    def test_review_order_and_integer_like_numbers_are_normalized(self):
        first = session()
        other = deepcopy(first)
        other["reviews"].reverse()
        other["startedAt"] = 1000.0
        self.assertEqual(parse_session(first, 3000), parse_session(other, 3000))
        self.assertEqual(parse_session(first, 3000).to_payload(), first)

    def test_invalid_payload_never_reaches_ledger(self):
        ledger = Mock()
        with self.assertRaises(InvalidSession):
            submit_flashcard_session(ledger, "A", "An", {}, 3000)
        ledger.award.assert_not_called()

    def test_numeric_validation_rejects_python_boolean_and_unsafe_json_integers(self):
        for value in (True, False, "1000", float("inf"), float("nan"), 2**53, None):
            with self.subTest(value=value):
                self.assertFalse(is_safe_integer(value))
        self.assertTrue(is_safe_integer(1000.0))

    def test_xp_boundaries_and_rating_independent_amount(self):
        self.assertEqual(calculate_flashcard_xp(1), 5)
        self.assertEqual(calculate_flashcard_xp(5000), 25000)
        for count in (0, -1, 5001, True, 1.5):
            with self.subTest(count=count), self.assertRaises(InvalidSession):
                calculate_flashcard_xp(count)

    def test_display_name_is_trimmed_bounded_and_has_fallback(self):
        ledger = Mock()
        submit_flashcard_session(ledger, "A", "  ", session(), 3000)
        self.assertEqual(ledger.award.call_args.args[1], "Người học")
        submit_flashcard_session(ledger, "A", "a" * 100, session(), 3000)
        self.assertEqual(len(ledger.award.call_args.args[1]), 80)


if __name__ == "__main__":
    unittest.main()
