package com.pdiot.harty.profile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pdiot.harty.R

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var changePasswordButton : Button
    private lateinit var oldPassword : EditText
    private lateinit var newPassword : EditText
    private lateinit var oldEmail : TextView
    private lateinit var userEmail : String
    private lateinit var currentUser : FirebaseUser
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        setUpNavigation()

        changePasswordButton = findViewById(R.id.changePasswordButton)
        oldPassword = findViewById(R.id.oldPassword)
        newPassword = findViewById(R.id.newPassword)
        oldEmail = findViewById(R.id.oldEmail)
        auth = Firebase.auth

        val user = auth.currentUser

        if (user != null) {
            currentUser = user
            userEmail = "Current email: " + currentUser.email
            oldEmail.text = userEmail
        }

        changePasswordButton.setOnClickListener {
            val oldPasswordString = oldPassword.text.toString().trim()
            val newPasswordString = newPassword.text.toString().trim()

            if (oldPasswordString.isEmpty()) {
                Toast.makeText(baseContext, "Please provide your old password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPasswordString.isEmpty() || (newPasswordString.length < 6)) {
                Toast.makeText(baseContext, "Please provide a password greater than 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser.email?.let { it1 ->
                auth.signInWithEmailAndPassword(it1, oldPasswordString)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            currentUser.updatePassword(newPasswordString).addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(baseContext, "Password updated.", Toast.LENGTH_SHORT).show()
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

            return@setOnClickListener
        }

    }

    private fun setUpNavigation() {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.backNavigationView)

        bottomNavView.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.back -> {
                    finish()
                }
            }
            true
        }
    }
}