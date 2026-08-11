package com.example.odysseyglyph

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LrcTrack(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val syncedLyrics: String?
)

object LRCLibClient {
    private const val BASE_URL = "https://lrclib.net/api"

    fun searchLyrics(query: String): List<LrcTrack> {
        val results = mutableListOf<LrcTrack>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$BASE_URL/search?q=$encodedQuery")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "OdysseyGlyphApp/1.0 (https://github.com/example/OdysseyGlyph)")
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                val jsonArray = JSONArray(response)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val synced = if (obj.isNull("syncedLyrics")) null else obj.getString("syncedLyrics")
                    
                    results.add(LrcTrack(
                        id = obj.optLong("id"),
                        trackName = obj.optString("trackName"),
                        artistName = obj.optString("artistName"),
                        albumName = obj.optString("albumName"),
                        syncedLyrics = synced
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }
}
