package com.splitview.pad

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * One row in the app picker. Entries either point at an installed package (which
 * we hand to the system multi-window launcher) or at a web address (which loads
 * straight into a pane).
 *
 * Icons are deliberately *not* loaded here. Reading every launcher icon costs
 * tens of milliseconds each, and on a tablet with 80 apps that was several
 * seconds of main-thread work before the picker could even appear. Rows now
 * resolve their own icon lazily, off the main thread, as they scroll into view.
 */
data class AppEntry(
    val label: String,
    val url: String,
    val packageName: String? = null
) {
    val isInstalledApp: Boolean get() = packageName != null
}

/**
 * Builds and caches the picker list. [warmUp] is called at startup so the list
 * is usually ready by the time the user opens the picker; [request] hands over
 * the cached copy immediately when it is, and otherwise calls back once the
 * background load finishes.
 */
object AppCatalog {

    private val io = Executors.newFixedThreadPool(2) { runnable ->
        Thread(runnable, "app-catalog").apply { isDaemon = true }
    }
    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var cached: List<AppEntry>? = null

    @Volatile
    private var loading = false

    private val iconCache = object : android.util.LruCache<String, Drawable>(96) {}

    private val WEB_APPS = listOf(
        "Google" to "https://www.google.com",
        "YouTube" to "https://m.youtube.com",
        "ChatGPT" to "https://chatgpt.com",
        "Wikipedia" to "https://www.wikipedia.org",
        "Google Docs" to "https://docs.google.com",
        "Gmail" to "https://mail.google.com",
        "Google Maps" to "https://maps.google.com",
        "Google Translate" to "https://translate.google.com",
        "Zalo Web" to "https://chat.zalo.me",
        "Messenger" to "https://www.messenger.com",
        "Telegram Web" to "https://web.telegram.org",
        "Facebook" to "https://m.facebook.com",
        "X" to "https://x.com",
        "Reddit" to "https://www.reddit.com",
        "TikTok" to "https://www.tiktok.com",
        "Shopee" to "https://shopee.vn",
        "Google News" to "https://news.google.com",
        "GitHub" to "https://github.com"
    )

    /** The curated web apps, available with no I/O at all. */
    val webApps: List<AppEntry> = WEB_APPS.map { (label, url) -> AppEntry(label, url) }

    /** Kick off the scan early so the picker opens against a warm cache. */
    fun warmUp(context: Context) {
        if (cached != null || loading) return
        loading = true
        val app = context.applicationContext
        io.execute {
            val entries = webApps + installedApps(app)
            cached = entries
            loading = false
        }
    }

    /**
     * Calls [onReady] on the main thread. If the scan has already finished this
     * happens synchronously, so opening the picker costs nothing extra.
     */
    fun request(context: Context, onReady: (List<AppEntry>) -> Unit) {
        cached?.let {
            onReady(it)
            return
        }
        val app = context.applicationContext
        io.execute {
            val entries = cached ?: (webApps + installedApps(app)).also { cached = it }
            main.post { onReady(entries) }
        }
    }

    /** Resolves one launcher icon off the main thread, then posts it back. */
    fun loadIcon(context: Context, packageName: String, onLoaded: (Drawable?) -> Unit) {
        iconCache.get(packageName)?.let {
            onLoaded(it)
            return
        }
        val app = context.applicationContext
        io.execute {
            val icon = runCatching {
                app.packageManager.getApplicationIcon(packageName)
            }.getOrNull()
            if (icon != null) iconCache.put(packageName, icon)
            main.post { onLoaded(icon) }
        }
    }

    private fun installedApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            queryLauncherActivities(pm, launcherIntent)
                .asSequence()
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    val pkg = info.activityInfo.packageName
                    AppEntry(
                        label = info.loadLabel(pm).toString(),
                        url = webEquivalent(pkg),
                        packageName = pkg
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun queryLauncherActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

    /**
     * If an installed app cannot be launched into the system split view we fall
     * back to its website, so the user still ends up with usable panes.
     */
    fun webEquivalent(packageName: String): String {
        val p = packageName.lowercase()
        return when {
            p.contains("zalo") -> "https://chat.zalo.me"
            p.contains("youtube") -> "https://m.youtube.com"
            p.contains("orca") || p.contains("messenger") -> "https://www.messenger.com"
            p.contains("katana") || p.contains("facebook") -> "https://m.facebook.com"
            p.contains("instagram") -> "https://www.instagram.com"
            p.contains("telegram") -> "https://web.telegram.org"
            p.contains("whatsapp") -> "https://web.whatsapp.com"
            p.contains("twitter") || p == "com.x.android" -> "https://x.com"
            p.contains("reddit") -> "https://www.reddit.com"
            p.contains("tiktok") || p.contains("trill") -> "https://www.tiktok.com"
            p.contains("shopee") -> "https://shopee.vn"
            p.contains("lazada") -> "https://www.lazada.vn"
            p.contains("netflix") -> "https://www.netflix.com"
            p.contains("spotify") -> "https://open.spotify.com"
            p.contains("twitch") -> "https://m.twitch.tv"
            p.contains("openai") || p.contains("chatgpt") -> "https://chatgpt.com"
            p.contains("wikipedia") -> "https://www.wikipedia.org"
            p.contains("maps") -> "https://maps.google.com"
            p.contains("gmail") -> "https://mail.google.com"
            p.contains("docs.editors") -> "https://docs.google.com"
            p.contains("github") -> "https://github.com"
            p.contains("chrome") || p.contains("browser") -> "https://www.google.com"
            else -> "https://www.google.com/search?q=" +
                java.net.URLEncoder.encode(packageName.substringAfterLast('.'), "UTF-8")
        }
    }
}
