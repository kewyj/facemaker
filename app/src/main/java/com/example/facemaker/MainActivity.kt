package com.example.facemaker

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.content.Intent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setLogo()

        findViewById<Button>(R.id.howToPlay).setOnClickListener {
            val intent = Intent(this@MainActivity, HowToPlay::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.credits).setOnClickListener {
            val intent = Intent(this@MainActivity, Credits::class.java)
            startActivity(intent)
        }
    }

    private fun setLogo() {
        findViewById<TextView>(R.id.logo).setText("FaceMaker\n" + getEmoji(0x1F60A) + getEmoji(0x1F614) + getEmoji(0x1F620))
    }
    private fun getEmoji(unicode : Int): String? {
        return String(Character.toChars(unicode))
    }
}
