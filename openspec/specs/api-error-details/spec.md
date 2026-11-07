# api-error-details Specification

## Purpose
How HTTP errors expose the backend's `problem+json` details (`errorCode`, `detail` and optional fields), so the app can react to a specific error code instead of a bare status.

## Requirements
### Requirement: HTTP errors carry the problem detail

Every non-2xx HTTP response returned to a repository SHALL carry, next to its status code, the fields the backend sends in its `application/problem+json` body: at least `errorCode` (also read from the legacy `code` key) and `detail`, plus the optional fields documented in api-contract §35–§39 (`productId`, `productName`, `reason`, `available`, `requested`, `changes`, `fieldErrors`). The body SHALL be parsed once, in the network layer, for every service the app calls.

#### Scenario: Problem body is parsed

- **WHEN** a call answers `409` with `{"errorCode":"PRICE_CHANGED","detail":"…","changes":[{"productId":"p1","productName":"QA X OK","expectedUnitPrice":210.0,"currentUnitPrice":199.0}]}`
- **THEN** the HTTP error result has code `409`, `errorCode` `PRICE_CHANGED`, the `detail` text, and one price change for `p1` with current unit price `199.0`

#### Scenario: Legacy code key

- **WHEN** a call answers an error body with `code` and no `errorCode`
- **THEN** the HTTP error result exposes that `code` as its `errorCode`

#### Scenario: Unreadable body

- **WHEN** a call answers an error with an empty body, a non-JSON body, or a body that cannot be read
- **THEN** the HTTP error result keeps its status code and has no problem detail, and no exception reaches the caller

### Requirement: One problem-detail parser

The app SHALL read `errorCode` and the other problem fields through a single shared parser in the network layer. The pairing token exchange SHALL use it too, and SHALL keep telling apart `PAIRING_NOT_APPROVED`, `PAIRING_REJECTED`, `PAIRING_TOKEN_EXPIRED` and `PAIRING_TOKEN_ALREADY_USED`.

#### Scenario: Pairing exchange keeps its error codes

- **WHEN** the token exchange answers `409` with `errorCode` `PAIRING_REJECTED`
- **THEN** pairing reports the rejection, exactly as before this change

