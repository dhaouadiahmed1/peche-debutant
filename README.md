# Pêche Débutant v0.1

A beginner fishing app for shore fishing at Ras Jebel, Bizerte, Ghar el Melh,
Cap Zebib and Sidi Ali El Mekki, North Tunisia (Mediterranean).

**Fully offline. No internet, no login, no ads, no permissions.**

## What is in v0.1

4 tabs, bottom navigation:

- **Fish (Poissons)** — 12 fish of the area, searchable. Each fish has its
  French, English and Tunisian name, scientific name, habitat, season,
  difficulty, common size, best baits, hook size, best rig and beginner tips.
  Tapping a "Best bait" chip jumps to that bait.
  Fish: Dorade royale, Loup, Sar, Marbré, Pageot, Mulet, Rouget, Oblade,
  Saupe, Maquereau, Orphie, Sole.
- **Baits (Appâts)** — 8 baits, each with what fish it catches best, how to
  keep it fresh, and how to hook it step by step (numbered steps).
  Baits: Ver de mer (ragworm), Bibi, Crevette, Sardine, Crabe, Moule,
  Calamar / Seiche, Pâte à pain.
- **Rigs (Montages)** — 5 rigs, each with when to use it, target fish, parts
  list with real sizes, step-by-step assembly, and a drawing made in Compose
  (lines, circles and boxes, no photos).
  Rigs: Montage Coulissant Simple, Paternoster 2 hameçons, Paternoster
  3 hameçons léger, Montage Loup, Montage Dorade au crabe / moule.
- **Guide (Débutant)** — gear checklist (4.20 m surfcasting rod, reel
  6000-8000, nylon 35/100, fluorocarbon 25-30/100, hooks, leads 80-120 g,
  bait elastic, pliers, bucket), best times, casting basics, how to spot a
  bite, safety, spots in the area (names only), and release small fish.

## Notes on the content

- Shore rod fishing in Tunisia needs no permit (order of 21 April 2025).
  This is a general note: check local rules before you fish.
- No prices, no GPS coordinates and no legal minimum sizes are in the app.
  Where a size or a rule is not known, the app says "check local rules" and
  "release small fish".
- Seasons are general windows from the brief (loup September to April,
  dorade spring to autumn). They are not precise local data.
- All content lives in one file:
  `android/app/src/main/java/app/pechedebutant/data/FishingData.kt`

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
- No network code at all: the app works with no internet.
