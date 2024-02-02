package com.example.facemaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class HowToPlay : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_how_to_play)
        setLogo()

        findViewById<Button>(R.id.htpBackToMenu).setOnClickListener {
            val intent = Intent(this@HowToPlay, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setLogo() {
        findViewById<TextView>(R.id.htpLogo).setText("FaceMaker\n" + getEmoji(0x1F60A) + getEmoji(0x1F614) + getEmoji(0x1F620))
    }
    private fun getEmoji(unicode : Int): String? {
        return String(Character.toChars(unicode))
    }
}