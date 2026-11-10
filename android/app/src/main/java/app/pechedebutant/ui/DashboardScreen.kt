// Pêche Débutant v0.4 — Accueil dashboard.
// Luxury dark navy + gold 3D style, dynamic like Instagram / Facebook:
// a stories row on top, a feed below that changes every day.
// v0.4 restyle, driven by the three design skills learned 2026-11-10:
// - UI/UX Pro Max: glassmorphism (frosted-glass cards over a deep navy
//   gradient), a consistent type scale (score 56sp, titles 22sp, body
//   15sp, captions 12sp), touch targets >= 48dp, bottom nav max 5 tabs.
// - Impeccable (Operate mode): scanability and consistency first;
//   refinement preserves the incumbent navy + gold identity — the brief
//   wins, so no green accent from the tool's palette suggestion.
// - Taste: one section per screen, equal edges, card lift 2px + photo
//   zoom 1.02 on press, hero entrance once, no tilt, no carousels.
// - Check blassa hero: live score from Open-Meteo, same endpoints and same
//   scoring rules as MeteoScreen.kt (spotScore is reused, not duplicated).
//   The dashboard only fetches for Ras Jebel (37.2476, 10.0697), the spot
//   in the app. If there is no connection, it shows the score saved earlier
//   today, or a prompt to open the Météo tab. It never invents weather.
// - Feed: Check blassa today, Poisson du jour, Appât du jour, Météo demain.
//   Fish and bait of the day rotate by day-of-year through the real lists.
// - Likes: heart button, saved on the phone only (SharedPreferences).
//   No account, no server, no Firebase, no login.
// - Share: Android share Intent (ACTION_SEND, text/plain, chooser). The
//   user picks WhatsApp himself. No WhatsApp package, no WhatsApp API,
//   nothing is sent automatically.
package app.pechedebutant.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import app.pechedebutant.data.BAIT_LIST
import app.pechedebutant.data.FISH_LIST
import app.pechedebutant.data.photoBitmap
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Dashboard palette: dark navy + gold (luxury 3D style).
private val Navy = Color(0xFF04121E)
private val NavyCard = Color(0xFF0A2E44)
private val NavyCard2 = Color(0xFF123E57)
private val NavyDeep = Color(0xFF0E4A63)
private val Gold = Color(0xFFD4AF37)
private val GoldLight = Color(0xFFFFF3C4)
private val GoldDark = Color(0xFF8C6D1F)
private val Cream = Color(0xFFF5F1E6)
private val Muted = Color(0xFFB9CBD6)
// Glassmorphism (v0.4): frosted-glass surfaces over the navy gradient.
// Semi-transparent white so the gradient shows through, 1px light border.
private val Glass = Color(0x14FFFFFF)
private val GlassBorder = Color(0x38FFFFFF)

// The spot in the app. Same fallback position as MeteoScreen.kt.
private const val DASH_LAT = 37.2476
private const val DASH_LON = 10.0697
private const val DASH_PLACE = "Ras Jebel"

/** Share [text] with the Android chooser. The user picks WhatsApp himself. */
fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Partager"))
}

// Bait ids use French bait names; photo slugs use the photo file names.
// Same mapping as MainActivity.kt (shrimp -> crevette, crab -> crabe,
// mussel -> moule, squid -> calamar, bread -> pain).
private fun baitPhotoSlug(baitId: String): String = when (baitId) {
    "shrimp" -> "crevette"
    "crab" -> "crabe"
    "mussel" -> "moule"
    "squid" -> "calamar"
    "bread" -> "pain"
    else -> baitId
}

private fun bestFishNowText(score: Int): String = when {
    score >= 7 -> "Loup, Dorade, Sar"
    score >= 5 -> "Sar, Mulet, Pageot"
    else -> "mouch wa9t behi, stenna"
}

// ------------------------------------------------------------------ Weather
// Same Open-Meteo endpoints as MeteoScreen.kt, same response fields.
// Only the Ras Jebel position is used here; the Météo tab handles GPS.

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

