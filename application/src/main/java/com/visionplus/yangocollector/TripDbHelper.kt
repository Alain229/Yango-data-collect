package com.visionplus.yangocollector

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TripDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "yango_collector.db"
        const val DB_VERSION = 1
        const val TABLE = "trips"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                horodatage_capture INTEGER NOT NULL,
                date_course TEXT,
                adresses_brutes TEXT,
                distance_km REAL,
                duree_min INTEGER,
                revenu_fcfa INTEGER,
                nom_passager TEXT,
                texte_brut_ocr TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insererTrip(trip: Trip): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("horodatage_capture", trip.horodatageCapture)
            put("date_course", trip.dateCourse)
            put("adresses_brutes", trip.adressesBrutes)
            put("distance_km", trip.distanceKm)
            put("duree_min", trip.dureeMin)
            put("revenu_fcfa", trip.revenuFcfa)
            put("nom_passager", trip.nomPassager)
            put("texte_brut_ocr", trip.texteBrutOcr)
        }
        return db.insert(TABLE, null, values)
    }

    fun compterTrips(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }
}
