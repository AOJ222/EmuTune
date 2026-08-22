# EmuTune — Device & Hardware Profiling APIs

**App context:** EmuTune is a gaming-handheld-first emulator optimisation app.
Kotlin, Compose + Material 3, `minSdk 29` (Android 10), `targetSdk 36`
(Android 16 / "Baklava").

This document lists the **legitimate, public** Android APIs EmuTune may use to
profile a device — and, critically, separates what is reliable from what is
**not**. Every entry carries a reliability classification.

| Classification | Meaning |
| --- | --- |
| **CONFIRMED** | Stated directly in official Android documentation / API reference. |
| **LIKELY** | Strong evidence from official sources, with version nuance or caveat. |
| **UNKNOWN** | Not reliably determinable at runtime. |
| **NOT_SUPPORTED** | No public Android API exists for this capability. |

> **Golden rule for EmuTune:** every profiled value is a *hint*, not ground
> truth. Handle missing/generic values explicitly (see §8) and never gate a
> feature's correctness on a value that can be `"UNKNOWN"`, empty, or NaN.

---

## 1. Manufacturer / model / device identity — `android.os.Build`

- **CONFIRMED.** `android.os.Build` exposes static fields populated from system
  properties (`ro.*`):

  | Field | Source | Notes |
  | --- | --- | --- |
  | `Build.MANUFACTURER` | `ro.product.manufacturer` | Product/hardware manufacturer |
  | `Build.BRAND` | `ro.product.brand` | Consumer-visible brand |
  | `Build.MODEL` | `ro.product.model` | End-user-visible product name |
  | `Build.DEVICE` | `ro.product.device` | Industrial design name (codename) |
  | `Build.PRODUCT` | `ro.product.name` | Overall product name |
  | `Build.HARDWARE` | `ro.hardware` | Hardware name (kernel cmdline / proc) |
  | `Build.BOARD` | `ro.product.board` | Board name |
  | `Build.FINGERPRINT` | — | Unique build identifier |
  | `Build.SDK_INT` | — | API level (e.g. 36) |
  | `Build.VERSION.RELEASE` | — | Human release string |

  - [Build reference](https://developer.android.com/reference/android/os/Build)

- **LIKELY / caveat.** These values come from system properties and can be
  spoofed by the OEM or a custom ROM. Treat them as **advisory**, never as a
  security boundary. `Build.MODEL` in particular is cosmetic (marketing name),
  not a reliable hardware key.

- **CONFIRMED.** `Build.SUPPORTED_ABIS`, `SUPPORTED_32_BIT_ABIS`,
  `SUPPORTED_64_BIT_ABIS` return ordered lists of instruction sets (most
  preferred first). `CPU_ABI`/`CPU_ABI2` are **deprecated since API 21** — use
  the `SUPPORTED_*_ABIS` arrays.
  - [Build reference](https://developer.android.com/reference/android/os/Build)

---

## 2. SoC / CPU identification — be honest about limits

- **CONFIRMED.** Android **does not provide a reliable SoC-name API**. This is
  the single most important profiling caveat for EmuTune. Historical techniques
  (`/proc/cpuinfo`, `/sys/firmware/devicetree/base/compatible`,
  `ro.board.platform`) are heuristics — often empty, vendor-specific, or
  requiring permissions EmuTune should not assume.

- **LIKELY (best-effort, API 31+).** `Build.SOC_MANUFACTURER` and
  `Build.SOC_MODEL` were added in Android 12 (API 31), reading
  `ro.soc.manufacturer` / `ro.soc.model`. Critically, the AOSP implementation
  returns the literal string **`"UNKNOWN"`** when the property is unset, and the
  value is often generic (e.g. `"arm64"` or an OEM marketing string) rather than
  a precise die name like `"Snapdragon 8 Gen 2"`.
  - [Build reference — SOC_MODEL / SOC_MANUFACTURER](https://developer.android.com/reference/android/os/Build#SOC_MODEL)
  - [Build.java source (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/os/Build.java)

- **Verdict for SoC name: NOT_SUPPORTED (as a *reliable* API) / UNKNOWN (in
  practice).** EmuTune may read `Build.SOC_MODEL`/`SOC_MANUFACTURER` on API 31+
  as a coarse hint, but **must** treat `"UNKNOWN"`, empty, or clearly generic
  values as "no data", and **must not** try to derive a precise SoC model from
  them.

- **CONFIRMED (ABI — reliable).** `Build.SUPPORTED_ABIS` is the trustworthy
  signal for instruction-set support (e.g. `arm64-v8a`, `armeabi-v7a`, `x86_64`),
  which is what EmuTune should actually use to select native binaries.
  - [Build reference](https://developer.android.com/reference/android/os/Build)

- **LIKELY (core count — approximate).** `Runtime.getRuntime().availableProcessors()`
  reports the number of processors available to the JVM; on recent Android this
  can reflect the current CPU affinity / foreground cgroup rather than the
  physical core count, so treat it as "cores currently available to me", not a
  hardware spec.

---

## 3. RAM — `ActivityManager.MemoryInfo`

- **CONFIRMED.** `ActivityManager.getMemoryInfo(ActivityManager.MemoryInfo)`
  populates:

  | Field | Meaning |
  | --- | --- |
  | `totalMem` | Total kernel-accessible memory — effectively device RAM, **excluding** below-kernel fixed allocations (DMA buffers, baseband RAM). API 16+. |
  | `availMem` | Currently available memory (not absolute; much is "in use but reclaimable"). |
  | `lowMemory` | `true` when the system considers itself under memory pressure. |
  | `threshold` | `availMem` level at which the system starts killing background processes. |
  | `advertisedMem` | Advertised "retail" memory (newer field; can differ from `totalMem` because the ODM may reserve memory, e.g. for TEE). |

  - [ActivityManager.MemoryInfo reference](https://developer.android.com/reference/android/app/ActivityManager.MemoryInfo)
  - [Manage your app's memory](https://developer.android.com/topic/performance/memory)

- **CONFIRMED.** `ActivityManager.isLowRamDevice()` returns whether the device is
  configured as low-RAM (currently "generally ~1 GB or less"), useful for
  toggling memory-heavy features off.
  - [ActivityManager reference](https://developer.android.com/reference/android/app/ActivityManager)

- **CONFIRMED / best practice.** Prefer `ComponentCallbacks2.onTrimMemory(int)`
  over polling `MemoryInfo` for reacting to memory pressure.
  - [Manage your app's memory](https://developer.android.com/topic/performance/memory)

---

## 4. Display resolution & refresh rate — `Display` / `Display.Mode`

- **CONFIRMED.** `Display.getSupportedModes()` returns `Display.Mode[]` — the
  supported display modes, which may include synthetic modes. Each
  `Display.Mode` exposes `getPhysicalWidth()`, `getPhysicalHeight()`, and
  `getRefreshRate()` (and `getModeId()`).
  - [Display reference](https://developer.android.com/reference/android/view/Display)

- **CONFIRMED.** `Display.getMode()` returns the currently **active** mode, and
  `Display.getRefreshRate()` returns the current refresh rate in Hz. This is the
  correct way to read the *current* refresh rate.
  - [Display reference](https://developer.android.com/reference/android/view/Display)

- **LIKELY / version nuance.** `Display.getSupportedRefreshRates()` exists but its
  behaviour differs by OS version: on Android 15 (API 35, "Vanilla Ice Cream")
  and below it returns only the **default** modes' rates; on Android 16 (API 36,
  "Baklava") and above it returns supported **render** rates. Prefer
  `getSupportedModes()` for a complete, version-stable picture.
  - [Display reference](https://developer.android.com/reference/android/view/Display)
  - [Display.java source (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/Display.java)

- **CONFIRMED.** `Display.getMetrics(DisplayMetrics)` / `getRealMetrics()` supply
  density and size, but `getMetrics` is **deprecated since API 30** for UI layout
  (use `WindowMetrics`). For pure profiling (dpi, physical dimensions) it remains
  usable; for layout decisions EmuTune should use `WindowMetrics`/Compose layout.
  - [Display reference](https://developer.android.com/reference/android/view/Display)

---

## 5. GPU renderer / vendor — OpenGL ES & EGL (and Vulkan)

- **CONFIRMED.** The standard way to read the GPU renderer/vendor is via the
  OpenGL ES strings:

  ```kotlin
  val vendor   = GLES20.glGetString(GLES20.GL_VENDOR)     // e.g. "Qualcomm", "ARM", "Imagination"
  val renderer = GLES20.glGetString(GLES20.GL_RENDERER)   // e.g. "Adreno (TM) 740", "Mali-G78"
  val version  = GLES20.glGetString(GLES20.GL_VERSION)
  val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS)
  ```

  EGL also exposes `EGL_VENDOR` / `EGL_VERSION` via
  `EGL14.eglQueryString(eglDisplay, EGL14.EGL_VENDOR)`.
  - [GLES20 reference](https://developer.android.com/reference/android/opengl/GLES20)
  - [Build an OpenGL ES environment](https://developer.android.com/develop/ui/views/graphics/opengl/environment)

- **CONFIRMED — hard requirement.** `glGetString` is only valid with a **current
  GL context on the calling thread**. In practice this means either calling it
  inside a `GLSurfaceView.Renderer.onSurfaceCreated(...)` or creating a throwaway
  offscreen context (a 1×1 EGL pbuffer surface) just to query the strings, then
  tearing it down.

- **CONFIRMED — limitations.** The strings are **implementation-provided** and
  may be generic or misleading: a driver may return `"Android"` as vendor or a
  generic renderer string; they are not a guaranteed, stable, or spoof-proof
  hardware identity. They are a *good heuristic*, not a fact.

- **LIKELY — Vulkan alternative.** Vulkan exposes richer, more precise device
  properties (`VkPhysicalDeviceProperties.deviceName`, `vendorID`, `deviceID`)
  via native code, but requires a Vulkan instance and native/JNI integration,
  and `deviceName` is still driver-supplied text. This is the most accurate
  public option but is materially more work than the GLES strings.
  - (Vulkan spec / Khronos — not a `developer.android.com` page; listed here as a
    directional note, not a confirmed Android API guarantee.)

- **Verdict.** Use GLES strings for a lightweight GPU hint (renderer/vendor).
  Do not treat them as a reliable SoC or GPU-die identity. Add Vulkan device
  props only if precise GPU identification becomes a hard product requirement.

---

## 6. Thermal APIs — `PowerManager`

- **CONFIRMED — status (API 29+).** `PowerManager.currentThermalStatus` and
  `PowerManager.addThermalStatusListener(...)` expose a **coarse, device-level**
  thermal status as one of `THERMAL_STATUS_NONE / LIGHT / MODERATE / SEVERE /
  CRITICAL / EMERGENCY / SHUTDOWN`. `OnThermalStatusChangedListener` was added in
  API 29.
  - [PowerManager reference](https://developer.android.com/reference/android/os/PowerManager)
  - [OnThermalStatusChangedListener reference](https://developer.android.com/reference/android/os/PowerManager.OnThermalStatusChangedListener)

- **CONFIRMED — headroom forecast (API 31+).** `PowerManager.getThermalHeadroom(int
  forecastSeconds)` returns a float ≥ 0.0 where **1.0 = the `THERMAL_STATUS_SEVERE`
  throttling threshold** (values may exceed 1.0 for heavier throttling; negatives
  clamp to 0.0). It returns **`NaN`** if the device does not support it, if the
  forecast window is invalid, or if it is called significantly faster than ~once
  per second. It only models **slow-moving sensors** (e.g. skin temperature) —
  not per-component CPU/GPU temperatures.
  - [PowerManager reference](https://developer.android.com/reference/android/os/PowerManager)
  - [Thermal API (Android game development)](https://developer.android.com/games/optimize/adpf/thermal)

- **LIKELY — listener (newer).** `PowerManager.addThermalHeadroomListener(...)` /
  `getThermalHeadroomThresholds()` provide a push model for headroom threshold
  changes. On Android 16 (API 36) the polling API's results may vary by device
  and the listener approach is the recommended path for threshold changes.
  - [PowerManager reference](https://developer.android.com/reference/android/os/PowerManager)
  - [Thermal (Android NDK)](https://developer.android.com/ndk/reference/group/thermal)

- **CONFIRMED — limitation.** Precise **per-component thermal sensors are largely
  restricted**: there is no public API to read individual CPU/GPU/battery
  temperature sensors. Reading `/sys/class/thermal/*` is an unofficial,
  permission-dependent heuristic — **not** a supported, stable API and **not**
  something EmuTune should depend on.
  - [Thermal API (Android game development)](https://developer.android.com/games/optimize/adpf/thermal)

- **Verdict.** EmuTune gets a **device-level** thermal signal only. Use it to
  degrade gracefully (reduce workload before throttling), never to display
  per-core temperatures. Always handle `NaN` headroom and the "status not yet
  updated" case (a high headroom value alongside `THERMAL_STATUS_NONE`).

---

## 7. `PackageManager` detection — see package-visibility.md

For detecting installed emulators, use
`PackageManager.getPackageInfo(packageName, 0)` inside a
`NameNotFoundException` guard, backed by a `<queries>` declaration. This is
covered in full in
[`package-visibility.md`](./package-visibility.md).

---

## 8. Explicit "unknown value" handling

EmuTune's profiler must normalise every field into one of three states:
**`KNOWN(value)` / `UNKNOWN` / `GENERIC(value)`**, where `GENERIC` flags values
that are technically present but carry no real signal (e.g. a GPU renderer of
`"Android"` or an SoC model of `"arm64"`).

Concrete normalisation rules:

| Field | Treat as UNKNOWN/GENERIC when… |
| --- | --- |
| `Build.SOC_MODEL` / `SOC_MANUFACTURER` | equals `"UNKNOWN"`, empty, or a generic architecture string |
| `Build.MODEL` / `MANUFACTURER` / etc. | empty (do not treat as authoritative regardless) |
| `ActivityManager.MemoryInfo.totalMem` | ≤ 0 (never expected, but guard) |
| `Display.getRefreshRate()` / `getMode()` | ≤ 0 (display not yet attached / no mode) |
| `GLES20.glGetString(...)` | null or a known-generic string (`"Android"`, `"OpenGL ES"`) |
| `PowerManager.getThermalHeadroom(...)` | `Float.isNaN(value)` → "thermal API unavailable" |
| `getPackageInfo(...)` | `NameNotFoundException` → "not visible to EmuTune" |

No UI surface may print a raw `"UNKNOWN"` to a user; all such states render as a
clear "unavailable" affordance instead.

---

## 9. Sources

- [Build reference](https://developer.android.com/reference/android/os/Build)
- [Build.java source (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/main/core/java/android/os/Build.java)
- [ActivityManager reference](https://developer.android.com/reference/android/app/ActivityManager)
- [ActivityManager.MemoryInfo reference](https://developer.android.com/reference/android/app/ActivityManager.MemoryInfo)
- [Manage your app's memory](https://developer.android.com/topic/performance/memory)
- [Display reference](https://developer.android.com/reference/android/view/Display)
- [Display.java source (AOSP)](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/view/Display.java)
- [GLES20 reference](https://developer.android.com/reference/android/opengl/GLES20)
- [Build an OpenGL ES environment](https://developer.android.com/develop/ui/views/graphics/opengl/environment)
- [PowerManager reference](https://developer.android.com/reference/android/os/PowerManager)
- [OnThermalStatusChangedListener reference](https://developer.android.com/reference/android/os/PowerManager.OnThermalStatusChangedListener)
- [Thermal API (Android game development)](https://developer.android.com/games/optimize/adpf/thermal)
- [Thermal (Android NDK)](https://developer.android.com/ndk/reference/group/thermal)
- [PackageManager reference](https://developer.android.com/reference/android/content/pm/PackageManager)
