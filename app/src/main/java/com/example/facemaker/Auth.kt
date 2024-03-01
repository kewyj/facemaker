/*!*****************************************************************************
\file Auth.kt
\author Kew Yu Jun
\date 1/3/2024
*******************************************************************************/
package com.example.facemaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

// Define the Auth class which extends AppCompatActivity
class Auth : AppCompatActivity() {

    // Override the onCreate method
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth) // Set the layout for this activity

        // Set OnClickListener for the signup button
        findViewById<Button>(R.id.signup).setOnClickListener {
            // Create an intent to navigate to the Register activity
            val intent = Intent(this@Auth, Register::class.java)
            startActivity(intent) // Start the Register activity
        }

        // Set OnClickListener for the login button
        findViewById<Button>(R.id.login).setOnClickListener {
            // Create an intent to navigate to the Login activity
            val intent = Intent(this@Auth, Login::class.java)
            startActivity(intent) // Start the Login activity
        }
    }
}