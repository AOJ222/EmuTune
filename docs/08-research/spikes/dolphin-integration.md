# Dolphin Emulator (Android) — Integration Surface Spike

**Date:** 2026-08-21
**Scope:** Determine which capabilities a third-party Android app (EmuTune) can legitimately use against Dolphin **without root, without Shizuku, without accessibility hacks**.
**Method:** Authoritative sources in order — Dolphin official docs/wiki, upstream Dolphin source (`github.com/dolphin-emu/dolphin`), maintainer issue/commit evidence, Android documentation. Every concrete claim is tied to a URL or repo file path below.

> Verdict scale: **CONFIRMED** (authoritative source states it directly), **LIKELY** (strong inference from source but not explicitly stated by maintainers), **UNKNOWN** (no authoritative evidence found), **NOT_SUPPORTED** (source explicitly proves it is not possible).
>
> Convention applied throughout: **LIKELY is NOT permission to claim a capability works.** Only CONFIRMED capabilities are listed as supported in the final section.

---

## Summary of findings

- Dolphin ships **one** Android package id for stable, beta, and development channels: `org.dolphinemu.dolphinemu`. There is **no separate beta/dev package id**. The only variant is a source-built debug build, `org.dolphinemu.dolphinemu.debug` (`applicationIdSuffix = ".debug"`).
- On Android 11+ (scoped storage), Dolphin's user data — including `Config/Dolphin.ini` and `GameSettings/<gameId>.ini` — lives in the **app-specific directory** `Android/data/org.dolphinemu.dolphinemu/files`, which other apps **cannot reach by file path**.
- Dolphin **does** export a `DocumentsProvider` (`org.dolphinemu.dolphinemu.user`) exposing its user directory to the system file manager — but it is protected by `android.permission.MANAGE_DOCUMENTS`, so third-party apps **cannot query it directly**. Only the system DocumentsUI can browse it, which means any third-party access is **user-mediated via SAF** (one file/folder at a time, manual navigation), not programmatic.
- Dolphin exports exactly **four** components total: two launcher activities, one deep-link activity (`dolphinemu://app/...`), and the system-only `DocumentsProvider`. There are **no exported services, receivers, or game-list providers**.
- The deep link can launch Dolphin into a specific game **only** if the game is already in Dolphin's library and identified by Dolphin's internal game id. There is no intent to launch an arbitrary ROM path.
- The **only sanctioned way to move config/user data** is Dolphin's own in-app "Import/Export User Data" (a zip), which a third-party app cannot invoke programmatically (its activity is not exported).
- **Bottom line for EmuTune:** `INSTALLATION_DETECTION` is genuinely supported. `GAME_LAUNCH` is supported but narrowly (game-id deep link into an already-scanned library). `CONFIG_READ`/`CONFIG_WRITE`/`PER_GAME_CONFIG`/`GAME_DETECTION`/programmatic `CONFIG_IMPORT`/`CONFIG_EXPORT` are **not claimable** without root/Shizuku — the only legitimate lever is fragile, user-mediated SAF.

---

## Q1. Package identifiers (stable / beta / dev channels)

**Verdict: CONFIRMED**

| Channel | Package id | Evidence |
| --- | --- | --- |
| Stable (Play Store + site release) | `org.dolphinemu.dolphinemu` | `Source/Android/app/build.gradle.kts` — `applicationId = "org.dolphinemu.dolphinemu"` |
| Beta | `org.dolphinemu.dolphinemu` | APKMirror lists the site "beta" APK as package `org.dolphinemu.dolphinemu` (e.g. `5.0-21088 beta`) |
| Development (nightly) | `org.dolphinemu.dolphinemu` | APKMirror "site version" (nightly) APKs use package `org.dolphinemu.dolphinemu` |
| Debug (built from source only) | `org.dolphinemu.dolphinemu.debug` | `build.gradle.kts` `debug { applicationIdSuffix = ".debug" }` |

