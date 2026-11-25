"""Firebase callable adapter; keep its public contract compatible with Android."""

import os
import time

from firebase_admin import auth, firestore, initialize_app
from firebase_functions import https_fn
from google.auth.credentials import AnonymousCredentials

from application.submit_flashcard_session import submit_flashcard_session
from data.firestore_xp_ledger import FirestoreXpLedger
from data.callable_codec import InvalidCallablePayload, decode_callable_data
from domain.flashcard_session import InvalidSession, InvalidXpAccount, OverlappingSession, SessionAlreadySubmitted

if (os.environ.get("FUNCTIONS_EMULATOR") == "true"
        and os.environ.get("FIRESTORE_EMULATOR_HOST")
        and os.environ.get("FIREBASE_AUTH_EMULATOR_HOST")):
    # Local-only credentials: all accessed services must be routed to emulators.
    initialize_app(credential=AnonymousCredentials())
else:
    initialize_app()


@https_fn.on_call(region="asia-southeast1", max_instances=5, timeout_sec=30, enforce_app_check=False)
def submitFlashcardSession(request: https_fn.CallableRequest) -> dict:
    if request.auth is None:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.UNAUTHENTICATED, "Sign in to submit a study session")
    uid = request.auth.uid
    # This is an ownership check, not an identity supplied by the caller.
    if not isinstance(request.data, dict) or request.data.get("ownerId") != uid:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.PERMISSION_DENIED,
                                 "Session owner does not match the signed-in user")
    try:
        user = auth.get_user(uid)
        if user.disabled:
            raise https_fn.HttpsError(https_fn.FunctionsErrorCode.PERMISSION_DENIED, "Account disabled")
        ledger = FirestoreXpLedger(firestore.client())
        return submit_flashcard_session(ledger, uid, user.display_name or "Người học",
                                        decode_callable_data(request.data.get("session")),
                                        time.time_ns() // 1_000_000).to_payload()
    except (InvalidSession, InvalidCallablePayload) as error:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.INVALID_ARGUMENT, str(error)) from error
    except SessionAlreadySubmitted as error:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.ALREADY_EXISTS, str(error)) from error
    except (OverlappingSession, InvalidXpAccount) as error:
        raise https_fn.HttpsError(https_fn.FunctionsErrorCode.FAILED_PRECONDITION, str(error)) from error
