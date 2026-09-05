package com.splitview.pad

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class AppPair(
    val id: String,
    val title: String,
    val urlPane1: String,
    val urlPane2: String
)

class AppPairManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_pairs_prefs", Context.MODE_PRIVATE)

    fun getSavedPairs(): List<AppPair> {
        val jsonString = prefs.getString("pairs_list", null) ?: return getDefaultPairs()
        val list = mutableListOf<AppPair>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AppPair(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        urlPane1 = obj.getString("urlPane1"),
                        urlPane2 = obj.getString("urlPane2")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return getDefaultPairs()
        }
        return list
    }

    fun savePair(title: String, url1: String, url2: String) {
        val current = getSavedPairs().toMutableList()
        val newPair = AppPair(
            id = System.currentTimeMillis().toString(),
            title = title,
            urlPane1 = url1,
            urlPane2 = url2
        )
        current.add(0, newPair)

        val jsonArray = JSONArray()
        for (pair in current) {
            val obj = JSONObject()
            obj.put("id", pair.id)
            obj.put("title", pair.title)
            obj.put("urlPane1", pair.urlPane1)
            obj.put("urlPane2", pair.urlPane2)
            jsonArray.put(obj)
        }
        prefs.edit().putString("pairs_list", jsonArray.toString()).apply()
    }

    fun deletePair(id: String) {
        val current = getSavedPairs().filter { it.id != id }
        saveAllPairs(current)
    }

    fun updatePair(id: String, newTitle: String, newUrl1: String, newUrl2: String) {
        val current = getSavedPairs().map {
            if (it.id == id) AppPair(id, newTitle, newUrl1, newUrl2) else it
        }
        saveAllPairs(current)
    }

    private fun saveAllPairs(list: List<AppPair>) {
        val jsonArray = JSONArray()
        for (pair in list) {
            val obj = JSONObject()
            obj.put("id", pair.id)
            obj.put("title", pair.title)
            obj.put("urlPane1", pair.urlPane1)
            obj.put("urlPane2", pair.urlPane2)
            jsonArray.put(obj)
        }
        prefs.edit().putString("pairs_list", jsonArray.toString()).apply()
    }

    private fun getDefaultPairs(): List<AppPair> {
        return listOf(
            AppPair("1", "YouTube + Google", "https://m.youtube.com", "https://www.google.com"),
            AppPair("2", "ChatGPT + Wikipedia", "https://chatgpt.com", "https://www.wikipedia.org"),
            AppPair("3", "Reddit + News", "https://www.reddit.com", "https://news.google.com")
        )
    }
}
