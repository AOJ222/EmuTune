# EmuTune — Android Permissions & Storage Constraints

**App context:** EmuTune is a gaming-handheld-first emulator optimisation app.
Kotlin, Compose + Material 3, `minSdk 29` (Android 10), `targetSdk 36`
(Android 16 / "Baklava").

This document records the legitimate Android platform constraints around file
access and the permission model EmuTune will adopt. Every claim is classified:

| Classification | Meaning |
| --- | --- |
| **CONFIRMED** | Stated directly in official Android documentation / API reference. |
| **LIKELY** | Strong evidence from official sources, with version nuance or caveat. |
| **UNKNOWN** | Not reliably determinable at runtime. |
| **NOT_SUPPORTED** | No public Android API exists for this capability. |

> **Governing principle: fail closed.** EmuTune assumes it has *no* access to
> any file, directory, or app until the user has explicitly granted it. It never
> requests broad storage access.

---

## 1. Scoped storage — the baseline

- **CONFIRMED.** Scoped storage was introduced in Android 10 (API 29) and is
  **mandatory** for apps targeting Android 11 (API 30) or higher. Since EmuTune
  targets API 36, scoped storage is non-negotiable.
  - [Data and file storage overview](https://developer.android.com/training/data-storage)
  - [Android storage use cases and best practices](https://developer.android.com/training/data-storage/use-cases)

- **CONFIRMED.** Under scoped storage an app has, by default:
  - Full read/write access to its **own** app-specific directories (internal
    `filesDir`/`cacheDir` and external `getExternalFilesDir()`/`getExternalCacheDir()`),
    **with no permission required**.
  - Access to shareable media (images/audio/video) only via `MediaStore`, and
    only to media it created, plus media the user explicitly grants.
  - **No** access to other apps' app-specific directories.
  - [Data and file storage overview](https://developer.android.com/training/data-storage)

- **CONFIRMED.** The `requestLegacyExternalStorage` opt-out flag is **ignored**
  once an app targets Android 11+ (API 30). It was a temporary migration aid for
  apps targeting Android 10 only.
  - [Storage updates in Android 11](https://developer.android.com/about/versions/11/privacy/storage)

---

## 2. `Android/data` — what an ordinary app can and cannot do

`Android/data/<package>` is the **external app-specific directory** of a given
package.

- **CONFIRMED.** EmuTune may freely read and write its **own**
  `Android/data/<EmuTune.package>` (equivalent to `getExternalFilesDir()`), with
  **no permission**. This is the correct location for its own writable data that
  must survive and be visible as app-specific storage.
  - [Data and file storage overview](https://developer.android.com/training/data-storage)

- **CONFIRMED.** EmuTune **cannot** read or write any *other* app's
  `Android/data/<other.package>` directory through filesystem APIs. This has been
  true since scoped storage, and Android 11 expanded the restriction so that even
  apps targeting Android 8.1 and lower cannot be reached this way.
  - [Scoped storage (AOSP)](https://source.android.com/docs/core/storage/scoped)

- **CONFIRMED.** The Storage Access Framework (SAF) **cannot** be used to select
  `Android/data/` or `Android/obb/` (or their subdirectories) on Android 11+
  (API 30). See §4.
  - [Storage updates in Android 11 — Document access restrictions](https://developer.android.com/about/versions/11/privacy/storage)

- **CONFIRMED.** Even an app granted **All files access**
  (`MANAGE_EXTERNAL_STORAGE`) still **cannot** access other apps' app-specific
  directories, because those appear as subdirectories of `Android/data/` on a
  storage volume and are excluded from the grant.
  - [Manage all files on a storage device](https://developer.android.com/training/data-storage/manage-all-files)

- **LIKELY / community-observed.** On Android 13–15, OEM file managers and
  third-party file managers have progressively lost the ability to browse
  `Android/data` of other apps. Some OEMs (e.g. Samsung) still expose parts of it
  over USB/MTP. This is device/OEM-specific and must **not** be relied upon.
  EmuTune must assume `Android/data` of other apps is inaccessible.

---

## 3. Media access (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_*`)

- **CONFIRMED.** `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` are legacy
  and only apply to shared media on Android 10/11. On Android 13+ (API 33) they
  are superseded by granular `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` /
  `READ_MEDIA_AUDIO`. On Android 14+ (API 34), apps can request only
  `READ_MEDIA_VISUAL_USER_SELECTED` for partial photo/video access.
  - [Data and file storage overview](https://developer.android.com/training/data-storage)

- **Relevance to EmuTune: LOW.** EmuTune reads ROM/BIOS/disc-image files the
  user owns, which are **not** media in the `MediaStore` sense. Media permissions
  are generally the wrong tool; SAF (§4) is the right one. EmuTune should **not**
  declare media permissions unless it gains a concrete media use case.

---

## 4. Storage Access Framework (SAF) — the intended tool

- **CONFIRMED.** The SAF lets the user pick files/directories through a
  system-controlled picker and grant the app scoped, **persistent** access to
  exactly what was selected:
  - `ACTION_OPEN_DOCUMENT` — pick an existing document (API 19+). Use with
    `CATEGORY_OPENABLE`; add `EXTRA_MIME_TYPES` to filter.
  - `ACTION_CREATE_DOCUMENT` — "save as" (API 19+).
  - `ACTION_OPEN_DOCUMENT_TREE` — grant access to a directory and its subtree
    (API 21+). Use `DocumentsContract.EXTRA_INITIAL_URI` to suggest a start folder.
  - Persist grants across reboots with `takePersistableUriPermission()`.
  - [Open files using the Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
  - [Access documents and other files from shared storage](https://developer.android.com/training/data-storage/shared/documents-files)

- **CONFIRMED — "content actually exposed/selectable".** The picker only lists
  documents that a **document provider** chooses to publish. The system's
  `ExternalStorageProvider` publishes shared storage but **not** other apps'
  app-specific internal storage (`/data/data/…`) or app-specific external
  storage (`Android/data/<pkg>`, `Android/obb/<pkg>`). SAF therefore grants
  access only to (a) what a provider exposes *and* (b) what the user explicitly
  selects. It is **not** a mechanism for arbitrary filesystem access.
  - [Open files using the Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)

- **CONFIRMED — directory restrictions (Android 11+/API 30+).**
  - `ACTION_OPEN_DOCUMENT_TREE` **cannot** be used to request access to: the root
    of the internal storage volume, the root of a "reliable" SD-card volume, or
    the `Download` directory.
  - Both `ACTION_OPEN_DOCUMENT_TREE` and `ACTION_OPEN_DOCUMENT` **cannot** be used
    to select individual files from `Android/data/` or `Android/obb/` (or their
    subdirectories).
  - [Storage updates in Android 11 — Document access restrictions](https://developer.android.com/about/versions/11/privacy/storage)

- **CONFIRMED.** With `ACTION_OPEN_DOCUMENT_TREE`, the app gains access **only**
  to the user-selected directory and its subtree — not to other apps' files
  outside it. This user-controlled boundary is the core privacy property EmuTune
  relies on.
  - [Access documents and other files from shared storage](https://developer.android.com/training/data-storage/shared/documents-files)

---

## 5. All files access (`MANAGE_EXTERNAL_STORAGE`) — not for EmuTune

- **CONFIRMED.** `MANAGE_EXTERNAL_STORAGE` ("All files access") is a **special
  app access**, not a normal runtime permission. It is requested by declaring it
  in the manifest and directing the user to system settings via
  `ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION` (or
  `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`), then checked with
  `Environment.isExternalStorageManager()`.
  - [Manage all files on a storage device](https://developer.android.com/training/data-storage/manage-all-files)

- **CONFIRMED.** Even when granted, it gives write access to *shared* storage
  only — it **excludes** `/Android/data/`, `/sdcard/Android`, and most of their
  subdirectories, and it never grants access to other apps' app-specific
  directories.
  - [Manage all files on a storage device](https://developer.android.com/training/data-storage/manage-all-files)

- **CONFIRMED — Play policy.** Google Play restricts All files access to apps
  whose **core functionality** requires broad file management: file managers,
  backup/restore, antivirus, document management. A declaration form is required,
  and misuse can suspend the app/account.
  - [Use of All files access (MANAGE_EXTERNAL_STORAGE) — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10467955)

- **Verdict: NOT_SUPPORTED for EmuTune's use case.** An emulator optimisation app
  does not qualify for All files access, and requesting it would be rejected (or
  put the app at review risk). EmuTune **must not** declare
  `MANAGE_EXTERNAL_STORAGE`.

---

## 6. The permission model EmuTune will actually use

| Storage need | Mechanism | Permission required? |
| --- | --- | --- |
| App's own config, caches, downloads, shader caches | Internal `filesDir`/`cacheDir` and external `getExternalFilesDir()`/`getExternalCacheDir()` | None |
| User's ROM / BIOS / disc images | `ACTION_OPEN_DOCUMENT` + `ACTION_OPEN_DOCUMENT_TREE`, persistable URI grants via `ContentResolver` | None (user consent via picker) |
| Broad filesystem access | — | **Never requested** |
| Other apps' `Android/data` | — | **Not possible** (and not needed) |
| Media library scan | `MediaStore` only if a media use case emerges | Only if/when needed |

**Failing-closed rules EmuTune must follow:**

1. Declare **zero** storage permissions in the manifest by default. SAF
   selection needs no permission.
2. Treat every `Uri` from SAF as opaque; access it only through
   `ContentResolver` (`openInputStream` / `openFileDescriptor`), never by
   guessing a file path.
3. Re-check `contentResolver.persistedUriPermissions` (or catch
   `SecurityException`) before touching a previously granted URI — grants can be
   revoked by the user at any time.
4. Store EmuTune's own files only in app-specific directories so they are
   readable/writable with no permission and uninstall cleanly with the app.
5. If a future feature genuinely needs media access, request the narrowest
   Android-13+ media permission, never the legacy broad ones.

---

## 7. Sources

- [Data and file storage overview](https://developer.android.com/training/data-storage)
- [Android storage use cases and best practices](https://developer.android.com/training/data-storage/use-cases)
- [Storage updates in Android 11](https://developer.android.com/about/versions/11/privacy/storage)
- [Manage all files on a storage device](https://developer.android.com/training/data-storage/manage-all-files)
- [Access documents and other files from shared storage](https://developer.android.com/training/data-storage/shared/documents-files)
- [Open files using the Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider)
- [Scoped storage (Android Open Source Project)](https://source.android.com/docs/core/storage/scoped)
- [Use of All files access (MANAGE_EXTERNAL_STORAGE) — Play Console Help](https://support.google.com/googleplay/android-developer/answer/10467955)
