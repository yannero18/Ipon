# Ipon — Android Prototype

A working skeleton implementing the architecture described in the Ipon proposal: Room over SQLite with a `WITHOUT ROWID` schema, scaled-integer (centavo) money math, an editorial Compose design system, and three real screens (Ledger, Add Transaction, Insights).

## Before you open this in Android Studio

**1. Gradle wrapper jar is missing.** This environment had no network access, so `gradle/wrapper/gradle-wrapper.jar` (a binary) could not be downloaded. `gradle-wrapper.properties` is already set to Gradle 8.7. Fix this with either:

```bash
# If you have Gradle installed locally:
gradle wrapper --gradle-version 8.7

# Or just open the project in Android Studio -- it will offer to
# generate the wrapper automatically on first sync.
```

**2. No real fonts are bundled.** Section 4 of the proposal specifies Fraunces (display) and Inter (body). This prototype ships with **system font fallbacks** (`FontFamily.Serif` / `FontFamily.SansSerif` / `FontFamily.Monospace`) in `ui/theme/Type.kt`, because the actual `.ttf` files would need to be downloaded from Google Fonts, and this build environment has no network access to fetch them. The app builds and runs correctly with the fallbacks. To get the real typefaces:

1. Download Fraunces, Inter, and Roboto Mono from [fonts.google.com](https://fonts.google.com)
2. Drop the `.ttf` files into `app/src/main/res/font/`
3. Swap the three `FontFamily` values in `Type.kt` for the commented example at the bottom of that file

**3. Launcher icon is a placeholder.** A simple coin glyph on Jeepney Orange, not a final asset.

**4. First-run Room schema validation.** `IponDatabase` uses `createFromAsset("database/ipon_schema.sql")` so the `transactions` table can be declared `WITHOUT ROWID` (Room's annotations can't express that clause). `app/build.gradle.kts` also sets `exportSchema = true` via the Room Gradle plugin, which generates a schema JSON from the `@Entity` definitions on build. Room validates the asset's actual columns against that generated schema at app startup. The two are kept in manual lockstep in this repo — if you ever add/rename/remove a field on `TransactionEntity`, you must edit `ipon_schema.sql` to match in the same commit, or the app will crash on first launch with a Room schema-mismatch exception.

## What's real vs. what's a stand-in

| Component | Status |
|---|---|
| `Money` (scaled-integer, centavo-first) | Fully implemented, unit-tested (`MoneyTest.kt`) |
| Room + `WITHOUT ROWID` schema | Real, via bundled `assets/database/ipon_schema.sql` and `createFromAsset` |
| UUIDv4 primary keys | Real |
| Banker's rounding (`HALF_EVEN`) | Real, used in `Money.scaledBy()` and the insights percentage calc |
| Tabular figures (`tnum`) | Real, via `fontFeatureSettings = "tnum"` in `Type.kt` |
| Editorial palette / organic squircles | Real, matches the proposal's exact hex values and corner radii |
| Haptic currency feedback | Real, against Android's `VibrationEffect` API (26+) |
| Spring-physics row entry animation | Real, via Compose's `spring()` with `DampingRatioMediumBouncy` |
| **Merchant classifier (Section 3)** | **Honest stand-in.** Section 3 describes an on-device foundation model. That needs a labeled dataset of real PH merchant/SMS strings that doesn't exist yet. `MerchantClassifier.kt` implements a transparent keyword-rule classifier instead, behind the same interface a real model would use later — see that file's header comment. |
| Sound-on-save ("coin drop" audio) | Not implemented in this pass — flagged as a follow-up, not faked |

## Architecture

```
data/
  local/        Room entities, DAO, database, raw schema SQL
  model/        Domain models decoupled from Room entities
  repository/   Mediates DAO <-> domain models, exposes Flow
di/             Manual dependency container (no Hilt, kept dependency-free)
ui/
  theme/        Color, Shape, Type tokens straight from Section 4
  components/   Reusable composables (BalanceCard, TransactionRow, FAB)
  screens/      Ledger, AddTransaction, Insights — each with its own ViewModel
  navigation/   Single NavHost wiring the three screens
util/
  Money.kt               Scaled-integer currency type
  MerchantClassifier.kt  Categorization (see honesty note above)
  HapticCurrencyFeedback.kt
```

## Running tests

```bash
./gradlew test
```

`MoneyTest.kt` checks the proposal's own worked example (₱150.50 → 15050 minor units), verifies that summing ten ₱0.10 amounts is exactly ₱1.00 (the classic float-drift failure this architecture exists to avoid), and confirms `HALF_EVEN` rounding behavior.

## Explicitly out of scope for this prototype

- The on-device merchant classification model itself (needs real training data first)
- Dark mode (the proposal's palette is built for a specific light, print-lookbook feel — see the note in `Theme.kt`)
- Envelope budgeting (shown as a nav placeholder in the design mockup, not wired up)
- Sound-on-save
