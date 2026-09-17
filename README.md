# Mahjong Scorer

Offline Android app for tracking mahjong scores across a full game, supporting both
Japanese (Riichi) and Hong Kong scoring. No network access is used anywhere.

## Project layout

- `:scoring` — pure Kotlin/JVM module with no Android dependency: tile model, hand
  decomposition, and the two scoring engines (`JapaneseScoringEngine`,
  `HongKongScoringEngine`). Has real JUnit tests (`./gradlew :scoring:test`).
- `:app` — the Android app (Kotlin + Jetpack Compose + Room), depends on `:scoring`.

## App flow

Home → choose Japanese/Hong Kong → enter 4 player names (East/South/West/North seating)
→ round screen (per-player score + wind, "WIN!" button per player) → tile picker to enter
the winning 14-tile hand → automatic yaku/fan detection and scoring → back to round screen
→ "End Game" saves final scores to history and returns home.

**Crash/close safety**: every score-changing action (`declareRiichi`, applying a win, a
draw, editing dora) is persisted to a local Room database (`GameRepository.saveGameState`)
immediately, synchronously with the state update. On relaunch, `GameViewModel` loads the
most recent unfinished game and the UI navigates straight back into the round screen — no
in-progress game is lost if the app is killed or closed accidentally. (The one exception:
if the app is killed *while the win-entry tile picker is open but not yet confirmed*, that
one in-progress tile selection is lost — the already-applied scores before it are not.)

## Rule-set decisions

- **Japanese starting score: 25,000** — the standard tournament/online (Tenhou, Majsoul)
  starting score. A starting-score field on the setup screen could be added later if you
  want to change it per game.
- **Win entry: full 14-tile hand, auto-detected scoring** — the user taps every tile
  the winning hand contains, plus context flags (riichi, ippatsu, tsumo/ron, dora
  indicators, etc.); the engine finds the highest-scoring valid decomposition and its
  yaku/fan itself, rather than asking the user to name yaku manually.
- **Japanese mechanics implemented**: riichi sticks (1000-point bets, pooled, paid to the
  next winner), honba counters (300/discard or 100/player on tsumo, reset when the dealer
  changes), dealer repeat on dealer win or dealer-tenpai draw, exhaustive draw tenpai/noten
  payments (3000-point pool split by tenpai count), dora/uradora/aka (red 5) counting.

## Simplifications (documented, not silent)

These keep the scoring engine tractable while staying correct for the vast majority of
real hands. They're implemented as clear, isolated rules rather than vague approximations:

- **No kan (quad) tracking.** The tile picker always builds a 14-tile hand of 4 sets + a
  pair (or seven pairs / thirteen orphans); kans, kan-dora, and rinshan draws aren't
  modeled as a distinct tile count. `isRinshan`/is`Chankan` are available as manual
  situational flags for when they apply.
- **Open melds are a single "closed hand?" toggle**, not per-meld call tracking (which
  specific tiles were pon'd/chi'd from whom). This is enough to gate menzen-only yaku
  (riichi, menzen tsumo, pinfu, iipeiko, etc.) correctly; it slightly simplifies fu
  scoring for open hands (all triplets in an open hand are treated as open/minkou for fu,
  rather than tracking which ones were actually called).
- **Yakuman are not doubled.** Hands like daisuushii or a 13-sided kokushi wait, which some
  rule sets score as *double* yakuman, are scored as a single yakuman (13 han / limit
  hand) here — the more common convention in casual and many competitive rule sets.
- **Hong Kong fan table**: uses the textbook doubling formula
  `points = baseUnit * 2^(fan-1)` (default `baseUnit = 2`), capped at a configurable
  limit-hand fan (default 10). Minimum fan to win defaults to 3 ("old style" Hong Kong),
  both are engine parameters (`HongKongWinInput.baseUnit`/`minFanToWin`) if your table
  differs. Flower/bonus tiles are not modeled.
- **Hand ambiguity**: when a 14-tile hand can be read more than one valid way (e.g. a
  shape that's both toitoi and a run-based hand), the engine scores every valid reading
  and reports the highest-scoring one, per standard rules.

## Building

This was developed in a sandboxed environment without access to Google's Maven repository
(`dl.google.com`), so the Android Gradle Plugin, AndroixX/Compose/Room artifacts could not
be resolved or compiled here — only the dependency-light `:scoring` module could be built
and tested (`./gradlew :scoring:test`, passing). The `:app` module's Kotlin/Compose code
was written and reviewed carefully but **has not been compiled**. Build it the first time
somewhere with normal internet access (see below); standard first-build dependency
resolution is all that's required, no special setup.

### Option A — Android Studio (easiest, recommended)

1. Install [Android Studio](https://developer.android.com/studio) if you don't have it.
2. `File → Open`, pick the cloned `mahjongapp` repo root (the folder with `settings.gradle.kts`).
3. Let Gradle sync finish (first sync downloads dependencies — needs internet, takes a
   few minutes). If it complains about an Android SDK, use `Tools → SDK Manager` to
   install one — Android Studio does this automatically on first run for most people.
4. Connect your phone:
   - On the phone: **Settings → About phone**, tap "Build number" 7 times to unlock
     Developer Options, then **Settings → Developer options → USB debugging** (enable it).
   - Plug the phone into your computer with a USB cable. A prompt appears on the phone
     asking to allow USB debugging from this computer — accept it.
5. In Android Studio's toolbar, pick your phone from the device dropdown (top, next to
   the green ▶ Run button), then click ▶ **Run 'app'**. It builds, installs, and launches
   on your phone automatically.

No cable? Use Wi-Fi debugging instead: **Developer options → Wireless debugging**, then in
Android Studio `Device Manager → Pair Devices Using Wi-Fi` and follow the on-screen pairing
code.

### Option B — command line (`gradlew` + `adb`)

Requires the [Android command-line tools](https://developer.android.com/studio#command-tools)
or a full Android Studio install (for `adb`), plus USB debugging enabled on the phone as in
step 4 above.

```bash
cd mahjongapp
./gradlew assembleDebug        # builds app/build/outputs/apk/debug/app-debug.apk
adb devices                    # confirm your phone shows up (accept the USB-debugging prompt if asked)
adb install app/build/outputs/apk/debug/app-debug.apk
```

Then find "Mahjong Scorer" in your phone's app drawer.

### Notes

- No Google Play / signing setup needed for this — it's a debug build for your own device.
- The app requests no special permissions and never touches the network, so there's nothing
  else to configure.
- If Gradle sync fails on unfamiliar SDK/build-tools versions, let Android Studio's prompt
  auto-install the missing pieces rather than editing version numbers by hand.
