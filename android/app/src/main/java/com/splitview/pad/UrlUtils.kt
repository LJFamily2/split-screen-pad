package com.splitview.pad

/** Shared address-bar behaviour: type a domain, get the site; type words, get a search. */
object UrlUtils {

    private val LOOKS_LIKE_DOMAIN = Regex("^[\\w-]+(\\.[\\w-]+)+(/.*)?$")

    fun normalize(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Prefs.DEFAULT_URL_2
        if (trimmed.startsWith("http://", true) ||
            trimmed.startsWith("https://", true) ||
            trimmed.startsWith("file://", true) ||
            trimmed.startsWith("about:", true)
        ) {
            return trimmed
        }
        if (trimmed.startsWith("localhost", true) || LOOKS_LIKE_DOMAIN.matches(trimmed)) {
            return "https://$trimmed"
        }
        return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(trimmed, "UTF-8")
    }

    /** Short label for an address bar: "youtube.com/watch" rather than the full URL. */
    fun prettify(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
        .trimEnd('/')

    fun host(url: String): String = try {
        java.net.URI(url).host?.removePrefix("www.") ?: prettify(url)
    } catch (e: Exception) {
        prettify(url)
    }
}
