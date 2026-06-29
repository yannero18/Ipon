# Ipon — Project Devlog

A running record of what Ipon is, what's been built, why specific decisions were made, what broke and how it got fixed, and what's still open. Meant to be read by future-you (or a future Claude session) to pick this project up without re-deriving any of the reasoning from scratch.

---

## 1. What Ipon Is

**Ipon** (Tagalog for "save"/"savings") is an offline-first, zero-login expense journal built specifically for the Philippine market. The core pitch, from the original proposal:

- No accounts, no passwords, no internet permission at all — not just unused, structurally absent from the Android manifest
- Centavo-exact money math (scaled-integer `Long`, never `Double`/`Float`) so nothing rounds away over thousands of transactions
- An editorial, print-lookbook visual language — rice paper background, ocean teal structure, jeepney orange as the *only* action accent, terracotta reserved for alerts, organic asymmetric "squircle" shapes instead of uniform rounded corners
- A philosophy of **never writing to the ledger without the user's direct action** — recurring transactions are confirmed, not auto-posted; there is no cloud sync to quietly disagree with what's on the device

Target stack: native Android, Kotlin + Jetpack Compose, Room over SQLite.

---

## 2. Sequence of Work

### Phase 1 — The proposal itself
Started from a written proposal document (Executive Summary through "Intangible Interaction Mechanics," Section 5, which was incomplete). Gave honest feedback before building anything:
- Flagged overclaiming language ("mathematically impossible" UUID collisions, "Sub-10ms" performance claims, "local foundation models" with no real training data) and rewrote those sections more defensibly.
- Finished Section 5 (haptics, spring-physics motion, sound-as-confirmation, swipe resistance).
- Noted the proposal had no business/market section — flagged, left out of scope by choice.

### Phase 2 — Design mockup
Built a standalone `design-mockup.html` (no JS framework, just HTML/CSS) implementing the exact palette and squircle geometry from the proposal: rice paper `#F9F6F0`, ocean teal `#1D5D6B`, jeepney orange `#F28C38`, terracotta `#D1664F`, kape brown `#4A3B32`. Rendered it with `wkhtmltoimage` to visually verify before moving on, rather than trusting CSS blind.

