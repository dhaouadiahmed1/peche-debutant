package app.pechedebutant.data

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

// v0.5 — Solunar best hours ("Meilleures heures").
// Local astronomy math only, no server, no account.
// Standard low-precision lunar calculation (Meeus, Astronomical
// Algorithms, simplified): moon phase from the synodic month, moon
// position from the mean elements, then transit / rise / set from the
// observer's latitude/longitude.
// EVERY value is an estimate: the UI labels it "estimation d'après la
// lune" and shows the rule used. If a value cannot be computed
// (e.g. no moonrise that day), it is null and the UI shows "—".
// Accuracy is roughly ±15 minutes — good enough to pick a fishing
// window, never presented as exact science.

data class MoonInfo(
    val phaseName: String,
    val illuminationPct: Int,
    val ageDays: Double
)

data class FishingPeriod(
    val startMin: Int, // minutes from local midnight
    val endMin: Int,
    val kind: String // "Majeure" or "Mineure"
)

data class SolunarDay(
    val date: LocalDate,
    val moon: MoonInfo,
    val periods: List<FishingPeriod>,
    val moonriseMin: Int?,
    val moonsetMin: Int?,
    val moonTransitMin: Int?
)

object Solunar {

    private const val SYNODIC_MONTH = 29.530588853
    private const val NEW_MOON_JD = 2451550.1 // 2000-01-06 18:14 UTC
    private const val J2000 = 2451545.0

    private fun Double.toRad() = this * PI / 180.0
    private fun Double.toDeg() = this * 180.0 / PI
    private fun Double.norm360(): Double {
        var v = this % 360.0
        if (v < 0) v += 360.0
        return v
    }

    private fun julianDay(date: LocalDate): Double {
        // Julian day number at 0h UT of the given date.
        val y = date.year
        val m = date.monthValue
        val d = date.dayOfMonth
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        val jdn = d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
        return jdn.toDouble() - 0.5 // 0h UT
    }

    fun moonInfo(date: LocalDate): MoonInfo {
        val jd = julianDay(date) + 0.5 // noon, close enough for the phase
        var age = (jd - NEW_MOON_JD) % SYNODIC_MONTH
        if (age < 0) age += SYNODIC_MONTH
        val illum = ((1.0 - cos(2.0 * PI * age / SYNODIC_MONTH)) / 2.0 * 100.0)
            .toInt().coerceIn(0, 100)
        val name = when {
            age < 1.84 || age >= 27.68 -> "Nouvelle lune"
            age < 5.53 -> "Premier croissant"
            age < 9.22 -> "Premier quartier"
            age < 12.91 -> "Lune gibbeuse croissante"
            age < 16.61 -> "Pleine lune"
            age < 20.30 -> "Lune gibbeuse décroissante"
            age < 23.99 -> "Dernier quartier"
            else -> "Dernier croissant"
        }
        return MoonInfo(name, illum, age)
    }

    // Moon right ascension / declination in degrees (low precision).
    private fun moonRaDec(jd: Double): Pair<Double, Double> {
        val days = jd - J2000
        val lPrime = (218.316 + 13.176396 * days).norm360()
        val mAnom = (134.963 + 13.064993 * days).norm360()
        val f = (93.272 + 13.229350 * days).norm360()
        val lambda = lPrime + 6.289 * sin(mAnom.toRad())
        val beta = 5.128 * sin(f.toRad())
        val eps = 23.4393.toRad()
        val ra = atan2(
            sin(lambda.toRad()) * cos(eps) - tan(beta.toRad()) * sin(eps),
            cos(lambda.toRad())
        ).toDeg().norm360()
        val dec = asin(
            sin(beta.toRad()) * cos(eps) +
                cos(beta.toRad()) * sin(eps) * sin(lambda.toRad())
        ).toDeg()
        return ra to dec
    }

    // Greenwich Mean Sidereal Time at 0h UT, in degrees.
    private fun gmst0(jd: Double): Double {
        val t = (jd - J2000) / 36525.0
        return (
            280.46061837 + 360.98564736629 * (jd - J2000) +
                0.000387933 * t * t - t * t * t / 38710000.0
            ).norm360()
    }

    fun solunarFor(date: LocalDate, lat: Double, lon: Double): SolunarDay {
        val zone = ZoneId.systemDefault()
        val offsetHours = zone.rules.getOffset(date.atStartOfDay(zone).toInstant())
            .totalSeconds / 3600.0
        val jd = julianDay(date)
        val (ra, dec) = moonRaDec(jd)

        // Upper transit in UT hours.
        var transitUt = (ra - gmst0(jd) - lon) / 15.04107
        transitUt = ((transitUt % 24.0) + 24.0) % 24.0

        val phi = lat.toRad()
        val delta = dec.toRad()
        // Moon upper limb, standard altitude for rise/set.
        val h0 = 0.125.toRad()
        val cosH0 = (sin(h0) - sin(phi) * sin(delta)) / (cos(phi) * cos(delta))

        var riseUt: Double? = null
        var setUt: Double? = null
        if (cosH0 in -1.0..1.0) {
            val h0deg = acos(cosH0).toDeg()
            riseUt = ((transitUt - h0deg / 15.04107) % 24.0 + 24.0) % 24.0
            setUt = ((transitUt + h0deg / 15.04107) % 24.0 + 24.0) % 24.0
        }

        fun utToLocalMin(utHours: Double): Int {
            val local = utHours + offsetHours
            val norm = ((local % 24.0) + 24.0) % 24.0
            return (norm * 60.0).toInt()
        }

        val transitMin = utToLocalMin(transitUt)
        val riseMin = riseUt?.let { utToLocalMin(it) }
        val setMin = setUt?.let { utToLocalMin(it) }

        // Solunar rule (standard):
        // Major periods: 2 h centred on the moon transit (upper) and on
        // the lower transit (about 12 h 25 min later).
        // Minor periods: 1 h centred on moonrise and on moonset.
        val periods = mutableListOf<FishingPeriod>()
        fun addPeriod(centerMin: Int, halfWidthMin: Int, kind: String) {
            val start = centerMin - halfWidthMin
            val end = centerMin + halfWidthMin
            if (start in 0 until 1440) {
                periods += FishingPeriod(start.coerceAtLeast(0), end.coerceAtMost(1439), kind)
            }
        }
        addPeriod(transitMin, 60, "Majeure")
        val lowerTransitMin = (transitMin + 12 * 60 + 25) % 1440
        addPeriod(lowerTransitMin, 60, "Majeure")
        riseMin?.let { addPeriod(it, 30, "Mineure") }
        setMin?.let { addPeriod(it, 30, "Mineure") }
        periods.sortBy { it.startMin }

        return SolunarDay(
            date = date,
            moon = moonInfo(date),
            periods = periods,
            moonriseMin = riseMin,
            moonsetMin = setMin,
            moonTransitMin = transitMin
        )
    }

    fun solunarTodayAndTomorrow(lat: Double, lon: Double): List<SolunarDay> {
        val today = LocalDate.now()
        return listOf(
            solunarFor(today, lat, lon),
            solunarFor(today.plusDays(1), lat, lon)
        )
    }

    fun formatMin(min: Int): String {
        val h = (min / 60).coerceIn(0, 23)
        val m = (min % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    fun formatPeriod(p: FishingPeriod): String =
        "${formatMin(p.startMin)} – ${formatMin(p.endMin)}"
}
