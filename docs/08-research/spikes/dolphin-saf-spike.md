# Spike — Dolphin SAF config access (real device)

**Date:** 2026-08-23
**Purpose:** Confirm, on real hardware, whether EmuTune can read and write
Dolphin's configuration through the Storage Access Framework with a single
explicit user grant. This determines whether "set the best settings for them"
is SAF-automatic, or requires Shizuku / Lab Runner.

**Classify every result:** `CONFIRMED` / `LIKELY` / `UNKNOWN` / `NOT_SUPPORTED`.
`LIKELY` is not permission to build a product feature on it.

---

## Why this matters

Dolphin stores config in app-private storage
(`Android/data/org.dolphinemu.dolphinemu/files/Config/Dolphin.ini` and
`GameSettings/*.ini`), which is unreachable by path. Dolphin also ships a
`DocumentsProvider` (authority `org.dolphinemu.dolphinemu.user`, root id
`"root"`, guarded by `MANAGE_DOCUMENTS`). The provider is correctly registered
and `grantUriPermissions=true`, but it is **unverified** whether the system
file picker surfaces Dolphin's folder to a third-party app on a real device.

This spike answers that one question definitively.

---

## Prerequisites

- AYN Thor Max (or any real Android 11+ device) with the EmuTune debug build installed.
- Dolphin installed (`org.dolphinemu.dolphinemu`) with **at least one game scanned** so its user directory is non-trivial.
- `adb` available (for logcat and for the adb-shell fallback in §5).

---

## Procedure

### §1 — Launch the system picker

From the EmuTune debug build, trigger `ACTION_OPEN_DOCUMENT_TREE` (a debug-only
"Grant Dolphin config access" action). Observe the picker.

- [ ] Does a **"Dolphin Emulator"** root appear in the picker's list of
  available roots/providers?

### §2 — Navigate into Dolphin's root

- [ ] Can you drill into that root and see the `Config/` and `GameSettings/`
  folders (and `.ini` files) listed?

### §3 — Grant access

- [ ] Select the root (or the `Config` folder) and confirm.
- [ ] Does EmuTune receive a non-null tree URI, and does
  `takePersistableUriPermission` succeed (no exception)?

### §4 — Read a config file

Using the granted tree URI, resolve `Config/Dolphin.ini` as a child document
and open an input stream.

- [ ] Does the read succeed and return real INI text (e.g. `[Core]`,
  `[Video_Settings]` sections)?

### §5 — adb-shell ground-truth fallback (if §1–§4 fail)

If the picker never surfaces Dolphin, use adb (Tier-3-style, for diagnosis only):

```bash
adb shell content query --uri content://org.dolphinemu.dolphinemu.user/root
```

This checks the provider directly from a privileged shell (which does hold
`MANAGE_DOCUMENTS`-class access on `shell`). If this works but §1 doesn't, the
blocker is picker *surfacing*, not the provider itself.

---

## What to record

For each § step: **PASS / FAIL / NOT-TESTED**, plus the exact observed text or
error. Capture `adb logcat` for any `SecurityException`,
`IllegalArgumentException` (bad authority), or picker-empty behaviour.

---

## Result — CONFIRMED (real device, 2026-08-23, AYN Thor / Android 13)

§1–§3 all PASS. The system picker surfaced "Dolphin Emulator"; navigating into its
root showed `Config`; granting returned a **persisted** URI. Observed grants:

```
content://org.dolphinemu.dolphinemu.user/tree/root%2F
content://org.dolphinemu.dolphinemu.user/tree/root%2FConfig
```

`takePersistableUriPermission` succeeded with read+write flags, so the grant
survives app restarts. Read access to `Config/Dolphin.ini` is therefore available;
write access is plausible (`grantUriPermissions=true` + write flag) but was not
exercised end-to-end in this spike — treat write as CONFIRMED-for-grant, pending a
one-line write test.

## Classification

| §1–§4 outcome | Verdict | Next step |
| --- | --- | --- |
| All PASS | **CONFIRMED** — SAF read (and, if the write open works, write) is available | Build the read → diff → write → verify loop |
| §1 FAIL, §5 PASS | **NOT_SUPPORTED** for picker surfacing; provider works only via privileged shell | Proceed to Shizuku (Tier 2) or Lab Runner (Tier 3) |
| §1–§2 PASS, §4 FAIL (read denied) | **NOT_SUPPORTED** for read | Guided config only (current state) |
| Read PASS, write untested | **LIKELY** for read; write still needs its own test | Test `openDocument` with `"w"` mode against `GameSettings/*.ini` |

---

## Decision

- **CONFIRMED →** Milestone 2 work item: `ConfigAccessService` behind the
  existing `EmulatorAdapter` seam (`CONFIG_READ`/`CONFIG_WRITE` become real for
  Dolphin when a grant is held), feeding the existing `ConfigTransactionManager`.
  The delta ("current → recommended") then drives the OPTIMISE action end to end.
- **NOT_SUPPORTED →** keep `CONFIG_WRITE = false`; guided configuration stays
  the product path; Shizuku (opt-in) and Lab Runner (desktop/ADB) are the
  legitimate routes to full access.

Do not ship the SAF path as automatic until §1–§4 all PASS on this device.
