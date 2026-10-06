package com.geecee.escapelauncher.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.geecee.escapelauncher.core.data.repository.db.normaliseUsageKeys

/**
 * Version 1 wrote the date in usage keys with the device language's digits, so in some languages
 * the keys changed when the language did. This rewrites them all with ISO dates, adding together
 * rows for the same app and day. The table itself is unchanged.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = mutableListOf<Pair<String, Long>>()
        db.query("SELECT packageName, totalTime FROM app_usage").use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(cursor.getString(0) to cursor.getLong(1))
            }
        }

        val normalised = normaliseUsageKeys(rows)
        if (normalised == rows.toMap()) return

        db.execSQL("DELETE FROM app_usage")
        normalised.forEach { (key, totalTime) ->
            db.execSQL(
                "INSERT INTO app_usage (packageName, totalTime) VALUES (?, ?)",
                arrayOf<Any?>(key, totalTime)
            )
        }
    }
}
