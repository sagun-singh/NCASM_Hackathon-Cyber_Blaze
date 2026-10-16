# App Risk Auditor — PS-01 On-Device App Risk Auditor

**Team: Cyber Blaze | NCASM Hackathon**

An enterprise-grade Android app that audits every installed app and answers:
- **Q1** Which apps can do the most harm? *(sorted by risk score)*
- **Q2** Why is each app flagged? *(3 plain-English reasons)*
- **Q3** What can I do now? *(Open Settings + Uninstall buttons)*

---

## Architecture

| Person | Role | Package | Branch |
|--------|------|---------|--------|
| 1 | Scanner | `scanner/` | `feature/person1-scanner` |
| 2 | Brain (scoring) | `scoring/` | `feature/person2-scoring` |
| 3 | Special access + actions | `specialaccess/`, `actions/` | `feature/person3-special-access` |
| 4 | UI, rubric, demo, glue | `ui/`, `docs/` | `feature/person4-ui` |

---

## Hard Rules (enforced in code)

- ❌ No `INTERNET` permission in manifest
- ❌ No network calls, analytics, or cloud SDKs
- ❌ No root, no hidden APIs, no reflection on framework internals
- ✅ `allowBackup="false"` — enterprise data security
- ✅ `QUERY_ALL_PACKAGES` only — required for security audit tools
- ✅ Works fully offline
- ✅ `minSdk 26` (Android 8.0+)

---

## Key Features

- **Risk Score 0–100** in 4 bands: LOW / MEDIUM / HIGH / CRITICAL
- **9 Permission Rooms** (Location, Microphone, Camera, Contacts, Storage, Phone, Messages, Sensors, Other)
- **Special Access detection**: Accessibility, Device Admin, Notification Listener, Overlay, Install-Apps, Usage Stats
- **Spy Card™ persona** — unique differentiator ("The Letter Opener", etc.)
- **Phone Safety Grade** + "3 Quick Wins" device summary
- **Installer tracking** — Play Store vs sideloaded

---

## Build

```bash
# Clone and open in Android Studio, or:
./gradlew assembleDebug    # build APK
./gradlew :app:test        # run unit tests
```

Requires Android SDK (API 34) and Gradle 8.5+.

---

## Shared Contract

All team members import from `contract/Contract.kt` and `contract/Constants.kt`.  
**Never re-type constant strings; always import from `Constants`.**

### Glue (Person 4, minute 40):
```kotlin
val apps    = scanApps(ctx, includeSystem)
val special = findSpecialAccess(ctx)
apps.forEach { it.special = special[it.pkg] ?: emptySet() }
val results = apps.map { it to scoreApp(it) }
```

---

## Person 1 — Scanner (this branch)

**Files owned:**
- [`scanner/AppScanner.kt`](app/src/main/java/com/enterprise/riskauditor/scanner/AppScanner.kt)
- [`scanner/PermissionLevelResolver.kt`](app/src/main/java/com/enterprise/riskauditor/scanner/PermissionLevelResolver.kt)
- [`contract/Contract.kt`](app/src/main/java/com/enterprise/riskauditor/contract/Contract.kt) *(shared read-only)*
- [`contract/Constants.kt`](app/src/main/java/com/enterprise/riskauditor/contract/Constants.kt) *(shared read-only)*

**Public API:**
```kotlin
AppScanner.scanApps(ctx: Context, includeSystem: Boolean = false): List<AppInfo>
AppScanner.scanAppsSuspend(ctx, includeSystem)   // suspend, Dispatchers.Default
AppScanner.loadAppIcon(ctx, pkg): Drawable?       // null-safe, never throws
AppScanner.fakeApps(): List<AppInfo>              // 4 fake apps for P2/P4 dev
```

**Unit tests:** `AppScannerTest.kt` — 18 pure-JVM tests, zero Android dependencies.

---

*Nothing leaves the device. No telemetry. No cloud. Pure on-device security.*
