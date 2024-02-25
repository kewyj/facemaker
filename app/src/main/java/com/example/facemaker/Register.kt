package com.example.facemaker

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class Register : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = Firebase.auth
        findViewById<Button>(R.id.signupbutton).setOnClickListener {
            var textEmail = findViewById<EditText>(R.id.register_email).text.toString()
            var textPass = findViewById<EditText>(R.id.register_password).text.toString()


            registerUser(textEmail, textPass)
        }
    }
    private fun registerUser(email : String, pass : String) {
        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this) {task->
            if (task.isSuccessful) {
                val user = hashMapOf(
                    "username" to email.toString().substringBefore('@'),
                    "facesmade" to 0,
                    "highscore" to 0,
                )
                val db = Firebase.firestore
                val documentId = email.toString().substringBefore('@').replace(".", "_")
                val userCollection = db.collection("user_details")
                val userDocument = userCollection.document(documentId)
                userDocument.set(user)
                    .addOnSuccessListener {
                        auth.signOut()
                        val intent = Intent(this@Register, Login::class.java)
                        startActivity(intent)
                    }
                    .addOnFailureListener {e->
                        findViewById<TextView>(R.id.registerFeedback).text = "Failed to create profile!"
                    }


            }else {
                val exception = task.exception
                Log.w(TAG, "createUserWithEmail:failure", task.exception)
                Toast.makeText(
                    baseContext,
                    "Authentication failed.",
                    Toast.LENGTH_SHORT,
                ).show()
                when (exception) {
                    is FirebaseAuthWeakPasswordException -> {
                        findViewById<TextView>(R.id.registerFeedback).text = "Password too weak!"
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        findViewById<TextView>(R.id.registerFeedback).text = "Email is invalid!"
                    }
                    is FirebaseAuthUserCollisionException -> {
                        findViewById<TextView>(R.id.registerFeedback).text = "User already exists!"
                    }
                    else -> {
                        findViewById<TextView>(R.id.registerFeedback).text = "Undefined exception, please try again."
                    }
                }
            }
        }
    }
}