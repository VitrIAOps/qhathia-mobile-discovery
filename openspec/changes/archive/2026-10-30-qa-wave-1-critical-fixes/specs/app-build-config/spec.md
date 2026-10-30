## MODIFIED Requirements

### Requirement: QA / profiling build variant

The **`profile`** build variant SHALL be **non-debuggable** and **`profileable`** (so the Android Studio profiler attaches with accurate metrics), SHALL be **signed with the debug keystore** so QA can install it without production signing, and SHALL keep **network API request/response logging enabled** even though it is non-debuggable (via a logging flag decoupled from `BuildConfig.DEBUG`). Every build that logs network calls SHALL redact personal data and secrets from the logs: the `Authorization` header and the values of `customerName`, `customerPhone`, `phone`, `deviceAccessToken`, `accessToken`, `token`, `key` and `exitPinHash` SHALL be replaced by a mask. R8/minification SHALL be left off to match `release` behavior without keep-rule risk.

#### Scenario: Profiling build is installable and profileable

- **WHEN** the `profile` variant is assembled
- **THEN** the APK is signed (debug keystore) and installable on a QA device
- **AND** its manifest marks the app `profileable` and not `debuggable`

#### Scenario: API logs survive in the non-debuggable profiling build

- **WHEN** the app runs from the `profile` build and performs an API call
- **THEN** the request/response is logged (Ktor logging), despite the build being non-debuggable

#### Scenario: Customer data is masked in the logs

- **WHEN** the app sends an order or share request with a consumer name and cell-phone from a build that logs network calls
- **THEN** the logged request shows a mask instead of the name and the cell-phone

#### Scenario: Tokens are masked in the logs

- **WHEN** a logged request carries an `Authorization` header or a response carries a token field
- **THEN** the log shows a mask instead of the token value
