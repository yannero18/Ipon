package com.ipon.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class IponDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

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
                // The bundled schema declares the transactions table WITHOUT ROWID
                // (see assets/database/ipon_schema.sql) -- Room's own DDL generator
                // cannot express that clause, so the table is created from this
                // asset on first open rather than from the @Entity annotations.
                .createFromAsset("database/ipon_schema.sql")
                .build()
    }
}
