package com.qhathia.mobile.discovery.core.domain.model

/**
 * Terminal/intermediate states pushed over the pairing-session channel while the tablet waits for the
 * merchant to approve or reject the code (api-contract.md §5).
 */
enum class PairingStatus {
    /** The merchant has not decided yet; keep showing the code. */
    Waiting,

    /** The merchant approved the code; the tablet may now exchange the session for a device token. */
    Approved,

    /** The merchant rejected the code; a new code must be generated to retry. */
    Rejected,

    /** The session/code expired before a decision; a new code must be generated. */
    Expired,
}
