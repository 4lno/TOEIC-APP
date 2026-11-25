"""Decode Firebase callable integer wrappers before domain validation.

Android serializes Kotlin Long as a protobuf Int64Value JSON object. The Python
Functions SDK currently leaves that object in request.data rather than decoding it.
"""

import re


class InvalidCallablePayload(ValueError):
    pass


_INT64 = "type.googleapis.com/google.protobuf.Int64Value"
_UINT64 = "type.googleapis.com/google.protobuf.UInt64Value"


def decode_callable_data(value: object, _depth: int = 0) -> object:
    if _depth > 64:
        raise InvalidCallablePayload("Callable data is nested too deeply")
    if isinstance(value, list):
        return [decode_callable_data(item, _depth + 1) for item in value]
    if isinstance(value, dict):
        tag = value.get("@type")
        if tag in (_INT64, _UINT64):
            raw = value.get("value")
            if (value.keys() != {"@type", "value"} or not isinstance(raw, str)
                    or re.fullmatch(r"-?[0-9]{1,20}", raw) is None):
                raise InvalidCallablePayload("Invalid callable integer wrapper")
            number = int(raw)
            low, high = (-(2**63), 2**63 - 1) if tag == _INT64 else (0, 2**64 - 1)
            if not low <= number <= high:
                raise InvalidCallablePayload("Callable integer is outside its 64-bit range")
            return number
        # Unknown typed objects remain maps, as required by the callable protocol.
        return {key: decode_callable_data(item, _depth + 1) for key, item in value.items()}
    return value
