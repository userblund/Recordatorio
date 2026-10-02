package com.example.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderEntity::class, CustomTemplateEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun customTemplateDao(): CustomTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val migration2to3 = object : Migration(2, 3) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceIntervalValue INTEGER NOT NULL DEFAULT 1")
                        db.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceIntervalUnit TEXT NOT NULL DEFAULT 'MINUTE'")
                        db.execSQL("ALTER TABLE custom_templates ADD COLUMN recurrenceIntervalValue INTEGER NOT NULL DEFAULT 1")
                        db.execSQL("ALTER TABLE custom_templates ADD COLUMN recurrenceIntervalUnit TEXT NOT NULL DEFAULT 'MINUTE'")
                        db.execSQL("UPDATE reminders SET recurrenceIntervalValue = recurrenceIntervalHours, recurrenceIntervalUnit = 'HOUR' WHERE recurrenceType = 'INTERVAL_HOURS'")
                        db.execSQL("UPDATE reminders SET recurrenceIntervalValue = recurrenceIntervalMinutes, recurrenceIntervalUnit = 'MINUTE' WHERE recurrenceType = 'INTERVAL_MINUTES'")
                        db.execSQL("UPDATE custom_templates SET recurrenceIntervalValue = recurrenceIntervalHours, recurrenceIntervalUnit = 'HOUR' WHERE recurrenceType = 'INTERVAL_HOURS'")
                        db.execSQL("UPDATE custom_templates SET recurrenceIntervalValue = recurrenceIntervalMinutes, recurrenceIntervalUnit = 'MINUTE' WHERE recurrenceType = 'INTERVAL_MINUTES'")
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recordatorio_database"
                )
                    .addMigrations(migration2to3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
