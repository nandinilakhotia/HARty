package com.pdiot.harty.profile

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.pdiot.harty.MainActivity
import com.pdiot.harty.R
import com.pdiot.harty.settings.SettingsActivity
import com.pdiot.harty.utils.Constants


class ProfileActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var changeEmailButton: Button
    private lateinit var changePasswordButton: Button
    private lateinit var viewDataButton: Button
    private lateinit var deleteAccountButton: Button
    private lateinit var welcomeMessage : TextView
    private lateinit var guestMessage : TextView
    private lateinit var auth: FirebaseAuth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setUpNavigation()

        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (!(sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF) && (sharedPreferences.getString(Constants.LAST_SENSOR_USED,"").equals("Respeck")))) {
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "false").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false").apply()
        }

        loginButton = findViewById(R.id.login_button)
        changeEmailButton = findViewById(R.id.change_email_button)
        changePasswordButton = findViewById(R.id.change_password_button)
        viewDataButton = findViewById(R.id.view_data_button)
        deleteAccountButton = findViewById(R.id.delete_account_button)
        welcomeMessage = findViewById(R.id.welcomeMessage)
        guestMessage = findViewById(R.id.guestMessage)
        auth = Firebase.auth

        val currentUser = auth.currentUser

        if(currentUser != null){
            welcomeMessage.text = "Hi, " +  currentUser.displayName + "!"
            loginButton.text = "Logout"
            guestMessage.isVisible = false;
            changeEmailButton.isEnabled = true;
            changeEmailButton.isClickable = true;
            changePasswordButton.isEnabled = true;
            changePasswordButton.isClickable = true;
            viewDataButton.isEnabled = true;
            viewDataButton.isClickable = true;
            deleteAccountButton.isEnabled = true;
            deleteAccountButton.isClickable = true;
        } else {
            welcomeMessage.text = "Hi, Guest!"
            loginButton.text = "Login"
            guestMessage.isVisible = true;
            changeEmailButton.isEnabled = false;
            changeEmailButton.isClickable = false;
            changePasswordButton.isEnabled = false;
            changePasswordButton.isClickable = false;
            viewDataButton.isEnabled = false;
            viewDataButton.isClickable = false;
            deleteAccountButton.isEnabled = false;
            deleteAccountButton.isClickable = false;
        }

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            sharedPreferences.edit().putString(Constants.RESPECK_STATUS, "Disconnected").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "false").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false").apply()
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "LINK").apply()
            sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, "true").apply()
        }

        loginButton.setOnClickListener {
            if (loginButton.text.equals("Login")) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            } else {
                auth.signOut()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
        }

        changeEmailButton.setOnClickListener {
            val intent = Intent(this, ChangeEmailActivity::class.java)
            startActivity(intent)
        }

        changePasswordButton.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        viewDataButton.setOnClickListener {
            val intent = Intent(this, HistoricDataActivity::class.java)
            startActivity(intent)
        }

        deleteAccountButton.setOnClickListener {
            val intent = Intent(this, DeleteAccountActivity::class.java)
            startActivity(intent)
        }


    }

    private fun setUpNavigation() {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavView.selectedItemId = R.id.profile

        bottomNavView.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.home -> {
                    startActivity(Intent(applicationContext, MainActivity::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.profile -> {
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
    }
}