Key source-level facts:

- The Gradle build declares a single `applicationId` and **no product flavors** for beta/dev. The only build types are `release` and `debug`, so beta and development are distribution **channels/tracks of the same package**, not separate packages.

```kotlin
defaultConfig {
    applicationId = "org.dolphinemu.dolphinemu"
    ...
}
...
buildTypes {
    release { ... }
    debug {
        applicationIdSuffix = ".debug"
        ...
    }
}
```

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/build.gradle.kts>

- `namespace = "org.dolphinemu.dolphinemu"` (same file).
- The official download page describes the two channels as "Release builds" vs "Development/Beta builds", both distributing the same app id: <https://dolphin-emu.org/download/>

Implication: **INSTALLATION_DETECTION needs to match exactly one id** (`org.dolphinemu.dolphinemu`) for all real-world installs. The `.debug` id is irrelevant unless someone sideloads a self-built debug APK.

---

## Q2. Package visibility requirements (Android 11+)

**Verdict: CONFIRMED**

Two distinct facts:

1. **Dolphin's own manifest declares no `<queries>` element.** The fetched `AndroidManifest.xml` contains no `<queries>` block (it uses the legacy `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permission model).
2. **A third-party app must declare its own `<queries>`** to see/interact with Dolphin on Android 11+, because Android package-visibility filtering applies to the *requesting* app.

The components a third-party app would want to reference, and thus must account for in `<queries>`, are the exported ones:

- `MainActivity` — `MAIN` + `LAUNCHER` (reachable via `getLaunchIntentForPackage`; the launcher category is generally visible, but explicit package queries are the robust route).
- `TvMainActivity` — `MAIN` + `LEANBACK_LAUNCHER`.
- `AppLinkActivity` — `VIEW` action with scheme `dolphinemu` / host `app`.
- `DocumentProvider` — `android.content.action.DOCUMENTS_PROVIDER` (system-only; see Q4/Q5).

A `<queries>` declaration matching the package name, the `VIEW`+`dolphinemu` scheme, or the launcher intent covers all of these. There are no `<provider android:authorities>` visibility tricks to rely on beyond the `DOCUMENTS_PROVIDER` filter.

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>

Android reference: <https://developer.android.com/training/package-visibility>

---

## Q3. User-directory / scoped-storage behaviour

**Verdict: CONFIRMED**

Dolphin's user directory is resolved in `DirectoryInitialization.getUserDirectoryPath()`:

- **Default (Android 11+, scoped storage):** `context.getExternalFilesDir(null)` → `Android/data/org.dolphinemu.dolphinemu/files/` (app-specific external storage).
- **Legacy fallback:** `/storage/emulated/0/dolphin-emu` — used only when scoped storage is disabled (Android 10 or older, or a specific legacy-upgrade path), the app-specific dir is empty, and the legacy dir exists.

The config layout within that user directory (confirmed by source and wiki):

- `Config/Dolphin.ini` — global config.
- `GameSettings/<gameId>.ini` — per-game config.

Source code: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/DirectoryInitialization.kt>

The manifest declares `requestLegacyExternalStorage="true"` and `preserveLegacyExternalStorage="true"` plus `READ_EXTERNAL_STORAGE`/`WRITE_EXTERNAL_STORAGE`, but these are **ignored on a fresh Android 11+ install** (scoped storage is mandatory). Official wiki states this directly:

> "Dolphin 5.0-15348 and newer comply with Google's scoped storage policy and normally use the directory `Android/data/org.dolphinemu.dolphinemu/files` … difficult to access in most file managers on Android 11 and newer, and by default all data in the directory will be deleted when you uninstall Dolphin."

Source: <https://dolphin-emu.org/docs/guides/controlling-global-user-directory/>

**Accessibility consequence:** the app-specific dir under `Android/data/...` is **not readable/writable by other apps via filesystem paths** on Android 11+. Third-party file-path access to config is therefore **NOT_SUPPORTED** on modern Android (see Q10).

