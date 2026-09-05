package com.splitview.pad

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A saved two-pane workspace the user can relaunch in one tap. */
data class AppPair(
    val id: String,
    val title: String,
    val urlPane1: String,
    val urlPane2: String
)

class AppPairManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("app_pairs_prefs", Context.MODE_PRIVATE)

    fun getSavedPairs(): List<AppPair> {
        val raw = prefs.getString(KEY_PAIRS, null) ?: return defaultPairs().also { saveAll(it) }
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                AppPair(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    urlPane1 = obj.getString("urlPane1"),
                    urlPane2 = obj.getString("urlPane2")
                )
            }
        } catch (e: Exception) {
            defaultPairs()
        }
    }

    fun savePair(title: String, url1: String, url2: String): AppPair {
        val pair = AppPair(System.currentTimeMillis().toString(), title, url1, url2)
        saveAll(listOf(pair) + getSavedPairs())
        return pair
    }

    fun deletePair(id: String) = saveAll(getSavedPairs().filter { it.id != id })

    fun renamePair(id: String, newTitle: String) = saveAll(
        getSavedPairs().map { if (it.id == id) it.copy(title = newTitle) else it }
    )

    fun updatePair(id: String, newTitle: String, newUrl1: String, newUrl2: String) = saveAll(
        getSavedPairs().map {
            if (it.id == id) AppPair(id, newTitle, newUrl1, newUrl2) else it
        }
    )

    private fun saveAll(pairs: List<AppPair>) {
        val array = JSONArray()
        pairs.forEach { pair ->
            array.put(
                JSONObject()
                    .put("id", pair.id)
                    .put("title", pair.title)
                    .put("urlPane1", pair.urlPane1)
                    .put("urlPane2", pair.urlPane2)
            )
        }
        prefs.edit().putString(KEY_PAIRS, array.toString()).apply()
    }

    private fun defaultPairs() = listOf(
        AppPair("seed-1", "YouTube + Google", "https://m.youtube.com", "https://www.google.com"),
        AppPair("seed-2", "ChatGPT + Wikipedia", "https://chatgpt.com", "https://www.wikipedia.org"),
        AppPair("seed-3", "Docs + Translate", "https://docs.google.com", "https://translate.google.com")
    )

    private companion object {
        const val KEY_PAIRS = "pairs_list"
    }
}
