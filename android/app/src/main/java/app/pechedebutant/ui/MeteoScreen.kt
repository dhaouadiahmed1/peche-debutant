// Pêche Débutant v0.6 — Météo & Marée tab, now "Prévisions".
// Data: Open-Meteo, free, no API key, no login.
//   Weather: https://api.open-meteo.com/v1/forecast (7 days, hourly)
//   Marine:  https://marine-api.open-meteo.com/v1/marine (3 days, hourly)
//   "Weather data by Open-Meteo.com"
// Location: Android LocationManager only (no Google Play Services).
// Fallback when there is no permission or no GPS fix: Ras Jebel 37.2476, 10.0697.
// Tide times are the local maxima / minima of the hourly sea_level_height_msl
// series. In the Mediterranean here the tide is small (~0.2-0.3 m); wind and
// waves matter more than the tide.
// Fish activity (Activité tab) is our own local estimate, see
// data/FishActivity.kt — never presented as exact science.
// This tab needs internet. All other tabs stay fully offline.
package app.pechedebutant.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.pechedebutant.data.FishActivity
import app.pechedebutant.data.Solunar
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// Fallback position: Ras Jebel, the spot in the app. Only used when there is
// no location permission or no GPS fix.
const val FALLBACK_LAT = 37.2476
const val FALLBACK_LON = 10.0697
private const val FALLBACK_NAME = "Ras Jebel"

data class DayForecast(
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val rainProb: Int,
    val windMax: Double,
    val gustMax: Double,
    val sunrise: String,
    val sunset: String
)

data class HourlyWeather(
    val time: String, // ISO local time, e.g. "2026-11-12T14:00"
    val temp: Double,
    val rainProb: Int,
    val wind: Double,
    val gusts: Double,
    val humidity: Int,
    val feelsLike: Double
)

data class HourlyWave(
    val time: String, // ISO local time, e.g. "2026-11-12T14:00"
    val waveHeight: Double,
    val wavePeriod: Double,
    val waveDir: Double,
    val swellHeight: Double,
    val swellDir: Double,
    val swellPeriod: Double,
    val windWaveHeight: Double
)

// date is "yyyy-MM-dd"; empty ("") means "unknown / from the old cache".
data class TideEvent(val time: String, val height: Double, val high: Boolean, val date: String = "")

data class MeteoData(
    val temperature: Double,
    val windSpeed: Double,
    val windDir: Double,
    val windGusts: Double,
    val humidity: Int,
    val precipitation: Double,
    val weatherCode: Int,
    val rainProbToday: Int,
    val waveHeight: Double,
    val wavePeriod: Double,
    val waveDir: Double,
    val seaTemp: Double,
    val days: List<DayForecast>,
    val tides: List<TideEvent>,
    val tideRisingNow: Boolean,
    // v0.6 — new fields, all with defaults so the dashboard cache and
    // the widget (which build a MeteoData with the old fields only)
    // keep compiling unchanged.
    val feelsLike: Double = 0.0,
    val pressure: Double = 0.0,
    val uvIndex: Double = 0.0,
    val cloudCover: Int = 0,
    val hourly: List<HourlyWeather> = emptyList(),
    val hourlyWaves: List<HourlyWave> = emptyList(),
    val swellHeight: Double = 0.0,
    val swellDir: Double = 0.0,
    val swellPeriod: Double = 0.0,
    val windWaveHeight: Double = 0.0
)

data class SpotReason(val mark: String, val text: String)

data class SpotScore(val score: Int, val verdict: String, val reasons: List<SpotReason>)

private fun compass(degrees: Double): String {
    val names = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSO", "SO", "OSO", "O", "ONO", "NO", "NNO"
    )
    val index = ((degrees % 360.0) / 22.5 + 0.5).toInt() % 16
    return names[index]
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "Ciel dégagé"
    1 -> "Presque dégagé"
    2 -> "Partiellement nuageux"
    3 -> "Nuageux"
    45, 48 -> "Brouillard"
    51, 53, 55 -> "Bruine"
    56, 57 -> "Bruine verglaçante"
    61 -> "Pluie faible"
    63 -> "Pluie"
    65 -> "Pluie forte"
    66, 67 -> "Pluie verglaçante"
    71, 73, 75, 77 -> "Neige"
    80, 81, 82 -> "Averses"
    85, 86 -> "Averses de neige"
    95 -> "Orage"
    96, 99 -> "Orage avec grêle"
    else -> "Météo inconnue"
}