---

## Q4. DocumentsProvider — authority and exposed roots/documents

**Verdict: CONFIRMED**

Dolphin ships a custom `DocumentsProvider`:

- **Class:** `org.dolphinemu.dolphinemu.features.DocumentProvider`
- **Authority:** `${applicationId}.user` → **`org.dolphinemu.dolphinemu.user`**
- **Exported:** `android:exported="true"`
- **Guarded by:** `android:permission="android.permission.MANAGE_DOCUMENTS"` (signature|privileged — held only by the system DocumentsUI)
- **Intent filter:** `android.content.action.DOCUMENTS_PROVIDER`
- **Enabled:** `@bool/enableDocumentProvider` — `true` on API 24+ (`values-v24/bools.xml`), `false` below API 24 (`values/bools.xml`). Since `minSdk = 24`, it is effectively **always enabled** on supported devices.

Manifest excerpt:

```xml
<provider
    android:name=".features.DocumentProvider"
    android:authorities="${applicationId}.user"
    android:grantUriPermissions="true"
    android:exported="true"
    android:permission="android.permission.MANAGE_DOCUMENTS"
    android:enabled="@bool/enableDocumentProvider">
    <intent-filter>
        <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
    </intent-filter>
</provider>
```

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>

Roots/documents exposed (from `DocumentProvider.kt`):

- **Root id:** `"root"`
- **Root title:** `R.string.app_name_suffixed` ("Dolphin Emulator")
- **Root flags:** `FLAG_SUPPORTS_CREATE | FLAG_SUPPORTS_RECENTS | FLAG_SUPPORTS_SEARCH | FLAG_LOCAL_ONLY | FLAG_SUPPORTS_IS_CHILD`
- **Root content:** `DirectoryInitialization.getUserDirectoryPath(context)` — i.e. the entire user directory (Config, GameSettings, saves, etc.).

The provider implements `queryRoots`, `queryDocument`, `queryChildDocuments`, `openDocument`, `openDocumentThumbnail`, `createDocument`, `deleteDocument`, `renameDocument`, `isChildDocument`.

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/DocumentProvider.kt>

Dolphin itself links to this root via `DocumentsContract.buildRootUri("$packageName.user", "root")` in `UserDataActivity` ("Open User Data Folder").

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/activities/UserDataActivity.kt>

---

## Q5. SAF possibilities — can config/user files be selected/read/written through SAF?

**Verdict: LIKELY (user-mediated only); NOT_SUPPORTED (programmatic)**

Two sharply different answers:

1. **Direct programmatic access → NOT_SUPPORTED.** The provider is protected by `android.permission.MANAGE_DOCUMENTS`. A third-party app does not hold this signature|privileged permission, so any direct `ContentResolver` query to `content://org.dolphinemu.dolphinemu.user/...` throws `SecurityException`. EmuTune **cannot enumerate or read Dolphin's config tree on its own**.

2. **User-mediated SAF → LIKELY.** The system DocumentsUI *does* hold `MANAGE_DOCUMENTS` and can browse the provider. When EmuTune launches `ACTION_OPEN_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE`, or `ACTION_CREATE_DOCUMENT`, the system picker should surface Dolphin's "Dolphin Emulator" root; when the user navigates in and picks a file/folder, DocumentsUI grants EmuTune a URI grant (read, or write for `CREATE_DOCUMENT`/write-mode open). Because `android:grantUriPermissions="true"` and the root is `LOCAL_ONLY` with normal flags, there is no flag blocking tree grants.

Why **LIKELY** and not **CONFIRMED**: I could not find a maintainer statement or doc explicitly confirming Dolphin's root appears in *third-party* `ACTION_OPEN_DOCUMENT_TREE` pickers. Dolphin's own code opens the folder with a direct `ACTION_VIEW` on the root URI rather than the generic picker. The mechanism is standard Android and the provider is correctly registered, but the exact picker visibility has not been verified end-to-end.

