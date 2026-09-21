# Bow Tune

Android starter app for compound bow tuning: paper-tear diagnosis, a persisted tune checklist, gear/setups, session logs, on-device **Shot Vision** (pose coaching + paper-tear photo assist), and brand guides (Mathews Limb Shift, PSE EZ.220, Hoyt XTS, Bowtech DeadLock, Elite S.E.T.).

Built with **Kotlin**, **Jetpack Compose (Material 3)**, **Navigation**, **DataStore**, **Room**, **CameraX**, and **ML Kit Pose Detection** (on-device, no paid API keys).

## Open & run in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug / recent stable recommended) with Android SDK 35 and JDK 17.
2. **File → Open** and select this project folder (`Bowtune`).
3. When prompted, let Android Studio sync Gradle.  
   - If `gradle/wrapper/gradle-wrapper.jar` is missing, Android Studio / the IDE Gradle sync will generate it from `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9). You can also run `gradle wrapper` if you have Gradle installed locally.
4. Copy `local.properties.example` → `local.properties` only if needed. Android Studio normally creates `local.properties` with your `sdk.dir`. **Do not commit `local.properties`** (it can contain machine-specific paths).
5. Select an emulator or device, then click **Run** on the `app` configuration (`com.strobingn.bowtune`).

Minimum SDK: 26 · Target / compile SDK: 35 · Version: **1.3.0** (versionCode 5)

## Branding / theme

UI is **grayscale only** (blacks, greys, whites). `BowTuneTheme` sets **`dynamicColor = false`** by default so Material You cannot inject purple/lavender or other chromatic accents on device wallpapers.

## App tabs

| Tab | What it does |
|-----|----------------|
| **Home** | Active bow, last tear, checklist %, next due, FOC/spine/KE tools, backup, report, coaching + accessibility. |
| **Paper Tear** | Tap-zone diagram + Easton CR left/right/4–6 ft steps, TTS, tear history. |
| **Checklist** | Templates (full / paper / hunting / indoor) with % complete and per-template reset. |
| **Gear** | Multi-bow profiles (Set active), arrows + FOC, adjustment logbook, side-by-side compare, cam/cable service dates. |
| **Sessions** | Group / paper / bare / walk-back / broadhead logs with conditions, shareable report, brand short wizards + LIFT 29.5. |
| **Vision** | On-device camera + ML Kit pose landmarks. **Form** mode: live skeleton overlay, freeze-frame coaching heuristics (shoulder tilt, elbow angles, head/lean), save notes to a `TuneSession`. **Paper tear** mode: capture a still, manually confirm tear type (assist only — no trained tear CV), jump into Paper Tear guidance. |
| **Guides** | **Advanced Tuning library** (bare shaft walkthroughs, fletched, nock clocking, walk-back, French, planing, broadheads, CR paper depth, torque, yoke/Limb Shift, tiller/timing, dynamic spine) plus Mathews Limb Shift, PSE EZ.220, Hoyt XTS, Bowtech DeadLock, Elite S.E.T. |

## Shot Vision notes

- Requires **CAMERA** permission (runtime prompt).
- Pose works best with decent lighting and a **side-ish / 3⁄4** angle at full-draw-ish stance.
- Coaching tips are **heuristics**, not a coach — low-confidence states are called out in the UI.
- Paper-tear photo flow is labeled **assist**; you confirm the tear type yourself.

## Download a debug APK (GitHub Actions)

A workflow builds `assembleDebug` on every push to `main` and on manual runs:

1. Open the repo on GitHub → **Actions**.
2. Select the **Build Debug APK** workflow.
3. Open the latest successful run.
4. Under **Artifacts**, download **bowtune-debug-apk** (retained 30 days).
5. Unzip the artifact and install the `.apk` on a device (allow install from unknown sources / via `adb install`).

Stable release asset (overwritten each successful `main` build):

https://github.com/Strobingn/Bowtune/releases/download/bowtune-debug-latest/app-debug.apk

You can also start a build with **Actions → Build Debug APK → Run workflow**.

## Gradle wrapper note

This repo includes `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`.  
If `gradle-wrapper.jar` is not present (binary often omitted from text-only pushes), Android Studio will generate it on sync, and the CI workflow downloads it before `./gradlew assembleDebug`.


## Advanced Tuning library (1.3.0)

Reachable from **Home**, **Guides**, **Logs** (including the bare/walk-back/paper/broadhead session editor), and **Tear** (after the order-of-attack card). Expandable cards plus optional stepped “Walk through on the range” screens.

Compound + mechanical release first. Easton CR paper left/right rest directions on the Tear tab are unchanged (RH nock-left → rest right / Limb Shift L; nock-right → rest left / Limb Shift R; clearance before stiff-spine on rights). Finger/recurve inversions are labeled in the library, not applied to Tear.

A **Bare shaft** checklist template walks the field-first sequence (setup → optional paper → nock → horizontal → walk-back/French/BH).

## Guided sessions

- **Sessions → LIFT 29.5 — Vertical Tune (90 min)** — step-through protocol for Mathews LIFT 29.5 prioritizing bare-shaft vertical first (baseline → bare vertical → paper confirm → torque → walk-back → balance → final 30). End screen includes a decision tree and **required** four-number wrap-up (`bareShaftHl20`, `bareShaftLr20`, `walkBackLr30`, `final30GroupSize`). Completing saves a `GuidedTuneSession` and a `TuneSession` summary note with those numbers.

## Project layout

```
app/src/main/java/com/strobingn/bowtune/
  data/           # Room, DataStore, paper-tear guidance, checklist, LIFT guided plan, FormAnalysis
  data/advanced/  # Advanced Tuning library copy (bare, French, walk-back, nock, paper depth, …)
  ui/screens/     # papertear, checklist, gear, sessions, vision, guides
  ui/navigation/  # bottom nav + NavHost
  ui/theme/       # Material 3 grayscale theme (dynamicColor off)
```

## License / disclaimer

Tuning guidance is educational — always follow your bow manufacturer’s manuals and safe shop practices. Small adjustments, one change at a time, and verify with paper / bare shaft / walk-back before hunting. Shot Vision pose tips are assistive heuristics only.
