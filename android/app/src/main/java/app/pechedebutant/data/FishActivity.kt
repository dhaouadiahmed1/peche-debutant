// v0.6 — Prévisions, "L'activité des poissons".
// OWN fish-activity algorithm, written from scratch, clean-room:
// the idea (a 0-100 score per half hour, best around the moon transit
// and moonrise/moonset) is the standard solunar idea; the numbers below
// are ours, computed on the phone only from Solunar.kt (local moon math)
// plus sunrise/sunset from Open-Meteo. No server, no account.
// EVERY value is an estimate: the UI labels it "Estimation" and shows
// the rule used. Never presented as exact science.
package app.pechedebutant.data

import java.time.LocalDate
import kotlin.math.exp

/** One half-hour step of the day: minutes from local midnight, score 0-100. */
data class ActivityPoint(val minuteOfDay: Int, val score: Int)

data class FishActivityDay(
    val date: LocalDate,
    val points: List<ActivityPoint>, // 48 points, one every 30 minutes
    val dailyScore: Int, // best half-hour of the day (max of the 48 points), 0-100
    val stars: Int, // 0-3
    val periods: List<FishingPeriod>, // major/minor periods from Solunar.kt
    val moon: MoonInfo
)

object FishActivity {

    // Bell curve (Gaussian): peak at the centre, fading with distance.
    // sigma is chosen so the curve is at about a third of its peak at
    // the edge of the stated window (e.g. ±75 min for a major period).
    private fun bell(minute: Int, centre: Int, peak: Double, halfWidthMin: Int): Double {
        val sigma = halfWidthMin * 0.6
        val d = minute - centre
        return peak * exp(-(d * d) / (2.0 * sigma * sigma))
    }

    /**
     * Fish activity for one day.
     * [sunriseMin]/[sunsetMin] are minutes from local midnight (from
     * Open-Meteo daily sunrise/sunset); null means "unknown, skip".
     * Rule: base 20, +45 bell around each major-period centre (±75 min),
     * +25 bell around each minor-period centre (±45 min), +12 bell around
     * sunrise and sunset (±60 min). Clamped to 0-100.
     */
    fun activityFor(
        date: LocalDate,
        lat: Double,
        lon: Double,
        sunriseMin: Int?,
        sunsetMin: Int?
    ): FishActivityDay {
        val solunar = Solunar.solunarFor(date, lat, lon)
        val points = (0 until 48).map { step ->
            val minute = step * 30
            var score = 20.0
            for (period in solunar.periods) {
                val centre = (period.startMin + period.endMin) / 2
                if (period.kind == "Majeure") {
                    score += bell(minute, centre, 45.0, 75)
                } else {
                    score += bell(minute, centre, 25.0, 45)
                }
            }
            sunriseMin?.let { score += bell(minute, it, 12.0, 60) }
            sunsetMin?.let { score += bell(minute, it, 12.0, 60) }
            ActivityPoint(minute, score.coerceIn(0.0, 100.0).toInt())
        }
        // Daily score = the best half-hour of the day (max of the 48
        // points), not the mean: the mean of the curve is always around
        // 30 (base 20 + short bells), so a mean-based score would sit at
        // 0 stars every day and look broken. The max is the "how good is
        // the best window today" answer, on the same 0-100 scale.
        val daily = points.maxOf { it.score }
        val stars = when {
            daily >= 85 -> 3
            daily >= 70 -> 2
            daily >= 40 -> 1
            else -> 0
        }
        return FishActivityDay(
            date = date,
            points = points,
            dailyScore = daily,
            stars = stars,
            periods = solunar.periods,
            moon = solunar.moon
        )
    }

    fun starsText(stars: Int): String = "★".repeat(stars) + "☆".repeat((3 - stars).coerceAtLeast(0))
}
