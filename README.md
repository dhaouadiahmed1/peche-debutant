# Pêche Débutant v0.3

A beginner fishing app for shore fishing at Ras Jebel, Bizerte, Ghar el Melh,
Cap Zebib and Sidi Ali El Mekki, North Tunisia (Mediterranean).

**Fish, Baits, Rigs and Guide tabs: fully offline, no login, no ads.**
The Météo & Marée tab and the Accueil hero need internet (see below).

## What is new in v0.3

- **Accueil (new first tab, the default screen)** — a luxury dashboard,
  dark navy (`#04121E` / `#0A2E44`) and gold (`#D4AF37`), big 3D-style
  cards with shadows, rounded 28 dp corners and a subtle press-scale
  animation:
  - **Stories row** on top (gold rings, like Instagram / Facebook):
    Dorade, Loup, Météo, Bibi, Montage, Guide. Tapping a story opens
    that fish, bait, rig list, guide or the Météo tab.
  - **Check blassa hero**: a gold-ring score circle with the live score
    out of 10 (same Open-Meteo data and same scoring rules as the Météo
    tab), big weather numbers (air, wind, waves, sea temperature), and
    big cards for Poissons, Appâts, Montages and Météo & Marée.
    The dashboard fetches for Ras Jebel only. If there is no connection
    it shows the score saved earlier today, or a prompt to open the
    Météo tab. It never invents weather numbers.
  - **Feed du jour** below (scrollable, like Instagram / Facebook):
    Check blassa today, Poisson du jour, Appât du jour and Météo demain.
    The fish and bait of the day rotate through the real lists by
    day-of-year, so the feed changes every day. Each post shows the
    real bundled photo where one exists.
  - **Likes**: a heart button on each feed post, saved on the phone
    only (SharedPreferences). No account, no server.
  - **Partager (WhatsApp share, no account)**: a Partager button on
    each feed post, on the Check blassa hero, and on every fish and
    bait detail screen. It opens the Android share chooser
    (`ACTION_SEND`, `text/plain`) with a French text built from the
    real content, and the user picks WhatsApp himself. No WhatsApp
    package is hardcoded, no WhatsApp API is used, nothing is sent
    automatically. No account, no server, no Firebase, no login.

## What is in v0.2

6 tabs, bottom navigation:

- **Fish (Poissons)** — 12 fish of the area, searchable. Each fish has its
  French, English and Tunisian name, scientific name, habitat, season,
  difficulty, common size, best baits, hook size, best rig and beginner tips.
  Tapping a "Best bait" chip jumps to that bait. **New in v0.2: a real photo
  at the top of every fish card and detail, saved inside the app.**
  Fish: Dorade royale, Loup, Sar, Marbré, Pageot, Mulet, Rouget, Oblade,
  Saupe, Maquereau, Orphie, Sole.
- **Baits (Appâts)** — 8 baits, each with what fish it catches best, how to
  keep it fresh, and how to hook it step by step (numbered steps).
  **New in v0.2: a real photo at the top of every bait card and detail,
  saved inside the app.**
  Baits: Ver de mer (ragworm), Bibi, Crevette, Sardine, Crabe, Moule,
  Calamar / Seiche, Pâte à pain.
- **Rigs (Montages)** — 5 rigs, each with when to use it, target fish, parts
  list with real sizes, step-by-step assembly, and a drawing made in Compose
  (lines, circles and boxes, no photos). Unchanged from v0.1.
  Rigs: Montage Coulissant Simple, Paternoster 2 hameçons, Paternoster
  3 hameçons léger, Montage Loup, Montage Dorade au crabe / moule.
- **Guide (Débutant)** — gear checklist (4.20 m surfcasting rod, reel
  6000-8000, nylon 35/100, fluorocarbon 25-30/100, hooks, leads 80-120 g,
  bait elastic, pliers, bucket), best times, casting basics, how to spot a
  bite, safety, spots in the area (names only), and release small fish.
  Unchanged from v0.1.
