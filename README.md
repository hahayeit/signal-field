# Signal Field

**Signal Field 1.2** is a standalone Android app for examining nearby Wi-Fi and Bluetooth Low Energy broadcasts, mapping observation locations, and keeping an exportable field log. It also includes an offline Monroe/Ouachita Parish police-radio reference and an evidence-based filter for possible law-enforcement equipment identifiers.

The app runs on Android 8.0 and later. It needs no account, backend, subscription, or API key. The included APK is a development-signed sideload build, not a Play Store release.

## Download and install

- [Download the Android APK](../../raw/refs/heads/main/SignalField.apk)
- [Download the source ZIP](../../raw/refs/heads/main/SignalField-source.zip)
- [Browse the source](source/)
- [APK SHA-256 checksum](SHA256SUMS.txt)

Download the APK to your Android device, open it, and allow installation from your browser or file manager when Android prompts. Version 1.2 uses the same signing certificate as the earlier Signal Field builds and can be installed as an update. Keep the existing app installed to preserve its local log.

Grant precise location while using the app and Nearby devices permission on Android 12+. Enable Android Location services and the relevant Wi-Fi/Bluetooth radio. Internet access is only needed for online map imagery and opening source links.

## Features

### Wi-Fi scanning

View network names, BSSIDs, RSSI, frequencies, and advertised security. Results are sorted by signal strength and can be filtered by name or address. Android may throttle scan requests; cached results are labeled and excluded from new logged observations.

### Bluetooth LE scanning

Listen for nearby advertisements in a 60-second foreground session. Inspect advertised names, addresses, signal strength, service UUIDs, manufacturer data, service data, and raw advertisement bytes. Stop manually or let the session finish automatically. Scanning and location updates stop when the app leaves the foreground.

### Possible law-enforcement equipment filter

In **BLE**, enable **Possible law-enforcement equipment only**, then tap **Start BLE**. The Police tab also offers a shortcut.

The matcher recognizes exact Bluetooth SIG assignments in their proper advertisement fields:

- Manufacturer company identifier `0x034D`: TASER International, Inc.
- Service UUID `0xFC81`: Axon Enterprise, Inc.
- Service UUIDs `0xFE6B` and `0xFE6C`: TASER International, Inc.

Service-data UUIDs are checked too. Device names, MAC prefixes, and arbitrary payload substrings are not used for classification. Every match displays its supporting identifier, signal strength, and last matching advertisement time. Results older than 30 seconds become stale; stopping the scan labels results as retained observations. A new session clears the session list.

**These are possible equipment matches, not confirmed police officers or vehicles.** Vendor identifiers may be spoofed or appear on equipment used outside law enforcement. Coverage is limited to these identifiers. No match does not mean no police are nearby. RSSI is not an exact distance measurement, and the app does not decode recording triggers or infer camera state.

### Observation map

Optional satellite imagery from Esri and street tiles from OpenStreetMap display the phone's location, its reported accuracy, and up to 500 recent geotagged observations. Pins represent where the phone heard a signal—not the transmitter's actual position. A location fix must be available when the observation is recorded; fixes older than 60 seconds are excluded.

### Local police-radio directory

The Police tab provides 23 offline reference entries for Monroe/Ouachita Parish, Louisiana: six police/sheriff entries, 12 shared Monroe LWIN site frequencies, two corrections channels, and three commercial channels whose police affiliation is unverified.

Search by agency, frequency, or talkgroup ID. Separate filters keep police/sheriff references distinct from shared radio infrastructure, corrections, and unverified commercial listings. Tap an entry to copy its identifier or inspect its source and provenance.

This directory does not receive VHF/UHF/LWIN radio audio or integrate an external receiver. Trunked-system site frequencies alone do not isolate police traffic; agencies are distinguished by talkgroup IDs. Encryption indications are retained without promising monitorable audio.

### Field log and export

Store up to 3,000 recent observations in private app storage and export JSON through Android's file picker. Matching evidence, rule version, source, and limitations accompany equipment observations. Repeated observations with the same address and evidence are logged at most once per 10 seconds; changed evidence can create a separate entry. Clear the saved log explicitly from the Log tab.

## Privacy and networking

There is no login, telemetry, advertising, camera access, or background tracking service. Android backup is disabled. Logs remain on the phone unless explicitly exported; exported files include broadcast addresses and any recorded phone locations.

Loading a map is optional and shares the device's IP address and viewed tile area with the selected imagery provider. Signal names and addresses are not uploaded to map providers. Uninstalling the app removes its private local data.

## Build from source

The native Android implementation uses Java and a bundled Leaflet map renderer. No Gradle or remote build service is required by the provided build script.

Requirements: PowerShell, a JDK (tested with JDK 21), Android SDK Build Tools 35.0.0, and Android SDK Platform 35. From the repository root:

```powershell
./source/build.ps1 -JdkHome 'C:/path/to/jdk' -BuildTools 'C:/path/to/sdk/build-tools/35.0.0' -AndroidJar 'C:/path/to/sdk/platforms/android-35/android.jar'
```

The script compiles Java, creates DEX, packages resources, aligns the APK, and signs it with a development key generated under `source/.build`. The original signing key is **not included in this repository**. A build signed with a newly generated key cannot update the supplied APK in place. Preserve your own signing key for future updates.

## Validation and current status

The included version is `versionCode 3` / `versionName 1.2`. It compiles successfully and passes APK v2/v3 signature verification and ZIP alignment checks. The Java checks cover:

- 20 equipment-matcher checks, including exact matches, namespace confusion, unrelated identifiers, duplicate evidence, and freshness boundaries.
- 14 scan-data checks, covering byte encoding, signal thresholds, and timestamp validity.
- 14 directory checks, covering category separation, searches, identifiers, and source-mode retention.

Map integration checks cover observation markers, phone accuracy, clearing markers, and treating broadcast names as text. Test fixtures are synthetic. Full Android-device installation/UI tests and tests against real Axon/TASER hardware have not been performed; this is a development build with those validation steps outstanding.

After building, run the tests from the repository root:

```powershell
& 'C:/path/to/jdk/bin/javac.exe' -encoding UTF-8 -cp source/.build/classes -d source/.build/test-classes source/tests/EquipmentMatcherTest.java source/tests/SignalRulesTest.java source/tests/PoliceDirectoryTest.java
& 'C:/path/to/jdk/bin/java.exe' -cp 'source/.build/classes;source/.build/test-classes' EquipmentMatcherTest
& 'C:/path/to/jdk/bin/java.exe' -cp 'source/.build/classes;source/.build/test-classes' SignalRulesTest
& 'C:/path/to/jdk/bin/java.exe' -cp 'source/.build/classes;source/.build/test-classes' PoliceDirectoryTest
node source/tests/map.test.cjs
```

## Sources and third-party notices

Identifier and directory sources were checked on November 4, 2026. Screenshot-derived conventional entries retain their provenance in the app; current on-air activity is not asserted.

- [Bluetooth SIG Assigned Numbers](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf)
- [RadioReference LWIN system](https://www.radioreference.com/db/sid/4347)
- [RadioReference Monroe LWIN site](https://www.radioreference.com/db/site/16177)
- [Android Wi-Fi scanning documentation](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- [Android Bluetooth permissions documentation](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

Leaflet 1.9.4 is bundled under the [BSD-2-Clause license](source/assets/LEAFLET-LICENSE.txt). Esri and OpenStreetMap attribution appears in the map. Android SDK and JDK tools are not distributed here. Signal Field is an independent project and is not the original SOPHIA software or an official Axon, TASER, or law-enforcement product.
