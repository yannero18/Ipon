# Ipon

**Ipon** (Tagalog for "save" / "savings") is a private, offline-first personal finance journal built specifically for the Philippine market.

No account. No login. No internet connection required — or even possible. Ipon never asks for network access, which means your financial life never leaves your phone. Everything you log, every envelope you set, every peso you save toward a goal stays on your device, under your control, until you decide to export or delete it yourself.

## Why Ipon

Most budgeting apps want you to connect a bank account, create a profile, and sync to the cloud. Ipon does the opposite. It's built around a simple idea: **your money, your data, your device — nothing else involved.**

- **Centavo-exact.** Every amount is stored as a whole number of centavos, never as a floating-point decimal — so nothing ever quietly rounds away over thousands of entries.
- **Built for how money actually moves here.** Categories, merchant recognition, and the visual language are shaped around everyday Philippine spending: jeepneys and tricycles, sari-sari stores, sweldo, padala, SSS and PhilHealth contributions — not a generic template translated after the fact.
- **Nothing logs itself without you.** Recurring bills show up as a card to confirm, never a silent entry. There is no auto-categorization you can't see or override, and no background sync quietly disagreeing with what's on your screen.

## What you can do with it

**Track.** Log income and expenses in seconds, with smart category suggestions that improve the more you use the app — built entirely from your own corrections, never shared anywhere.

**Plan.** Set monthly spending envelopes (with a one-tap suggestion based on your own 3-month average), confirm recurring bills on your terms, save toward specific goals, and pay down debt with a clear snowball or avalanche order.

**Understand.** See where your money actually goes each month, how it's trending versus last month, and how your income compares to the classic 50/30/20 framework or a zero-based "every peso has a job" view. A one-tap mood check-in pairs how you feel with what actually happened that day. At year's end, a recap pulls it all together.

**Stay in control.** Export everything to a CSV file in your own Downloads folder whenever you want a copy. Clear everything permanently if you want to start fresh. See exactly what the app has learned about your spending habits, and forget any of it with one tap.

## Screens at a glance

| | |
|---|---|
| **Ledger** | Your running balance, recent activity, and a daily mood check-in |
| **Plan** | Envelopes, Recurring bills, Goals, Debts, and Budget frameworks (50/30/20, zero-based) |
| **Insights** | Category breakdown, month-over-month trends, and honest savings suggestions |
| **Settings** | Export your data, manage what the app has learned, privacy details |

## Privacy, plainly stated

Ipon requests no `INTERNET` permission at all — not "unused," not "off by default," but structurally absent from the app. There is nothing in this app that could send your data anywhere, because the capability to do so was never built. The only permission requested is `VIBRATE`, for the haptic feedback when you log a transaction.

Because there's no cloud, there's no automatic backup either — that's the deliberate cost of the privacy model, not an oversight. Export your data to CSV any time you want a copy you control.

---

## For developers

The rest of this document covers the technical build: architecture, setup, what's genuinely implemented versus an honest stand-in, and what's intentionally out of scope. See `DEVLOG.md` for the complete build history and the reasoning behind specific decisions made along the way.

### Building the APK

```bash
./gradlew assembleDebug
```

The resulting debug APK lands in `app/build/outputs/apk/debug/app-debug.apk` — install it with `adb install app/build/outputs/apk/debug/app-debug.apk`, or open the project in Android Studio and hit Run.

**No data is bundled or seeded.** The schema SQL only contains `CREATE TABLE` statements, never `INSERT`. A fresh install starts with a genuinely empty Ledger — no envelopes, no goals, no debts, nothing pre-filled. The only thing that persists without the person doing anything is a single onboarding-seen flag (`SharedPreferences`), which holds no financial data and only controls whether the first-launch intro shows again.

Three environment issues are known to come up in a fresh Codespace or container and are not bugs in the app itself:

1. **JDK version.** If `./gradlew` fails with an error referencing a JDK version string deep in a Kotlin/Gradle DSL exception, the environment's default JDK is too new for this project's toolchain. Install/select JDK 17 and make sure it's active for the shell running Gradle (`java -version` should print `17.x`). If using SDKMAN, a per-project `.sdkmanrc` pinning JDK 17 is the most reliable fix, since SDKMAN's shell init can silently override a plain `export JAVA_HOME`.
2. **Missing Android SDK.** A plain devcontainer won't have it preinstalled. Install the SDK command-line tools, set `ANDROID_HOME`, and create `local.properties` with `sdk.dir=<path>` at the repo root.
3. **Gradle wrapper jar.** If `gradle/wrapper/gradle-wrapper.jar` is missing, run `gradle wrapper --gradle-version 8.7` once a system Gradle is available, or open the project in Android Studio, which offers to regenerate it automatically.

### Fonts (cosmetic only)

The original design called for Fraunces (display) and Inter (body). This build uses system font fallbacks (`FontFamily.Serif` / `FontFamily.SansSerif` / `FontFamily.Monospace` in `ui/theme/Type.kt`) instead — a deliberate choice, not an unfinished task. No font files are bundled by design. To use the named typefaces instead:

1. Download Fraunces, Inter, and Roboto Mono from [fonts.google.com](https://fonts.google.com)
2. Drop the `.ttf` files into `app/src/main/res/font/`
3. Swap the three `FontFamily` values in `Type.kt` for the commented example at the bottom of that file

### First-run Room schema validation

`IponDatabase` uses `createFromAsset("database/ipon_schema.sql")` so every table can be declared `WITHOUT ROWID` (Room's annotations can't express that clause). `exportSchema = true` generates a schema JSON from the `@Entity` definitions at build time, which Room validates against the asset's actual columns at app startup. The two are kept in manual lockstep — if you add/rename/remove a field on any entity, edit `ipon_schema.sql` to match in the same commit, or the app will crash on first launch with a schema-mismatch exception.

### Architecture

```
data/
  local/        Room entities (9 tables), DAOs, database, raw WITHOUT ROWID schema SQL
  model/        Domain models decoupled from Room entities, plus pure calculation
                logic (trends, suggestions, payoff ordering, budget frameworks)
  repository/   Mediates DAO <-> domain models, exposes Flow; combines stored
                values with live ledger aggregates (never cached counters)
di/             Manual dependency container (no Hilt, kept dependency-free)
ui/
  theme/        Color, Shape, Type tokens
  icons/        Custom hand-drawn category icons (no emoji)
  components/   Reusable composables (BalanceCard, TransactionRow, CategoryLabel)
  screens/      Ledger, AddTransaction, Plan (Envelopes/Recurring/Goals/Debts/Budget),
                Insights, Settings, Recap, Onboarding -- each with its own ViewModel
  navigation/   Single NavHost wiring all screens (4 bottom tabs + modal/sub-routes)
util/
  Money.kt               Scaled-integer currency type
  MerchantClassifier.kt  Keyword-rule categorization (see honesty note below)
  HapticCurrencyFeedback.kt
```

### Running tests

```bash
./gradlew test
```

`MoneyTest.kt` checks the worked example (₱150.50 → 15050 minor units), verifies that summing ten ₱0.10 amounts is exactly ₱1.00 (the classic float-drift failure this architecture exists to avoid), and confirms `HALF_EVEN` rounding behavior.

### What's real vs. what's an honest stand-in

| Component | Status |
|---|---|
| `Money` (scaled-integer, centavo-first) | Fully implemented, unit-tested (`MoneyTest.kt`) |
| Room + `WITHOUT ROWID` schema (9 tables) | Real, via bundled `assets/database/ipon_schema.sql` and `createFromAsset`, schema version 7 |
| UUIDv4 primary keys | Real |
| Banker's rounding (`HALF_EVEN`) | Real, used throughout money math and percentage calculations |
| Tabular figures (`tnum`) | Real, via `fontFeatureSettings = "tnum"` |
| Editorial palette / organic squircles | Real, custom design system carried through every screen |
| Haptic currency feedback | Real, against Android's `VibrationEffect` API (26+) |
| Spring-physics row entry animation | Real, via Compose's `spring()` with `DampingRatioMediumBouncy` |
| Split Expense/Income categories | Real. Separate enums implementing a shared `TransactionCategory` interface, so a merchant string can never be classified as both. |
| **Merchant classifier** | **Honest stand-in.** A genuine on-device ML model would need a labeled dataset of real PH merchant/SMS strings that doesn't exist yet. `MerchantClassifier.kt` implements a transparent, broad keyword-rule classifier instead, scoped by transaction type, covering common PH expense and income patterns. |
| Envelopes, auto-suggested caps | Real. Live spend aggregates, never a cached counter that can drift. Caps can be suggested from a real trailing 3-month average. |
| Recurring transactions | Real, with a deliberate constraint: the app never silently posts a transaction. A due template surfaces as a card to confirm; confirming it atomically writes the transaction and stamps the template. |
| Goals | Real. Append-only contribution log, so progress is always a live `SUM()` over real entries. |
| Debts (snowball/avalanche) | Real. Mirrors the Goals pattern — remaining balance is always derived, never cached. Pure sorting logic for payoff order, not a payment-schedule simulator. |
| 50/30/20 and zero-based budgeting | Real. Pure math held up against actual income/spend; explicitly framed as a reference framework, not a rule the app enforces. Notes honestly where data is incomplete (goal contributions aren't yet month-scoped) rather than faking a number. |
| Daily Reflection | Real. A collapsible card on the Ledger pairing a one-tap mood with that day's actual numbers — not a generic standalone mood tracker. No streaks, no guilt mechanics. |
| Category memory ("the app learns") | Real, explicitly not a trained model. Remembers the user's own corrections per merchant and recalls them ahead of the generic classifier. Viewable and individually deletable in Settings. |
| Savings suggestions, recurring pattern detection | Real, rule-based, fully transparent — every suggestion states its own numeric reasoning. Never framed as AI. |
| Spending velocity | Real. "About N days of balance left," extrapolated from this month's actual daily spend rate. Framed as a pace estimate, never a guaranteed forecast. |
| Export to CSV | Real. Writes every table to the device's actual Downloads folder via `MediaStore`, with zero extra permissions. |
| Custom app icon, category icons | Real hand-drawn vectors — no emoji, no placeholder assets. |
| Onboarding | Real. Skippable 3-page intro shown once, tracked via a single `SharedPreferences` boolean. |
| Sound-on-save | **Not implemented** — flagged as a follow-up, not faked. |
| Dark mode | **Not implemented**, deliberately out of scope — the palette is built for a specific light, editorial feel. |

### Explicitly out of scope

- Bulk/multi-select delete (one transaction at a time)
- Re-importing an exported CSV (export is one-way)
- Calendar view of the Ledger with date-level pace estimation — queued for a future, focused session
- A real Gradle `Migration` path (currently `fallbackToDestructiveMigration()` — fine pre-launch, would need replacing before anyone's data depends on it surviving an update)
- **Deliberately not built, by design:** bank/e-wallet account linking, multi-account or multi-currency tracking, shared/family budgets, bill auto-pay, credit score monitoring. All of these require cloud sync or third-party financial APIs, which conflict directly with the zero-cloud, zero-login premise this app is built around.
