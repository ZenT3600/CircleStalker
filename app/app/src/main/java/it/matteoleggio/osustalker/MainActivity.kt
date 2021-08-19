package it.matteoleggio.osustalker

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.beardedhen.androidbootstrap.TypefaceProvider
import it.matteoleggio.osustalker.Logic.Api
import it.matteoleggio.osustalker.Logic.Helpers.*


class MainActivity : AppCompatActivity() {
    private lateinit var sharedPrefs: SharedPreferences

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, ScoreService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(it)
                return
            }
            startService(it)
        }
    }

    override fun onDestroy() {
        val apiKey: String? = sharedPrefs.getString(getString(R.string.api_key_preference), "")
        val mode: Int = sharedPrefs.getInt(getString(R.string.mode_preference), -1)
        val eventsNum: Int = sharedPrefs.getInt(getString(R.string.recent_events_preference), -1)
        val apiCallTime: Float = sharedPrefs.getFloat(getString(R.string.api_call_time_preference), -1f)
        val scoreTime: Int = sharedPrefs.getInt(getString(R.string.score_time_preference), -1)
        val failedScore: Boolean = sharedPrefs.getBoolean(getString(R.string.failed_score_preference), false)
        val scoreposting: Boolean = sharedPrefs.getBoolean(getString(R.string.scoreposting_preference), true)
        with(sharedPrefs.edit()) {
            putString(getString(R.string.api_key_preference), apiKey)
            putInt(getString(R.string.mode_preference), mode)
            putInt(getString(R.string.recent_events_preference), eventsNum)
            putFloat(getString(R.string.api_call_time_preference), apiCallTime)
            putInt(getString(R.string.score_time_preference), scoreTime)
            putBoolean(getString(R.string.failed_score_preference), failedScore)
            putBoolean(getString(R.string.scoreposting_preference), scoreposting)
            commit()
        }
        super.onDestroy()
    }

    override fun onStop() {
        val apiKey: String? = sharedPrefs.getString(getString(R.string.api_key_preference), "")
        val mode: Int = sharedPrefs.getInt(getString(R.string.mode_preference), -1)
        val eventsNum: Int = sharedPrefs.getInt(getString(R.string.recent_events_preference), -1)
        val apiCallTime: Float = sharedPrefs.getFloat(getString(R.string.api_call_time_preference), -1f)
        val scoreTime: Int = sharedPrefs.getInt(getString(R.string.score_time_preference), -1)
        val failedScore: Boolean = sharedPrefs.getBoolean(getString(R.string.failed_score_preference), false)
        val scoreposting: Boolean = sharedPrefs.getBoolean(getString(R.string.scoreposting_preference), true)
        with(sharedPrefs.edit()) {
            putString(getString(R.string.api_key_preference), apiKey)
            putInt(getString(R.string.mode_preference), mode)
            putInt(getString(R.string.recent_events_preference), eventsNum)
            putFloat(getString(R.string.api_call_time_preference), apiCallTime)
            putInt(getString(R.string.score_time_preference), scoreTime)
            putBoolean(getString(R.string.failed_score_preference), failedScore)
            putBoolean(getString(R.string.scoreposting_preference), scoreposting)
            commit()
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TypefaceProvider.registerDefaultIconSets()
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val apiKey: String = sharedPrefs.getString(getString(R.string.api_key_preference), "")!!

        // Views
        val apiView = findViewById<Group>(R.id.api_view)
        val mainView = findViewById<Group>(R.id.main_view)

        // Api View Elements
        val apiInputBox = findViewById<EditText>(R.id.api_input_textbox)
        val getButton = findViewById<Button>(R.id.api_get_button)
        val okButton = findViewById<Button>(R.id.api_ok_button)

        // Main View Elements
        val mainInputBox = findViewById<EditText>(R.id.main_input_textbox)
        val searchButton = findViewById<Button>(R.id.main_search_button)
        val settingsButton = findViewById<Button>(R.id.main_settings_button)
        val subsButton = findViewById<Button>(R.id.main_subs_button)

        if (apiKey != "") {
            // API Key is set and valid
            actionOnService(Actions.START)

            apiView.visibility = View.INVISIBLE
            mainView.visibility = View.VISIBLE

            searchButton.setOnClickListener {
                val givenUserString = mainInputBox.text.toString()
                if (givenUserString != "") {
                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    intent.putExtra("userString", givenUserString)
                    startActivity(intent)
                }
            }

            settingsButton.setOnClickListener {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }

            subsButton.setOnClickListener {
                val intent = Intent(this@MainActivity, SubsActivity::class.java)
                startActivity(intent)
            }
        } else {
            // First time opening app or invalid API Key
            with(sharedPrefs.edit()) {
                putInt(getString(R.string.mode_preference), 0)
                putInt(getString(R.string.recent_events_preference), 10)
                putFloat(getString(R.string.api_call_time_preference), 1f)
                putInt(getString(R.string.score_time_preference), 3)
                putString(getString(R.string.users_to_check_preference), "")
                putBoolean(getString(R.string.failed_score_preference), false)
                putBoolean(getString(R.string.scoreposting_preference), true)
                apply()
            }

            apiView.visibility = View.VISIBLE
            mainView.visibility = View.INVISIBLE

            getButton.setOnClickListener {
                val apiKeyUrl = "https://old.ppy.sh/p/api/"
                apiKeyUrl.asUri()?.openInBrowser(this)
            }

            okButton.setOnClickListener {
                val userApiKey = apiInputBox.text.toString()
                println(userApiKey)
                val apiController = Api(userApiKey)
                val valid = apiController.keyIsValid()
                println(valid)
                if (valid) {
                    actionOnService(Actions.START)
                    with(this@MainActivity.sharedPrefs.edit()) {
                        putString(getString(R.string.api_key_preference), userApiKey)
                        apply()
                    }
                    apiView.visibility = View.INVISIBLE
                    mainView.visibility = View.VISIBLE

                    searchButton.setOnClickListener {
                        val givenUserString = mainInputBox.text.toString()
                        if (givenUserString != "") {
                            val intent = Intent(this@MainActivity, SearchActivity::class.java)
                            intent.putExtra("userString", givenUserString)
                            startActivity(intent)
                        }
                    }

                    settingsButton.setOnClickListener {
                        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                        startActivity(intent)
                    }

                    subsButton.setOnClickListener {
                        val intent = Intent(this@MainActivity, SubsActivity::class.java)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Invalid API Key!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun String?.asUri(): Uri? {
        try {
            return Uri.parse(this)
        } catch (e: Exception) {}
        return null
    }

    private fun Uri?.openInBrowser(context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, this)
        ContextCompat.startActivity(context, browserIntent, null)
    }
}