# Signal Field 1.2

A standalone Android app inspired by the Wi-Fi, BLE and satellite-map dashboard in the supplied SOPHIA video. This is an independently built app, not the original SOPHIA software.

## Install

Requires Android 8.0 or later. Transfer `SignalField.apk` to your phone, open it, and allow installation from the browser/file manager when Android asks. This APK is signed with a development certificate for sideloading; it is not a Play Store release.

Open Signal Field, select Wi-Fi or BLE and start a scan. Grant precise location while using the app, plus Nearby devices on Android 12+. Turn on Android Location services, Wi-Fi and/or Bluetooth. BLE runs for 60 seconds or until stopped. Leaving the app stops BLE and location updates; returning does not automatically restart them.

## Features

- Wi-Fi: SSID, BSSID, RSSI, frequency and advertised security; strongest-first list and name/address filter.
- Bluetooth LE: broadcast name, address, RSSI, service UUIDs, manufacturer data and raw advertisement hex. Tap a card for details.
- Satellite / street map: Esri and OpenStreetMap imagery with attribution, phone position and accuracy circle, and up to 500 recent geotagged observations. Tap Enable / refresh my location to acquire a GPS fix. A fresh fix must be available when a signal is recorded for it to receive a map pin.
- Private local field log: latest 3,000 observations; repeated devices logged at most once per 10 seconds. JSON export through Android's file picker; explicit clear-log action.
- No login, advertising, telemetry, background service, camera access or sample detections.

## Limits and data handling

Device types and ownership are unverified. A Wi-Fi or Bluetooth signal is not proof of a Flock camera or any other camera. The app does not claim the video's camera-identification or range capabilities, locate hidden devices, read camera feeds, or triangulate transmitter positions. RSSI categories describe signal strength, not distance.

Map pins are **the phone's observation location**, not the transmitter's position. GPS fixes older than 60 seconds are excluded. Wi-Fi records older than 15 seconds, or returned without a confirmed fresh scan, are labeled cached and excluded from new logged observations. Android can throttle or reject Wi-Fi scan requests. Some devices do not advertise, and randomized addresses may appear as different devices.

Scan logs remain in app-private storage, with Android backup disabled. They persist on normal app backgrounding and are erased on uninstall or Clear field log. Exports include broadcast identifiers and recorded phone locations. Loading maps is optional and shares your IP address and viewed tile area with Esri or OpenStreetMap; signal names and addresses stay local. Internet is needed for map tiles, not for radio scanning.

## Build on Windows

No Gradle or remote build service is required. Install a JDK (tested with 21), official Android SDK Build Tools 35.0.0, and Platform 35. Run PowerShell:

```powershell
./build.ps1 -JdkHome 'C:/path/to/jdk' -BuildTools 'C:/path/to/sdk/build-tools/35.0.0' -AndroidJar 'C:/path/to/sdk/platforms/android-35/android.jar'
```

The script compiles Java, creates DEX, packages resources and assets, aligns the APK, and generates/uses a local development signing key under `.build`. Preserve that key for future updates to your own builds. A newly generated key will not update an installation signed with another key; uninstalling first clears app data. Production publishing requires your own protected release signing key.

Leaflet 1.9.4 is bundled under its BSD-2-Clause license; see `assets/LEAFLET-LICENSE.txt`. Android SDK/JDK tools are not included.

## Validation

The supplied APK compiled successfully, passed Android APK v2/v3 signature verification and ZIP alignment validation. Fourteen Java checks cover signal thresholds, advertisement byte encoding, and freshness boundaries; map integration checks cover observation pins, phone accuracy, marker clearing and safe treatment of broadcast names.

No Android device or emulator was available for a full installation, visual or hardware scan test. Permission dialogs, GPS, real radio scans, online tile loading and lifecycle behavior still need validation on your phone.

Run the logic and map tests after building:

```powershell
& 'C:/path/to/jdk/bin/javac.exe' -cp .build/classes -d .build/test-classes tests/SignalRulesTest.java
& 'C:/path/to/jdk/bin/java.exe' -cp '.build/classes;.build/test-classes' SignalRulesTest
node tests/map.test.cjs
```