Practical consequence: the only legitimate, non-root way for EmuTune to read or write Dolphin config files is to **ask the user to navigate the system picker into Dolphin's folder and grant access**, one file or folder at a time. This is manual, fragile, and not scriptable — it cannot back an automated "sync my config" feature.

Sources:
- Manifest (permission + grantUriPermissions): <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>
- `DocumentProvider.kt` (root flags): <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/DocumentProvider.kt>
- Android `MANAGE_DOCUMENTS` (signature|privileged): <https://developer.android.com/reference/android/Manifest.permission#MANAGE_DOCUMENTS>

---

## Q6. Exported components (activities, services, receivers, providers)

**Verdict: CONFIRMED**

Complete inventory from `AndroidManifest.xml`. Only four components are `android:exported="true"`; everything else is `false`.

| Component | Exported | Intent / notes |
| --- | --- | --- |
| `.ui.main.MainActivity` | **true** | `MAIN` + `LAUNCHER` |
| `.ui.main.TvMainActivity` | **true** | `MAIN` + `LEANBACK_LAUNCHER` |
| `.activities.AppLinkActivity` | **true** | `VIEW`, scheme `dolphinemu`, host `app` |
| `.features.DocumentProvider` | **true** | `DOCUMENTS_PROVIDER`, guarded by `MANAGE_DOCUMENTS` |
| `.features.settings.ui.SettingsActivity` | false | — |
| `.features.cheats.ui.CheatsActivity` | false | — |
| `.activities.EmulationActivity` | false | — |
| `.activities.CustomFilePickerActivity` | false | has `GET_CONTENT` filter but explicitly `exported="false"` (internal only) |
| `.activities.ConvertActivity` | false | — |
| `.activities.UserDataActivity` | false | — |
| `.features.netplay.ui.NetplaySetupActivity` / `NetplayActivity` | false | — |
| `.features.riivolution.ui.RiivolutionBootActivity` | false | — |
| `.services.SyncChannelJobService` | false | `BIND_JOB_SERVICE` |
| `.services.SyncProgramsJobService` | false | `BIND_JOB_SERVICE` |
| `androidx.core.content.FileProvider` | false | authority `${applicationId}.filesprovider` |

There are **no exported broadcast receivers** and **no exported services**.

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>

Note: the `FileProvider` (`org.dolphinemu.dolphinemu.filesprovider`) is `exported="false"` and therefore not usable by other apps even with a granted URI.

---

## Q7. Import / export functions

**Verdict: CONFIRMED (in-app only; not invocable by third parties)**

Dolphin's Android app offers **User Data Import** and **User Data Export** in its `UserDataActivity` (a zip of the whole user directory):

- **Export:** `ActivityResultContracts.CreateDocument("application/zip")` → zips `DirectoryInitialization.getUserDirectory()` to a user-chosen destination (`dolphin-emu.zip`).
- **Import:** `ActivityResultContracts.OpenDocument()` (`application/zip`) → validates the zip contains `Config/Dolphin.ini`, then deletes the existing user dir and extracts the zip (with path-traversal protection).

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/activities/UserDataActivity.kt>

Relevant strings: "Import User Data", "Export User Data", "Your user data (settings, saves, etc.) …" — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/res/values/strings.xml>

Two important limitations for EmuTune:

1. **There is no INI-only import/export.** The unit is the entire user directory (config + saves + GameSettings). Config changes are made in-app via the settings UI, not via file import.
2. **`UserDataActivity` is `exported="false"`**, so EmuTune **cannot programmatically trigger** Dolphin's import/export. EmuTune could, however, *produce* a Dolphin-compatible user-data zip (`Config/Dolphin.ini` at the zip root) that the user then imports manually inside Dolphin — a manual interop path, not an automated capability.

---

## Q8. Launch intents / deep links (launch into a specific game)

