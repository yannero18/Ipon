-- Ipon schema, version 7.
--
-- Room's @Entity annotation cannot emit "WITHOUT ROWID" -- it always generates
-- a standard CREATE TABLE. To get the clustered-index behavior described in
-- the proposal (Section 2), the table is created from this file via
-- Room.databaseBuilder(...).createFromAsset("database/ipon_schema.sql")
-- and Room's generated DAO/query code is verified against it at compile time
-- via the exported schema in app/schemas/.
--
-- IMPORTANT: if you add/rename/remove a column on TransactionEntity,
-- EnvelopeEntity, RecurringTemplateEntity, GoalEntity,
-- GoalContributionEntity, DailyReflectionEntity, MerchantCategoryMemoryEntity,
-- DebtEntity, or DebtPaymentEntity, this file and the entity must be changed
-- together, or Room's schema validation will fail at app startup
-- (RoomOpenHelper mismatch). See IponDatabase.kt for the version number and
-- migration wiring -- a destructive fallback migration is used for this
-- prototype (see comment there), so schema changes during development are
-- safe to make without writing real Migration objects yet.

CREATE TABLE IF NOT EXISTS transactions (
    id TEXT NOT NULL PRIMARY KEY,
    amountMinorUnits INTEGER NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    merchantRaw TEXT,
    note TEXT,
    occurredAtEpochMillis INTEGER NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL,
    isAutoCategorized INTEGER NOT NULL DEFAULT 0
) WITHOUT ROWID;

CREATE INDEX IF NOT EXISTS index_transactions_occurredAtEpochMillis
    ON transactions (occurredAtEpochMillis);

CREATE INDEX IF NOT EXISTS index_transactions_category
    ON transactions (category);

-- Section 4 design mockup's "Envelopes" nav tab: one budget cap per
-- expense category per calendar month.
CREATE TABLE IF NOT EXISTS envelopes (
    id TEXT NOT NULL PRIMARY KEY,
    category TEXT NOT NULL,
    periodYearMonth TEXT NOT NULL,
    capMinorUnits INTEGER NOT NULL,
    createdAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE INDEX IF NOT EXISTS index_envelopes_periodYearMonth
    ON envelopes (periodYearMonth);

-- One cap per category per month -- upserting an envelope for a category
-- that already has one for that month should replace it, not duplicate it.
CREATE UNIQUE INDEX IF NOT EXISTS index_envelopes_category_period
    ON envelopes (category, periodYearMonth);

-- Recurring transaction TEMPLATES -- the user confirms each cycle rather
-- than the app silently posting on their behalf. See
-- RecurringTemplateEntity.kt's header comment for why.
CREATE TABLE IF NOT EXISTS recurring_templates (
    id TEXT NOT NULL PRIMARY KEY,
    label TEXT NOT NULL,
    amountMinorUnits INTEGER NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    merchantRaw TEXT,
    frequency TEXT NOT NULL,
    dayOfPeriod INTEGER NOT NULL,
    lastConfirmedPeriodKey TEXT,
    isPaused INTEGER NOT NULL DEFAULT 0,
    createdAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

-- Savings goals. See GoalEntity.kt's header comment for why contributions
-- live in a separate append-only table rather than a cached running total.
CREATE TABLE IF NOT EXISTS goals (
    id TEXT NOT NULL PRIMARY KEY,
    label TEXT NOT NULL,
    targetMinorUnits INTEGER NOT NULL,
    emoji TEXT NOT NULL,
    isArchived INTEGER NOT NULL DEFAULT 0,
    createdAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS goal_contributions (
    id TEXT NOT NULL PRIMARY KEY,
    goalId TEXT NOT NULL,
    amountMinorUnits INTEGER NOT NULL,
    note TEXT,
    contributedAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId
    ON goal_contributions (goalId);

-- One mood check-in per calendar day -- re-saving the same day replaces
-- (see DailyReflectionDao's REPLACE-on-conflict insert) rather than
-- accumulating duplicate entries for one day.
CREATE TABLE IF NOT EXISTS daily_reflections (
    id TEXT NOT NULL PRIMARY KEY,
    dayKey TEXT NOT NULL,
    mood TEXT NOT NULL,
    note TEXT,
    createdAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE UNIQUE INDEX IF NOT EXISTS index_daily_reflections_dayKey
    ON daily_reflections (dayKey);

-- Learned merchant -> category mappings, built from the user's own
-- corrections. See MerchantCategoryMemoryEntity.kt's header comment for
-- why the primary key is the (merchantKey, type) pair rather than
-- merchantKey alone.
CREATE TABLE IF NOT EXISTS merchant_category_memory (
    merchantKey TEXT NOT NULL,
    type TEXT NOT NULL,
    category TEXT NOT NULL,
    confirmCount INTEGER NOT NULL DEFAULT 1,
    lastUsedEpochMillis INTEGER NOT NULL,
    PRIMARY KEY (merchantKey, type)
) WITHOUT ROWID;

-- Debts being tracked for payoff. See DebtEntity.kt's header comment for
-- why remaining balance is derived (originalBalance - SUM(payments)) and
-- never stored as a separate, cacheable column.
CREATE TABLE IF NOT EXISTS debts (
    id TEXT NOT NULL PRIMARY KEY,
    label TEXT NOT NULL,
    originalBalanceMinorUnits INTEGER NOT NULL,
    interestRatePercent REAL,
    isArchived INTEGER NOT NULL DEFAULT 0,
    createdAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS debt_payments (
    id TEXT NOT NULL PRIMARY KEY,
    debtId TEXT NOT NULL,
    amountMinorUnits INTEGER NOT NULL,
    paidAtEpochMillis INTEGER NOT NULL
) WITHOUT ROWID;

CREATE INDEX IF NOT EXISTS index_debt_payments_debtId
    ON debt_payments (debtId);

-- Room requires this bookkeeping table to exist for schema-identity checks
-- when a database is created via createFromAsset rather than auto-generated.
CREATE TABLE IF NOT EXISTS room_master_table (
    id INTEGER PRIMARY KEY,
    identity_hash TEXT
);