### Phase 3 — Working Android prototype, first pass
Built a complete Gradle project structure from scratch:
- `Money` value class — scaled-integer centavo storage, `HALF_EVEN` (banker's) rounding, unit-tested (`MoneyTest.kt`) including the proposal's own worked example (₱150.50 → 15050 minor units) and a float-drift regression test (ten ₱0.10 additions must equal exactly ₱1.00).
- Room database with a **bundled raw SQL schema** (`assets/database/ipon_schema.sql`) loaded via `createFromAsset`, specifically because Room's `@Entity` annotation cannot emit `WITHOUT ROWID` — that clause only exists in hand-written SQL, kept in manual lockstep with the Kotlin entities.
- Three screens: Ledger (home), Add Transaction, Insights.
- A `MerchantClassifier` — explicitly **not** the "on-device foundation model" the proposal described, since no labeled PH merchant-string dataset exists. Built as an honest, transparent keyword-rule classifier instead, documented as a stand-in behind a swappable interface.
- Compose theme tokens (`Color.kt`, `Shape.kt`, `Type.kt`) translating the proposal's exact palette and squircle radii into code.
- Haptic feedback (`HapticCurrencyFeedback.kt`) against Android's real `VibrationEffect` API.
- Spring-physics row entry animation (`DampingRatioMediumBouncy`) for the "coin settling into a jar" feel from Section 5.

Packaged and delivered as a zip. Documented every honest gap in a `README.md` (missing Gradle wrapper jar, no bundled custom fonts, classifier scope) rather than letting them surface silently later.

### Phase 4 — "What's next" / feature expansion
User asked what else the app needed. Proposed and built, in dependency order:

1. **Split Expense/Income categories** — two separate enums (`ExpenseCategory`, `IncomeCategory`) behind a shared `TransactionCategory` interface, so a merchant string can never be misclassified across the expense/income boundary. Extended `MerchantClassifier` with real PH-specific income patterns (sweldo, OFW remittances, tindahan/sari-sari income, freelance/sideline, abuloy).
2. **Envelopes** — monthly spending caps per expense category. Spend totals are always a *live* aggregate over real transactions (`combine()` of stored caps + `observeCategoryBreakdownBetween`), never a cached counter that could drift. Includes "copy last month's envelopes forward."
3. **Recurring transactions** — explicitly designed as **templates the user confirms**, not auto-posting rules — this was a deliberate trust decision, not a technical limitation. Confirming a due template atomically writes the transaction and stamps the template via `Room.withTransaction`, so a killed app mid-write can never leave a transaction logged with its template still thinking it's "due."
4. **Goals** — savings targets with a separate **append-only contribution log** (not a cached "amount saved" counter), so progress is always a real `SUM()` over actual entries.
5. **Daily Reflection** — the feature meant to actually deliver the proposal's "anxiety-inducing chore → mindful ritual" thesis. A small collapsible card embedded directly on the Ledger screen (not a separate destination to remember), pairing a one-tap mood with that day's real spend/entry count. One entry per calendar day, no streaks, no guilt mechanics — deliberately calm, not gamified.
6. **Settings** — reached via a gear icon on the Ledger header (kept off the bottom nav, which was already at 5 tabs). Shows real app version via `BuildConfig`, an honest privacy statement matching what's *actually* true in the manifest, and a two-step-confirmed "clear all data" wired to Room's built-in `clearAllTables()`.

Each feature was built as a full vertical slice every time: Room entity → DAO → schema SQL update (with version bump) → domain model → repository → ViewModel → Compose screen → DI wiring → nav graph registration. Database schema went from version 1 → 5 over the course of this, six tables total by the end (`transactions`, `envelopes`, `recurring_templates`, `goals`, `goal_contributions`, `daily_reflections`).

### Phase 5 — Visual identity + "smarter" features
User said the app "felt unfinished" and "not smart enough," and didn't want emoji used for category icons. Two parallel workstreams:

**Visual identity:**
- New custom app icon — an alkansya (coin jar) silhouette, replacing the placeholder square. Deliberately used Ocean Teal as the background rather than Jeepney Orange, because Section 4 of the proposal reserves orange specifically for action triggers (the FAB), not general branding — caught and corrected this mid-build after first trying orange.
- Hand-drawn custom `ImageVector` icons (`IponIcons.kt`) for every expense/income category — tricycle (Transpo), bowl (Food), lightbulb (Bills), basket (Groceries), handshake (Utang), peso-coin (Salary), etc. — replacing emoji app-wide via a `TransactionCategory.icon()` extension function and a reusable `CategoryLabel` composable.
- Goal emoji (🏡💍🎓, user-picked from a curated list) were deliberately *left alone* — that's personalization of a personal goal, a different problem from category branding consistency, so it wasn't "fixed."
- Rendered icon shapes with `wkhtmltoimage` before committing to them, to visually verify hand-written vector path math rather than trusting it blind.

**"Smarter" features:**
- Month-over-month category trends on Insights ("Up 18% from last month") — computed by calling the existing `observeCategoryBreakdownBetween` query twice (this month vs. last month) and diffing in the repository layer. No new cached statistic; can't drift from the real ledger.
- Envelope pace projection ("At this pace, projected to reach ₱X by month's end") — pure arithmetic, `(spent so far / days elapsed) × days in month`, explicitly *not* framed as AI/forecasting. Suppressed in the first 2 days of a month (too little data) and suppressed once already over budget (redundant with the existing "Over by ₱X" message).

### Phase 6 — Getting it to actually build
The user moved this into a GitHub Codespace and started running real builds. Each failure was a genuine bug or environment gap, fixed in order:

| # | Error | Cause | Fix |
|---|---|---|---|
| 1 | `java.lang.IllegalArgumentException: 25.0.2` deep in Kotlin's Gradle DSL compiler | Codespace's installed JDK was version 25 — too new for this project's Gradle 8.7 / Kotlin 1.9.24 toolchain to parse | Install/switch to JDK 17, set `JAVA_HOME` |
| 2 | `SDK location not found` | No Android SDK installed in the Codespace at all (plain devcontainer, not Android-specific image) | Installed Android SDK cmdline-tools, set `ANDROID_HOME`, created `local.properties` |
| 3 | `The string "--" is not permitted within comments` in `ic_launcher_background.xml` | Used `--` as a prose em-dash inside an XML comment — XML forbids `--` anywhere in a comment body, not just as delimiters | Replaced with commas; wrote a Python regex script to scan *every* XML comment in the project for the same mistake (found none elsewhere) |
| 4 | Four Kotlin compile errors: `Cannot find a parameter with this name: interactionSource`; two `Unresolved reference: size`; two "experimental API" errors on `TopAppBar` | (a) Used a `clickable()` overload parameter that doesn't exist on that signature; (b) missing `androidx.compose.foundation.layout.size` import in `AddTransactionScreen.kt`; (c) Material3's `TopAppBar` is `@ExperimentalMaterial3Api` and wasn't opted into | (a) simplified to the basic `clickable(onClick=...)` call; (b) added the missing import; (c) added `@OptIn(ExperimentalMaterial3Api::class)` to both affected screens. Scripted a project-wide check for each pattern afterward to confirm no other instances existed. |

As of the last message in this thread, the build had progressed past all of the above and the user was about to re-run it with these four fixes applied — **result not yet confirmed.**

---

## 3. Current Feature Inventory

| Feature | Status |
|---|---|
| `Money` (scaled-integer, centavo-exact) | Built, unit-tested |
| Room DB, `WITHOUT ROWID`, 6 tables, schema v5 | Built |
| Expense/Income category split + PH-aware classifier | Built (classifier is an honest keyword-rule stand-in, not a trained model) |
| Ledger (home screen) | Built |
| Add Transaction | Built |
| Envelopes | Built |
| Recurring (confirm-only, never auto-posts) | Built |
| Goals (append-only contribution log) | Built |
| Daily Reflection (embedded on Ledger) | Built |
| Insights + month-over-month trends | Built |
| Settings (privacy statement, clear-all-data) | Built |
| Custom app icon (alkansya/coin jar) | Built |
| Custom category icons (replacing emoji) | Built |
| Envelope spend-pace projection | Built |
| Sound-on-save ("coin drop" audio) | **Not built** — flagged, not faked |
| Export/backup before clearing data | **Not built** — flagged as a real gap once Settings added a destructive action with no export option |
| Real Gradle `Migration` path | **Not built** — using `fallbackToDestructiveMigration()`, which is fine pre-launch and dangerous post-launch (documented in code comments) |
| Dark mode | **Not built**, deliberately out of scope — the palette is built for a specific light/print-lookbook feel |
| Bottom nav at 5 tabs | Flagged as worth revisiting — past Material Design's usual comfort ceiling, and more than the original 4-tab mockup showed |

---

## 4. Standing Decisions Worth Remembering

- **Never auto-write to the ledger.** Recurring transactions are confirmed by the user, every time. This is a trust decision, not a technical shortcut — revisit only with deliberate intent, not convenience.
- **Derive, never cache, anything that can drift.** Envelope spend, goal progress, and trend percentages are all computed live from real transaction/contribution rows, specifically to avoid a cached counter silently diverging from reality.
- **Jeepney Orange is reserved for action triggers only** (the FAB, primary buttons) — never used decoratively or as a general brand color. Caught and fixed a violation of this rule on the app icon itself mid-session.
- **Terracotta is reserved for genuine alerts** (over-budget, spend trending up, destructive actions) — audited this explicitly at one point to confirm no decorative use had crept in.
- **No cloud, no backup, by design** — and Settings says so plainly, including the real cost ("if you clear data or lose this device, it's gone"), rather than hiding that tradeoff.
- **Honesty over impressiveness in claims.** The merchant classifier is explicitly documented as *not* the on-device ML model the original proposal described, because the training data doesn't exist. The pace projection is explicitly *not* framed as AI/forecasting — it's labeled as what it actually is, simple rate extrapolation.

---

## 5. How to Pick This Back Up

1. Confirm the build status from Phase 6 — did the four-fix round actually get `assembleDebug` to succeed?
2. If yes: install the APK on a device/emulator and actually use it — haptics, spring animations, and icon legibility can only really be judged by feel, not by reading code.
3. If no: paste/upload the new error log (full text or a file, not a truncated terminal screenshot) and continue the same fix-and-verify loop.
4. Known next candidates, in no particular priority order: sound-on-save, an export-before-clear-data option in Settings, revisiting the 5-tab bottom nav, real Gradle migrations before any real user data exists.

---

## 6. Phase 7 — Nav restructure + honest "automation"

After the build succeeded (Phase 6), the user asked for two things: fix the 5-tab bottom nav, and add "automation" that "learns" to generate better insights/strategy. The second request got a deliberate scoping conversation before any code, since "AI that learns" covers everything from completely reasonable to actively dishonest given this app's architecture.

**What was ruled out and why:** anything implying a trained ML model (repeats the exact overclaiming mistake already corrected once in the original proposal's Section 3), and anything that "learns across users" (requires cloud sync, which breaks the zero-cloud premise the whole app is built on).

**What got built instead — three honest, on-device, rule-based features:**

1. **Nav restructure.** Bottom nav dropped from 5 tabs to 4: Ledger, Plan, Insights, Settings. Envelopes/Recurring/Goals became sub-tabs inside one new `PlanScreen` (a simple `TabRow` switching between the three existing screens, completely unmodified internally). Settings was promoted from a hidden gear icon to a real tab, since the gear icon became redundant once Settings had its own bottom-nav slot — removed it from the Ledger header rather than leaving two paths to the same screen.

2. **Category memory.** New `merchant_category_memory` table (schema v6) storing a learned merchant-string → category mapping, built entirely from the user's own corrections. Checked *before* the keyword classifier in `AddTransactionViewModel`; a hit overrides the generic suggestion and is surfaced honestly in the UI as "You usually pick X for this" (distinct from "Suggested: X" for the keyword-rule case). Every save is treated as a vote — agreeing with the existing memory increments a confidence counter, disagreeing resets it to 1. Caught and fixed a real design mistake mid-build: initially planned the primary key as `merchantKey` alone, then realized lookups are scoped by transaction type too, so switched to a composite `(merchantKey, type)` key to match how it's actually queried.

3. **Savings strategy suggestions.** `generateSavingsSuggestions()` — a pure function, no I/O, over data already computed elsewhere (category trends, envelope progress, income/expense totals). Four rules: a category trending up 25%+ with real money behind it, any over-budget envelope, spending exceeding income this month, and a recurring-but-uncapped category. Every suggestion states its own numeric reasoning in the UI so it's verifiable, not a black box. Surfaced on Insights as "Worth a look," above the existing trends section.

4. **Recurring pattern detection.** `detectRecurringPatterns()` scans ~120 days of transaction history, groups by normalized merchant string, and suggests "want to turn this into a Recurring template?" when a merchant appears 3+ times with amounts within 15% of their own average and a roughly weekly (5-9 day) or monthly (25-35 day) gap between occurrences. Deliberately conservative thresholds — a once-in-a-while nudge, not a constantly-firing feature. Excludes merchants already covered by an existing template. Dismissal is in-memory only (a session-scoped "not now"), not persisted, since a permanent dismissal felt like more state than a low-stakes nudge deserved.

**A real architectural mistake caught mid-build:** `normalize()` for merchant-string keys initially lived as a method on `CategoryMemoryRepository` (a repository-layer class). The pattern-detection logic (`detectRecurringPatterns`, in the model layer) needed the same normalization, which would have meant the model layer importing a repository class — a layering violation. Fixed by extracting the function to `util/normalizeMerchantKey()`, a dependency-free location both layers can call.

All three features ride on data and computations that already existed (transactions, envelopes, trends) — no new ML infrastructure, no new permissions, nothing that changes the zero-cloud privacy story. Schema is now at version 6, 7 tables total.

As of this entry, **not yet rebuilt/retested** — the nav restructure and all three automation features are written and internally verified (no duplicate symbols, no dangling references, DI wiring checked) but have not gone through an actual Gradle build since being added.

---

## 7. Phase 8 — Edit/delete, export, onboarding

User asked for more feature suggestions. Proposed a ranked list (data-safety items first, polish/delight items lower priority), and the user chose all three "worth doing soon" items: edit/delete transactions, export data, and an onboarding flow. Built in that order, since edit/delete touches the most foundational UI (the transaction row) and the others are additive.

1. **Edit/delete transactions.** A real, surprising gap caught by inspection — `TransactionRow` had zero `onClick`, even though `TransactionRepository.update()`/`delete()` had existed since the very first build. Fixed by extending `AddTransactionViewModel` with edit-mode support (`initializeForEdit`, `loadTransactionForEditing`, a branching `save()` that calls `update()` instead of `add()` when editing, and a new `delete()`), and by collapsing "add" and "edit" into one nav route (`add_transaction?transactionId={id}`) with a nullable argument, rather than duplicating the form screen. A `Delete` action appears in the top bar only in edit mode, behind one confirmation dialog. `TransactionRow` is now clickable end to end: Ledger → row tap → pre-filled edit screen.

2. **Export to CSV.** `ExportRepository` writes every table to one CSV via `MediaStore.Downloads`, landing in the real Downloads folder with zero added permissions on API 29+ (scoped storage handles this natively). Caught and fixed a real gap while building this: the envelope query used (`observeForPeriod`) only covered the *current* month, which would have silently exported incomplete historical data. Added a proper `getAllEver()` query instead of disclaiming the gap in a comment, since the data genuinely exists and there was no good reason not to include it. Also distinguished "not supported on this Android version" (a real `UnsupportedOperationException`, API < 29) from a generic write failure, rather than collapsing both into one vague error message. Settings' "clear all data" copy was updated to nudge toward exporting first.

3. **Onboarding.** A skippable, 3-page `HorizontalPager` intro shown once on first launch, gated by a `SharedPreferences` boolean (the one deliberate exception to "everything lives in Room" — a single flag isn't worth a table or a migration). Copy was written to describe what the app *actually does* (no login, no cloud, PH-specific categories, recurring requires confirmation) rather than aspirational marketing language.

**A repeated mistake caught and fixed mid-session:** while editing `AndroidManifest.xml` to document why no storage permission was needed for export, used `--` as a prose dash inside an XML comment again — the exact bug that broke the build earlier in Phase 6. Caught it this time with a scripted regex check before it reached the person, and re-ran the same check across every XML file in the project (not just the one just edited) to confirm nothing else had the same problem. Worth remembering: this specific habit (prose em-dashes inside `<!-- -->`) is a recurring risk in this project specifically and should be checked every time an XML file is touched, not assumed fixed after the first correction.

As of this entry: 70 Kotlin files, schema still at version 6 (no new tables this round — export reads existing tables, onboarding uses SharedPreferences, edit/delete reuses existing entities). **Not yet rebuilt/retested** since these three features were added.

---

## 8. Phase 9 — Rulebook expansion, learned categories settings, Year-in-Ipon recap

User asked for more suggestions across features/settings/UI. Proposed a list spanning function, settings, and display categories; user picked the Year-in-Ipon recap and the learned-categories Settings screen, and separately asked for a "mind map" / "self-learning AI" for categories and a calendar view with date-based estimation.

**Important scoping conversation before building:** "self-learning AI" and "mind map" were explicitly walked back to their honest equivalents -- expanding the existing keyword rulebook (a bigger, better-researched rule list, not a model) and making the existing category-memory system visible/editable (not a new learning mechanism). This is the same correction pattern as Phase 1 (the original proposal's "on-device foundation model" overclaim) and Phase 7 (ruling out cross-user "learning" for the automation features) -- consistently declining to dress up rule-based logic as AI, even when explicitly asked for "AI."

**Calendar + date-based estimation** (tap a date, see that day's history; "the 15th is usually Bills-heavy") was explicitly deferred to a future session at the user's own suggestion, rather than squeezed in alongside three other builds. Captured in the README's out-of-scope list so it isn't lost. Conceptually it would reuse the same pace-projection math already proven out in Envelopes (Phase 6), just bucketed by day-of-week/day-of-month instead of by category-per-month -- worth remembering when it's picked up, since the building blocks already exist.

**What got built:**

1. **Expanded `MerchantClassifier` rulebook.** Substantially broadened patterns across every existing category (more transport apps, food chains, telecoms, e-wallets, online sellers) plus a brand new `ExpenseCategory.GOVERNMENT` ("Government / Dues") for SSS, PhilHealth, Pag-IBIG, BIR, LTO, and barangay/NBI clearance fees -- a real, common PH expense category that had nowhere to go before (it was falling into "Other"). Adding a new enum value to `ExpenseCategory` was treated as a small migration in its own right: every exhaustive `when` over the enum (the icon mapping in `CategoryIconMapping.kt`, the chip-tint mapping in `TransactionRow.kt`) needed an explicit `GOVERNMENT` case before the project would compile, which is exactly the safety net that pattern was built to provide back in Phase 5. A new hand-drawn `IponIcons.Government` vector (a columned-building silhouette) was added and rendered for visual verification before being wired in, same discipline as every other custom icon.

2. **Settings: "Your learned categories."** `CategoryMemoryRepository` gained `observeAll()` and `forget(memory)`, and a new screen lists every merchant→category memory with a per-entry "Forget" button -- making the category-memory system (built in Phase 7) visible and correctable instead of an invisible black box. Reached via a new "Personalization" section in Settings.

3. **Year-in-Ipon recap.** A new `RecapRepository` combines `TransactionRepository`, `GoalRepository`, and `DailyReflectionRepository` (a genuine cross-cutting aggregation, deliberately not bolted onto any single one of them) into one `YearRecap`: net income/expense, top 3 categories, busiest month, mood check-in distribution, and goals progress for the current year. Pure aggregation logic (`buildYearRecap`) is separated from the repository's I/O, kept trivially unit-testable. Reached via a link from Insights.

**A real layering violation caught and fixed while building the recap:** `CategorySlice` (a trivial category+total pair) had been living in `data/repository/TransactionRepository.kt` since the very first build. The recap's aggregation logic (`buildYearRecap`, in `data/model`) needed to reference it, which would have meant the model layer importing a repository file -- the same class of mistake as the `normalize()` function caught in Phase 7. Fixed the same way: moved `CategorySlice` to the model layer, updated all four call sites (`TransactionRepository.kt`, `InsightsViewModel.kt`, `InsightsScreen.kt`, plus the new `YearRecap.kt`) to import from its new home.

As of this entry: 76 Kotlin files, schema still at version 6 (no new tables -- the rulebook expansion is pure logic, the learned-categories screen reads/writes the existing memory table, and the recap is a read-only aggregation over existing tables). **Not yet rebuilt/retested** since these three features were added.

---

## 9. Phase 10 — "Top budget app" feature parity, evaluated honestly

User said the app felt "too generic" compared to top budget-tracker apps and asked specifically about auto-budgeting and 50/30/20. Rather than building feature-parity blindly, did an explicit pass mapping common budget-app features into two buckets:

**Genuinely fits Ipon's single-device, manual-entry model:** 50/30/20 and other budget frameworks (pure math over data already tracked), auto-suggested envelope caps from history, net worth snapshots, bill due-date reminders, spending velocity, zero-based budgeting, debt payoff tracking.

**Structurally conflicts with Ipon's premise:** bank/e-wallet account linking (needs internet + third-party APIs), multi-account/multi-currency tracking (adds complexity against the "simple PH personal journal" pitch), shared/family budgets (needs sync between devices), bill auto-pay (Ipon only records what happened, it doesn't move money), credit score monitoring (needs a credit bureau API). All of these were named explicitly as deliberately out of scope, not silently dropped -- the constraint (zero-cloud, zero-login, single device) was treated as a real design boundary, not an oversight to apologize for.

User picked four to build: auto-suggested envelope caps, 50/30/20, zero-based budgeting, spending velocity, and (after "suggest more please") added a debt payoff tracker as a fifth.

**What got built, in dependency order:**

1. **Auto-suggested envelope caps.** `TransactionRepository.observeCategoryAverages(monthCount)` computes a trailing 3-month average per category by combining N month-range Flows with the dynamic-arity `combine(List<Flow<T>>)` overload (the first time this project needed that variant rather than the fixed 2-5-argument ones). Wired into the existing `SetCapDialog` as a one-tap "use ₱X (your 3-month average)" suggestion, shown only when there's no existing cap to override.

2. **50/30/20 and zero-based budgeting**, built together as a new "Budget" sub-tab inside Plan (now 5 sub-tabs: Envelopes, Recurring, Goals, Debts, Budget). The needs/wants/savings category mapping for 50/30/20 (`ExpenseCategory.fiftyThirtyTwentyBucket()`) was deliberately written as one explicit, visible `when` table rather than buried logic -- bucketing categories into "needs" vs "wants" is a judgment call, not an objective fact, so the mapping itself needed to be inspectable. Zero-based budgeting ("every peso has a job") compares income against envelope caps + active recurring expenses; building it surfaced a real gap -- `GoalRepository.observeProgress()` only exposes lifetime contribution totals, not month-scoped ones -- so goal contributions are honestly shown as ₱0 "assigned" for now with a visible note, rather than computing a plausible-looking but wrong number from lifetime data.

3. **Spending velocity** ("At this pace, about N days of balance left") on the Ledger's balance card. Added `estimateDaysOfRunway()` alongside the existing `projectSpend()` in `CategoryTrend.kt` -- and immediately caught a real near-miss while editing that file: the first `str_replace` attempt *replaced* `projectSpend` instead of adding alongside it, which would have silently broken `Envelope.kt`'s pace-projection feature from Phase 9. Caught by checking grep output for both function names immediately after the edit, before moving on -- restored both functions in the same file correctly.

4. **Debt payoff tracker**, the most structurally distinct addition: new `DebtEntity`/`DebtPaymentEntity` tables (schema v7, 9 entity tables total) mirroring the Goal/GoalContribution append-only pattern for the same reason -- remaining balance is always derived (`originalBalance - SUM(payments)`), never a cached, driftable column. Supports both snowball (smallest balance first, for momentum) and avalanche (highest interest first, mathematically optimal) payoff ordering via a pure sorting function, `List<DebtProgress>.orderedForPayoff(method)` -- this doesn't simulate a payment schedule or compute a payoff date, it only answers "which debt should extra money go to first," which is the actual decision these methods exist to make.

As of this entry: 86 Kotlin files, schema at version 7, 9 entity tables. **Not yet rebuilt/retested** since this batch -- the largest single addition yet (4 new screens' worth of wiring, 2 new DB tables, a new dynamic-arity `combine()` usage, a new sealed PlanTab case). Worth a real build before adding anything further.