Android behavior references:
- https://developer.android.com/develop/connectivity/wifi/wifi-scan
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

## Version 1.1: Police reference

The Police tab defaults to Police / sheriff for Monroe and Ouachita Parish, Louisiana. Search by agency, frequency or TGID. Filters separate shared LWIN site channels, corrections and unverified commercial channels. Tap an entry to copy its value or view its source. This is a starter directory, not a complete local radio inventory.

There are 23 entries: six police/sheriff references (four dispatch talkgroups and two conventional sheriff channels), 12 shared Monroe LWIN site frequencies, two corrections channels and three commercial LTR channels with no established police affiliation. Nonlocal LWIN sites in the screenshots were not included in the local directory. Source metadata is available per entry, including screenshot provenance and dates.

Conventional frequencies come from the supplied screenshots. LWIN dispatch talkgroups and the Monroe site were cross-checked against RadioReference on November 4, 2026:
- https://www.radioreference.com/db/sid/4347
- https://www.radioreference.com/db/site/16177

The phone cannot tune these VHF/UHF/700 MHz channels using Wi-Fi or Bluetooth. This feature filters reference records only; it does not detect nearby police, filter live Wi-Fi/BLE observations as police, provide radio audio, or integrate an external receiver. Shared site frequencies do not identify a particular agency; trunked radio users are distinguished by talkgroup IDs. Source mode D enc for Monroe patrol dispatch is retained as an encryption indication, with no promise of monitorable audio.

This update uses versionCode 2 and the same development certificate as the original APK, permitting an in-place update without intentionally clearing the log. Directory tests cover category separation, searches, channel counts and TGID display. Full Android-device UI and installation testing remains outstanding.

## Version 1.2: Possible law-enforcement equipment filter

Open **BLE**, enable **Possible law-enforcement equipment only**, and tap **Start BLE**. The Police tab also has a shortcut into this view. It uses the existing 60-second foreground BLE scan; it does not require extra permissions or a background service.

The initial rules match structured advertised identifiers assigned by Bluetooth SIG:
- Manufacturer-specific company ID `0x034D`: TASER International, Inc.
- Service UUID `0xFC81`: Axon Enterprise, Inc.
- Service UUIDs `0xFE6B` and `0xFE6C`: TASER International, Inc.

Service UUIDs in service-data fields are also recognized. Android parses the advertisement fields; the matcher checks complete Bluetooth-base UUIDs and exact company IDs in their respective namespaces. It does not search arbitrary payload substrings, device names or MAC address prefixes. These are vendor identifier matches, not validated device-model fingerprints, trigger decoders or police-vehicle identification.

Source: Bluetooth SIG Assigned Numbers, checked November 4, 2026:
https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf

Each result displays its exact matching evidence, RSSI and last matching advertisement time. Matching results have their own session list so subsequent advertisements lacking the identifier do not erase the earlier observation or falsely refresh its timestamp. A result is recent only within 30 seconds of its matching advertisement while scanning; older results are labeled stale, and stopped scans explicitly show retained matches. Starting a new scan clears the session results. The separate field log persists.

No match means only that no supported identifier was observed; it does not establish that no police are nearby. Identifiers can be spoofed, may occur on equipment used outside law enforcement, and may not be advertised on all equipment/firmware. The app does not identify a vehicle or operator, infer exact range, or assert camera recording state. No simulated matches or all-clear indicators are included.

Matching evidence, rule version, source and limitations are included in logged observations and JSON exports. A change in matching evidence is logged separately so the first match is not lost to a preceding unmatched observation's 10-second log cooldown. Otherwise repeated observations of the same address and evidence are limited to once per 10 seconds. Saved logs are historical records, not current-presence indicators.

Twenty matcher tests cover namespace confusion, unrelated identifiers, exact UUID matching, duplicate evidence, unknown fields and monotonic-time freshness. Test fixtures are synthetic; no real Axon/TASER hardware or Android-device end-to-end test has been performed. The APK is versionCode 3 / versionName 1.2, signed with the original development certificate for an in-place update.
