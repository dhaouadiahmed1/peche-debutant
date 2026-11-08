// Pêche Débutant v0.2 — Météo & Marée tab.
// Data: Open-Meteo, free, no API key, no login.
//   Weather: https://api.open-meteo.com/v1/forecast
//   Marine:  https://marine-api.open-meteo.com/v1/marine
//   "Weather data by Open-Meteo.com"
// Location: Android LocationManager only (no Google Play Services).
// Fallback when there is no permission or no GPS fix: Ras Jebel 37.2476, 10.0697.
// Tide times are the local maxima / minima of the hourly sea_level_height_msl
// series. In the Mediterranean here the tide is small (~0.2-0.3 m); wind and
// waves matter more than the tide.
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Fallback position: Ras Jebel, the spot in the app. Only used when there is
// no location permission or no GPS fix.
private const val FALLBACK_LAT = 37.2476
private const val FALLBACK_LON = 10.0697
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

data class TideEvent(val time: String, val height: Double, val high: Boolean)

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
    val tideRisingNow: Boolean
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

/** Tide events for [today] (yyyy-MM-dd): local maxima / minima of sea level. */
private fun tideEvents(times: List<String>, seaLevel: List<Double>, today: String): List<TideEvent> {
    val events = mutableListOf<TideEvent>()
    for (i in 1 until seaLevel.size - 1) {
        if (!times[i].startsWith(today)) continue
        val prev = seaLevel[i - 1]
        val now = seaLevel[i]
        val next = seaLevel[i + 1]
        if (now > prev && now >= next) {
            events.add(TideEvent(hourMinute(times[i]), now, true))
        } else if (now < prev && now <= next) {
            events.add(TideEvent(hourMinute(times[i]), now, false))
        }
    }
    return events
}

private suspend fun fetchMeteo(lat: Double, lon: Double): MeteoData? = withContext(Dispatchers.IO) {
    val weatherUrl =
        "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
            "relative_humidity_2m,precipitation,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max" +
            "&timezone=auto&forecast_days=3"
    val marineUrl =
        "https://marine-api.open-meteo.com/v1/marine?latitude=$lat&longitude=$lon" +
            "&current=wave_height,wave_direction,wave_period,sea_surface_temperature" +
            "&hourly=wave_height,sea_level_height_msl&timezone=auto&forecast_days=2"
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
                    maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(i),
                    minTemp = daily.getJSONArray("temperature_2m_min").getDouble(i),
                    rainProb = daily.getJSONArray("precipitation_probability_max").optInt(i, 0),
                    windMax = daily.getJSONArray("wind_speed_10m_max").getDouble(i),
                    gustMax = daily.getJSONArray("wind_gusts_10m_max").getDouble(i),
                    sunrise = daily.getJSONArray("sunrise").getString(i),
                    sunset = daily.getJSONArray("sunset").getString(i)
                )
            )
        }
        val marineCurrent = marine.getJSONObject("current")
        val marineHourly = marine.getJSONObject("hourly")
        val hourlyTimes = marineHourly.getJSONArray("time")
        val times = mutableListOf<String>()
        val seaLevel = mutableListOf<Double>()
        for (i in 0 until hourlyTimes.length()) {
            times.add(hourlyTimes.getString(i))
            seaLevel.add(marineHourly.getJSONArray("sea_level_height_msl").getDouble(i))
        }
        val now = LocalDateTime.now()
        val today = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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
            tides = tideEvents(times, seaLevel, today),
            tideRisingNow = tideRisingNow
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
                text = "Météo & Marée — $placeName",
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
                item {
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
                            InfoLine("Pluie maintenant", "${"%.1f".format(data.precipitation)} mm")
                            InfoLine("Risque de pluie", "${data.rainProbToday}%")
                            InfoLine("Vagues", "${"%.2f".format(data.waveHeight)} m, période ${"%.1f".format(data.wavePeriod)} s, ${compass(data.waveDir)}")
                            InfoLine("Eau de mer", "${"%.1f".format(data.seaTemp)}°C")
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Marée — aujourd'hui",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (data.tides.isEmpty()) {
                                Text(
                                    text = "Pas de marée trouvée pour aujourd'hui.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                data.tides.forEach { tide ->
                                    InfoLine(
                                        if (tide.high) "Marée haute" else "Marée basse",
                                        "${tide.time} — ${"%.2f".format(tide.height)} m"
                                    )
                                }
                            }
                            Text(
                                text = "En Méditerranée, la marée est petite ici (environ 0,2 à 0,3 m entre haute et basse). Le vent et les vagues comptent plus que la marée.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            val today = data.days.firstOrNull()
                            if (today != null) {
                                InfoLine("Lever du soleil", hourMinute(today.sunrise))
                                InfoLine("Coucher du soleil", hourMinute(today.sunset))
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Prévisions — 3 jours",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.days.forEachIndexed { index, day ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Text(
                                        text = dayLabel(day.date, index),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${day.maxTemp.toInt()}° / ${day.minTemp.toInt()}°",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pluie ${day.rainProb}%",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Vent ${day.windMax.toInt()} km/h",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Rafales ${day.gustMax.toInt()} km/h",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
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