private suspend fun fetchDashboardMeteo(): MeteoData? = withContext(Dispatchers.IO) {
    val weatherUrl =
        "https://api.open-meteo.com/v1/forecast?latitude=$DASH_LAT&longitude=$DASH_LON" +
            "&current=temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m," +
            "relative_humidity_2m,precipitation,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset," +
            "precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max" +
            "&timezone=auto&forecast_days=3"
    val marineUrl =
        "https://marine-api.open-meteo.com/v1/marine?latitude=$DASH_LAT&longitude=$DASH_LON" +
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
            tides = emptyList(),
            tideRisingNow = tideRisingNow
        )
    } catch (_: Exception) {
        null
    }
}

// Last score saved today, so the hero never shows invented numbers when
// there is no connection. Only today's save is used; yesterday's is stale.
private const val METEO_PREFS = "peche_debutant_meteo"

private fun saveMeteoCache(context: Context, data: MeteoData) {
    val today = data.days.getOrNull(0) ?: return
    val tomorrow = data.days.getOrNull(1)
    context.getSharedPreferences(METEO_PREFS, Context.MODE_PRIVATE).edit()
        .putString("date", today.date)
        .putFloat("temp", data.temperature.toFloat())
        .putFloat("wind", data.windSpeed.toFloat())
        .putFloat("windDir", data.windDir.toFloat())
        .putFloat("gusts", data.windGusts.toFloat())
        .putInt("humidity", data.humidity)
        .putFloat("precip", data.precipitation.toFloat())
        .putInt("code", data.weatherCode)
        .putInt("rainProb", data.rainProbToday)
        .putFloat("wave", data.waveHeight.toFloat())
        .putFloat("wavePeriod", data.wavePeriod.toFloat())
        .putFloat("waveDir", data.waveDir.toFloat())
        .putFloat("seaTemp", data.seaTemp.toFloat())
        .putBoolean("tideRising", data.tideRisingNow)
        .putFloat("todayMax", today.maxTemp.toFloat())
        .putFloat("todayMin", today.minTemp.toFloat())
        .putInt("todayRain", today.rainProb)
        .putFloat("todayWindMax", today.windMax.toFloat())
        .putFloat("todayGustMax", today.gustMax.toFloat())
        .putString("sunrise", today.sunrise)
        .putString("sunset", today.sunset)
        .putString("tomorrowDate", tomorrow?.date ?: "")
        .putFloat("tomorrowMax", (tomorrow?.maxTemp ?: 0.0).toFloat())
        .putFloat("tomorrowMin", (tomorrow?.minTemp ?: 0.0).toFloat())
        .putInt("tomorrowRain", tomorrow?.rainProb ?: 0)
        .putFloat("tomorrowWindMax", (tomorrow?.windMax ?: 0.0).toFloat())
        .putFloat("tomorrowGustMax", (tomorrow?.gustMax ?: 0.0).toFloat())
        .apply()
}

