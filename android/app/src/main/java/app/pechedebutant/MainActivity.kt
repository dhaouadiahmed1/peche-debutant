package app.pechedebutant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.pechedebutant.data.BAIT_LIST
import app.pechedebutant.data.FISH_LIST
import app.pechedebutant.data.GEAR_CHECKLIST
import app.pechedebutant.data.RIG_LIST
import app.pechedebutant.data.SPOTS
import app.pechedebutant.data.baitById
import app.pechedebutant.data.fishById
import app.pechedebutant.data.photoBitmap
import app.pechedebutant.data.rigById
import app.pechedebutant.ui.AddCatchScreen
import app.pechedebutant.ui.CatchDetailScreen
import app.pechedebutant.ui.HomeScreen
import app.pechedebutant.ui.JournalScreen
import app.pechedebutant.ui.MeteoScreen
import app.pechedebutant.ui.shareText
import app.pechedebutant.ui.RigLegend
import app.pechedebutant.ui.RigSchematic
import app.pechedebutant.ui.theme.PecheDebutantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PecheDebutantTheme { PecheApp() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

// v0.4: 5 tabs only (UI/UX Pro Max: max 5 in a bottom nav). The Guide
// leaves the nav but stays in the app: its "guide" route still exists
// below, reached from the dashboard Guide card and the Guide story.
private val TABS = listOf(
    Tab("home", "Accueil", Icons.Filled.Home),
    Tab("fish", "Fish", Icons.Filled.List),
    Tab("baits", "Baits", Icons.Filled.Star),
    Tab("rigs", "Rigs", Icons.Filled.Build),
    Tab("meteo", "Météo", Icons.Filled.Cloud)
)

// Bait ids use French bait names; photo slugs use the photo file names:
// shrimp -> crevette, crab -> crabe, mussel -> moule, squid -> calamar,
// bread -> pain. All other bait ids match their photo slug.
private fun baitPhotoSlug(baitId: String): String = when (baitId) {
    "shrimp" -> "crevette"
    "crab" -> "crabe"
    "mussel" -> "moule"
    "squid" -> "calamar"
    "bread" -> "pain"
    else -> baitId
}

/** A bundled real photo, decoded from Base64, or nothing if there is none. */
@Composable
private fun PhotoImage(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PecheApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    val isDetail = route.startsWith("fish/") || route.startsWith("bait/") || route.startsWith("rig/") ||
        route.startsWith("journal/")
    val title = when {
        route.startsWith("fish/") -> "Fish detail"
        route.startsWith("bait/") -> "Bait detail"
        route.startsWith("rig/") -> "Rig detail"
        route.startsWith("journal/catch/") -> "Ma prise"
        route.startsWith("journal/add") -> "Nouvelle prise"
        route.startsWith("journal/") -> "Journal"
        route == "home" -> "Accueil"
        route == "baits" -> "Baits (Appâts)"
        route == "rigs" -> "Rigs (Montages)"
        route == "guide" -> "Beginner guide (Débutant)"
        route == "meteo" -> "Météo & Marée"
        else -> "Fish (Poissons)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isDetail) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo("home") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            composable("home") { HomeScreen(nav) }
            composable("fish") { FishListScreen(nav) }
            composable("baits") { BaitListScreen(nav) }
            composable("rigs") { RigListScreen(nav) }
            composable("guide") { GuideScreen() }
            composable("meteo") { MeteoScreen() }
            // v0.5 — Journal: catch log + saved spots. The specific routes
            // come first: "journal/add" must not match "journal/{tab}".
            composable("journal/add") { AddCatchScreen(nav, null) }
            composable("journal/add/{fishId}") { entry ->
                AddCatchScreen(nav, entry.arguments?.getString("fishId"))
            }
            composable("journal/catch/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                CatchDetailScreen(nav, id)
            }
            // One route with a section argument ("prises" or "spots"),
            // reached from the two Accueil dashboard cards.
            composable("journal/{tab}") { entry ->
                JournalScreen(nav, entry.arguments?.getString("tab") ?: "prises")
            }
            composable("fish/{id}") { entry ->
                val fish = fishById(entry.arguments?.getString("id") ?: "")
                if (fish != null) FishDetailScreen(fish, nav) else NotFound("Fish not found.")
            }
            composable("bait/{id}") { entry ->
                val bait = baitById(entry.arguments?.getString("id") ?: "")
                if (bait != null) BaitDetailScreen(bait) else NotFound("Bait not found.")
            }
            composable("rig/{id}") { entry ->
                val rig = rigById(entry.arguments?.getString("id") ?: "")
                if (rig != null) RigDetailScreen(rig) else NotFound("Rig not found.")
            }
        }
    }
}

