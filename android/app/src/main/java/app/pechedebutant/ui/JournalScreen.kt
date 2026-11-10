// Pêche Débutant v0.5 — Journal: catch log ("Mes prises") + saved GPS
// spots ("Mes spots").
// On-device only (SQLite, JournalDb.kt). No account, no server, no sync.
// - Catches: fish (one of the 12), optional length / weight / bait /
//   notes / position, plus an automatic weather snapshot (wind, wave,
//   sea temp, Check blassa score) taken from today's saved météo cache.
//   If no weather was loaded today, the snapshot stays empty ("—"):
//   the app never invents weather.
// - Spots: saved only when the user taps "Enregistrer ma position".
//   Nothing is pre-seeded with coordinates — the 5 Guide spots stay
//   by name only. "Naviguer" opens the spot in the maps app of the
//   user's choice (geo: intent, no map UI in v0.5).
// - NO photo capture in v0.5 (camera + FileProvider is a later step).
package app.pechedebutant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import app.pechedebutant.data.BAIT_LIST
import app.pechedebutant.data.CatchEntry
import app.pechedebutant.data.FISH_LIST
import app.pechedebutant.data.JournalDb
import app.pechedebutant.data.SpotEntry
import app.pechedebutant.data.baitById
import app.pechedebutant.data.fishById
import app.pechedebutant.data.photoBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ------------------------------------------------------------------ helpers

private fun lastKnownLocationJournal(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
    val network = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
    return listOfNotNull(gps, network).maxByOrNull { it.time }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
        sin(dLon / 2) * sin(dLon / 2)
    return 2 * r * asin(sqrt(a))
}

private fun formatCatchDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH)
    return sdf.format(Date(millis))
}

private data class WeatherSnapshot(
    val windKmh: Double,
    val waveM: Double,
    val seaTempC: Double,
    val score: Int
)

// Today's saved météo cache (from the Accueil hero or the Météo tab).
// Null when nothing was loaded today — the snapshot then stays empty.
private fun todayWeatherSnapshot(context: Context): WeatherSnapshot? {
    val cached = loadMeteoCache(context) ?: return null
    return WeatherSnapshot(
        windKmh = cached.windSpeed,
        waveM = cached.waveHeight,
        seaTempC = cached.seaTemp,
        score = spotScore(cached).score
    )
}

