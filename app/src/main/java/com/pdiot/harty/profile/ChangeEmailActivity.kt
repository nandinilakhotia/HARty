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
import org.w3c.dom.Text

class ChangeEmailActivity : AppCompatActivity() {

    private lateinit var changeEmailButton : Button
    private lateinit var password : EditText
    private lateinit var oldEmail : TextView
    private lateinit var userEmail : String
    private lateinit var currentUser : FirebaseUser
    private lateinit var newEmail : EditText
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_email)

        setUpNavigation()

        changeEmailButton = findViewById(R.id.changeEmailButton)
        password = findViewById(R.id.passwordInput)
        newEmail = findViewById(R.id.newEmail)
        oldEmail = findViewById(R.id.oldEmail)
        auth = Firebase.auth

        val user = auth.currentUser

        if (user != null) {
            currentUser = user
            userEmail = "Current email: " + currentUser.email
            oldEmail.text = userEmail
        }

        changeEmailButton.setOnClickListener {
            val passwordString = password.text.toString().trim()
            val emailString = newEmail.text.toString().trim()

            if (emailString.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                Toast.makeText(baseContext, "Please provide a valid email address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordString.isEmpty()) {
                Toast.makeText(baseContext, "Please provide a password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentUser.email?.let { it1 ->
                auth.signInWithEmailAndPassword(it1, passwordString)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {

                            currentUser.updateEmail(emailString).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(baseContext, "Email updated.", Toast.LENGTH_SHORT).show()
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