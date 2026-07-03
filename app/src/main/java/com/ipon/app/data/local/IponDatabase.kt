package com.ipon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        EnvelopeEntity::class,
        RecurringTemplateEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        DailyReflectionEntity::class,
        MerchantCategoryMemoryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        ReportEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class IponDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    abstract fun goalDao(): GoalDao
    abstract fun dailyReflectionDao(): DailyReflectionDao
    abstract fun merchantCategoryMemoryDao(): MerchantCategoryMemoryDao
    abstract fun debtDao(): DebtDao
    abstract fun reportDao(): ReportDao

    companion object {
        private const val DATABASE_NAME = "ipon.db"

        @Volatile
        private var instance: IponDatabase? = null

        fun getInstance(context: Context): IponDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2 added the envelopes table and its indices
                db.execSQL("CREATE TABLE IF NOT EXISTS envelopes (id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, periodYearMonth TEXT NOT NULL, capMinorUnits INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_envelopes_periodYearMonth ON envelopes (periodYearMonth);")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_envelopes_category_period ON envelopes (category, periodYearMonth);")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 to 3 added recurring_templates
                db.execSQL("CREATE TABLE IF NOT EXISTS recurring_templates (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, merchantRaw TEXT, frequency TEXT NOT NULL, dayOfPeriod INTEGER NOT NULL, lastConfirmedPeriodKey TEXT, isPaused INTEGER NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 to 4 added goals and goal_contributions
                db.execSQL("CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, targetMinorUnits INTEGER NOT NULL, emoji TEXT NOT NULL, isArchived INTEGER NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE TABLE IF NOT EXISTS goal_contributions (id TEXT NOT NULL PRIMARY KEY, goalId TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, note TEXT, contributedAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId ON goal_contributions (goalId);")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 to 5 added daily_reflections and merchant_category_memory
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_reflections (id TEXT NOT NULL PRIMARY KEY, dayKey TEXT NOT NULL, mood TEXT NOT NULL, note TEXT, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_reflections_dayKey ON daily_reflections (dayKey);")
                db.execSQL("CREATE TABLE IF NOT EXISTS merchant_category_memory (merchantKey TEXT NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, confirmCount INTEGER NOT NULL DEFAULT 1, lastUsedEpochMillis INTEGER NOT NULL, PRIMARY KEY (merchantKey, type)) WITHOUT ROWID;")
            }
        }
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 5 to 6 added debts and debt_payments
                db.execSQL("CREATE TABLE IF NOT EXISTS debts (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, originalBalanceMinorUnits INTEGER NOT NULL, interestRatePercent REAL, isArchived INTEGER NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE TABLE IF NOT EXISTS debt_payments (id TEXT NOT NULL PRIMARY KEY, debtId TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, paidAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments (debtId);")
            }
        }
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 6 to 7 added isAutoCategorized column to transactions table if it wasn't already there
                try {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN isAutoCategorized INTEGER NOT NULL DEFAULT 0;")
                } catch (e: Exception) {
                    // Ignore error if the column already exists (e.g., table created from assets containing version 7)
                }
            }
        }
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS reports (id TEXT NOT NULL PRIMARY KEY, periodYearMonth TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            }
        }
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE envelopes ADD COLUMN priority INTEGER NOT NULL DEFAULT 2;")
            }
        }
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE goals ADD COLUMN deadline TEXT;")
            }
        }
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE envelopes ADD COLUMN customIcon TEXT;")
            }
        }

        private fun executeSchema(db: SupportSQLiteDatabase) {
            // Deprecated helper to initialize full schema, now handled incrementally or via createFromAsset
            db.execSQL("CREATE TABLE IF NOT EXISTS transactions (id TEXT NOT NULL PRIMARY KEY, amountMinorUnits INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, merchantRaw TEXT, note TEXT, occurredAtEpochMillis INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, isAutoCategorized INTEGER NOT NULL DEFAULT 0) WITHOUT ROWID;")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_occurredAtEpochMillis ON transactions (occurredAtEpochMillis);")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions (category);")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS envelopes (id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, periodYearMonth TEXT NOT NULL, capMinorUnits INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, priority INTEGER NOT NULL DEFAULT 2, customIcon TEXT) WITHOUT ROWID;")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_envelopes_periodYearMonth ON envelopes (periodYearMonth);")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_envelopes_category_period ON envelopes (category, periodYearMonth);")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS recurring_templates (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, merchantRaw TEXT, frequency TEXT NOT NULL, dayOfPeriod INTEGER NOT NULL, lastConfirmedPeriodKey TEXT, isPaused INTEGER NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, targetMinorUnits INTEGER NOT NULL, emoji TEXT NOT NULL, isArchived INTEGER NOT NULL DEFAULT 0, deadline TEXT, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS goal_contributions (id TEXT NOT NULL PRIMARY KEY, goalId TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, note TEXT, contributedAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_goal_contributions_goalId ON goal_contributions (goalId);")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS daily_reflections (id TEXT NOT NULL PRIMARY KEY, dayKey TEXT NOT NULL, mood TEXT NOT NULL, note TEXT, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_reflections_dayKey ON daily_reflections (dayKey);")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS merchant_category_memory (merchantKey TEXT NOT NULL, type TEXT NOT NULL, category TEXT NOT NULL, confirmCount INTEGER NOT NULL DEFAULT 1, lastUsedEpochMillis INTEGER NOT NULL, PRIMARY KEY (merchantKey, type)) WITHOUT ROWID;")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS debts (id TEXT NOT NULL PRIMARY KEY, label TEXT NOT NULL, originalBalanceMinorUnits INTEGER NOT NULL, interestRatePercent REAL, isArchived INTEGER NOT NULL DEFAULT 0, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            
            db.execSQL("CREATE TABLE IF NOT EXISTS debt_payments (id TEXT NOT NULL PRIMARY KEY, debtId TEXT NOT NULL, amountMinorUnits INTEGER NOT NULL, paidAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments (debtId);")
            db.execSQL("CREATE TABLE IF NOT EXISTS reports (id TEXT NOT NULL PRIMARY KEY, periodYearMonth TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL) WITHOUT ROWID;")
        }

        private fun build(context: Context): IponDatabase =
            Room.databaseBuilder(context, IponDatabase::class.java, DATABASE_NAME)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11
                )
                .build()
    }
}
