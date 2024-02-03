package com.example.facemaker

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Choreographer
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class Game : AppCompatActivity() {
    private val faces = mapOf(
        "happy" to 0x1F60A,
        "sad" to 0x1F614,
        "angry" to 0x1F620
    )
    private var currScore : Int = 0
    private var currFace :String= "happy"
    private var timeLeft :Long = 60000000000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        currScore = 0
        timeLeft = 60000000000
        currFace = faces.keys.toList()[Random.nextInt(faces.size)]
        updateFace()
        setScore()

        val timer = object : CountDownTimer(61000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                findViewById<TextView>(R.id.timer).text = "Time Left: " + secondsLeft
            }

            override fun onFinish() {
                val intent = Intent(this@Game, GameOver::class.java)
                intent.putExtra("score", currScore)
                startActivity(intent)
            }
        }

        timer.start()
    }

    fun getCurrentFace() : String {
        return currFace
    }

    fun incrementScore() {
        ++currScore
        setScore()
    }

    private fun setScore() {
        findViewById<TextView>(R.id.score).text = "Score:\n" + currScore
    }

    fun nextFace() {
        var randomFace : String = currFace
        while (randomFace == currFace) {
            randomFace = faces.keys.toList()[Random.nextInt(faces.size)]
        }
        currFace = randomFace
        updateFace()
    }

    private fun updateFace() {
        var faceInt : Int = faces[currFace]?:0x1F620
        findViewById<TextView>(R.id.faceToMake).setText(String(Character.toChars(faceInt)))
    }
}