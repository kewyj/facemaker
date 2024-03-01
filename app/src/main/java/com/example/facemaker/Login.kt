/*!*****************************************************************************
\file Login.kt
\author Kew Yu Jun
\date 1/3/2024
*******************************************************************************/
package com.example.facemaker

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth

class Login : AppCompatActivity() {
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val user = Firebase.auth.currentUser
        if (user != null) {
            val intent = Intent(this@Login, MainActivity::class.java)
            startActivity(intent)
        }

        auth = Firebase.auth



        findViewById<Button>(R.id.loginbutton).setOnClickListener {
            var textEmail = findViewById<EditText>(R.id.login_email).text.toString()
            var textPass = findViewById<EditText>(R.id.login_password).text.toString()


            loginUser(textEmail, textPass)
        }

        findViewById<Button>(R.id.login_backbutton).setOnClickListener {
            val intent = Intent(this@Login, MainActivity::class.java)
            startActivity(intent)
        }
    }

    fun loginUser(email : String, pass : String) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {task->
            if (task.isSuccessful) {
                val intent = Intent(this@Login, MainActivity::class.java)
                startActivity(intent)
            } else {
                val exception = task.exception
                Log.w(ContentValues.TAG, "signInWithEmailAndPassword:failure", exception)
                Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                when (exception) {
                    is FirebaseAuthInvalidUserException -> {
                        findViewById<TextView>(R.id.loginFeedback).text = "User not found!"
                    }
                    is FirebaseAuthInvalidCredentialsException -> {
                        findViewById<TextView>(R.id.loginFeedback).text = "Email or password is wrong!"
                    }
                    is FirebaseNetworkException -> {
                        findViewById<TextView>(R.id.loginFeedback).text = "Could not connect to network!"
                    }
                    else -> {
                        findViewById<TextView>(R.id.loginFeedback).text = "Undefined exception!"
                    }
                }
            }
        }
    }
}