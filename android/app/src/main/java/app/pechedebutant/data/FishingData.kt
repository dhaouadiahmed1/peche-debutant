// Pêche Débutant v0.1 — all content lives here, offline, no network.
// Sources: AHMED's own bait (ragworm) and the brief for Ras Jebel / Bizerte.
// No prices, no GPS coordinates, no legal minimum sizes are stated anywhere:
// where a size or a rule is not known, the app says "check local rules" and
// "release small fish". Seasons below are the general windows from the brief
// (loup Sept-Apr, dorade spring-autumn); they are not precise local data.
package app.pechedebutant.data

data class Fish(
    val id: String,
    val frenchName: String,
    val englishName: String,
    val tunisianName: String,
    val scientificName: String,
    val habitat: String,
    val season: String,
    val difficulty: String,
    val commonSize: String,
    val bestBaitIds: List<String>,
    val hookSize: String,
    val bestRig: String,
    val tips: List<String>
)

data class Bait(
    val id: String,
    val frenchName: String,
    val englishName: String,
    val bestFor: String,
    val keepFresh: String,
    val hookingSteps: List<String>
)

enum class RigDiagram {
    SLIDING, PATERNOSTER_2, PATERNOSTER_3, BASS, DORADE
}

data class Rig(
    val id: String,
    val frenchName: String,
    val englishName: String,
    val whenToUse: String,
    val targetFish: String,
    val parts: List<String>,
    val steps: List<String>,
    val diagram: RigDiagram
)

