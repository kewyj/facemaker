package com.example.facemaker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Auth : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        findViewById<Button>(R.id.signup).setOnClickListener {
            val intent = Intent(this@Auth, Register::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.login).setOnClickListener {
            val intent = Intent(this@Auth, Login::class.java)
            startActivity(intent)
        }
    }
}