private fun loadMeteoCache(context: Context): MeteoData? {
    val prefs = context.getSharedPreferences(METEO_PREFS, Context.MODE_PRIVATE)
    val todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    if (prefs.getString("date", "") != todayDate) return null
    val today = DayForecast(
        date = todayDate,
        maxTemp = prefs.getFloat("todayMax", 0f).toDouble(),
        minTemp = prefs.getFloat("todayMin", 0f).toDouble(),
        rainProb = prefs.getInt("todayRain", 0),
        windMax = prefs.getFloat("todayWindMax", 0f).toDouble(),
        gustMax = prefs.getFloat("todayGustMax", 0f).toDouble(),
        sunrise = prefs.getString("sunrise", "") ?: "",
        sunset = prefs.getString("sunset", "") ?: ""
    )
    val tomorrowDate = prefs.getString("tomorrowDate", "") ?: ""
    val days = if (tomorrowDate.isEmpty()) {
        listOf(today)
    } else {
        listOf(
            today,
            DayForecast(
                date = tomorrowDate,
                maxTemp = prefs.getFloat("tomorrowMax", 0f).toDouble(),
                minTemp = prefs.getFloat("tomorrowMin", 0f).toDouble(),
                rainProb = prefs.getInt("tomorrowRain", 0),
                windMax = prefs.getFloat("tomorrowWindMax", 0f).toDouble(),
                gustMax = prefs.getFloat("tomorrowGustMax", 0f).toDouble(),
                sunrise = "",
                sunset = ""
            )
        )
    }
    return MeteoData(
        temperature = prefs.getFloat("temp", 0f).toDouble(),
        windSpeed = prefs.getFloat("wind", 0f).toDouble(),
        windDir = prefs.getFloat("windDir", 0f).toDouble(),
        windGusts = prefs.getFloat("gusts", 0f).toDouble(),
        humidity = prefs.getInt("humidity", 0),
        precipitation = prefs.getFloat("precip", 0f).toDouble(),
        weatherCode = prefs.getInt("code", 0),
        rainProbToday = prefs.getInt("rainProb", 0),
        waveHeight = prefs.getFloat("wave", 0f).toDouble(),
        wavePeriod = prefs.getFloat("wavePeriod", 0f).toDouble(),
        waveDir = prefs.getFloat("waveDir", 0f).toDouble(),
        seaTemp = prefs.getFloat("seaTemp", 0f).toDouble(),
        days = days,
        tides = emptyList(),
        tideRisingNow = prefs.getBoolean("tideRising", false)
    )
}

// ------------------------------------------------------------------ Feed

private data class FeedPost(
    val id: String,
    val tag: String,
    val title: String,
    val body: String,
    val photoSlug: String?,
    val route: String,
    val baseLikes: Int,
    val shareText: String
)

private data class Story(val label: String, val initial: String, val route: String)

private val STORIES = listOf(
    Story("Dorade", "D", "fish/dorade"),
    Story("Loup", "L", "fish/loup"),
    Story("Météo", "M", "meteo"),
    Story("Bibi", "B", "bait/bibi"),
    Story("Montage", "M", "rigs"),
    Story("Guide", "G", "guide")
)

