// Pêche Débutant v0.5 — Home-screen widget.
// Standard Android AppWidget (AppWidgetProvider + RemoteViews).
// Shows today's Check blassa score (from the saved-today cache; if
// none, "Ouvrir l'app" — never an invented score) + today's solunar
// best hours for Ras Jebel (local moon math, Solunar.kt).
// Tapping it opens the app. Updates via WorkManager periodic work
// (every 6 h) and on every onUpdate call. No new permission.
package app.pechedebutant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.pechedebutant.MainActivity
import app.pechedebutant.R
import app.pechedebutant.data.Solunar
import app.pechedebutant.ui.FALLBACK_LAT
import app.pechedebutant.ui.FALLBACK_LON
import app.pechedebutant.ui.loadMeteoCache
import app.pechedebutant.ui.spotScore
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class FishingWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAllWidgets(context)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "fishing-widget-update",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(6, TimeUnit.HOURS).build()
        )
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        updateAllWidgets(applicationContext)
        return Result.success()
    }
}

fun updateAllWidgets(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ids = manager.getAppWidgetIds(
        ComponentName(context, FishingWidgetProvider::class.java)
    )
    ids.forEach { id ->
        manager.updateAppWidget(id, buildWidgetViews(context))
    }
}

private fun buildWidgetViews(context: Context): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_check_blassa)
    val cached = loadMeteoCache(context)
    if (cached != null) {
        val spot = spotScore(cached)
        views.setTextViewText(
            R.id.widget_score,
            "Check blassa : ${spot.verdict} ${spot.score}/10"
        )
    } else {
        views.setTextViewText(
            R.id.widget_score,
            "Ouvrir l'app pour le score du jour"
        )
    }
    val solunar = Solunar.solunarFor(LocalDate.now(), FALLBACK_LAT, FALLBACK_LON)
    val majors = solunar.periods.filter { it.kind == "Majeure" }
    views.setTextViewText(
        R.id.widget_solunar,
        if (majors.isNotEmpty()) {
            "Meilleures heures : " + majors.joinToString(", ") { Solunar.formatPeriod(it) }
        } else {
            "Meilleures heures : —"
        }
    )
    views.setTextViewText(
        R.id.widget_moon,
        "Ras Jebel · ${solunar.moon.phaseName}, ${solunar.moon.illuminationPct}%"
    )
    val intent = Intent(context, MainActivity::class.java)
    val pending = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_root, pending)
    return views
}
