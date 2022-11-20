package com.pdiot.harty.profile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.pdiot.harty.R

class HistoricDataActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var button : Button
// ...


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historic_data)


    }
}