private fun buildFeed(meteo: MeteoData?): List<FeedPost> {
    val posts = mutableListOf<FeedPost>()
    val dayOfYear = LocalDate.now().dayOfYear
    if (meteo != null) {
        val spot = spotScore(meteo)
        val wind = meteo.windSpeed.toInt()
        val wave = "%.2f".format(meteo.waveHeight)
        posts.add(
            FeedPost(
                id = "check",
                tag = "CHECK BLASSA · AUJOURD'HUI",
                title = "${spot.verdict} — ${spot.score} / 10",
                body = "A $DASH_PLACE : vent $wind km/h, vagues $wave m, " +
                    "mer ${"%.1f".format(meteo.seaTemp)}°C. " +
                    "Meilleurs poissons : ${bestFishNowText(spot.score)}. " +
                    "Meilleur appât : ver de mer, crevette.",
                photoSlug = null,
                route = "meteo",
                baseLikes = 12,
                shareText = "Pêche Débutant — Check blassa à $DASH_PLACE : " +
                    "${spot.verdict} ${spot.score}/10, vent $wind km/h, " +
                    "vagues $wave m. Meilleurs poissons : " +
                    "${bestFishNowText(spot.score)}. " +
                    "Meilleur appât : ver de mer."
            )
        )
    } else {
        posts.add(
            FeedPost(
                id = "check",
                tag = "CHECK BLASSA · AUJOURD'HUI",
                title = "Check blassa",
                body = "Pas de météo chargée. Ouvre l'onglet Météo pour " +
                    "charger la météo et le score du jour. " +
                    "Cet onglet a besoin d'internet.",
                photoSlug = null,
                route = "meteo",
                baseLikes = 12,
                shareText = "Pêche Débutant — Ouvre l'application pour voir " +
                    "si la place est bonne pour pêcher aujourd'hui à $DASH_PLACE."
            )
        )
    }
    val fish = FISH_LIST[dayOfYear % FISH_LIST.size]
    val fishBaits = fish.bestBaitIds.mapNotNull { id ->
        BAIT_LIST.firstOrNull { it.id == id }?.frenchName
    }.joinToString(", ")
    posts.add(
        FeedPost(
            id = "fish-day",
            tag = "POISSON DU JOUR",
            title = fish.frenchName,
            body = "${fish.englishName} — ${fish.tunisianName}. " +
                "Meilleurs appâts : $fishBaits. Hameçon ${fish.hookSize}. " +
                fish.bestRig + ".",
            photoSlug = fish.id,
            route = "fish/${fish.id}",
            baseLikes = 8,
            shareText = "Pêche Débutant — Poisson du jour : " +
                "${fish.frenchName} (${fish.tunisianName}). " +
                "Meilleurs appâts : $fishBaits. Hameçon ${fish.hookSize}."
        )
    )
    val bait = BAIT_LIST[dayOfYear % BAIT_LIST.size]
    posts.add(
        FeedPost(
            id = "bait-day",
            tag = "APPÂT DU JOUR",
            title = bait.frenchName,
            body = "${bait.englishName}. Idéal pour : ${bait.bestFor}.",
            photoSlug = baitPhotoSlug(bait.id),
            route = "bait/${bait.id}",
            baseLikes = 5,
            shareText = "Pêche Débutant — Appât du jour : " +
                "${bait.frenchName} (${bait.englishName}). " +
                "Idéal pour : ${bait.bestFor}."
        )
    )
    val tomorrow = meteo?.days?.getOrNull(1)
    if (tomorrow != null) {
        posts.add(
            FeedPost(
                id = "tomorrow",
                tag = "MÉTÉO · DEMAIN",
                title = "Demain : ${tomorrow.maxTemp.toInt()}° / ${tomorrow.minTemp.toInt()}°",
                body = "Pluie ${tomorrow.rainProb}% · " +
                    "Vent ${tomorrow.windMax.toInt()} km/h · " +
                    "Rafales ${tomorrow.gustMax.toInt()} km/h.",
                photoSlug = null,
                route = "meteo",
                baseLikes = 3,
                shareText = "Pêche Débutant — Météo demain à $DASH_PLACE : " +
                    "${tomorrow.maxTemp.toInt()}° / ${tomorrow.minTemp.toInt()}°, " +
                    "pluie ${tomorrow.rainProb}%, " +
                    "vent ${tomorrow.windMax.toInt()} km/h, " +
                    "rafales ${tomorrow.gustMax.toInt()} km/h."
            )
        )
    } else {
        posts.add(
            FeedPost(
                id = "tomorrow",
                tag = "ASTUCE DU JOUR",
                title = "Le matin et le soir",
                body = "Le matin tôt et le soir sont les meilleurs moments. " +
                    "Le loup aime le vent, la dorade aime la mer calme.",
                photoSlug = null,
                route = "meteo",
                baseLikes = 3,
                shareText = "Pêche Débutant — Astuce : le matin tôt et le " +
                    "soir sont les meilleurs moments pour pêcher à $DASH_PLACE."
            )
        )
    }
    return posts
}

// ------------------------------------------------------------------ UI

/** Big glass card. On press it lifts 2px (Taste house rule) — the lift is
 *  a translation only, so the layout bounds never shift (pro-rules). */
@Composable
private fun PressableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift by animateFloatAsState(
        if (pressed) -2f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "lift"
    )
    val density = LocalDensity.current
    Card(
        modifier = modifier
            .graphicsLayer { translationY = lift * density.density }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Glass),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        content = content
    )
}

/** Heart button. The like is saved on the phone only, no account. */
@Composable
private fun LikeButton(postId: String, baseLikes: Int) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("peche_debutant_likes", Context.MODE_PRIVATE)
    }
    var liked by remember(postId) {
        mutableStateOf(prefs.getBoolean("liked_$postId", false))
    }
    val count = baseLikes + if (liked) 1 else 0
    OutlinedButton(
        onClick = {
            liked = !liked
            prefs.edit().putBoolean("liked_$postId", liked).apply()
        },
        modifier = Modifier.heightIn(min = 48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (liked) Gold else Cream
        )
    ) {
        Icon(
            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (liked) "J'aime" else "Aimer"
        )
        Spacer(Modifier.width(6.dp))
        Text("$count")
    }
}