@Composable
private fun JournalPhotoImage(
    slug: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(slug) { photoBitmap(slug) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

private fun navigateToSpot(context: Context, spot: SpotEntry) {
    val uri = Uri.parse(
        "geo:${spot.lat},${spot.lon}?q=${spot.lat},${spot.lon}(${Uri.encode(spot.name)})"
    )
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

// ------------------------------------------------------------------ Journal

@Composable
fun JournalScreen(nav: NavHostController, initialTab: String = "prises") {
    val context = LocalContext.current
    val db = remember { JournalDb(context) }
    var tab by remember(initialTab) {
        mutableStateOf(if (initialTab == "spots") "spots" else "prises")
    }
    var catches by remember { mutableStateOf(db.listCatches()) }
    var spots by remember { mutableStateOf(db.listSpots()) }
    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var confirmDeleteCatch by remember { mutableStateOf<CatchEntry?>(null) }
    var confirmDeleteSpot by remember { mutableStateOf<SpotEntry?>(null) }
    var showAddSpot by remember { mutableStateOf(false) }
    var pendingSpotLocation by remember { mutableStateOf<Location?>(null) }
    var spotMessage by remember { mutableStateOf("") }

    fun refresh() {
        catches = db.listCatches()
        spots = db.listSpots()
    }

    val spotPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val known = lastKnownLocationJournal(context)
            if (known != null) {
                pendingSpotLocation = known
                showAddSpot = true
                spotMessage = ""
            } else {
                spotMessage = "Position indisponible — active le GPS et réessaie."
            }
        } else {
            spotMessage = "Permission de position refusée — le spot ne peut pas être enregistré."
        }
    }

    fun requestSpotLocation() {
        if (hasLocationPermission(context)) {
            val known = lastKnownLocationJournal(context)
            if (known != null) {
                pendingSpotLocation = known
                showAddSpot = true
                spotMessage = ""
            } else {
                // One fresh fix, then give up with a message.
                val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                try {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            pendingSpotLocation = location
                            showAddSpot = true
                            spotMessage = ""
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    manager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
                    spotMessage = "Recherche de ta position…"
                } catch (_: Exception) {
                    spotMessage = "Position indisponible — active le GPS et réessaie."
                }
            }
        } else {
            spotPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Current position, only for the distance to each spot. Read once, if
    // the permission is already granted; the Météo tab already asks for it.
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            currentLocation = lastKnownLocationJournal(context)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Journal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tes prises et tes spots, gardés sur ton téléphone seulement. Pas de compte, pas de serveur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { tab = "prises" },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = tab != "prises"
                ) { Text("Prises") }
                Button(
                    onClick = { tab = "spots" },
                    modifier = Modifier.weight(1f).height(48.dp),
                    enabled = tab != "spots"
                ) { Text("Spots") }
            }
        }

        if (tab == "prises") {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val biggest = catches.filter { it.lengthCm != null }
                            .maxByOrNull { it.lengthCm ?: 0 }
                        val biggestWeight = catches.filter { it.weightG != null }
                            .maxByOrNull { it.weightG ?: 0 }
                        Text(
                            text = "${catches.size} prise(s)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val biggestText = when {
                            biggest != null -> {
                                val fishName = fishById(biggest.fishId)?.frenchName ?: biggest.fishId
                                "Plus grosse : $fishName, ${biggest.lengthCm} cm"
                            }
                            biggestWeight != null -> {
                                val fishName = fishById(biggestWeight.fishId)?.frenchName ?: biggestWeight.fishId
                                "Plus grosse : $fishName, ${biggestWeight.weightG} g"
                            }
                            else -> "Plus grosse : —"
                        }
                        Text(text = biggestText, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Photo de la prise : pas encore dans cette version.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { nav.navigate("journal/add") },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Ajouter")
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle prise")
                }
            }
            if (catches.isEmpty()) {
                item {
                    Text(
                        text = "Aucune prise pour le moment. Ajoute ta première prise !",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(catches) { entry ->
                    val fish = fishById(entry.fishId)
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { nav.navigate("journal/catch/${entry.id}") }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            JournalPhotoImage(
                                slug = entry.fishId,
                                contentDescription = fish?.frenchName ?: entry.fishId,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = fish?.frenchName ?: entry.fishId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = formatCatchDate(entry.dateTimeMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val details = buildString {
                                    entry.lengthCm?.let { append("$it cm") }
                                    entry.weightG?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append("$it g")
                                    }
                                    entry.baitId?.let { baitById(it)?.frenchName }?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append(it)
                                    }
                                }
                                if (details.isNotEmpty()) {
                                    Text(text = details, style = MaterialTheme.typography.bodyMedium)
                                }
                                val weather = buildString {
                                    entry.spotScore?.let { append("Check blassa $it/10") }
                                    entry.windKmh?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append("vent ${it.toInt()} km/h")
                                    }
                                    entry.waveM?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append("vagues ${"%.2f".format(it)} m")
                                    }
                                }
                                Text(
                                    text = if (weather.isEmpty()) "Météo : —" else weather,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { confirmDeleteCatch = entry }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Button(
                    onClick = { requestSpotLocation() },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Position")
                    Spacer(Modifier.width(8.dp))
                    Text("Enregistrer ma position")
                }
                if (spotMessage.isNotEmpty()) {
                    Text(
                        text = spotMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (showAddSpot && pendingSpotLocation != null) {
                item {
                    AddSpotForm(
                        location = pendingSpotLocation!!,
                        onSaved = {
                            refresh()
                            showAddSpot = false
                            pendingSpotLocation = null
                        },
                        onCancel = {
                            showAddSpot = false
                            pendingSpotLocation = null
                        }
                    )
                }
            }
            if (spots.isEmpty()) {
                item {
                    Text(
                        text = "Aucun spot enregistré. Enregistre ta position quand tu es sur un bon coin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(spots) { spot ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Place, contentDescription = "Spot")
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = spot.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { confirmDeleteSpot = spot }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                                }
                            }
                            Text(
                                text = "Lat ${"%.5f".format(spot.lat)}, Lon ${"%.5f".format(spot.lon)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val currentLoc = currentLocation
                            if (currentLoc != null) {
                                val km = distanceKm(
                                    currentLoc.latitude, currentLoc.longitude,
                                    spot.lat, spot.lon
                                )
                                Text(
                                    text = "Distance : ${"%.1f".format(km)} km de ta position",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(
                                    text = "Distance : — (position indisponible)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (spot.notes.isNotBlank()) {
                                Text(text = spot.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navigateToSpot(context, spot) },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.Navigation, contentDescription = "Naviguer")
                                Spacer(Modifier.width(8.dp))
                                Text("Naviguer")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (confirmDeleteCatch != null) {
        val entry = confirmDeleteCatch!!
        AlertDialog(
            onDismissRequest = { confirmDeleteCatch = null },
            title = { Text("Supprimer cette prise ?") },
            text = { Text("Cette prise sera supprimée du journal.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteCatch(entry.id)
                    confirmDeleteCatch = null
                    refresh()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCatch = null }) { Text("Annuler") }
            }
        )
    }
    if (confirmDeleteSpot != null) {
        val spot = confirmDeleteSpot!!
        AlertDialog(
            onDismissRequest = { confirmDeleteSpot = null },
            title = { Text("Supprimer ce spot ?") },
            text = { Text("Ce spot sera supprimé du journal.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteSpot(spot.id)
                    confirmDeleteSpot = null
                    refresh()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSpot = null }) { Text("Annuler") }
            }
        )
    }
}

// ------------------------------------------------------------------ Add spot

@Composable
private fun AddSpotForm(
    location: Location,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { JournalDb(context) }
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Nouveau spot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lat ${"%.5f".format(location.latitude)}, " +
                    "Lon ${"%.5f".format(location.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du spot") },
                placeholder = { Text("Ex : Chatt Mami, le rocher") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optionnel)") },
                modifier = Modifier.fillMaxWidth()
            )
            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "Donne un nom à ton spot."
                            return@Button
                        }
                        db.addSpot(
                            SpotEntry(
                                name = name.trim(),
                                lat = location.latitude,
                                lon = location.longitude,
                                notes = notes.trim(),
                                createdMillis = System.currentTimeMillis()
                            )
                        )
                        onSaved()
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Enregistrer") }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Annuler") }
            }
        }
    }
}

