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
was written and reviewed carefully but **has not been compiled**. To build the full app,
open the project in Android Studio (or run `./gradlew assembleDebug`) somewhere with normal
internet access; standard first-build dependency resolution is all that's required.
