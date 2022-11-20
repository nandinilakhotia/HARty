package com.pdiot.harty.profile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pdiot.harty.R
import retrofit2.http.DELETE

class DeleteAccountActivity : AppCompatActivity() {

    private lateinit var deleteButton: Button
    private lateinit var email: TextView
    private lateinit var password: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser : FirebaseUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delete_account)

        setUpNavigation()

        deleteButton = findViewById(R.id.delete_account_button)
        email = findViewById(R.id.email)
        password = findViewById(R.id.password)
        auth = Firebase.auth

        val user = auth.currentUser

        if (user != null) {
            currentUser = user
        }

        deleteButton.setOnClickListener {
            val emailString = email.text.toString().trim()
            val passwordString = password.text.toString().trim()

            if (emailString.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                Toast.makeText(baseContext, "Please provide a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordString.isEmpty()) {
                Toast.makeText(baseContext, "Please provide your old password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(emailString, passwordString)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        currentUser.delete().addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(baseContext, "Account deleted :(", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, ProfileActivity::class.java)
                                startActivity(intent)
                            } else {
                                val taskException = task.exception.toString().substringAfter(": ")
                                Toast.makeText(baseContext, taskException, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val taskException = task.exception.toString().substringAfter(": ")
                        // If sign in fails, display a message to the user.
                        Toast.makeText(baseContext, taskException, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        }

    private fun setUpNavigation() {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.backNavigationView)

        bottomNavView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.back -> {
                    finish()
                }
            }
            true
        }
    }
}