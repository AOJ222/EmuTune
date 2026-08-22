# EmuTune — Package Visibility & Installed-App Detection

**App context:** EmuTune is a gaming-handheld-first emulator optimisation app.
Kotlin, Compose + Material 3, `minSdk 29` (Android 10), `targetSdk 36`
(Android 16 / "Baklava").

This document records how Android 11+ limits an app's ability to see other
installed apps, and the concrete strategy EmuTune will use to detect known
emulators without violating Play policy.

| Classification | Meaning |
| --- | --- |
| **CONFIRMED** | Stated directly in official Android documentation / API reference. |
| **LIKELY** | Strong evidence from official sources, with version nuance or caveat. |
| **UNKNOWN** | Not reliably determinable at runtime. |
| **NOT_SUPPORTED** | No public Android API exists for this capability. |

---

## 1. Package visibility filtering (Android 11+ / API 30+)

- **CONFIRMED.** When an app targets Android 11 (API 30) or higher, the system
  **filters** other apps out of the results of package queries by default. This
  affects `getPackageInfo()`, `getInstalledApplications()`,
  `queryIntentActivities()`, and related `PackageManager` methods, and also
  explicit interactions such as starting another app's service.
  - [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)

- **CONFIRMED.** Some packages are **visible automatically** (for example: the
  app itself, packages providing components the app explicitly interacts with,
  and system packages in certain cases). Everything else must be declared via the
  `<queries>` manifest element or obtained via `QUERY_ALL_PACKAGES`.
  - [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)

- **CONFIRMED.** Google Play treats the list of installed apps as **personal and
  sensitive user data**, which is why this filtering exists and why broad
  visibility is heavily restricted.
  - [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)

---

## 2. The `<queries>` manifest element

- **CONFIRMED.** `<queries>` is the privacy-preserving way to declare which
  *other* apps your app needs to see. It supports three declaration forms:
  1. **By package name** — `<package android:name="org.example.emu" />`
  2. **By intent signature** — `<intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent>` (or a custom action)
  3. **By provider authority** — `<provider android:authorities="org.example.emu.provider" />`
  - [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)
  - [`<queries>` manifest element reference](https://developer.android.com/guide/topics/manifest/queries-element)

- **CONFIRMED.** Declaring a `<package>` entry makes that package (and its
  components) appear in `PackageManager` results; an `<intent>` entry makes any
  app exposing a matching `<intent-filter>` visible; a `<provider>` entry makes
  apps exposing a matching `<provider>` visible.
  - [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)

- **LIKELY / build-tooling note.** `<queries>` requires a recent Android Gradle
  Plugin; with a `targetSdk 36` project on current AGP this is satisfied by
  default.

---

## 3. `QUERY_ALL_PACKAGES` — why EmuTune must not use it

- **CONFIRMED.** `QUERY_ALL_PACKAGES` removes the filter and lets the app see
  **all** installed apps. It only takes effect when targeting API 30+ on
  Android 11+.
  - [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)

- **CONFIRMED — Play policy.** Google Play permits `QUERY_ALL_PACKAGES` only for
  apps whose **core user-facing functionality** requires broad visibility, and
  only these permitted use classes: **device search, antivirus, file managers,
  browsers** (and a few tightly scoped equivalents). The developer must:
  - submit the **Permissions Declaration Form** in Play Console,
  - prove a less intrusive (targeted `<queries>`) method is insufficient,
  - never sell/share the inventory for ads or analytics.
  - [Use of the broad package (App) visibility (QUERY_ALL_PACKAGES) permission — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10158779)
  - [Permissions and APIs that access sensitive information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)

- **Verdict: NOT_SUPPORTED for EmuTune.** Detecting a known set of emulators is
  exactly the "targeted, finite query" case Play prefers. EmuTune **must not**
  declare `QUERY_ALL_PACKAGES` — it does not qualify and would be rejected.

- **CONFIRMED.** Workarounds that approximate broad visibility (e.g. probing many
  package names without `QUERY_ALL_PACKAGES`) are **also** restricted to
  core-functionality + interoperability uses. This is acceptable for EmuTune
  *only because* its queries are for legitimate interoperability with a known
  emulator set, not for an app inventory.
  - [Permissions and APIs that access sensitive information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)

---

## 4. Detecting installed apps via `PackageManager`

- **CONFIRMED.** The correct, policy-compliant check is
  `PackageManager.getPackageInfo(packageName, flags)` inside a
  `try`/`catch (PackageManager.NameNotFoundException)` block — an exception means
  "not visible/not installed". With `<queries>` declared, a matching package is
  visible and `getPackageInfo` succeeds.
  - [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)
  - [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)

- **CONFIRMED.** `getPackageInfo` (and `getInstalledApplications`) return a
  **filtered** view on API 30+. A package that is installed but not covered by
  `<queries>` (or automatic visibility) is treated as absent — so detection must
  be interpreted as "visible to EmuTune", not a ground-truth inventory.
  - [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)

- **LIKELY / nuance.** `PackageManager.MATCH_ALL` is documented as "if set and
  the platform is doing any filtering, the filtering will not happen" — but it
  does **not** bypass the package-visibility rules; it only disables other
  kinds of filtering. Do not treat it as a `QUERY_ALL_PACKAGES` substitute.
  - [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)

---

## 5. Recommended `<queries>` strategy for EmuTune

**Goal:** detect whether specific known emulators are installed, so EmuTune can
apply per-emulator optimisation presets or offer deep links — *without* broad
visibility.

**Recommended approach (failing closed, least privilege):**

1. **Declare each known emulator by package name** in `<queries>`:

   ```xml
   <queries>
     <package android:name="com.retroarch" />
     <package android:name="com.retroarch.aarch64" />
     <package android:name="org.dolphinemu.dolphinemu" />
     <package android:name="org.ppsspp.ppsspp" />
     <package android:name="org.citra.emu" />
     <package android:name="me.magnum.melonds" />
     <!-- add more as the supported list grows -->
   </queries>
   ```

2. **Detect** with a thin helper:

   ```kotlin
   fun PackageManager.isInstalled(packageName: String): Boolean =
       try {
           getPackageInfo(packageName, 0)
           true
       } catch (_: PackageManager.NameNotFoundException) {
           false
       }
   ```

3. **Interpret conservatively.** A `false` result means "not visible to
   EmuTune" — which may be "not installed" *or* "installed but not declared".
   Never claim to the user that a device "has no emulators".

**Package-name caveat (IMPORTANT):** emulator package IDs change with forks,
renames, and store removals. The identifiers above are **illustrative starting
points, not a verified registry** — each must be confirmed against current
store listings / upstream repositories before shipping. Maintain the list in one
central, easily auditable location (e.g. a single `EmulatorRegistry` object) so
it can be reviewed and updated without touching detection logic.

**When to prefer `<intent>` over `<package>`:** if EmuTune wants to discover
*any* app that can handle a specific action (e.g. an emulator that advertises a
custom "launch game" intent), declare an `<intent>` filter for that action
instead of enumerating package names. This is the robust choice when package
names are unstable but intent contracts are stable.
- [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)

---

## 6. Sources

- [Package visibility filtering on Android](https://developer.android.com/training/package-visibility)
- [Declare package visibility needs](https://developer.android.com/training/package-visibility/declaring)
- [`<queries>` manifest element reference](https://developer.android.com/guide/topics/manifest/queries-element)
- [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)
- [Use of the broad package (App) visibility (QUERY_ALL_PACKAGES) permission — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10158779)
- [Permissions and APIs that access sensitive information — Play Console Help](https://support.google.com/googleplay/android-developer/answer/16558241)
