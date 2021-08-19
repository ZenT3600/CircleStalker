package it.matteoleggio.osustalker

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

class SubsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subs)

        val sharedPref: SharedPreferences = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val users = sharedPref.getString(getString(R.string.users_to_check_preference), "").toString()
        val subsGroup = findViewById<LinearLayout>(R.id.subs)

        if (users == "" || users == ", ") {}
        else {
            users.drop(2).split(", ").toTypedArray().forEach {
                val textViewSub = TextView(this)
                textViewSub.text = it
                textViewSub.textSize = 16f
                textViewSub.gravity = Gravity.CENTER
                val space = Space(this)
                space.layoutParams = LinearLayout.LayoutParams(100, 25)
                subsGroup.addView(textViewSub)
                subsGroup.addView(space)
            }
        }
    }
}