private fun hourMinute(iso: String): String {
    return try {
        LocalDateTime.parse(iso).format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) {
        iso
    }
}

/** Minutes from local midnight for an ISO date-time, or null if unparsable. */
private fun minutesOfDay(iso: String): Int? {
    return try {
        val t = LocalDateTime.parse(iso)
        t.hour * 60 + t.minute
    } catch (_: Exception) {
        null
    }
}

private fun dayLabel(isoDate: String, index: Int): String {
    if (index == 0) return "Auj."
    return try {
        val date = LocalDateTime.parse(isoDate + "T00:00")
        date.format(DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.FRENCH))
    } catch (_: Exception) {
        isoDate
    }
}

private fun fetchJson(urlString: String): JSONObject? {
    return try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

// Defensive reads: a missing field never crashes, it becomes 0.
private fun JSONArray?.doubleAt(i: Int): Double = this?.optDouble(i, 0.0) ?: 0.0
private fun JSONArray?.intAt(i: Int): Int = this?.optInt(i, 0) ?: 0

/** Tide events for the given dates ("yyyy-MM-dd"): local maxima / minima of sea level. */
private fun tideEvents(times: List<String>, seaLevel: List<Double>, dates: List<String>): List<TideEvent> {
    val events = mutableListOf<TideEvent>()
    for (i in 1 until seaLevel.size - 1) {
        val date = times[i].substringBefore("T")
        if (date !in dates) continue
        val prev = seaLevel[i - 1]
        val now = seaLevel[i]
        val next = seaLevel[i + 1]
        if (now > prev && now >= next) {
            events.add(TideEvent(hourMinute(times[i]), now, true, date))
        } else if (now < prev && now <= next) {
            events.add(TideEvent(hourMinute(times[i]), now, false, date))
        }
    }
    return events
}

private suspend fun fetchMeteo(lat: Double, lon: Double): MeteoData? = withContext(Dispatchers.IO) {
    val weatherUrl =
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,apparent_temperature,wind_speed_10m,wind_direction_10m," +
            "wind_gusts_10m,relative_humidity_2m,precipitation,weather_code,pressure_msl," +
            "cloud_cover" +
            "&hourly=temperature_2m,precipitation_probability,wind_speed_10m,wind_gusts_10m," +
            "relative_humidity_2m,apparent_temperature,uv_index,pressure_msl,cloud_cover" +
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max" +
            "&timezone=auto&forecast_days=7"
    val marineUrl =
        "https://marine-api.open-meteo.com/v1/marine?latitude=$lat&longitude=$lon" +
            "&current=wave_height,wave_direction,wave_period,sea_surface_temperature," +
            "swell_wave_height,swell_wave_direction,swell_wave_period,wind_wave_height" +
            "&hourly=wave_height,wave_direction,wave_period,sea_level_height_msl," +
            "swell_wave_height,swell_wave_direction,swell_wave_period,wind_wave_height," +
            "sea_surface_temperature" +
            "&timezone=auto&forecast_days=3"
    val weather = fetchJson(weatherUrl) ?: return@withContext null
    val marine = fetchJson(marineUrl) ?: return@withContext null
    try {
        val current = weather.getJSONObject("current")
        val daily = weather.getJSONObject("daily")
        val dailyTime = daily.getJSONArray("time")
        val days = mutableListOf<DayForecast>()
        for (i in 0 until dailyTime.length()) {
            days.add(
                DayForecast(
                    date = dailyTime.getString(i),
                    maxTemp = daily.getJSONArray("temperature_2m_max").doubleAt(i),
                    minTemp = daily.getJSONArray("temperature_2m_min").doubleAt(i),
                    rainProb = daily.optJSONArray("precipitation_probability_max").intAt(i),
                    windMax = daily.getJSONArray("wind_speed_10m_max").doubleAt(i),
                    gustMax = daily.getJSONArray("wind_gusts_10m_max").doubleAt(i),
                    sunrise = daily.getJSONArray("sunrise").getString(i),
                    sunset = daily.getJSONArray("sunset").getString(i)
                )
            )
        }
        // Hourly weather (7 days of hours; the UI shows the next 12 h).
        val hourlyWeather = mutableListOf<HourlyWeather>()
        val weatherHourly = weather.optJSONObject("hourly")
        if (weatherHourly != null) {
            val hTimes = weatherHourly.optJSONArray("time")
            val count = hTimes?.length() ?: 0
            for (i in 0 until count) {
                hourlyWeather.add(
                    HourlyWeather(
                        time = hTimes?.optString(i, "") ?: "",
                        temp = weatherHourly.optJSONArray("temperature_2m").doubleAt(i),
                        rainProb = weatherHourly.optJSONArray("precipitation_probability").intAt(i),
                        wind = weatherHourly.optJSONArray("wind_speed_10m").doubleAt(i),
                        gusts = weatherHourly.optJSONArray("wind_gusts_10m").doubleAt(i),
                        humidity = weatherHourly.optJSONArray("relative_humidity_2m").intAt(i),
                        feelsLike = weatherHourly.optJSONArray("apparent_temperature").doubleAt(i)
                    )
                )
            }
        }
        val marineCurrent = marine.getJSONObject("current")
        val marineHourly = marine.getJSONObject("hourly")
        val hourlyTimes = marineHourly.getJSONArray("time")
        val times = mutableListOf<String>()
        val seaLevel = mutableListOf<Double>()
        val hourlyWaves = mutableListOf<HourlyWave>()
        for (i in 0 until hourlyTimes.length()) {
            times.add(hourlyTimes.getString(i))
            seaLevel.add(marineHourly.optJSONArray("sea_level_height_msl").doubleAt(i))
            hourlyWaves.add(
                HourlyWave(
                    time = hourlyTimes.getString(i),
                    waveHeight = marineHourly.optJSONArray("wave_height").doubleAt(i),
                    wavePeriod = marineHourly.optJSONArray("wave_period").doubleAt(i),
                    waveDir = marineHourly.optJSONArray("wave_direction").doubleAt(i),
                    swellHeight = marineHourly.optJSONArray("swell_wave_height").doubleAt(i),
                    swellDir = marineHourly.optJSONArray("swell_wave_direction").doubleAt(i),
                    swellPeriod = marineHourly.optJSONArray("swell_wave_period").doubleAt(i),
                    windWaveHeight = marineHourly.optJSONArray("wind_wave_height").doubleAt(i)
                )
            )
        }
        val now = LocalDateTime.now()
        val today = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dates3 = listOf(
            today,
            LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
        val currentHour = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'"))
        val hourIndex = times.indexOf(currentHour)
        val tideRisingNow = hourIndex > 0 && seaLevel[hourIndex] > seaLevel[hourIndex - 1]
        MeteoData(
            temperature = current.getDouble("temperature_2m"),
            windSpeed = current.getDouble("wind_speed_10m"),
            windDir = current.getDouble("wind_direction_10m"),
            windGusts = current.getDouble("wind_gusts_10m"),
            humidity = current.getInt("relative_humidity_2m"),
            precipitation = current.getDouble("precipitation"),
            weatherCode = current.getInt("weather_code"),
            rainProbToday = if (days.isNotEmpty()) days[0].rainProb else 0,
            waveHeight = marineCurrent.getDouble("wave_height"),
            wavePeriod = marineCurrent.getDouble("wave_period"),
            waveDir = marineCurrent.getDouble("wave_direction"),
            seaTemp = marineCurrent.getDouble("sea_surface_temperature"),
            days = days,
            tides = tideEvents(times, seaLevel, dates3),
            tideRisingNow = tideRisingNow,
            feelsLike = current.optDouble("apparent_temperature", 0.0),
            pressure = current.optDouble("pressure_msl", 0.0),
            uvIndex = weatherHourly?.optJSONArray("uv_index")?.let {
                // UV "now": the hourly value at the current hour, if present.
                val idx = hourlyWeather.indexOfFirst { h -> h.time == currentHour }
                if (idx >= 0) it.optDouble(idx, 0.0) else 0.0
            } ?: 0.0,
            cloudCover = current.optInt("cloud_cover", 0),
            hourly = hourlyWeather,
            hourlyWaves = hourlyWaves,
            swellHeight = marineCurrent.optDouble("swell_wave_height", 0.0),
            swellDir = marineCurrent.optDouble("swell_wave_direction", 0.0),
            swellPeriod = marineCurrent.optDouble("swell_wave_period", 0.0),
            windWaveHeight = marineCurrent.optDouble("wind_wave_height", 0.0)
        )
    } catch (_: Exception) {
        null
    }
}

/**
 * "Check blassa" score, out of 10. Rules, shown to the user as reasons:
 * wind < 15 km/h: +2, 15-25: +1, > 25: 0.
 * gusts < 25 km/h: +1, else 0.
 * wave < 0.5 m: +2, 0.5-1.0 m: +1, > 1.0 m: 0.
 * rain probability < 20%: +1, else 0.
 * tide rising now: +1, else 0.
 * within 2 h of sunrise or sunset: +2, daytime: +1, night: 0.
 * Verdict: >= 7 BAHYA, 5-6 MTEWASTA, <= 4 MOUCH BAHYA.
 * Unchanged in v0.6.
 */
fun spotScore(data: MeteoData): SpotScore {
    var score = 0
    val reasons = mutableListOf<SpotReason>()

    if (data.windSpeed < 15.0) {
        score += 2
        reasons.add(SpotReason("✓", "Vent faible : ${data.windSpeed.toInt()} km/h (moins de 15)"))
    } else if (data.windSpeed <= 25.0) {
        score += 1
        reasons.add(SpotReason("~", "Vent moyen : ${data.windSpeed.toInt()} km/h (15 à 25)"))
    } else {
        reasons.add(SpotReason("✗", "Vent fort : ${data.windSpeed.toInt()} km/h (plus de 25)"))
    }

    if (data.windGusts < 25.0) {
        score += 1
        reasons.add(SpotReason("✓", "Rafales faibles : ${data.windGusts.toInt()} km/h (moins de 25)"))
    } else {
        reasons.add(SpotReason("✗", "Rafales fortes : ${data.windGusts.toInt()} km/h (25 ou plus)"))
    }

    if (data.waveHeight < 0.5) {
        score += 2
        reasons.add(SpotReason("✓", "Mer calme : vagues ${"%.2f".format(data.waveHeight)} m (moins de 0,5)"))
    } else if (data.waveHeight <= 1.0) {
        score += 1
        reasons.add(SpotReason("~", "Vagues moyennes : ${"%.2f".format(data.waveHeight)} m (0,5 à 1,0)"))
    } else {
        reasons.add(SpotReason("✗", "Mer agitée : vagues ${"%.2f".format(data.waveHeight)} m (plus de 1,0)"))
    }

    if (data.rainProbToday < 20) {
        score += 1
        reasons.add(SpotReason("✓", "Peu de pluie : ${data.rainProbToday}% (moins de 20%)"))
    } else {
        reasons.add(SpotReason("✗", "Risque de pluie : ${data.rainProbToday}% (20% ou plus)"))
    }

    if (data.tideRisingNow) {
        score += 1
        reasons.add(SpotReason("✓", "La marée monte en ce moment"))
    } else {
        reasons.add(SpotReason("~", "La marée ne monte pas en ce moment"))
    }

    val now = LocalDateTime.now()
    val today = data.days.firstOrNull()
    var timePoints = 0
    var timeText = "Nuit"
    if (today != null) {
        try {
            val sunrise = LocalDateTime.parse(today.sunrise)
            val sunset = LocalDateTime.parse(today.sunset)
            val nearSunrise = Duration.between(now, sunrise).abs().toHours() < 2
            val nearSunset = Duration.between(now, sunset).abs().toHours() < 2
            val daytime = now.isAfter(sunrise) && now.isBefore(sunset)
            if (nearSunrise || nearSunset) {
                timePoints = 2
                timeText = "Aube ou crépuscule (moins de 2 h du lever ou coucher du soleil)"
            } else if (daytime) {
                timePoints = 1
                timeText = "Journée"
            } else {
                timePoints = 0
                timeText = "Nuit"
            }
        } catch (_: Exception) {
            timePoints = 0
            timeText = "Nuit"
        }
    }
    score += timePoints
    val timeMark = if (timePoints == 2) "✓" else if (timePoints == 1) "~" else "✗"
    reasons.add(SpotReason(timeMark, timeText))

    val verdict = when {
        score >= 7 -> "BAHYA"
        score >= 5 -> "MTEWASTA"
        else -> "MOUCH BAHYA"
    }
    return SpotScore(score, verdict, reasons)
}

private fun bestFishNow(score: Int): String = when {
    score >= 7 -> "Loup, Dorade, Sar"
    score >= 5 -> "Sar, Mulet, Pageot"
    else -> "mouch wa9t behi, stenna"
}

@SuppressLint("MissingPermission")
private fun lastKnownLocation(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    val network = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
    return listOfNotNull(gps, network).maxByOrNull { it.time }
}

private sealed interface MeteoState {
    data object Loading : MeteoState
    data object Error : MeteoState
    data class Loaded(val data: MeteoData) : MeteoState
}

// v0.6 — the 5 Prévisions sub-tabs inside the Météo tab.
private val PREVISION_TABS = listOf("Activité", "Vagues", "Marées", "Météo", "Soleil & lune")

@Composable
fun MeteoScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<MeteoState>(MeteoState.Loading) }
    var lat by remember { mutableStateOf(FALLBACK_LAT) }
    var lon by remember { mutableStateOf(FALLBACK_LON) }
    var placeName by remember { mutableStateOf(FALLBACK_NAME) }
    var isFallback by remember { mutableStateOf(true) }
    var locationNote by remember { mutableStateOf("Position par défaut : Ras Jebel.") }
    // v0.6 — selected Prévisions sub-tab, kept across recompositions.
    var selectedTab by remember { mutableStateOf(0) }

    fun load(latitude: Double, longitude: Double) {
        state = MeteoState.Loading
        scope.launch {
            val data = fetchMeteo(latitude, longitude)
            state = if (data != null) MeteoState.Loaded(data) else MeteoState.Error
        }
    }

    fun useLocation(location: Location?) {
        if (location != null) {
            lat = location.latitude
            lon = location.longitude
            isFallback = false
            placeName = "Ta position"
            locationNote = "Position GPS du téléphone."
            load(location.latitude, location.longitude)
        } else {
            lat = FALLBACK_LAT
            lon = FALLBACK_LON
            isFallback = true
            placeName = FALLBACK_NAME
            locationNote = "Pas de position GPS — Ras Jebel par défaut."
            load(FALLBACK_LAT, FALLBACK_LON)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val known = lastKnownLocation(context)
            if (known != null) {
                useLocation(known)
            } else {
                // Ask for one fresh fix, then fall back to Ras Jebel.
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            useLocation(location)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
                } catch (_: Exception) {
                    useLocation(null)
                }
            }
        } else {
            useLocation(null)
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            useLocation(lastKnownLocation(context))
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Prévisions — $placeName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lat ${"%.4f".format(lat)}, Lon ${"%.4f".format(lon)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = locationNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Données : Open-Meteo, gratuit, sans compte. Weather data by Open-Meteo.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (val current = state) {
            is MeteoState.Loading -> {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Chargement de la météo…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            is MeteoState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Pas de connexion — vérifie internet et réessaie.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Cet onglet a besoin d'internet. Les autres onglets (Poissons, Appâts, Montages, Guide) marchent sans internet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { load(lat, lon) }) { Text("Réessayer") }
                        }
                    }
                }
            }
            is MeteoState.Loaded -> {
                val data = current.data
                val spot = spotScore(data)
                val verdictColor = when (spot.verdict) {
                    "BAHYA" -> Color(0xFF2E7D32)
                    "MTEWASTA" -> Color(0xFF8D6E00)
                    else -> Color(0xFFB3261E)
                }
                // Check blassa — unchanged in v0.6, still at the top.
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Check blassa : ${spot.verdict} — ${spot.score} / 10",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = verdictColor
                            )
                            Spacer(Modifier.height(8.dp))
                            spot.reasons.forEach { reason ->
                                Text(
                                    text = "${reason.mark}  ${reason.text}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Meilleurs poissons maintenant : ${bestFishNow(spot.score)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Meilleur appât maintenant : ver de mer, crevette",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                // v0.6 — 5 Prévisions sub-tabs.
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PREVISION_TABS.forEachIndexed { index, label ->
                            val selected = index == selectedTab
                            Button(
                                onClick = { selectedTab = index },
                                modifier = Modifier.weight(1f).height(48.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp),
                                colors = if (selected) {
                                    ButtonDefaults.buttonColors()
                                } else {
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                when (selectedTab) {
                    0 -> item { ActivityTab(data, lat, lon) }
                    1 -> item { WavesTab(data) }
                    2 -> item { TidesTab(data) }
                    3 -> item { WeatherTab(data) }
                    else -> item { SunMoonTab(data, lat, lon) }
                }
                item {
                    Button(
                        onClick = { load(lat, lon) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Actualiser") }
                    Text(
                        text = "Astuce : le matin tôt et le soir sont les meilleurs moments. Le loup aime le vent, la dorade aime la mer calme.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Activité

@Composable
private fun ActivityTab(data: MeteoData, lat: Double, lon: Double) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayDay = data.days.getOrNull(0)
    val tomorrowDay = data.days.getOrNull(1)
    val todayActivity = FishActivity.activityFor(
        today, lat, lon,
        todayDay?.sunrise?.let { minutesOfDay(it) },
        todayDay?.sunset?.let { minutesOfDay(it) }
    )
    val tomorrowActivity = FishActivity.activityFor(
        tomorrow, lat, lon,
        tomorrowDay?.sunrise?.let { minutesOfDay(it) },
        tomorrowDay?.sunset?.let { minutesOfDay(it) }
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Activité des poissons — aujourd'hui",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Estimation, pas une science exacte. Règle : base 20, " +
                    "+45 autour du passage de la lune (majeure, ±75 min), " +
                    "+25 autour du lever et du coucher de la lune (mineure, ±45 min), " +
                    "+12 autour du lever et du coucher du soleil (±60 min).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${todayActivity.dailyScore} / 100  ${FishActivity.starsText(todayActivity.stars)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lune : ${todayActivity.moon.phaseName} — ${todayActivity.moon.illuminationPct}% éclairée",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            // Bar chart of the 48 half-hour points, no chart library.
            Row(
                Modifier.fillMaxWidth().height(120.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                todayActivity.points.forEach { point ->
                    val high = point.score >= 70
                    Box(
                        Modifier
                            .weight(1f)
                            .height((point.score * 120 / 100).dp.coerceAtLeast(2.dp))
                            .background(
                                if (high) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00h", style = MaterialTheme.typography.bodySmall)
                Text("06h", style = MaterialTheme.typography.bodySmall)
                Text("12h", style = MaterialTheme.typography.bodySmall)
                Text("18h", style = MaterialTheme.typography.bodySmall)
                Text("24h", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Meilleures heures — aujourd'hui",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (todayActivity.periods.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium)
            } else {
                todayActivity.periods.forEach { period ->
                    InfoLine(
                        if (period.kind == "Majeure") "Période majeure" else "Période mineure",
                        Solunar.formatPeriod(period)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Demain — ${tomorrowActivity.dailyScore} / 100  " +
                    FishActivity.starsText(tomorrowActivity.stars) +
                    " — ${tomorrowActivity.moon.phaseName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (tomorrowActivity.periods.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium)
            } else {
                tomorrowActivity.periods.forEach { period ->
                    InfoLine(
                        if (period.kind == "Majeure") "Période majeure" else "Période mineure",
                        Solunar.formatPeriod(period)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Vagues

@Composable
private fun WavesTab(data: MeteoData) {
    val now = LocalDateTime.now()
    // Next 24 h, one point every 3 h.
    val next24 = data.hourlyWaves.filter { h ->
        runCatching { LocalDateTime.parse(h.time) }.getOrNull()?.let { !it.isBefore(now) } == true
    }.filterIndexed { index, _ -> index % 3 == 0 }.take(8)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Vagues — maintenant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            InfoLine("Vagues", "${"%.2f".format(data.waveHeight)} m, période ${"%.1f".format(data.wavePeriod)} s, ${compass(data.waveDir)}")
            InfoLine("Houle", "${"%.2f".format(data.swellHeight)} m, période ${"%.1f".format(data.swellPeriod)} s, ${compass(data.swellDir)}")
            InfoLine("Vagues de vent", "${"%.2f".format(data.windWaveHeight)} m")
            InfoLine("Eau de mer", "${"%.1f".format(data.seaTemp)}°C")
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Prochaines 24 h (toutes les 3 h)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (next24.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium)
            } else {
                next24.forEach { h ->
                    InfoLine(
                        hourMinute(h.time),
                        "${"%.2f".format(h.waveHeight)} m — ${"%.1f".format(h.wavePeriod)} s"
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Midi — jours suivants",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            data.days.drop(1).take(2).forEachIndexed { index, day ->
                val noon = data.hourlyWaves.firstOrNull { it.time.startsWith(day.date + "T12:00") }
                InfoLine(
                    dayLabel(day.date, index + 1),
                    if (noon != null) {
                        "${"%.2f".format(noon.waveHeight)} m — ${"%.1f".format(noon.wavePeriod)} s"
                    } else {
                        "—"
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Marées

@Composable
private fun TidesTab(data: MeteoData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Marées — 3 jours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Heures calculées à partir du niveau de la mer (Open-Meteo) : " +
                    "hautes et basses = maxima et minima locaux, jamais inventées.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (data.days.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium)
            } else {
                data.days.take(3).forEachIndexed { index, day ->
                    Text(
                        text = dayLabel(day.date, index),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    val events = data.tides.filter { it.date == day.date }
                    if (events.isEmpty()) {
                        Text(
                            text = "Pas de marée trouvée pour ce jour.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        events.forEach { tide ->
                            InfoLine(
                                if (tide.high) "Marée haute" else "Marée basse",
                                "${tide.time} — ${"%.2f".format(tide.height)} m"
                            )
                        }
                    }
                }
            }
            Text(
                text = "En Méditerranée, la marée est petite ici (environ 0,2 à 0,3 m entre haute et basse). Le vent et les vagues comptent plus que la marée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// ------------------------------------------------------------------ Météo

@Composable
private fun WeatherTab(data: MeteoData) {
    val now = LocalDateTime.now()
    val next12 = data.hourly.filter { h ->
        runCatching { LocalDateTime.parse(h.time) }.getOrNull()?.let { !it.isBefore(now) } == true
    }.take(12)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "${"%.1f".format(data.temperature)}°C — ${weatherLabel(data.weatherCode)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            InfoLine("Vent", "${data.windSpeed.toInt()} km/h ${compass(data.windDir)}")
            InfoLine("Rafales", "${data.windGusts.toInt()} km/h")
            InfoLine("Humidité", "${data.humidity}%")
            InfoLine("Ressenti", "${"%.1f".format(data.feelsLike)}°C")
            InfoLine("Pression", "${data.pressure.toInt()} hPa")
            InfoLine("UV", "${"%.1f".format(data.uvIndex)}")
            InfoLine("Nuages", "${data.cloudCover}%")
            InfoLine("Pluie maintenant", "${"%.1f".format(data.precipitation)} mm")
            InfoLine("Risque de pluie", "${data.rainProbToday}%")
        }
    }
    if (next12.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Heure par heure — 12 h",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    next12.forEach { h ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(
                                    hourMinute(h.time),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${h.temp.toInt()}°C",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Pluie ${h.rainProb}%", style = MaterialTheme.typography.bodySmall)
                                Text("Vent ${h.wind.toInt()} km/h", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Prévisions — 7 jours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            data.days.forEachIndexed { index, day ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        dayLabel(day.date, index),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Pluie ${day.rainProb}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        "Vent ${day.windMax.toInt()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Soleil & lune

@Composable
private fun SunMoonTab(data: MeteoData, lat: Double, lon: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Soleil & lune — 3 jours",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lever et coucher du soleil : Open-Meteo. Lune : calcul local " +
                    "sur le téléphone (Solunar), sans internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            if (data.days.isEmpty()) {
                Text("—", style = MaterialTheme.typography.bodyMedium)
            } else {
                data.days.take(3).forEachIndexed { index, day ->
                    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
                        ?: LocalDate.now().plusDays(index.toLong())
                    val solunar = Solunar.solunarFor(date, lat, lon)
                    Text(
                        text = dayLabel(day.date, index),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    InfoLine("Lever du soleil", hourMinute(day.sunrise))
                    InfoLine("Coucher du soleil", hourMinute(day.sunset))
                    val daylight = runCatching {
                        val rise = LocalDateTime.parse(day.sunrise)
                        val set = LocalDateTime.parse(day.sunset)
                        val d = Duration.between(rise, set)
                        "${d.toHours()} h ${d.toMinutes() % 60} min"
                    }.getOrNull()
                    InfoLine("Jour", daylight ?: "—")
                    InfoLine(
                        "Lune",
                        "${solunar.moon.phaseName} — ${solunar.moon.illuminationPct}% éclairée"
                    )
                    InfoLine(
                        "Lever de lune",
                        solunar.moonriseMin?.let { Solunar.formatMin(it) } ?: "—"
                    )
                    InfoLine(
                        "Coucher de lune",
                        solunar.moonsetMin?.let { Solunar.formatMin(it) } ?: "—"
                    )
                    if (solunar.periods.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        solunar.periods.forEach { period ->
                            InfoLine(
                                if (period.kind == "Majeure") "Période majeure" else "Période mineure",
                                Solunar.formatPeriod(period)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
