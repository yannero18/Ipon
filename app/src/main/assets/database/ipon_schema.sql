-- Ipon v1 schema.
--
-- Room's @Entity annotation cannot emit "WITHOUT ROWID" -- it always generates
-- a standard CREATE TABLE. To get the clustered-index behavior described in
-- the proposal (Section 2), the table is created from this file via
-- Room.databaseBuilder(...).createFromAsset("database/ipon_schema.sql")
-- and Room's generated DAO/query code is verified against it at compile time
-- via the exported schema in app/schemas/.
--
-- IMPORTANT: if you add/rename/remove a column on TransactionEntity, this
-- file and the entity must be changed together, or Room's schema validation
-- will fail at app startup (RoomOpenHelper mismatch).

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

-- Room requires this bookkeeping table to exist for schema-identity checks
-- when a database is created via createFromAsset rather than auto-generated.
CREATE TABLE IF NOT EXISTS room_master_table (
    id INTEGER PRIMARY KEY,
    identity_hash TEXT
);
