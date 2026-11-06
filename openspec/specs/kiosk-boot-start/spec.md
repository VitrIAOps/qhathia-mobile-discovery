# kiosk-boot-start Specification

## Purpose
The kiosk-boot-start capability brings the showcase back by itself after the tablet reboots (F-12): a boot receiver opens it on Android 9 and earlier, and on every version the app can be the device home screen, which Android opens after each boot. Device-owner provisioning makes it the home screen without manual setup (kiosk-lock-mode).
## Requirements
### Requirement: Showcase starts after a reboot

When a paired tablet finishes booting, the showcase SHALL come to the front by itself and request pinning, and the heartbeat SHALL start. On Android 9 or earlier the app SHALL open itself when the boot ends. On every version the app SHALL be selectable as the device home screen, so Android opens it after each boot. When the app is already the default home screen it SHALL NOT open itself a second time. An unpaired tablet SHALL NOT be opened by the boot.

#### Scenario: Paired tablet reboots (Android 9 or earlier)

- **WHEN** a paired tablet that does not use Discovery as its home screen finishes booting and is unlocked
- **THEN** the showcase comes to the front and asks to pin, and a heartbeat is sent within 30 seconds

#### Scenario: Discovery is the home screen

- **WHEN** Discovery was chosen as the home screen and the tablet finishes booting, on any Android version
- **THEN** Android opens the showcase, it asks to pin, and only one showcase screen is opened

#### Scenario: Unpaired tablet reboots

- **WHEN** an unpaired tablet that does not use Discovery as its home screen finishes booting
- **THEN** the app is not opened

#### Scenario: Home button with Discovery as home

- **WHEN** Discovery is the home screen and the Home button is pressed outside the app (for example after leaving kiosk mode)
- **THEN** the showcase comes back to the front

