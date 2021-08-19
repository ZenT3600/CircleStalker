package it.matteoleggio.osustalker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import it.matteoleggio.osustalker.Logic.Api
import it.matteoleggio.osustalker.Logic.parseMods
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.math.round


class SearchActivity : AppCompatActivity() {
    var shouldDie = false

    override fun onBackPressed() {
        this.shouldDie = true
        finish()
    }


    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    private fun saveLatestEvent(
        events: ArrayList<Map<String, Any>>,
        apiController: Api,
        username: String,
        apiCallTime: Float,
        mapsPlayed: HashMap<String, Int>,
        userJson: Map<String, Any>,
    ) {
        val sharedPrefs = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)

        if (events.size > 0) {
            val it = events[0]
            val scoreJsonRepr = apiController.getScore(
                it["beatmap_id"].toString(),
                userJson["user_id"].toString(),
                sharedPrefs.getInt(getString(R.string.mode_preference), -1)
            )
            TimeUnit.SECONDS.sleep(apiCallTime.toLong())
            val scoreJsonArray: ArrayList<Map<String, Any>> =
                Gson().fromJson(scoreJsonRepr, Any::class.java) as ArrayList<Map<String, Any>>
            var scoreJson: Map<String, Any> =
                scoreJsonArray[mapsPlayed["${it["beatmapset_id"]}#${it["beatmap_id"]}"]!!]
            val lastScoreID = scoreJson["date"].toString()
            with(sharedPrefs.edit()) {
                putString("user_$username", lastScoreID)
                commit()
            }
        }
    }

    private fun activateSubscribeButton(subscribeButton: Button, username: String) {
        val sharedPrefs = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)

        val usersToCheck = sharedPrefs.getString(getString(R.string.users_to_check_preference), "")
        subscribeButton.text = if (usersToCheck!!.contains(username)) "Already Subscribed" else "Subscribe"
        subscribeButton.setOnClickListener {
            if (subscribeButton.text == "Subscribe") {
                with(sharedPrefs.edit()) {
                    putString(
                        getString(R.string.users_to_check_preference),
                        "$usersToCheck, $username"
                    )
                    commit()
                }
                subscribeButton.text = "Already Subscribed"
            } else {
                with(sharedPrefs.edit()) {
                    putString(
                        getString(R.string.users_to_check_preference),
                        usersToCheck.replace(", $username", "")
                    )
                    commit()
                }
                subscribeButton.text = "Subscribe"
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val sharedPrefs = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val nRecent = sharedPrefs.getInt(getString(R.string.recent_events_preference), 0)
        val apiCallTime = sharedPrefs.getFloat(getString(R.string.api_call_time_preference), -1f)

        val apiController = Api(sharedPrefs.getString(getString(R.string.api_key_preference), "").toString())
        val username = intent.getStringExtra("userString")!!.toLowerCase()
        val showFailedScores: Boolean = sharedPrefs.getBoolean(getString(R.string.failed_score_preference), false)

        val loadingBar = findViewById(R.id.loading_bar) as ProgressBar

        val subscribeButton = findViewById<Button>(R.id.subscribe_button)
        val userid = findViewById<TextView>(R.id.userid)
        val usernameView = findViewById<TextView>(R.id.username)
        val country = findViewById<TextView>(R.id.country)
        val globalrank = findViewById<TextView>(R.id.global_rank)
        val localrank = findViewById<TextView>(R.id.local_rank)
        val level = findViewById<TextView>(R.id.level)
        val playcount = findViewById<TextView>(R.id.playcount)
        val accuracy = findViewById<TextView>(R.id.accuracy)
        val countssh = findViewById<TextView>(R.id.count_ssh)
        val countss = findViewById<TextView>(R.id.count_ss)
        val countsh = findViewById<TextView>(R.id.count_sh)
        val counts = findViewById<TextView>(R.id.count_s)
        val counta = findViewById<TextView>(R.id.count_a)
        val eventsGroup = findViewById<LinearLayout>(R.id.events)

        thread( start = true, isDaemon = true ) {
            var userJson: Map<String, Any>
            try {
                val userJsonRepr = apiController.getUser(
                    username,
                    sharedPrefs.getInt(getString(R.string.mode_preference), -1)
                )
                val userJsonArray: ArrayList<Map<String, Any>> =
                    Gson().fromJson(userJsonRepr, Any::class.java) as ArrayList<Map<String, Any>>
                userJson = userJsonArray[0]
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "User not found!", Toast.LENGTH_SHORT).show()
                }
                finish()
                return@thread
            }
            subscribeButton.text = "Wait..."

            val userJsonSafe = HashMap<String, Any>()
            for ((key, value) in userJson) {
                var defaultValue = "[EMPTY]"
                if (key == "level" || key == "accuracy") {
                    defaultValue = "-1"
                }
                userJsonSafe[key] = if (value != null) value else defaultValue
            }

            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            val height = displayMetrics.heightPixels
            val width = displayMetrics.widthPixels
            
            runOnUiThread {
                userid.text = userJsonSafe["user_id"].toString()
                usernameView.text = userJsonSafe["username"].toString()
                country.text = userJsonSafe["country"].toString()
                globalrank.text = "Global #${userJsonSafe["pp_rank"].toString()}"
                localrank.text = "Country #${userJsonSafe["pp_country_rank"].toString()}"
                level.text = "${userJsonSafe["level"].toString().toDouble().toInt()} LVL"
                playcount.text = "${userJsonSafe["playcount"].toString()} PlayCount"
                accuracy.text =
                    "${userJsonSafe["accuracy"].toString().toDouble().round(2)}% Acc"
                countssh.text = " ${userJsonSafe["count_rank_ssh"].toString()} SSH "
                countss.text = " ${userJsonSafe["count_rank_ss"].toString()} SS "
                countsh.text = " ${userJsonSafe["count_rank_sh"].toString()} SH "
                counts.text = " ${userJsonSafe["count_rank_s"].toString()} S "
                counta.text = " ${userJsonSafe["count_rank_a"].toString()} A "
                Picasso.get()
                    .load("https://a.ppy.sh/${userJsonSafe["user_id"]}")
                    .into(findViewById<ImageView>(R.id.pfp_view))
            }

            try {
                val recentJsonRepr = apiController.getUserRecentPlays(
                    username,
                    sharedPrefs.getInt(getString(R.string.mode_preference), -1),
                    sharedPrefs.getInt(getString(R.string.recent_events_preference), -1)
                )
                val recentJson: ArrayList<Map<String, Any>> =
                    Gson().fromJson(recentJsonRepr, Any::class.java) as ArrayList<Map<String, Any>>

                if (recentJson.size < 1) {
                    val noScoresText = TextView(this)
                    noScoresText.textSize = 16f
                    noScoresText.gravity = Gravity.CENTER
                    noScoresText.text = "No recent scores :("
                    runOnUiThread {
                        eventsGroup.addView(noScoresText)
                    }
                    activateSubscribeButton(subscribeButton, username)
                    runOnUiThread {
                        loadingBar.visibility = View.GONE
                    }
                    return@thread
                }
                var i = 1
                var mapsPlayed = HashMap<String, Int>()
                for (event in recentJson) {
                    mapsPlayed[event["beatmap_id"].toString()] = 0
                }

                var lastScoreID = "0"
                for (event in recentJson) {
                    if (this@SearchActivity.shouldDie) {
                        break
                    }
                    if (event["rank"] == "F" && !(showFailedScores)) {
                        continue
                    }
                    if (i > nRecent) {
                        break
                    }

                    val beatmapJsonRepr = apiController.getBeatmap(
                        event["beatmap_id"].toString(),
                        sharedPrefs.getInt(getString(R.string.mode_preference), -1)
                    )
                    val beatmapJsonArray: ArrayList<Map<String, Any>> =
                        Gson().fromJson(
                            beatmapJsonRepr,
                            Any::class.java
                        ) as ArrayList<Map<String, Any>>
                    println(beatmapJsonArray)
                    var beatmapJson: Map<String, Any>
                    val imageView = ImageView(this)
                    try {
                        beatmapJson = beatmapJsonArray[0]
                        runOnUiThread {
                            Picasso.get()
                                .load("https://assets.ppy.sh/beatmaps/${beatmapJson["beatmapset_id"]}/covers/cover.jpg")
                                .resize(width, width / 4)
                                .into(imageView)
                        }
                    } catch (e: Exception) {
                        continue
                    }
                    val textViewMap = TextView(this)
                    textViewMap.textSize = 24f
                    textViewMap.text =
                        "${beatmapJson["artist"]} - ${beatmapJson["title"]} [${beatmapJson["version"]}] By ${beatmapJson["creator"]}"
                    textViewMap.gravity = Gravity.CENTER
                    val space = Space(this)
                    runOnUiThread {
                        eventsGroup.addView(imageView)
                        eventsGroup.addView(textViewMap)
                    }
                    TimeUnit.SECONDS.sleep(apiCallTime.toLong())

                    val textViewScore = TextView(this)
                    try {
                        if (i == 1) {
                            lastScoreID = event["date"].toString()
                            with(sharedPrefs.edit()) {
                                putString("user_$username", lastScoreID)
                                commit()
                            }
                            activateSubscribeButton(subscribeButton, username)
                            println("SCORE ID $lastScoreID")
                        }

                        textViewScore.text =
                            "${event["maxcombo"]} of Max Combo ${event["countmiss"]} x Miss +${
                                parseMods(
                                    event["enabled_mods"].toString()
                                )
                            } ${event["rank"]} Rank${if (event["perfect"].toString() == "1") " FC" else ""}"
                    } catch (e: Exception) {
                        textViewScore.text = "Unable to display, score could have been overwritten"
                    }
                    textViewScore.textSize = 16f
                    textViewScore.gravity = Gravity.CENTER
                    space.layoutParams = LinearLayout.LayoutParams(100, 150)
                    runOnUiThread {
                        eventsGroup.addView(textViewScore)
                        eventsGroup.addView(space)
                    }
                    TimeUnit.SECONDS.sleep(apiCallTime.toLong())

                    i++
                }
            } catch (e: Exception) {
                println(e)
            }
            runOnUiThread {
                if (eventsGroup.childCount == 0) {
                    val noScoresText = TextView(this)
                    noScoresText.textSize = 16f
                    noScoresText.gravity = Gravity.CENTER
                    noScoresText.text = "No recent scores :("
                    eventsGroup.addView(noScoresText)
                    activateSubscribeButton(subscribeButton, username)
                }
                loadingBar.visibility = View.GONE
            }
        }
    }
}