@Composable
private fun NotFound(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(120.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NumberedSteps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(text = step, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun BulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                Text(text = "•  ", style = MaterialTheme.typography.bodyMedium)
                Text(text = item, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------- Fish tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FishListScreen(nav: NavHostController) {
    var query by remember { mutableStateOf("") }
    val filtered = FISH_LIST.filter { fish ->
        query.isBlank() ||
            fish.frenchName.contains(query, ignoreCase = true) ||
            fish.englishName.contains(query, ignoreCase = true) ||
            fish.tunisianName.contains(query, ignoreCase = true) ||
            fish.scientificName.contains(query, ignoreCase = true)
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        item {
            Text(
                text = "12 fish of Ras Jebel and Bizerte. Tap a fish to see its baits, hook and rig.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search a fish") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        items(filtered) { fish ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { nav.navigate("fish/${fish.id}") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val fishBitmap = remember(fish.id) { photoBitmap(fish.id) }
                    if (fishBitmap != null) {
                        Image(
                            bitmap = fishBitmap.asImageBitmap(),
                            contentDescription = fish.frenchName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fish.frenchName.first().toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = fish.frenchName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${fish.englishName} — ${fish.tunisianName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${fish.habitat} • ${fish.difficulty}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "No fish found. Try another name.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FishDetailScreen(
    fish: app.pechedebutant.data.Fish,
    nav: NavHostController
) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            PhotoImage(
                slug = fish.id,
                contentDescription = fish.frenchName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = fish.frenchName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${fish.englishName} — ${fish.tunisianName}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = fish.scientificName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionTitle("Where and when")
            InfoRow("Lives", fish.habitat)
            InfoRow("Season", fish.season)
            InfoRow("Difficulty", fish.difficulty)
            InfoRow("Common size", fish.commonSize)
            SectionTitle("Tackle")
            InfoRow("Hook size", fish.hookSize)
            InfoRow("Best rig", fish.bestRig)
            // v0.5 — richer species card. Depth ranges are FishBase values
            // (pageot and sole verified against fishbase.se); water
            // temperature stays "Non renseigné" — FishBase publishes no
            // single verified range per species, so the app never invents
            // one. Edibility and lures are standard angling knowledge.
            SectionTitle("Fiche complète")
            InfoRow("Profondeur", fish.depthRange)
            InfoRow("Eau favorable", fish.waterTempRange)
            InfoRow("À table", fish.edibility)
            InfoRow("Leurres", fish.lures)
            SectionTitle("Best baits — tap to see how to hook it")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(fish.bestBaitIds) { baitId ->
                    val bait = baitById(baitId)
                    if (bait != null) {
                        FilterChip(
                            selected = false,
                            onClick = { nav.navigate("bait/${bait.id}") },
                            label = { Text(bait.frenchName) }
                        )
                    }
                }
            }
            SectionTitle("Beginner tips")
            BulletList(fish.tips)
            Spacer(Modifier.height(12.dp))
            // v0.5 — add a catch to the log, with this fish pre-filled.
            Button(
                onClick = { nav.navigate("journal/add/${fish.id}") },
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Ajouter une prise")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val baitNames = fish.bestBaitIds.mapNotNull { baitById(it)?.frenchName }
                        .joinToString(", ")
                    shareText(
                        context,
                        "Pêche Débutant — ${fish.frenchName} (${fish.tunisianName}, " +
                            "${fish.scientificName}). Meilleurs appâts : $baitNames. " +
                            "Hameçon ${fish.hookSize}. ${fish.bestRig}."
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Partager")
                Spacer(Modifier.width(8.dp))
                Text("Partager")
            }
            Text(
                text = "Release small fish. Check local rules for size limits.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- Baits tab

@Composable
private fun BaitListScreen(nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        item {
            Text(
                text = "8 baits. Tap a bait to see how to keep it and how to hook it, step by step.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(BAIT_LIST) { bait ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { nav.navigate("bait/${bait.id}") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val baitBitmap = remember(bait.id) { photoBitmap(baitPhotoSlug(bait.id)) }
                    if (baitBitmap != null) {
                        Image(
                            bitmap = baitBitmap.asImageBitmap(),
                            contentDescription = bait.frenchName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = bait.frenchName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bait.englishName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Best for: ${bait.bestFor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaitDetailScreen(bait: app.pechedebutant.data.Bait) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            PhotoImage(
                slug = baitPhotoSlug(bait.id),
                contentDescription = bait.frenchName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = bait.frenchName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = bait.englishName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionTitle("Best for")
            Text(text = bait.bestFor, style = MaterialTheme.typography.bodyMedium)
            SectionTitle("How to keep it")
            Text(text = bait.keepFresh, style = MaterialTheme.typography.bodyMedium)
            SectionTitle("How to hook it")
            NumberedSteps(bait.hookingSteps)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    shareText(
                        context,
                        "Pêche Débutant — ${bait.frenchName} (${bait.englishName}). " +
                            "Idéal pour : ${bait.bestFor}."
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Partager")
                Spacer(Modifier.width(8.dp))
                Text("Partager")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- Rigs tab

@Composable
private fun RigListScreen(nav: NavHostController) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        item {
            Text(
                text = "5 rigs (montages). Tap a rig to see its parts, how to build it, and a drawing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(RIG_LIST) { rig ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { nav.navigate("rig/${rig.id}") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        text = rig.frenchName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rig.englishName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "For: ${rig.targetFish}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RigDetailScreen(rig: app.pechedebutant.data.Rig) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Text(
                text = rig.frenchName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = rig.englishName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SectionTitle("When to use")
            Text(text = rig.whenToUse, style = MaterialTheme.typography.bodyMedium)
            InfoRow("Target fish", rig.targetFish)
            SectionTitle("Drawing")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    RigSchematic(diagram = rig.diagram)
                    RigLegend()
                }
            }
            SectionTitle("Parts")
            BulletList(rig.parts)
            SectionTitle("How to build it")
            NumberedSteps(rig.steps)
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- Guide tab

@Composable
private fun GuideScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Text(
                text = "Beginner guide — Ras Jebel and Bizerte",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Shore rod fishing in Tunisia needs no permit (order of 21 April 2025). This is a general note, check local rules before you fish.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            SectionTitle("Gear checklist")
            GEAR_CHECKLIST.forEach { item ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = item, style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionTitle("Best times")
            BulletList(
                listOf(
                    "Early morning and evening are the best times.",
                    "Loup is best from September to April, on windy days, at dawn and dusk.",
                    "Dorade is best from spring to autumn, on a calm sea.",
                    "Marbré and sole are best at night."
                )
            )

            SectionTitle("Casting basics")
            NumberedSteps(
                listOf(
                    "Put the bait on the hook and check it is held well.",
                    "Hold the rod behind you, lead and bait hanging down.",
                    "Look where you want to cast, then swing the rod forward in one smooth move.",
                    "Release the line when the rod points to the sea.",
                    "Put the rod in its stand, keep the line a little tight, and wait."
                )
            )

            SectionTitle("How to spot a bite")
            BulletList(
                listOf(
                    "The rod tip moves, shakes, or bends down: a fish is biting.",
                    "A small shake can be a small fish. A strong pull is a bigger fish.",
                    "Lift the rod up firmly to set the hook, then reel in slowly and steadily.",
                    "Keep the line tight all the time, or the fish will get off the hook."
                )
            )

            SectionTitle("Spots in this area")
            SPOTS.forEach { spot ->
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(
                        Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = spot, style = MaterialTheme.typography.bodyMedium)
                }
            }

            SectionTitle("Safety")
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Sun: hat, water and sunscreen. Rocks: wear good shoes, rocks are slippery. Hooks: sharp, keep them away from your hands and eyes, use pliers. Ragworms can nip your fingers. Never turn your back to the waves.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            SectionTitle("Release small fish")
            Text(
                text = "If a fish is small, put it back in the water gently and quickly. It will grow and you can catch it again later. Check local rules for size limits.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