- **Météo & Marée (new in v0.2)** — live weather and sea for your position:
  temperature, wind speed and direction, gusts, humidity, rain, wave height
  and period, sea surface temperature, sunrise and sunset, 3-day forecast,
  and today's high and low tide times. Plus a "Check blassa" score out of
  10 (BAHYA / MTEWASTA / MOUCH BAHYA) with the reasons listed, the best
  fish to target now, and the best bait now.
  - Position: the phone GPS (Android LocationManager, no Google Play
    Services). If there is no permission or no GPS fix, the app uses
    Ras Jebel (37.2476, 10.0697) and says so on screen.
  - Data: Open-Meteo, free, no API key, no login.
    Weather data by Open-Meteo.com.
  - Tide times are the local maxima and minima of the hourly sea level
    (sea_level_height_msl). In the Mediterranean here the tide is small
    (about 0.2 to 0.3 m between high and low), so the app says so: wind
    and waves matter more than the tide.
  - **This tab needs internet.** Without a connection it shows
    "Pas de connexion — vérifie internet et réessaie" with a Réessayer
    button. All other tabs still work fully offline.

## Photos

All 20 photos are real photographs, checked one by one. The Mulet photo
replaces a black-and-white drawing, and the Bibi photo is a real lugworm
held in a hand on the beach.

Photos are saved inside the app so it works offline at the beach. Because
the GitHub snapshot push only accepts text files, the photos are **not**
stored as binary files in the repo: each photo is resized (max 800 px wide,
JPEG quality 78) and embedded as a Base64 string in
`android/app/src/main/java/app/pechedebutant/data/PhotoData.kt`, decoded at
runtime. The same resized JPEGs are also in the `photos/` folder of the
deliverable zip for reference. Total: 20 photos, about 1.15 MB as JPEG,
about 1.54 MB as Base64.

Photo sources and credits:

- Most fish and baits: Wikimedia Commons (free licence).
- Mulet: ncfishes.com (Mugil cephalus photo).
- Sole: NW-IFCA (Solea solea photo).
- Bibi: aixcessive.de (lugworm in a hand on the beach).
- Saupe: Wikimedia Commons, original file (the thumbnail link was broken).

## Permissions (new in v0.2)

v0.1 had zero permissions. v0.2 adds three, only for the Météo & Marée tab:

- `INTERNET` — to fetch Open-Meteo weather and marine data.
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` — to use the phone
  GPS for the weather at your position. The app asks at runtime and works
  with the Ras Jebel fallback if you refuse.

## Notes on the content

- Shore rod fishing in Tunisia needs no permit (order of 21 April 2025).
  This is a general note: check local rules before you fish.
- No prices, no GPS coordinates (except the Ras Jebel fallback position
  shown on the Météo tab, 37.2476, 10.0697, which is the spot in the app)
  and no legal minimum sizes are in the app. Where a size or a rule is not
  known, the app says "check local rules" and "release small fish".
- Seasons are general windows from the brief (loup September to April,
  dorade spring to autumn). They are not precise local data.
- All fish, bait and rig content lives in one file:
  `android/app/src/main/java/app/pechedebutant/data/FishingData.kt`
- Météo code lives in
  `android/app/src/main/java/app/pechedebutant/ui/MeteoScreen.kt`

## How to build (GitHub Actions)

1. Create a GitHub repo and push this whole project to the `main` branch.
2. **Known issue, same as GoldApp and OpenLive:** the GitHub App cannot
   create workflow files through the API (403 "Resource not accessible by
   integration"). So after the push, add the file
   `.github/workflows/android.yml` yourself on the GitHub website
   (Add file → Create new file, same path, paste the content from this
   project). The file is included here in the zip for that purpose.
3. On every push to `main`, the workflow builds the debug APK with
   JDK 17 and Gradle 8.7 (`gradle assembleDebug` in `android/`).
4. Download the APK from the workflow run, under Artifacts:
   `peche-debutant-debug-apk`. Install it on the phone and test it.

## How to build (local, optional)

- Android Studio, JDK 17, Android SDK 34.
- Open the `android/` folder, wait for the Gradle sync, then
  Run → Run 'app', or `./gradlew assembleDebug` if you have a wrapper.

## Tech

- Kotlin + Jetpack Compose, Material 3, Navigation Compose.
- Package: `app.pechedebutant`. Display name: "Pêche Débutant".
- minSdk 26, targetSdk 34, compileSdk 34, Gradle 8.7, AGP 8.5.2.
- Theme: sea blue `#0B5A7A` and sand, light theme following the system.
- Network: only the Météo tab, with `HttpURLConnection` and `org.json`
  (built into Android). No OkHttp, no Retrofit, no other dependency added.
- Version: versionCode 3, versionName "0.3".
