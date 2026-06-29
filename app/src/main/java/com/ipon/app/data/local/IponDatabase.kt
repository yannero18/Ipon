package com.ipon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
        DebtPaymentEntity::class
    ],
    version = 7,
    exportSchema = true
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

    companion object {
        private const val DATABASE_NAME = "ipon.db"

        @Volatile
        private var instance: IponDatabase? = null

        fun getInstance(context: Context): IponDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): IponDatabase =
            Room.databaseBuilder(context, IponDatabase::class.java, DATABASE_NAME)
                // The bundled schema declares both tables WITHOUT ROWID
                // (see assets/database/ipon_schema.sql) -- Room's own DDL
                // generator cannot express that clause, so both tables are
                // created from this asset on first open rather than from
                // the @Entity annotations.
                .createFromAsset("database/ipon_schema.sql")
                // PROTOTYPE-ONLY CHOICE: this destructively wipes and
                // recreates all tables on any version bump rather than
                // running a real Migration. That is the right tradeoff
                // *only* before this app has real users with real data --
                // it means schema changes during active development (like
                // adding the envelopes table just now) never require
                // hand-written Migration objects. Before shipping a build
                // anyone actually keeps data in, replace this with explicit
                // Migration(1, 2) {...} objects via .addMigrations(...), or
                // every future schema change will silently delete the
                // user's entire ledger on app update.
                .fallbackToDestructiveMigration()
                .build()
    }
}