**Verdict: CONFIRMED (with significant constraints)**

Dolphin registers `AppLinkActivity` as an exported `VIEW` handler for scheme `dolphinemu`, host `app`:

```xml
<activity android:name=".activities.AppLinkActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:host="@string/host" android:scheme="@string/scheme"/>
    </intent-filter>
</activity>
```

`scheme = "dolphinemu"`, `host = "app"` (from `strings.xml`).

Deep-link formats (from `AppLinkHelper.kt`):

- Launch a game: `dolphinemu://app/play/<channelId>/<gameId>`
- Browse a platform tab: `dolphinemu://app/browse/<platformTabId>`

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/AppLinkHelper.kt>

Resolution logic (`AppLinkActivity.kt`) is the critical constraint: for `play`, it looks up `gameId` in the **internal game cache** (`GameFileCacheManager.getGameFileByGameId`). If the game is not already in Dolphin's library, the launch fails.

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/activities/AppLinkActivity.kt>

Net effect for EmuTune:

- **Supported:** launching Dolphin into a game the user has **already added to Dolphin's library**, addressed by Dolphin's internal game id (a short id like `RMCE01` for Mario Kart Wii; these ids are a stable, well-known convention).
- **Not supported:** launching an arbitrary ROM/ISO by path or content URI — there is no exported intent that accepts a file path; adding games goes through Dolphin's own (non-exported) file picker.
- Requires the `dolphinemu://app/...` `VIEW` intent to be declared in EmuTune's `<queries>` (or use an explicit component intent), per Q2.

---

## Q9. Game-library integration surfaces

**Verdict: NOT_SUPPORTED**

Dolphin exposes **no** game-list surface to other apps:

- No exported content provider for the game library.
- The game list is held internally in `GameFileCacheManager` and persisted to `gamelist.cache` in Dolphin's own cache dir.
- The Android TV integration uses leanback channels via `SyncChannelJobService`/`SyncProgramsJobService` — both `exported="false"` job services, not queryable by third parties.

Source: <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>

EmuTune cannot discover which games a user has in Dolphin, nor read Dolphin's game list. Any "detect what games are installed" capability for Dolphin is **not achievable** without root/Shizuku.

---

## Q10. Config read/write feasibility without root

**Verdict: NOT_SUPPORTED (programmatic); LIKELY (user-mediated SAF only)**

- **Filesystem path access → NOT_SUPPORTED.** On Android 11+, config lives in `Android/data/org.dolphinemu.dolphinemu/files/Config/Dolphin.ini` (and `GameSettings/*.ini`), inside the app-specific directory. Android scoped storage forbids other apps from reading/writing `Android/data/<other-app>` by path. The legacy `dolphin-emu` shared-storage location only applies on Android 10/older or a narrow legacy-upgrade case, and `requestLegacyExternalStorage` is ignored on Android 11+.
- **User-mediated SAF → LIKELY.** As in Q5, a user could grant EmuTune a SAF URI grant into Dolphin's `Config` folder (or an individual INI) via the system picker, after which EmuTune could read/write it — but this requires manual navigation and consent each time, and cannot be automated or guaranteed.

Therefore **EmuTune cannot claim programmatic `CONFIG_READ` or `CONFIG_WRITE`** for Dolphin without root/Shizuku. The honest capability is: "offer the user a SAF picker that *might* reach Dolphin's config folder, if they navigate there and grant access."

Sources:
- Wiki (scoped storage location + access difficulty): <https://dolphin-emu.org/docs/guides/controlling-global-user-directory/>
- Scoped storage PR (user dir must move to app-specific dir): <https://github.com/dolphin-emu/dolphin/pull/9696>
- Config path derivation (Config/… .ini, GameSettings/… .ini): `DirectoryInitialization` + settings file helper usage; see <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/DirectoryInitialization.kt>

---

## Integration conclusions (conservative capability list)