val FISH_LIST: List<Fish> = listOf(
    Fish(
        id = "dorade",
        frenchName = "Dorade royale",
        englishName = "Gilthead sea bream",
        tunisianName = "Wrata",
        scientificName = "Sparus aurata",
        habitat = "Sand and mixed ground",
        season = "Spring to autumn",
        difficulty = "Medium",
        commonSize = "Medium to large",
        bestBaitIds = listOf("ragworm", "crab", "mussel", "shrimp"),
        hookSize = "2 to 1/0",
        bestRig = "Montage Coulissant Simple (Sliding rig)",
        tips = listOf(
            "Fish on a calm sea, dorade is shy and feels the lead.",
            "Use a long fluorocarbon leader so the fish does not see the line."
        )
    ),
    Fish(
        id = "loup",
        frenchName = "Loup",
        englishName = "European sea bass",
        tunisianName = "Karouss",
        scientificName = "Dicentrarchus labrax",
        habitat = "Sand, rocks and surf, near the shore",
        season = "September to April",
        difficulty = "Hard",
        commonSize = "Medium to large",
        bestBaitIds = listOf("shrimp", "sardine", "ragworm"),
        hookSize = "1/0 to 2/0",
        bestRig = "Montage Loup (Sea bass rig)",
        tips = listOf(
            "Best in September to April, on windy days, at dawn and dusk.",
            "Keep the bait moving naturally, a live shrimp is the best bait."
        )
    ),
    Fish(
        id = "sar",
        frenchName = "Sar",
        englishName = "White seabream",
        tunisianName = "Sar",
        scientificName = "Diplodus sargus",
        habitat = "Rocks",
        season = "Spring to autumn",
        difficulty = "Easy",
        commonSize = "Small to medium",
        bestBaitIds = listOf("ragworm", "mussel", "shrimp"),
        hookSize = "6 to 4",
        bestRig = "Montage Paternoster 2 hameçons (Paternoster 2 hooks)",
        tips = listOf(
            "Fish close to the rocks, sar lives in the rocks.",
            "Small baits work best, do not use a hook that is too big."
        )
    ),
    Fish(
        id = "marbre",
        frenchName = "Marbré",
        englishName = "Striped seabream",
        tunisianName = "Marbré",
        scientificName = "Lithognathus mormyrus",
        habitat = "Sand",
        season = "Spring to autumn, best at night",
        difficulty = "Easy",
        commonSize = "Small to medium",
        bestBaitIds = listOf("ragworm", "shrimp"),
        hookSize = "6 to 4",
        bestRig = "Montage Coulissant Simple (Sliding rig) or Paternoster",
        tips = listOf(
            "Fish on sand, best at night.",
            "Ragworm is the first bait to try for marbré."
        )
    ),
    Fish(
        id = "pageot",
        frenchName = "Pageot",
        englishName = "Common pandora",
        tunisianName = "Pageot",
        scientificName = "Pagellus erythrinus",
        habitat = "Sand and mixed ground",
        season = "Spring to autumn",
        difficulty = "Medium",
        commonSize = "Small to medium",
        bestBaitIds = listOf("shrimp", "ragworm", "squid"),
        hookSize = "4 to 2",
        bestRig = "Montage Paternoster 2 hameçons (Paternoster 2 hooks)",
        tips = listOf(
            "A small strip of squid on the hook works well.",
            "Fish a little further out than for sar."
        )
    ),
    Fish(
        id = "mulet",
        frenchName = "Mulet",
        englishName = "Mullet",
        tunisianName = "Bouri",
        scientificName = "Mugil cephalus",
        habitat = "Lagoon and estuary, also sandy shore",
        season = "All year, best in spring and autumn",
        difficulty = "Medium",
        commonSize = "Medium",
        bestBaitIds = listOf("bread", "mussel", "ragworm"),
        hookSize = "8 to 6",
        bestRig = "Montage Paternoster 3 hameçons léger (Light 3-hook paternoster)",
        tips = listOf(
            "Mulet is a classic fish of the Ghar el Melh lagoon.",
            "Other lagoon fish: dorade, loup, sole and eel.",
            "Fish with a small hook and a light rig, mulet is shy."
        )
    ),
    Fish(
        id = "rouget",
        frenchName = "Rouget",
        englishName = "Red mullet",
        tunisianName = "Rouget",
        scientificName = "Mullus surmuletus",
        habitat = "Sand",
        season = "Spring to autumn",
        difficulty = "Easy",
        commonSize = "Small",
        bestBaitIds = listOf("ragworm", "shrimp"),
        hookSize = "8 to 6",
        bestRig = "Montage léger (Light rig)",
        tips = listOf(
            "Use a small piece of worm or shrimp.",
            "Fish on sand, in the morning."
        )
    ),
    Fish(
        id = "oblade",
        frenchName = "Oblade",
        englishName = "Saddled seabream",
        tunisianName = "Oblade",
        scientificName = "Oblada melanura",
        habitat = "Rocks",
        season = "Spring to autumn",
        difficulty = "Easy",
        commonSize = "Small to medium",
        bestBaitIds = listOf("shrimp", "ragworm", "bread"),
        hookSize = "8 to 6",
        bestRig = "Montage léger (Light rig)",
        tips = listOf(
            "Fish close to the rocks.",
            "Oblade often comes in a group, so fish again in the same place."
        )
    ),
    Fish(
        id = "saupe",
        frenchName = "Saupe",
        englishName = "Salema",
        tunisianName = "Saupe",
        scientificName = "Sarpa salpa",
        habitat = "Rocks and seaweed",
        season = "Spring to autumn",
        difficulty = "Easy",
        commonSize = "Small to medium",
        bestBaitIds = listOf("bread", "ragworm"),
        hookSize = "8 to 6",
        bestRig = "Montage léger (Light rig)",
        tips = listOf(
            "Bread dough or a small piece of seaweed can work.",
            "Saupe eats plants too, so do not only use worm."
        )
    ),
    Fish(
        id = "maquereau",
        frenchName = "Maquereau",
        englishName = "Mackerel",
        tunisianName = "Maquereau",
        scientificName = "Scomber",
        habitat = "Open water, near the surface",
        season = "Spring to autumn",
        difficulty = "Easy",
        commonSize = "Small to medium",
        bestBaitIds = listOf("sardine", "shrimp"),
        hookSize = "6 to 4",
        bestRig = "Montage Paternoster 2 hameçons (Paternoster 2 hooks)",
        tips = listOf(
            "Use a small strip of sardine.",
            "Maquereau moves in a group, fish fast when they are there."
        )
    ),
    Fish(
        id = "orphie",
        frenchName = "Orphie",
        englishName = "Garfish",
        tunisianName = "Orphie",
        scientificName = "Belone belone",
        habitat = "Surface, near the shore",
        season = "Spring to autumn, best at dawn",
        difficulty = "Medium",
        commonSize = "Medium, long and thin",
        bestBaitIds = listOf("sardine", "shrimp"),
        hookSize = "6 to 4",
        bestRig = "Montage Paternoster 2 hameçons (Paternoster 2 hooks)",
        tips = listOf(
            "Fish at the surface, at dawn.",
            "Orphie has a long thin mouth, strike gently."
        )
    ),
    Fish(
        id = "sole",
        frenchName = "Sole",
        englishName = "Sole",
        tunisianName = "Sole",
        scientificName = "Solea solea",
        habitat = "Sand, flat on the bottom",
        season = "Autumn to spring, best at night",
        difficulty = "Medium",
        commonSize = "Small to medium",
        bestBaitIds = listOf("ragworm", "mussel"),
        hookSize = "6 to 4",
        bestRig = "Montage Coulissant Simple (Sliding rig) on sand",
        tips = listOf(
            "Fish on sand at night, the bait must lie on the bottom.",
            "Sole bites gently, watch the rod tip closely."
        )
    )
)

