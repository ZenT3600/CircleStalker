package it.matteoleggio.osustalker

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.beardedhen.androidbootstrap.BootstrapEditText
import it.matteoleggio.osustalker.Logic.Api


class SettingsActivity : AppCompatActivity() {
    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val sharedPref: SharedPreferences = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val apiKey: String? = sharedPref.getString(getString(R.string.api_key_preference), "")
        val mode: Int = sharedPref.getInt(getString(R.string.mode_preference), -1)
        val eventsNum: Int = sharedPref.getInt(getString(R.string.recent_events_preference), -1)
        val apiCallTime: Float = sharedPref.getFloat(getString(R.string.api_call_time_preference), -1f) * 10
        val scoreCheckingTime: Int = sharedPref.getInt(getString(R.string.score_time_preference), -1)
        val showFailedScores: Boolean = sharedPref.getBoolean(getString(R.string.failed_score_preference), false)
        val scorePosting: Boolean = sharedPref.getBoolean(getString(R.string.scoreposting_preference), true)
        println(apiKey)
        println(mode)
        println(eventsNum)
        println(apiCallTime)

        val apiKeyInput = findViewById<BootstrapEditText>(R.id.settings_apikey_input)
        val modeGroup = findViewById<RadioGroup>(R.id.settings_mode_group)
        val eventsNumInput = findViewById<EditText>(R.id.settings_eventnum_input)
        val apiSeekBar = findViewById<SeekBar>(R.id.settings_apicalltime_bar)
        val apiCallDisplayValue = findViewById<TextView>(R.id.settings_apicalltime_value)
        val scoreSeekBar = findViewById<SeekBar>(R.id.settings_servicetime_bar)
        val scoreTimeDisplayValue = findViewById<TextView>(R.id.settings_servicetime_value)
        val showFailedScoresBox = findViewById<CheckBox>(R.id.settings_failed_box)
        val scorePostingBox = findViewById<CheckBox>(R.id.settings_scoreposting_box)
        apiCallDisplayValue.text = "Value: ${apiCallTime / 10}"
        scoreTimeDisplayValue.text = "Value: $scoreCheckingTime"
        showFailedScoresBox.isChecked = showFailedScores
        scorePostingBox.isChecked = scorePosting
        val saveButton = findViewById<Button>(R.id.settings_save_button)

        apiSeekBar.progress = apiCallTime.toInt() - 10
        apiSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var progress = progress
                apiCallDisplayValue.text = "Value: ${(progress.toFloat() + 10) / 10}"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        scoreSeekBar.progress = scoreCheckingTime
        scoreSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                var progress = if (progress > 0) progress else 1
                scoreTimeDisplayValue.text = "Value: $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        apiKeyInput.setText(apiKey)
        modeGroup.check(when (mode) {
            0 -> R.id.settings_mode_0
            1 -> R.id.settings_mode_1
            2 -> R.id.settings_mode_2
            3 -> R.id.settings_mode_3
            else -> {
                R.id.settings_mode_0
            }
        })
        eventsNumInput.setText(eventsNum.toString())

        saveButton.setOnClickListener {
            val newApiKey = apiKeyInput.text.toString()
            val newModeId = modeGroup.checkedRadioButtonId
            var newMode = 0
            newMode = when (newModeId) {
                R.id.settings_mode_0 -> 0
                R.id.settings_mode_1 -> 1
                R.id.settings_mode_2 -> 2
                R.id.settings_mode_3 -> 3
                else -> {
                    0
                }
            }
            val newEventsNum = eventsNumInput.text.toString().toInt()
            val newApiCallTime = (apiSeekBar.progress.toFloat() + 10f) / 10f
            val newScoreTime = scoreSeekBar.progress
            val newFailedSCore = showFailedScoresBox.isChecked
            val newScoreposting = scorePostingBox.isChecked

            with(sharedPref.edit()) {
                val apiController = Api(newApiKey)
                val valid = apiController.keyIsValid()
                println(valid)
                if (valid) {
                    putString(getString(R.string.api_key_preference), newApiKey)
                } else {
                    Toast.makeText(this@SettingsActivity, "Invalid API Key!", Toast.LENGTH_SHORT).show()
                }
                putInt(getString(R.string.mode_preference), newMode)
                putInt(getString(R.string.recent_events_preference), newEventsNum)
                putFloat(getString(R.string.api_call_time_preference), newApiCallTime)
                putInt(getString(R.string.score_time_preference), newScoreTime)
                putBoolean(getString(R.string.failed_score_preference), newFailedSCore)
                putBoolean(getString(R.string.scoreposting_preference), newScoreposting)
                commit()
            }
            finish()
        }
    }

}