Applying the rule "LIKELY is not permission to claim a capability works," here is what EmuTune may safely claim as a genuine capability against Dolphin, **without root/Shizuku/accessibility**:

| Capability | Verdict | Notes |
| --- | --- | --- |
| `INSTALLATION_DETECTION` | **Supported (CONFIRMED)** | Match `org.dolphinemu.dolphinemu`; declare it in `<queries>`. Stable/beta/dev share this id. |
| `GAME_LAUNCH` | **Supported, constrained (CONFIRMED)** | Deep link `dolphinemu://app/play/<channelId>/<gameId>` launches a game **already in Dolphin's library**, by Dolphin game id. Cannot launch an arbitrary ROM path. |
| `GAME_DETECTION` | **Not supported** | No game-list surface is exported. |
| `CONFIG_READ` | **Not supported (programmatically)** | App-specific dir not reachable; direct provider access blocked by `MANAGE_DOCUMENTS`. |
| `CONFIG_WRITE` | **Not supported (programmatically)** | Same as above. |
| `PER_GAME_CONFIG` | **Not supported** | `GameSettings/*.ini` lives in the same inaccessible user dir. |
| `CONFIG_IMPORT` | **Not supported (programmatically)** | Dolphin's import activity is not exported; EmuTune can only produce a zip for manual user import. |
| `CONFIG_EXPORT` | **Not supported (programmatically)** | Dolphin's export activity is not exported; requires manual user action inside Dolphin. |
| `SAF_READ` / `SAF_WRITE` (user-mediated) | **LIKELY (do not claim as a product capability)** | System picker may reach Dolphin's provider root and grant URI access with manual user navigation. Fragile, manual, unverifiable end-to-end. |

**One-sentence conclusion:** EmuTune can reliably **detect** Dolphin and **launch** a known, already-added game by game id, but it **cannot** read or write Dolphin's configuration (including per-game config) through any legitimate, non-root, non-Shizuku, non-accessibility mechanism — the only theoretical lever is a user-mediated SAF grant that is too fragile to build a product feature on.

---

## Source index

Primary (upstream Dolphin repository, `master`):

- `Source/Android/app/src/main/AndroidManifest.xml` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/AndroidManifest.xml>
- `Source/Android/app/build.gradle.kts` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/build.gradle.kts>
- `.../features/DocumentProvider.kt` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/features/DocumentProvider.kt>
- `.../utils/DirectoryInitialization.kt` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/DirectoryInitialization.kt>
- `.../utils/AppLinkHelper.kt` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/utils/AppLinkHelper.kt>
- `.../activities/AppLinkActivity.kt` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/activities/AppLinkActivity.kt>
- `.../activities/UserDataActivity.kt` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/activities/UserDataActivity.kt>
- `.../res/values/strings.xml` — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/res/values/strings.xml>
- `.../res/values/bools.xml` (provider `false`) — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/res/values/bools.xml>
- `.../res/values-v24/bools.xml` (provider `true`) — <https://github.com/dolphin-emu/dolphin/blob/master/Source/Android/app/src/main/res/values-v24/bools.xml>

Official docs / wiki:

- Controlling the Global User Directory (scoped storage) — <https://dolphin-emu.org/docs/guides/controlling-global-user-directory/>
- Download page (Release vs Development/Beta channels) — <https://dolphin-emu.org/download/>

Maintainer evidence:

- Scoped storage PR #9696 — <https://github.com/dolphin-emu/dolphin/pull/9696>
- Forum: maintainer on Android/data access ("use the import/export user data feature… or manage from a PC via USB/MTP") — <https://forums.dolphin-emu.org/Thread-please-move-app-data-folder>

Android reference:

- Package visibility (`<queries>`) — <https://developer.android.com/training/package-visibility>
- `MANAGE_DOCUMENTS` permission (signature|privileged) — <https://developer.android.com/reference/android/Manifest.permission#MANAGE_DOCUMENTS>
