package it.matteoleggio.osustalker

import android.app.*
import android.content.*
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.system.Os.link
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import it.matteoleggio.osustalker.Logic.Api
import it.matteoleggio.osustalker.Logic.Helpers.*
import it.matteoleggio.osustalker.Logic.parseMods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import android.content.IntentFilter
import android.app.PendingIntent

import android.content.Intent
import it.matteoleggio.osustalker.Logic.ScorePosting
import kotlin.random.Random.Default.nextInt


class ScoreService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> print("This should never happen. No action in the received intent")
            }
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification("Score Service", "Score Service is up and running", 1, false)
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Score Service Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun startService() {
        val sharedPref: SharedPreferences = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        if (isServiceStarted) return
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // Bypass Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        val waitTime = sharedPref.getInt(getString(R.string.score_time_preference), 3).toLong()
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    try {
                        val users = sharedPref.getString(getString(R.string.users_to_check_preference), "").toString()
                        if (users == "" || users == ", ") {}
                        else {
                            users.drop(2).split(", ").toTypedArray().forEach {
                                checkForNewScores(it)
                            }
                        }
                    } catch (e: Exception) {
                        println(e)
                    }
                }
                TimeUnit.MINUTES.sleep(waitTime)
            }
        }
    }

    private fun stopService() {
        Toast.makeText(this, "Score Service Stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            print("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun checkForNewScores(username: String) {
        println("Checking $username")

        val sharedPref: SharedPreferences = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val showFailedScores: Boolean = sharedPref.getBoolean(getString(R.string.failed_score_preference), false)
        val apiController = Api(sharedPref.getString(getString(R.string.api_key_preference), "").toString())
        val waitTime = sharedPref.getFloat(getString(R.string.api_call_time_preference), 1f).toLong()
        val userJsonRepr = apiController.getUser(
            username,
            sharedPref.getInt(getString(R.string.mode_preference), -1)
        )
        val userJsonArray: ArrayList<Map<String, Any>> =
            Gson().fromJson(userJsonRepr, Any::class.java) as ArrayList<Map<String, Any>>
        println(userJsonArray)
        val userJson = userJsonArray[0]
        println(userJson)
        val recentJsonRepr = apiController.getUserRecentPlays(
            username,
            sharedPref.getInt(getString(R.string.mode_preference), -1),
            sharedPref.getInt(getString(R.string.recent_events_preference), -1)
        )
        val recentJson: ArrayList<Map<String, Any>> =
            Gson().fromJson(recentJsonRepr, Any::class.java) as ArrayList<Map<String, Any>>

        var lastScoreID = sharedPref.getString("user_$username", "")

        println(recentJson)
        if (recentJson.size == 0) {
            return
        }

        var i = 0
        var done = false
        for (it in recentJson) {
            if (done) {
                continue
            }
            val beatmapJsonRepr = apiController.getBeatmap(
                it["beatmap_id"].toString(),
                sharedPref.getInt(getString(R.string.mode_preference), -1)
            )
            val beatmapJsonArray: ArrayList<Map<String, Any>> =
                Gson().fromJson(
                    beatmapJsonRepr,
                    Any::class.java
                ) as ArrayList<Map<String, Any>>
            println(beatmapJsonArray)
            var beatmapJson = beatmapJsonArray[0]
            println(it)
            println(it["date"].toString() + " | " + lastScoreID)
            if (it["rank"] == "F" && !(showFailedScores)) {
                continue
            }
            if (i == 0) {
                val lastScoreID = it["date"].toString()
                with(sharedPref.edit()) {
                    putString("user_$username", lastScoreID)
                    commit()
                }
                println("NEW SCORE ID $lastScoreID")
                i++
            }
            if (it["date"].toString() == lastScoreID) {
                println("Not a new score " + it["date"].toString())
                done = true
            } else {
                println("DONE: $done")
                if (!done) {
                    try {
                        println(it["maxcombo"])
                        createNotificationForScore(
                            "New Score",
                            "$username | ${beatmapJson["artist"]} - ${beatmapJson["title"]} [${beatmapJson["version"]}]",
                            " ${it["maxcombo"]} of Max Combo ${it["countmiss"]} x Miss +${parseMods(it["enabled_mods"].toString())} ${it["rank"]} Rank worth ${if (it["pp"] != null) it["pp"] else 0} pp",
                            7,
                            sharedPref.getInt(getString(R.string.mode_preference), -1),
                            sharedPref.getBoolean(getString(R.string.scoreposting_preference), true),
                            ScorePosting().generateScorePostTitle(userJson, it, beatmapJson)
                        )
                    } catch (e: Exception) {
                         createNotificationForScore(
                            "New Score",
                            "$username | Unable to display, score could have been overwritten",
                            "Sorry about that :(",
                            7,
                             sharedPref.getInt(getString(R.string.mode_preference), -1),
                             false,
                             "")
                    }
                }
            }

            TimeUnit.SECONDS.sleep(waitTime)
        }
    }

    private fun createNotification(title: String, description: String, channelInt: Int, important: Boolean): Notification {
        val notificationChannelId = "SCORE SERVICE CHANNEL $channelInt"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                title,
                if (important) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            ).let {
                it.description = description
                it.enableLights(true)
                it.lightColor = Color.RED
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(if (important) Notification.PRIORITY_HIGH else Notification.PRIORITY_LOW) // for under android 26 compatibility
            .build()
    }

    private fun createNotificationForScore(title: String, descriptionSmall: String, descriptionFull: String, channelInt: Int, mode: Int, addButton: Boolean, scorePostingText: String) {
        val notificationChannelId = "SCORE SERVICE CHANNEL $channelInt"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                title,
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.enableLights(true)
                it.lightColor = Color.RED
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder: NotificationCompat.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationCompat.Builder(
            this,
            notificationChannelId
        ) else NotificationCompat.Builder(this)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        builder
            .setContentTitle(title)
            .setContentText(descriptionSmall)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .setStyle(
                NotificationCompat.BigTextStyle()
                .bigText(descriptionFull)
                .setBigContentTitle(descriptionSmall)
            )
            .setSound(soundUri)

        if (addButton) {
            val brCopy: BroadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val clipboard: ClipboardManager =
                        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val text = intent.getStringExtra("text")
                    val clip = ClipData.newPlainText(text, text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@ScoreService, "Copied! ($text)", Toast.LENGTH_SHORT).show()
                }
            }
            val intentFilter = IntentFilter("$packageName.ACTION_COPY")
            registerReceiver(brCopy, intentFilter)
            val copy = Intent("$packageName.ACTION_COPY")
            copy.putExtra("text", scorePostingText)
            val piCopy =
                PendingIntent.getBroadcast(this, 0, copy, PendingIntent.FLAG_CANCEL_CURRENT)
            builder.addAction(R.drawable.ic_copy, "ScorePosting Title", piCopy)
            notificationManager.notify(nextInt(1, 99999999), builder.build())
        }
    }
}
