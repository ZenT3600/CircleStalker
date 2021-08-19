package it.matteoleggio.osustalker.Logic

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings.Global.getString
import it.matteoleggio.osustalker.R
import okhttp3.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread


const val baseEndpoint = "osu.ppy.sh"


class Api constructor (private val key: String) {

    fun keyIsValid(): Boolean {
        val response = get(
            baseEndpoint,
            listOf("api", "get_user"),
            hashMapOf("u" to "13567016", "m" to "0")
        )
        println(response.code)
        return response.code == 200
    }

    fun getUser(userString: String, mode: Int): String? {
        println(userString)
        println(mode)
        val response = get(
            baseEndpoint,
            listOf("api", "get_user"),
            hashMapOf("u" to userString, "m" to mode.toString())
        )
        println(response.code)
        return response.body?.string()
    }

    fun getScore(beatmapId: String, userId: String, mode: Int): String? {
        println(beatmapId)
        println(userId)
        val response = get(
            baseEndpoint,
            listOf("api", "get_scores"),
            hashMapOf("b" to beatmapId, "u" to userId, "m" to mode.toString(), "limit" to "100")
        )
        println(response.code)
        return response.body?.string()
    }

    fun getBeatmap(beatmapId: String, mode: Int): String? {
        println(beatmapId)
        val response = get(
            baseEndpoint,
            listOf("api", "get_beatmaps"),
            hashMapOf("b" to beatmapId, "m" to mode.toString())
        )
        println(response.code)
        return response.body?.string()
    }
    
    fun getUserRecentPlays(username: String, mode: Int, limit: Int): String? {
        val response = get(
            baseEndpoint,
            listOf("api", "get_user_recent"),
            hashMapOf("u" to username, "m" to mode.toString(), "limit" to limit.toString())
        )
        println(response.code)
        return response.body?.string()
    }

    private fun get(urlString: String, pathSegments: List<String>, params: HashMap<String, String>): Response {
        val client = OkHttpClient()

        val httpUrlBuilder: HttpUrl.Builder = HttpUrl.Builder()
            .scheme("https")
            .host(urlString)
        for (segment in pathSegments) {
            httpUrlBuilder.addPathSegment(segment)
        }
        httpUrlBuilder.addQueryParameter("k", this.key)
        for ((key, value) in params) {
            httpUrlBuilder.addQueryParameter(key, value)
        }
        val httpUrl = httpUrlBuilder.build()

        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .build()

        val queue = LinkedBlockingQueue<Response>()
        thread( start = true) {
            val response = client.newCall(request).execute()
            queue.add(response)
        }
        return queue.take()
    }
}