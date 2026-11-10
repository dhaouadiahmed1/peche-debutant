package app.pechedebutant.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// v0.5 — Journal on-device (catch log + saved spots).
// Hand-written SQLiteOpenHelper, same pattern Fishing Points uses.
// NO Room, no new dependency. Everything stays on the phone:
// no account, no server, no sync.
// NO photo capture in v0.5 (camera + FileProvider is a later step).

data class CatchEntry(
    val id: Long = 0,
    val dateTimeMillis: Long,
    val fishId: String,
    val lengthCm: Int? = null,
    val weightG: Int? = null,
    val baitId: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val notes: String = "",
    // Automatic weather snapshot at catch time (from the Open-Meteo fetch
    // or today's saved cache). Null / "—" when no weather was available —
    // the app never invents weather.
    val windKmh: Double? = null,
    val waveM: Double? = null,
    val seaTempC: Double? = null,
    val spotScore: Int? = null
)

data class SpotEntry(
    val id: Long = 0,
    val name: String,
    val lat: Double,
    val lon: Double,
    val notes: String = "",
    val createdMillis: Long
)

class JournalDb(context: Context) :
    SQLiteOpenHelper(context, "journal.db", null, 1) {

    companion object {
        private const val T_CATCHES = "catches"
        private const val T_SPOTS = "spots"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T_CATCHES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date_time INTEGER NOT NULL,
                fish_id TEXT NOT NULL,
                length_cm INTEGER,
                weight_g INTEGER,
                bait_id TEXT,
                lat REAL,
                lon REAL,
                notes TEXT NOT NULL DEFAULT '',
                wind_kmh REAL,
                wave_m REAL,
                sea_temp_c REAL,
                spot_score INTEGER
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_SPOTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                lat REAL NOT NULL,
                lon REAL NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                created INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v0.5 is the first version: no migration needed yet.
    }

    // ---- Catches ----

    fun addCatch(entry: CatchEntry): Long {
        val values = ContentValues().apply {
            put("date_time", entry.dateTimeMillis)
            put("fish_id", entry.fishId)
            entry.lengthCm?.let { put("length_cm", it) }
            entry.weightG?.let { put("weight_g", it) }
            entry.baitId?.let { put("bait_id", it) }
            entry.lat?.let { put("lat", it) }
            entry.lon?.let { put("lon", it) }
            put("notes", entry.notes)
            entry.windKmh?.let { put("wind_kmh", it) }
            entry.waveM?.let { put("wave_m", it) }
            entry.seaTempC?.let { put("sea_temp_c", it) }
            entry.spotScore?.let { put("spot_score", it) }
        }
        return writableDatabase.insert(T_CATCHES, null, values)
    }

    fun listCatches(): List<CatchEntry> {
        val out = mutableListOf<CatchEntry>()
        readableDatabase.query(
            T_CATCHES, null, null, null, null, null, "date_time DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += CatchEntry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    dateTimeMillis = c.getLong(c.getColumnIndexOrThrow("date_time")),
                    fishId = c.getString(c.getColumnIndexOrThrow("fish_id")),
                    lengthCm = if (c.isNull(c.getColumnIndexOrThrow("length_cm"))) null
                        else c.getInt(c.getColumnIndexOrThrow("length_cm")),
                    weightG = if (c.isNull(c.getColumnIndexOrThrow("weight_g"))) null
                        else c.getInt(c.getColumnIndexOrThrow("weight_g")),
                    baitId = c.getString(c.getColumnIndexOrThrow("bait_id")),
                    lat = if (c.isNull(c.getColumnIndexOrThrow("lat"))) null
                        else c.getDouble(c.getColumnIndexOrThrow("lat")),
                    lon = if (c.isNull(c.getColumnIndexOrThrow("lon"))) null
                        else c.getDouble(c.getColumnIndexOrThrow("lon")),
                    notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: "",
                    windKmh = if (c.isNull(c.getColumnIndexOrThrow("wind_kmh"))) null
                        else c.getDouble(c.getColumnIndexOrThrow("wind_kmh")),
                    waveM = if (c.isNull(c.getColumnIndexOrThrow("wave_m"))) null
                        else c.getDouble(c.getColumnIndexOrThrow("wave_m")),
                    seaTempC = if (c.isNull(c.getColumnIndexOrThrow("sea_temp_c"))) null
                        else c.getDouble(c.getColumnIndexOrThrow("sea_temp_c")),
                    spotScore = if (c.isNull(c.getColumnIndexOrThrow("spot_score"))) null
                        else c.getInt(c.getColumnIndexOrThrow("spot_score"))
                )
            }
        }
        return out
    }

    fun deleteCatch(id: Long) {
        writableDatabase.delete(T_CATCHES, "id = ?", arrayOf(id.toString()))
    }

    // ---- Spots ----

    fun addSpot(entry: SpotEntry): Long {
        val values = ContentValues().apply {
            put("name", entry.name)
            put("lat", entry.lat)
            put("lon", entry.lon)
            put("notes", entry.notes)
            put("created", entry.createdMillis)
        }
        return writableDatabase.insert(T_SPOTS, null, values)
    }

    fun listSpots(): List<SpotEntry> {
        val out = mutableListOf<SpotEntry>()
        readableDatabase.query(
            T_SPOTS, null, null, null, null, null, "created DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += SpotEntry(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    lat = c.getDouble(c.getColumnIndexOrThrow("lat")),
                    lon = c.getDouble(c.getColumnIndexOrThrow("lon")),
                    notes = c.getString(c.getColumnIndexOrThrow("notes")) ?: "",
                    createdMillis = c.getLong(c.getColumnIndexOrThrow("created"))
                )
            }
        }
        return out
    }

    fun deleteSpot(id: Long) {
        writableDatabase.delete(T_SPOTS, "id = ?", arrayOf(id.toString()))
    }
}
