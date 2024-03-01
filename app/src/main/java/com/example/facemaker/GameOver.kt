/*!*****************************************************************************
\file GameOver.kt
\author Kew Yu Jun, Chen Jia Wen
\date 1/3/2024
*******************************************************************************/
package com.example.facemaker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

class GameOver : AppCompatActivity() {
    private lateinit var username : String
    private var currScore : Int = -1
    private var highscore : Int = -1
    private var totalscore : Int = -1
    private lateinit var db : FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameover)

        db = Firebase.firestore

        username = GetUsername()
        currScore = SetScore()
        GetHighscore { hs, total->
            highscore = hs
            totalscore = total
            if (currScore > highscore) {
                findViewById<TextView>(R.id.gameover_highscore).text = "Highscore: " + currScore.toString()
            } else {
                findViewById<TextView>(R.id.gameover_highscore).text = "Highscore: " + highscore.toString()
            }
            UpdateDB {}
        }

        findViewById<Button>(R.id.mainMenu).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.playAgain).setOnClickListener {
            val intent = Intent(this, Game::class.java)
            startActivity(intent)
        }
    }

    fun GetUsername() : String{
        val user = Firebase.auth.currentUser
        var email : String = ""
        user?.let {
            val temp = it.email
            email = temp.toString().substringBefore('@').replace(".", "_")
        }
        return email
    }

    fun SetScore() : Int {
        var score = intent.extras?.getInt("score")
        findViewById<TextView>(R.id.score).text = "Score: " + score
        if (score == null) {
            return -1
        }
        return score.toInt()
    }

    fun GetHighscore(callback: (Int, Int) -> Unit) {

        val docRef = db.collection("user_details").document(username)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val highscore = document.get("highscore").toString().toInt()
                    val score = document.get("facesmade").toString().toInt()
                    callback(highscore, score)
                } else {
                    Log.d("GameOver", "No such document")
                    callback(-1, -1) // Or handle accordingly if document doesn't exist
                }
            }
            .addOnFailureListener { exception ->
                Log.d("GameOver", "get failed with ", exception)
                callback(-1, -1) // Or handle failure accordingly
            }
    }

    fun UpdateDB(completion: () -> Unit) {
        var hs :Int = highscore
        if (currScore > highscore) {
            hs = currScore
        }

        val scores = hashMapOf(
            "facesmade" to totalscore + currScore,
            "highscore" to hs,
            "username" to username
        )

        db.collection("user_details").document(username)
            .set(scores)
            .addOnCompleteListener {
                completion()
            }
    }
}