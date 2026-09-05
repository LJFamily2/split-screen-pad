package com.splitview.pad

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * One row in the app picker. Entries either point at an installed package (which
 * we hand to the system multi-window launcher) or at a web address (which loads
 * straight into a pane).
 */
data class AppEntry(
    val label: String,
    val url: String,
    val packageName: String? = null,
    val icon: Drawable? = null
) {
    val isInstalledApp: Boolean get() = packageName != null
}

/** Builds the picker list: curated web apps first, then everything installed. */
object AppCatalog {

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

    fun build(context: Context): List<AppEntry> {
        val entries = mutableListOf<AppEntry>()
        WEB_APPS.mapTo(entries) { (label, url) -> AppEntry(label, url) }
        entries += installedApps(context)
        return entries
    }

    fun installedApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        return try {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(launcherIntent, 0)
                .asSequence()
                .filter { it.activityInfo.packageName != context.packageName }
                .map { info ->
                    val pkg = info.activityInfo.packageName
                    AppEntry(
                        label = info.loadLabel(pm).toString(),
                        url = webEquivalent(pkg),
                        packageName = pkg,
                        icon = runCatching { info.loadIcon(pm) }.getOrNull()
                    )
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * If an installed app cannot be launched into the system split view we fall
     * back to its website, so the user still ends up with two usable panes.
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
            p.contains("gm") && p.contains("gmail") -> "https://mail.google.com"
            p.contains("docs.editors") -> "https://docs.google.com"
            p.contains("github") -> "https://github.com"
            p.contains("chrome") || p.contains("browser") -> "https://www.google.com"
            else -> "https://www.google.com/search?q=" +
                java.net.URLEncoder.encode(packageName.substringAfterLast('.'), "UTF-8")
        }
    }
}
