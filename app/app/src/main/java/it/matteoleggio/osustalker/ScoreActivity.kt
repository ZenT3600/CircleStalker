package it.matteoleggio.osustalker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat

class ScoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)
        
        val sharedPrefs = getSharedPreferences("it.matteoleggio.osustalker", Context.MODE_PRIVATE)
        val score = intent.getStringExtra("score").toString()
        val mode = intent.getStringExtra("mode").toString()     // osu, taiko, fruits, mania
        "https://osu.ppy.sh/scores/$mode/$score".asUri()?.openInBrowser(this)
        super.onDestroy()
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