val BAIT_LIST: List<Bait> = listOf(
    Bait(
        id = "ragworm",
        frenchName = "Ver de mer",
        englishName = "Ragworm",
        bestFor = "Dorade, loup, sar, marbré, sole — almost all fish here",
        keepFresh = "Keep in a little seawater, in the shade. Never fresh water, never in the sun. This is AHMED's bait: a reddish segmented worm. Careful, it can nip your fingers.",
        hookingSteps = listOf(
            "Find the head: the darker end, with small jaws. Hold the worm just behind the head.",
            "Push the hook point into the head.",
            "Slide the worm up the hook, threading it along the shank.",
            "Push the hook out again in the middle of the worm, so it stays on.",
            "Leave the tail free, about 2 to 3 cm. It must wriggle in the water."
        )
    ),
    Bait(
        id = "bibi",
        frenchName = "Bibi",
        englishName = "White worm",
        bestFor = "Dorade — one of the best baits for big dorade",
        keepFresh = "Keep cool and damp, in the shade. Bibi is soft and smells strong, so keep it in its box until you use it.",
        hookingSteps = listOf(
            "Take one bibi, it is soft so hold it gently.",
            "Push the hook point into the head end.",
            "Thread it up the hook, a little at a time so it does not tear.",
            "Push the hook out again in the middle to hold it.",
            "Leave a short tail free to move."
        )
    ),
    Bait(
        id = "shrimp",
        frenchName = "Crevette",
        englishName = "Shrimp",
        bestFor = "Loup, dorade, sar, marbré, pageot",
        keepFresh = "Use fresh shrimp, or live shrimp in a bucket of seawater with a little air. Keep cool and in the shade.",
        hookingSteps = listOf(
            "For a live shrimp, hold it gently by the body.",
            "Push the hook point through the tail, just under the shell.",
            "Thread the hook a little way along the body.",
            "Push the hook out again near the middle so it stays on.",
            "Leave the head and legs free, a live shrimp must still move."
        )
    ),
    Bait(
        id = "sardine",
        frenchName = "Sardine",
        englishName = "Sardine",
        bestFor = "Loup, maquereau, orphie",
        keepFresh = "Use fresh sardine, keep it cool in the shade. Do not use sardine that smells bad or is soft.",
        hookingSteps = listOf(
            "Cut a strip of sardine, about 3 to 4 cm, or use a small whole sardine.",
            "Push the hook point through the back of the strip, near the top.",
            "Thread the strip up the hook.",
            "Push the hook out again in the middle of the strip.",
            "Leave the point of the hook just out, so it can catch the fish."
        )
    ),
    Bait(
        id = "crab",
        frenchName = "Crabe",
        englishName = "Crab",
        bestFor = "Big dorade — the best bait for a big wrata",
        keepFresh = "Keep small live crabs cool and damp, in the shade, in a box with wet seaweed.",
        hookingSteps = listOf(
            "Take a small crab and remove the legs and the claws.",
            "Hold the crab by the body, flat side up.",
            "Push the hook point through the body, from one side to the other.",
            "Push the hook out again so the crab sits flat on the hook.",
            "Tie it with bait elastic thread so it does not fall off when you cast."
        )
    ),
    Bait(
        id = "mussel",
        frenchName = "Moule",
        englishName = "Mussel",
        bestFor = "Sar, dorade, mulet",
        keepFresh = "Open the mussel just before fishing. Use the flesh fresh, keep it cool in the shade.",
        hookingSteps = listOf(
            "Open the mussel and take out the flesh.",
            "Put the flesh on the hook, folding it over the point.",
            "Push the hook through the flesh once more to hold it.",
            "Wrap bait elastic thread around the flesh and the hook.",
            "The thread holds the soft flesh on the hook when you cast."
        )
    ),
    Bait(
        id = "squid",
        frenchName = "Calamar / Seiche",
        englishName = "Squid / Cuttlefish strip",
        bestFor = "Loup, dorade, pageot — tough bait, good for casting far",
        keepFresh = "Keep fresh or frozen, cool and in the shade. Cut strips just before you fish.",
        hookingSteps = listOf(
            "Cut a strip of squid, about 3 to 4 cm long and 1 cm wide.",
            "Push the hook point through one end of the strip.",
            "Thread the strip up the hook.",
            "Push the hook out again in the middle of the strip.",
            "Leave the end of the strip free to move in the water."
        )
    ),
    Bait(
        id = "bread",
        frenchName = "Pâte à pain",
        englishName = "Bread dough",
        bestFor = "Mulet, saupe, oblade",
        keepFresh = "Make the dough with bread and a little seawater, just before fishing. Keep it in a closed box, in the shade.",
        hookingSteps = listOf(
            "Take a small piece of dough, about the size of a pea.",
            "Roll it into a small ball between your fingers.",
            "Push the hook point into the ball.",
            "Push the dough around the hook so the point is just covered.",
            "Squeeze gently so the ball stays on, but not so hard that it goes flat."
        )
    )
)

