import unittest
from unittest.mock import Mock

from application.submit_flashcard_session import XpReceipt, submit_flashcard_session
from data.callable_codec import InvalidCallablePayload, decode_callable_data
from domain.flashcard_session import InvalidSession
from test_flashcard_session import session


def wrapped(number, kind="Int64Value"):
    return {"@type": f"type.googleapis.com/google.protobuf.{kind}", "value": str(number)}


class CallableCodecTest(unittest.TestCase):
    def test_android_long_fields_are_decoded_before_domain_validation(self):
        original = session()
        android = original | {key: wrapped(original[key]) for key in ("startedAt", "endedAt", "activeDurationMs")}
        android["reviews"] = [review | {"reviewedAt": wrapped(review["reviewedAt"])} for review in original["reviews"]]
        decoded = decode_callable_data(android)
        self.assertEqual(original, decoded)
        ledger = Mock()
        ledger.award.return_value = XpReceipt(10, 10, False)
        self.assertEqual(submit_flashcard_session(ledger, "A", "An", decoded, 3000).earned_xp, 10)

    def test_signed_and_unsigned_boundaries_follow_callable_protocol(self):
        self.assertEqual(decode_callable_data(wrapped(-(2**63))), -(2**63))
        self.assertEqual(decode_callable_data(wrapped(2**63 - 1)), 2**63 - 1)
        self.assertEqual(decode_callable_data(wrapped(2**64 - 1, "UInt64Value")), 2**64 - 1)

    def test_malformed_and_out_of_range_wrappers_are_rejected(self):
        for payload in (wrapped("1.5"), wrapped(True), wrapped(2**63), wrapped(-1, "UInt64Value"),
                        wrapped(2**64, "UInt64Value"), wrapped(1000) | {"extra": 1},
                        {"@type": wrapped(0)["@type"], "value": 1000}):
            with self.subTest(payload=payload), self.assertRaises(InvalidCallablePayload):
                decode_callable_data(payload)

    def test_unknown_types_and_plain_values_are_preserved(self):
        payload = {"@type": "example.Unknown", "value": "1000"}
        self.assertEqual(decode_callable_data(payload), payload)
        self.assertEqual(decode_callable_data([None, True, "1000", 1000]), [None, True, "1000", 1000])

    def test_decoding_does_not_relax_domain_numeric_limits(self):
        payload = session() | {"startedAt": wrapped(2**53)}
        with self.assertRaises(InvalidSession):
            submit_flashcard_session(Mock(), "A", "An", decode_callable_data(payload), 3000)

    def test_excessive_nesting_is_rejected(self):
        nested = 1
        for _ in range(66):
            nested = [nested]
        with self.assertRaises(InvalidCallablePayload):
            decode_callable_data(nested)
