package com.qhathia.mobile.discovery.core.domain.model

/** Why exchanging a pairing session for the device token failed (api-contract.md §5). */
enum class PairingExchangeError {
    /** 409 `PAIRING_NOT_APPROVED`: the merchant has not approved the code yet. */
    NotApproved,

    /** 409 `PAIRING_REJECTED`: the merchant rejected the code. */
    Rejected,

    /** 410 `PAIRING_TOKEN_EXPIRED`: the session expired. */
    Expired,

    /**
     * 410 `PAIRING_TOKEN_ALREADY_USED`: the token was already handed out. Never retried; if this app
     * did not get it, the merchant must generate a new code.
     */
    AlreadyUsed,

    /** 404: the session does not exist. */
    NotFound,

    /** No answer from the server (offline, timeout). Already retried by the repository. */
    Connectivity,

    /** Any other failure. */
    Other,
}

/** Failure carried by `DevicePairingRepository.exchangeForDeviceToken`. */
class PairingExchangeException(val error: PairingExchangeError) :
    Exception("Pairing token exchange failed: $error")