val RIG_LIST: List<Rig> = listOf(
    Rig(
        id = "sliding",
        frenchName = "Montage Coulissant Simple",
        englishName = "Sliding rig, 1 hook",
        whenToUse = "Calm sea. The best first rig for dorade, marbré and sole on sand.",
        targetFish = "Dorade, marbré, sole",
        parts = listOf(
            "Main line: nylon 35/100",
            "Sliding lead: 80 to 120 g, or a grip lead if there is current",
            "Bead and swivel",
            "Leader: fluorocarbon 25 to 30/100, 1 to 1.5 m long",
            "Hook: size 2 to 1/0 for dorade, 6 to 4 for marbré and sole"
        ),
        steps = listOf(
            "Thread the sliding lead onto the main line.",
            "Thread a bead onto the main line, after the lead.",
            "Tie a swivel to the end of the main line.",
            "Tie the fluorocarbon leader (1 to 1.5 m) to the other side of the swivel.",
            "Tie the hook to the end of the leader.",
            "Bait the hook. The lead slides freely, so the fish feels no lead when it bites."
        ),
        diagram = RigDiagram.SLIDING
    ),
    Rig(
        id = "paternoster2",
        frenchName = "Montage Paternoster 2 hameçons",
        englishName = "Paternoster, 2 hooks",
        whenToUse = "A little wave. The best all-round rig for a beginner.",
        targetFish = "Sar, pageot, maquereau, orphie",
        parts = listOf(
            "Main line: nylon 35/100",
            "Lead: 80 to 120 g at the bottom",
            "2 snoods (branch lines): fluorocarbon 25 to 30/100, about 30 cm each",
            "2 hooks: size 6 to 4 for sar, 4 to 2 for pageot",
            "2 swivels or knots to fix the snoods"
        ),
        steps = listOf(
            "Tie the lead to the end of the main line.",
            "Tie the first snood about 40 cm above the lead.",
            "Tie the second snood about 40 cm above the first one.",
            "Tie a hook to the end of each snood.",
            "Bait both hooks with different baits to see what the fish want.",
            "Cast out and keep the line a little tight."
        ),
        diagram = RigDiagram.PATERNOSTER_2
    ),
    Rig(
        id = "paternoster3",
        frenchName = "Montage Paternoster 3 hameçons léger",
        englishName = "Light paternoster, 3 hooks",
        whenToUse = "Calm sea, small baits, small fish. Good when many small fish are biting.",
        targetFish = "Marbré, sar, mulet, rouget",
        parts = listOf(
            "Main line: nylon 35/100",
            "Small lead: 60 to 80 g at the bottom",
            "3 snoods: fluorocarbon 25/100, about 25 cm each",
            "3 hooks: size 8 to 6",
            "Beads and swivels to fix the snoods"
        ),
        steps = listOf(
            "Tie the small lead to the end of the main line.",
            "Tie the first snood about 30 cm above the lead.",
            "Tie the second snood about 30 cm above the first one.",
            "Tie the third snood about 30 cm above the second one.",
            "Tie a small hook (8 to 6) to each snood.",
            "Bait with small pieces of worm or shrimp."
        ),
        diagram = RigDiagram.PATERNOSTER_3
    ),
    Rig(
        id = "bass",
        frenchName = "Montage Loup",
        englishName = "Sea bass rig",
        whenToUse = "Dawn and dusk, windy days, September to April. For loup.",
        targetFish = "Loup (sea bass)",
        parts = listOf(
            "Main line: nylon 35/100",
            "Sliding lead: 80 to 120 g",
            "Bead and swivel",
            "Leader: fluorocarbon 25 to 30/100, 1.5 to 2 m long",
            "Hook: strong, size 1/0 to 2/0"
        ),
        steps = listOf(
            "Thread the sliding lead onto the main line.",
            "Thread a bead, then tie a swivel to the end of the main line.",
            "Tie the long fluorocarbon leader (1.5 to 2 m) to the swivel.",
            "Tie the strong hook (1/0 to 2/0) to the end of the leader.",
            "Bait with a live shrimp or a strip of sardine.",
            "Cast out and wait. Loup bites at dawn and dusk, be patient."
        ),
        diagram = RigDiagram.BASS
    ),
    Rig(
        id = "dorade-crab",
        frenchName = "Montage Dorade au crabe / moule",
        englishName = "Dorade crab / mussel rig",
        whenToUse = "Calm sea, when you want a big dorade. Crab and mussel are the best baits for a big wrata.",
        targetFish = "Dorade (big fish)",
        parts = listOf(
            "Main line: nylon 35/100",
            "Sliding lead: 80 to 120 g",
            "Bead and swivel",
            "Leader: fluorocarbon 30/100, 1 to 1.5 m long",
            "Hook: strong, size 1/0",
            "Bait elastic thread to hold the crab or mussel"
        ),
        steps = listOf(
            "Thread the sliding lead onto the main line.",
            "Thread a bead, then tie a swivel to the end of the main line.",
            "Tie the fluorocarbon leader (1 to 1.5 m) to the swivel.",
            "Tie the strong hook (1/0) to the end of the leader.",
            "Hook the crab or the mussel flesh, then wrap bait elastic thread around it.",
            "Cast gently so the bait stays on, and wait for a strong bite."
        ),
        diagram = RigDiagram.DORADE
    )
)

fun fishById(id: String): Fish? = FISH_LIST.firstOrNull { it.id == id }
fun baitById(id: String): Bait? = BAIT_LIST.firstOrNull { it.id == id }
fun rigById(id: String): Rig? = RIG_LIST.firstOrNull { it.id == id }

// Spots in the area. Names only, no coordinates.
val SPOTS: List<String> = listOf(
    "Chatt Mami, Ras Jebel beach",
    "Ghar el Melh",
    "Cap Zebib",
    "Bizerte Corniche",
    "Sidi Ali El Mekki"
)

val GEAR_CHECKLIST: List<String> = listOf(
    "Rod: surfcasting rod, about 4.20 m",
    "Reel: size 6000 to 8000",
    "Main line: nylon 35/100",
    "Leader: fluorocarbon 25 to 30/100",
    "Hooks: sizes 8, 6, 4, 2, 1/0 and 2/0",
    "Leads: 80 to 120 g, sliding and grip leads",
    "Swivels and beads",
    "Bait elastic thread",
    "Pliers, to remove the hook safely",
    "Bucket with seawater, for the bait",
    "Knife and scissors",
    "Sun hat, water and sunscreen"
)
