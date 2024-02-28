package com.example.facemaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

class Leaderboard : AppCompatActivity() {
    private lateinit var db : FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)
        recyclerView = findViewById<RecyclerView>(R.id.leaderboardrecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LeaderboardAdapter()
        recyclerView.adapter = adapter
        db = Firebase.firestore

        findViewById<Button>(R.id.lbBackToMenu).setOnClickListener {
            val intent = Intent(this@Leaderboard, MainActivity::class.java)
            startActivity(intent)
        }

        GetHighScoreData { highscore ->
            adapter.setData(highscore)
        }

        findViewById<Button>(R.id.sortbyhighscore).setOnClickListener {
            GetHighScoreData { highscore->
                adapter.setData(highscore)
            }
        }

        findViewById<Button>(R.id.sortbytotalscore).setOnClickListener {
            GetTotalScoreData {totalscore->
                adapter.setData(totalscore)
            }
        }

    }
    private fun GetTotalScoreData(callback: (MutableMap<String, Long>) -> Unit) {
        var userRef = db.collection("user_details")
        userRef.orderBy("facesmade", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { documents ->
                val totalscore: MutableMap<String, Long> = mutableMapOf()

                for (document in documents) {
                    val username = document["username"] as String
                    val facesmade = document["facesmade"] as Long
                    totalscore[username] = facesmade
                }

                callback(totalscore)
            }
            .addOnFailureListener { exception ->
                Log.w("leaderboard", "Error getting documents: ", exception)
            }
    }
    private fun GetHighScoreData(callback: (MutableMap<String, Long>) -> Unit) {
        var userRef = db.collection("user_details")
        userRef.orderBy("highscore", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { documents ->
                val highscore: MutableMap<String, Long> = mutableMapOf()

                for (document in documents) {
                    val username = document["username"] as String
                    val score = document["highscore"] as Long
                    highscore[username] = score
                }

                callback(highscore)
            }
            .addOnFailureListener { exception ->
                Log.w("leaderboard", "Error getting documents: ", exception)
            }
    }
}