@Composable
private fun FeedPostCard(post: FeedPost, nav: NavHostController) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Glass),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = post.tag,
                color = Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            val bitmap = remember(post.photoSlug) {
                post.photoSlug?.let { slug -> photoBitmap(slug) }
            }
            // Photo zoom state is read unconditionally: never call remember
            // or collectIsPressedAsState inside the if-branch below.
            val photoInteraction = remember { MutableInteractionSource() }
            val photoPressed by photoInteraction.collectIsPressedAsState()
            val photoZoom by animateFloatAsState(
                if (photoPressed) 1.02f else 1f,
                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
                label = "photoZoom"
            )
            if (bitmap != null) {
                // Photo zooms 1.02 inside its frame while pressed (Taste).
                // The photo is a real control: tapping it opens the post.
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer(scaleX = photoZoom, scaleY = photoZoom)
                        .clickable(
                            interactionSource = photoInteraction,
                            indication = null,
                            onClick = { nav.navigate(post.route) }
                        )
                )
                Spacer(Modifier.height(10.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0E8AA8), Color(0xFF0B5A7A), Gold)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = post.title,
                color = Cream,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = post.body,
                color = Muted,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                LikeButton(post.id, post.baseLikes)
                OutlinedButton(
                    onClick = { nav.navigate(post.route) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Cream)
                ) { Text("Voir") }
                OutlinedButton(
                    onClick = { shareText(context, post.shareText) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Partager")
                    Spacer(Modifier.width(6.dp))
                    Text("Partager")
                }
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavHostController) {
    val context = LocalContext.current
    var meteo by remember { mutableStateOf(loadMeteoCache(context)) }
    var loading by remember { mutableStateOf(meteo == null) }
    // Hero entrance: plays once, when the screen first shows.
    var heroVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        heroVisible = true
        val fresh = fetchDashboardMeteo()
        if (fresh != null) {
            saveMeteoCache(context, fresh)
            meteo = fresh
        }
        loading = false
    }
    // One authored moment (craft-floor): the hero fades and settles in once.
    val heroAlpha by animateFloatAsState(
        if (heroVisible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "heroAlpha"
    )
    val heroScale by animateFloatAsState(
        if (heroVisible) 1f else 0.97f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "heroScale"
    )

    val spot = meteo?.let { spotScore(it) }
    // Local copy: 'meteo' is a delegated property (by remember), so Kotlin
    // cannot smart-cast it after a null check. Use this plain val instead.
    val currentMeteo = meteo
    val feed = buildFeed(meteo)
    val todayLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.FRENCH)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Navy, NavyCard, NavyDeep))
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Pêche Débutant",
                color = Cream,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$DASH_PLACE · $todayLabel",
                color = Muted,
                fontSize = 14.sp
            )
        }

        // Stories row: gold rings, like Instagram / Facebook.
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(STORIES) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { nav.navigate(story.route) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Gold, Color(0xFF0E8AA8), Gold)
                                    )
                                )
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(NavyCard2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = story.initial,
                                color = Gold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(text = story.label, color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Hero: Check blassa, gold ring, big score. Glassmorphism (v0.4):
        // frosted-glass surface over the navy gradient, gold border.
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(alpha = heroAlpha, scaleX = heroScale, scaleY = heroScale),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Glass),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CHECK BLASSA",
                        color = Gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    if (loading && meteo == null) {
                        CircularProgressIndicator(color = Gold)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Chargement de la météo…",
                            color = Muted,
                            fontSize = 14.sp
                        )
                    } else if (spot != null && currentMeteo != null) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(GoldLight, Gold, GoldDark)
                                    )
                                )
                                .border(2.dp, GoldLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${spot.score}/10",
                                    color = Navy,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = spot.verdict,
                                    color = Navy,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (spot.score >= 7) "Tu peux pêcher maintenant" else "Regarde les raisons dans l'onglet Météo",
                            color = Cream,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vent ${currentMeteo.windSpeed.toInt()} km/h · " +
                                "Vagues ${"%.2f".format(currentMeteo.waveHeight)} m · " +
                                "Eau ${"%.1f".format(currentMeteo.seaTemp)}°C",
                            color = Muted,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        Text(
                            text = "Pas de météo chargée",
                            color = Cream,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ouvre l'onglet Météo pour charger la météo et le score du jour. Cet onglet a besoin d'internet.",
                            color = Muted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { nav.navigate("meteo") },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Gold,
                                contentColor = Navy
                            )
                        ) { Text("Ouvrir Météo", fontWeight = FontWeight.Bold) }
                        if (spot != null && currentMeteo != null) {
                            OutlinedButton(
                                onClick = {
                                    shareText(
                                        context,
                                        "Pêche Débutant — Check blassa à $DASH_PLACE : " +
                                            "${spot.verdict} ${spot.score}/10, " +
                                            "vent ${currentMeteo.windSpeed.toInt()} km/h, " +
                                            "vagues ${"%.2f".format(currentMeteo.waveHeight)} m. " +
                                            "Meilleurs poissons : " +
                                            "${bestFishNowText(spot.score)}. " +
                                            "Meilleur appât : ver de mer."
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Partager")
                                Spacer(Modifier.width(6.dp))
                                Text("Partager")
                            }
                        }
                    }
                }
            }
        }

        // Big weather numbers.
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cells = listOf(
                    "Air" to (meteo?.let { "${"%.1f".format(it.temperature)}°" } ?: "—"),
                    "Vent" to (meteo?.let { "${it.windSpeed.toInt()} km/h" } ?: "—"),
                    "Vagues" to (meteo?.let { "${"%.2f".format(it.waveHeight)} m" } ?: "—"),
                    "Eau" to (meteo?.let { "${"%.1f".format(it.seaTemp)}°" } ?: "—")
                )
                cells.forEach { (label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Glass),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value,
                                color = Cream,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(text = label, color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            Text(
                text = "Position : $DASH_PLACE. Ouvre l'onglet Météo pour ta position GPS.",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Big 3D cards: the four sections.
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PressableCard(
                    onClick = { nav.navigate("fish") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Poissons",
                        color = Cream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp)
                    )
                    Text(
                        text = "12 poissons\nDorade, Loup, Sar…",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(18.dp, 4.dp, 18.dp, 18.dp)
                    )
                }
                PressableCard(
                    onClick = { nav.navigate("baits") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Appâts",
                        color = Cream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp)
                    )
                    Text(
                        text = "8 appâts\nVer de mer, Bibi…",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(18.dp, 4.dp, 18.dp, 18.dp)
                    )
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PressableCard(
                    onClick = { nav.navigate("rigs") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Montages",
                        color = Cream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp)
                    )
                    Text(
                        text = "5 montages\nCoulissant, Paternoster…",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(18.dp, 4.dp, 18.dp, 18.dp)
                    )
                }
                PressableCard(
                    onClick = { nav.navigate("meteo") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Météo & Marée",
                        color = Cream,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp)
                    )
                    Text(
                        text = "Marée et prévisions\n3 jours",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(18.dp, 4.dp, 18.dp, 18.dp)
                    )
                }
            }
        }
        // Guide lives here now: the bottom nav has 5 tabs in v0.4, and the
        // Guide is reached from this card and from the Guide story.
        item {
            PressableCard(
                onClick = { nav.navigate("guide") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Guide Débutant",
                    color = Cream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(18.dp, 18.dp, 18.dp, 0.dp)
                )
                Text(
                    text = "Matériel, lancer, sécurité, spots — tout pour commencer",
                    color = Muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(18.dp, 4.dp, 18.dp, 18.dp)
                )
            }
        }

        // Dynamic feed, like Instagram / Facebook.
        item {
            Text(
                text = "Feed du jour",
                color = Cream,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "Le contenu change chaque jour : le score, le poisson et l'appât du jour, et la météo de demain.",
                color = Muted,
                fontSize = 13.sp
            )
        }
        items(feed) { post ->
            FeedPostCard(post, nav)
        }
        item {
            Text(
                text = "Les likes sont gardés sur ton téléphone seulement. Pas de compte, pas de serveur.",
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