// ------------------------------------------------------------------ Add catch

@Composable
fun AddCatchScreen(nav: NavHostController, preselectedFishId: String? = null) {
    val context = LocalContext.current
    val db = remember { JournalDb(context) }
    val preselected = preselectedFishId?.let { fishById(it) }
    var selectedFish by remember { mutableStateOf(preselected) }
    var fishExpanded by remember { mutableStateOf(false) }
    var selectedBaitId by remember { mutableStateOf<String?>(null) }
    var lengthText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var usePosition by remember { mutableStateOf(false) }
    var catchLocation by remember { mutableStateOf<Location?>(null) }
    var positionMessage by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val snapshot = remember { todayWeatherSnapshot(context) }

    val catchPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            val known = lastKnownLocationJournal(context)
            if (known != null) {
                catchLocation = known
                usePosition = true
                positionMessage = ""
            } else {
                positionMessage = "Position indisponible — active le GPS."
            }
        } else {
            positionMessage = "Permission de position refusée."
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Nouvelle prise",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "La météo du moment est enregistrée automatiquement " +
                    "si elle a été chargée aujourd'hui (Accueil ou Météo). " +
                    "Sinon elle reste vide — jamais inventée.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Text(
                text = "Poisson *",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Box {
                OutlinedButton(
                    onClick = { fishExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(selectedFish?.frenchName ?: "Choisir un poisson")
                }
                DropdownMenu(
                    expanded = fishExpanded,
                    onDismissRequest = { fishExpanded = false }
                ) {
                    FISH_LIST.forEach { fish ->
                        DropdownMenuItem(
                            text = { Text(fish.frenchName) },
                            onClick = {
                                selectedFish = fish
                                fishExpanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            Text(
                text = "Appât (optionnel)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedBaitId == null,
                        onClick = { selectedBaitId = null },
                        label = { Text("Aucun") }
                    )
                }
                items(BAIT_LIST) { bait ->
                    FilterChip(
                        selected = selectedBaitId == bait.id,
                        onClick = { selectedBaitId = bait.id },
                        label = { Text(bait.frenchName) }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it.filter { c -> c.isDigit() } },
                    label = { Text("Longueur (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() } },
                    label = { Text("Poids (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optionnel)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    if (hasLocationPermission(context)) {
                        val known = lastKnownLocationJournal(context)
                        if (known != null) {
                            catchLocation = known
                            usePosition = true
                            positionMessage = ""
                        } else {
                            positionMessage = "Position indisponible — active le GPS."
                        }
                    } else {
                        catchPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Position")
                Spacer(Modifier.width(8.dp))
                Text("Utiliser ma position")
            }
            val loc = catchLocation
            if (usePosition && loc != null) {
                Text(
                    text = "Position : Lat ${"%.5f".format(loc.latitude)}, " +
                        "Lon ${"%.5f".format(loc.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (positionMessage.isNotEmpty()) {
                Text(
                    text = positionMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        item {
            val snap = snapshot
            Text(
                text = if (snap != null) {
                    "Météo enregistrée avec la prise : vent ${snap.windKmh.toInt()} km/h, " +
                        "vagues ${"%.2f".format(snap.waveM)} m, " +
                        "mer ${"%.1f".format(snap.seaTempC)}°C, " +
                        "Check blassa ${snap.score}/10."
                } else {
                    "Météo : — (pas chargée aujourd'hui)"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Button(
                onClick = {
                    val fish = selectedFish
                    if (fish == null) {
                        error = "Choisis un poisson."
                        return@Button
                    }
                    val loc = if (usePosition) catchLocation else null
                    val snap = snapshot
                    db.addCatch(
                        CatchEntry(
                            dateTimeMillis = System.currentTimeMillis(),
                            fishId = fish.id,
                            lengthCm = lengthText.toIntOrNull(),
                            weightG = weightText.toIntOrNull(),
                            baitId = selectedBaitId,
                            lat = loc?.latitude,
                            lon = loc?.longitude,
                            notes = notes.trim(),
                            windKmh = snap?.windKmh,
                            waveM = snap?.waveM,
                            seaTempC = snap?.seaTempC,
                            spotScore = snap?.score
                        )
                    )
                    nav.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("Enregistrer la prise") }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ------------------------------------------------------------------ Catch detail

@Composable
fun CatchDetailScreen(nav: NavHostController, catchId: Long) {
    val context = LocalContext.current
    val db = remember { JournalDb(context) }
    var entry by remember { mutableStateOf<CatchEntry?>(null) }
    var showDelete by remember { mutableStateOf(false) }

    LaunchedEffect(catchId) {
        entry = db.listCatches().firstOrNull { it.id == catchId }
    }

    val currentEntry = entry
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentEntry == null) {
            item {
                Text(
                    text = "Prise introuvable.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val fish = fishById(currentEntry.fishId)
            item {
                JournalPhotoImage(
                    slug = currentEntry.fishId,
                    contentDescription = fish?.frenchName ?: currentEntry.fishId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = fish?.frenchName ?: currentEntry.fishId,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatCatchDate(currentEntry.dateTimeMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        val rows = mutableListOf<Pair<String, String>>()
                        currentEntry.lengthCm?.let { rows += "Longueur" to "$it cm" }
                        currentEntry.weightG?.let { rows += "Poids" to "$it g" }
                        currentEntry.baitId?.let { baitById(it)?.frenchName }
                            ?.let { rows += "Appât" to it }
                        if (currentEntry.lat != null && currentEntry.lon != null) {
                            rows += "Position" to
                                "Lat ${"%.5f".format(currentEntry.lat)}, " +
                                "Lon ${"%.5f".format(currentEntry.lon)}"
                        }
                        if (currentEntry.notes.isNotBlank()) {
                            rows += "Notes" to currentEntry.notes
                        }
                        if (rows.isEmpty()) {
                            Text(
                                text = "Aucun détail enregistré.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            rows.forEach { (label, value) ->
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
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Météo au moment de la prise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val weather = buildString {
                            currentEntry.spotScore?.let { append("Check blassa $it/10") }
                            currentEntry.windKmh?.let {
                                if (isNotEmpty()) append(" · ")
                                append("vent ${it.toInt()} km/h")
                            }
                            currentEntry.waveM?.let {
                                if (isNotEmpty()) append(" · ")
                                append("vagues ${"%.2f".format(it)} m")
                            }
                            currentEntry.seaTempC?.let {
                                if (isNotEmpty()) append(" · ")
                                append("mer ${"%.1f".format(it)}°C")
                            }
                        }
                        Text(
                            text = if (weather.isEmpty()) {
                                "— (pas de météo chargée ce jour-là)"
                            } else {
                                weather
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer cette prise")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDelete && currentEntry != null) {
        val toDelete = currentEntry
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Supprimer cette prise ?") },
            text = { Text("Cette prise sera supprimée du journal.") },
            confirmButton = {
                TextButton(onClick = {
                    db.deleteCatch(toDelete.id)
                    showDelete = false
                    nav.popBackStack()
                }) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Annuler") }
            }
        )
    }
}
