# Morphe Truecaller Privacy & Debloat Patches

A modular bytecode patch bundle built on the [Morphe](https://github.com/MorpheApp) patcher framework targeting **Truecaller (v26.39.6)**. It surgically neutralizes background surveillance workers, suppresses telemetry and profiling, eliminates third-party ad networks, and removes UI bloat while keeping core Caller ID, contact cards, and spam protection intact.

---

## Features & Patch Modules

### 1. Privacy & Surveillance Neutralization
- **Surveillance Worker Killer (`TruecallerWorkerNeutralizePatch`)**:
  - Intercepts AndroidX WorkManager and `TrackedCoroutineWorker` execution.
  - Hooks `c(Lorc;)Ljava/lang/Object;` to return `Boolean.FALSE` so action payloads are never triggered.
  - Neutralizes 28 background workers including `PhonebookSyncWorker` (contact address book harvesting), `CallHistoryFullSyncWorker` (call log uploading), `InsightsOneOffEnrichmentWorker` (SMS transaction parsing), `BatteryStatusWorker`, `ActivityStatusWorker`, `PostPresenceWorker`, `PostLastSeenWorker`, `JointActionsWorker`, `StandaloneActionWorker`, `StartupActionsWorker`, and `BackupWorker`.
- **Telemetry Suppression (`TruecallerDisableTelemetryPatch`)**:
  - Disables technical session telemetry (`AppStartTracker`).
  - Empties batch log queue extraction (`BatchLogClient` / `Lljq;`).
  - Neutralizes cohort retention profiling and heartbeat pings (`Lucm;` / `RetentionTracker$RetentionPeriod`).
  - Neutralizes notification interaction profiling (`Lyi10;`) while preserving UI click pending intents.
  - Silences physical motion, screen unlock, and ringer mode surveillance broadcast receivers.
- **Storage & Local Telemetry Minimization (`TruecallerStoragePrivacyPatch`)**:
  - Hooks `AnalyticsRepository` (`Lv4i;`) and dispatchers (`Lbdo;`) to drop event insertions into `persisted_event` in `analytics.db`.
  - Drops profile view tracking into `profile_view_events` in `tc.db` (`Li0v;`).
  - Eliminates `SessionStorage.xml` lifecycle tracking (`Lb3i;` and `Ly2i;`).
  - Disables Google Firebase Crashlytics, DataTransport, AppMeasurement, and Firebase Heartbeat storage (`Lrcm;`).
- **Network Blackhole & GDPR Enforcement (`TruecallerNetworkBlackholePatch`)**:
  - Forces European GDPR privacy domain (`DOMAIN_REGION_1` / `eu`).
  - Blackholes all telemetry, analytics, survey, presence, and ad endpoints to local loopback (`127.0.0.1`) while allowing core Caller ID and spam definition endpoints.

### 2. Ad Removal & SDK Neutralization
- **21 Ad Networks & Tracking SDKs Neutralized (`TruecallerSdkNeutralizePatch`)**:
  - Google AdMob, Meta Audience Network, Unity Ads, Amazon Ads, Criteo, Truecaller AdRouter, Microsoft Clarity, CleverTap, AppsFlyer, Moloco, Huawei HMS Ads, Freshchat, InMobi, Appnext, Vungle, Fyber, Mintegral, IronSource, HyBid, and MLKit.
- **Ad UI Removal (`TruecallerAdRemovalPatch`)**:
  - Strips banner ads, native call card ads, and search result sponsored ads.
- **Promotional Neutralization (`TruecallerPromotionalNeutralizePatch`)**:
  - Suppresses Premium subscription paywalls, Gold gift banners, and promotional nudges.

### 3. Layout & Workflow Debloating
- **`TruecallerHideAssistantTabPatch`**: Removes the AI Assistant tab from the bottom navigation bar.
- **`TruecallerHideScamsTabPatch`**: Removes the Scams tab from navigation.
- **`TruecallerHideFamilyProtectionPatch`**: Hides the Family Protection upsell buttons.
- **`TruecallerBypassPermissionGatePatch`**: Allows full dialer and caller ID functionality without mandatory SMS, Call Log, or Contact upload permissions.
- **`TruecallerGmsSignInBypassPatch`**: Enables direct SMS OTP login without requiring Google Mobile Services sign-in.
- **`TruecallerDisableUpdateNagPatch`**: Disables in-app update checks and force-update dialogs.

---

## Repository Structure

```
morphe-truecaller-patch/
├── build.gradle.kts           # Root Gradle build script
├── settings.gradle.kts        # Module settings
├── gradle.properties          # Build properties
├── gradlew / gradlew.bat      # Gradle wrapper
├── local-repo/                # Local maven dependencies for Morphe patcher API
├── patches-list.json          # Generated patch metadata manifest
├── patches/                   # Kotlin patch source code
│   └── src/main/kotlin/app/morphe/patches/truecaller/
│       ├── TruecallerPrivacyMasterPatch.kt
│       ├── ads/
│       │   ├── TruecallerAdRemovalPatch.kt
│       │   ├── TruecallerPromotionalNeutralizePatch.kt
│       │   └── TruecallerSdkNeutralizePatch.kt
│       ├── layout/
│       │   ├── TruecallerHideAssistantTabPatch.kt
│       │   ├── TruecallerHideFamilyProtectionPatch.kt
│       │   └── TruecallerHideScamsTabPatch.kt
│       ├── misc/
│       │   ├── TruecallerDisableUpdateNagPatch.kt
│       │   └── TruecallerGmsSignInBypassPatch.kt
│       ├── privacy/
│       │   ├── TruecallerBypassPermissionGatePatch.kt
│       │   ├── TruecallerDisableTelemetryPatch.kt
│       │   ├── TruecallerNetworkBlackholePatch.kt
│       │   ├── TruecallerStoragePrivacyPatch.kt
│       │   └── TruecallerWorkerNeutralizePatch.kt
│       └── shared/
│           ├── Constants.kt
│           └── Fingerprints.kt
└── decompile/
    └── scripts/               # Binary auditing and reverse engineering scripts
```

---

## Building & Patching

### 1. Compile the Patch Bundle (`.mpp`)
```bash
./gradlew :patches:buildAndroid generatePatchesList --no-daemon
```
The compiled bundle will be output to:
`patches/build/libs/patches-1.13.1.mpp`

### 2. Apply Patches to Truecaller APK
```bash
java -jar morphe-desktop-1.15.1-dev.7-all.jar patch \
  --patches=patches/build/libs/patches-1.13.1.mpp \
  --force -o truecaller_patched.apk com.truecaller_26.39.6.apkm
```

### 3. Strip Meta Audience Network Assets (Optional)
```bash
zip -d truecaller_patched.apk "assets/audience_network/*" || true
```

### 4. Zipalign & Sign
```bash
zipalign -f -p 4 truecaller_patched.apk truecaller_aligned.apk
apksigner sign --ks debug.keystore --ks-pass pass:android --out truecaller_patched.apk truecaller_aligned.apk
```

---

## Forensic Audit & Verification

To verify that all telemetry, surveillance databases, background workers, and network sockets remain clean even when Truecaller is set as the default dialer with full permissions:

```bash
python3 decompile/scripts/dialer_audit.py
python3 decompile/scripts/comprehensive_audit.py
```
