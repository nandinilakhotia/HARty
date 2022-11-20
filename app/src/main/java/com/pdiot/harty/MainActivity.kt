package com.pdiot.harty

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.pdiot.harty.onboarding.OnBoardingActivity
import com.pdiot.harty.profile.ProfileActivity
import com.pdiot.harty.settings.BluetoothSpeckService
import com.pdiot.harty.settings.SettingsActivity
import com.pdiot.harty.utils.Constants
import com.pdiot.harty.utils.RESpeckLiveData
import com.pdiot.harty.utils.Utils
import kotlinx.android.synthetic.main.dashboard_step_counter.*
import java.lang.Math.sqrt

class MainActivity : AppCompatActivity() {

    // permissions
    lateinit var permissionAlertDialog: AlertDialog.Builder
    private lateinit var bluetoothSetting : TextView
    private lateinit var wifiSetting : TextView
    private lateinit var respeckStatus : TextView
    private lateinit var refreshButton : Button
    private lateinit var totalSteps : EditText
    private lateinit var progressBar : CircularProgressBar

    val permissionsForRequest = arrayListOf<String>()
    var isUserFirstTime = false

    var locationPermissionGranted = false
    var bluetoothPermissionGranted = false
    var cameraPermissionGranted = false

    private var previousMagnitude = 0.0
    private lateinit var displaySteps : TextView


    // broadcast receiver
    val filter = IntentFilter()
    lateinit var respeckLiveUpdateReceiver: BroadcastReceiver
    lateinit var looperRespeck: Looper

    val filterTestRespeck = IntentFilter(Constants.ACTION_RESPECK_LIVE_BROADCAST)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionAlertDialog = AlertDialog.Builder(this)
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)

        performOnboarding()

        setupNavigation()

        setupPermissions()

        dashboard(sharedPreferences)

        setupBluetoothService(sharedPreferences)

        stepCounter(sharedPreferences)

        totalSteps = findViewById(R.id.TotalSteps)
        totalSteps.onFocusChangeListener = View.OnFocusChangeListener{_, hasFocus ->
            if (!hasFocus) {
                progressBar.progressMax = totalSteps.text.toString().toFloat()
            }
        }


    }

    private fun dashboard(sharedPreferences : SharedPreferences) {
        bluetoothSetting = findViewById(R.id.bluetooth_setting)
        respeckStatus = findViewById(R.id.status_respeck)
        wifiSetting = findViewById(R.id.wifi_setting)
        refreshButton = findViewById(R.id.refresh_button)

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        if (mBluetoothAdapter.isEnabled) {
            bluetoothSetting.text = "Phone Bluetooth Status : ON"
        } else {
            bluetoothSetting.text = "Phone Bluetooth Status : OFF"
        }

        if (wifiManager.isWifiEnabled) {
            wifiSetting.text = "Phone Wifi Status : ON"
        } else {
            wifiSetting.text = "Phone Wifi Status : OFF"
        }


        if (sharedPreferences.contains(Constants.RESPECK_STATUS)) {
            respeckStatus.text = sharedPreferences.getString(Constants.RESPECK_STATUS, "")
            if (respeckStatus.text.equals("Connected")) {
                respeckStatus.setTextColor(resources.getColor(R.color.green))
            } else {
                respeckStatus.setTextColor(resources.getColor(R.color.red))
            }
        } else {
            respeckStatus.text = "Disconnected"
        }

        refreshButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        displaySteps = findViewById(R.id.stepsTaken)
        displaySteps.text = sharedPreferences.getString(Constants.STEPS, "0")
    }

    private fun setupNavigation() {
        val bottomNavView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavView.selectedItemId = R.id.home

        bottomNavView.setOnNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.home -> {
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.profile -> {
                    startActivity(Intent(applicationContext, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
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

    private fun performOnboarding() {
        // check whether the onboarding screen should be shown
        val sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.PREF_USER_FIRST_TIME)) {
            isUserFirstTime = false
        }
        else {
            isUserFirstTime = true
            sharedPreferences.edit().putBoolean(Constants.PREF_USER_FIRST_TIME, false).apply()
            val introIntent = Intent(this, OnBoardingActivity::class.java)
            startActivity(introIntent)
        }
    }

    private fun setupPermissions() {

        Log.i("Permissions", "Bluetooth permission = " + bluetoothPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        else {
            bluetoothPermissionGranted = true
        }

        // location permission
        Log.i("Permissions", "Location permission = " + locationPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsForRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsForRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        else {
            locationPermissionGranted = true
        }

        // camera permission
        Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
        if (ActivityCompat.checkSelfPermission(applicationContext,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permissions", "Camera permission = " + cameraPermissionGranted)
            permissionsForRequest.add(Manifest.permission.CAMERA)
        }
        else {
            cameraPermissionGranted = true
        }

        if (permissionsForRequest.size >= 1) {
            ActivityCompat.requestPermissions(this,
                permissionsForRequest.toTypedArray(),
                Constants.REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun stepCounter(sharedPreferences: SharedPreferences) {
        var steps = sharedPreferences.getString(Constants.STEPS, "0")?.toInt()

        filter.addAction(Constants.ACTION_RESPECK_CONNECTED)
        filter.addAction(Constants.ACTION_RESPECK_DISCONNECTED)

        try {
            respeckLiveUpdateReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {

                    Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                    val action = intent.action

                    if (action == Constants.ACTION_RESPECK_LIVE_BROADCAST) {
                        val liveData = intent.getSerializableExtra(Constants.RESPECK_LIVE_DATA) as RESpeckLiveData
                        Log.d("Live", "onReceive: liveData = " + liveData)

                        // get all relevant intent contents
                        val x = liveData.accelX
                        val y = liveData.accelY
                        val z = liveData.accelZ

                        var magnitude = sqrt(x*x.toDouble() + y*y.toDouble() + z*z.toDouble())

                        if (steps != null) {
                            if ((previousMagnitude - magnitude) > 0.45) {
                                steps += 1

                            }
                        }

                        runOnUiThread {
                            displaySteps.text = steps.toString()
                            progress_circular.apply {
                                if (steps != null) {
                                    setProgressWithAnimation(steps.toFloat())
                                }
                            }

                        }


                        sharedPreferences.edit().putString(Constants.STEPS, displaySteps.text.toString()).apply()



                        previousMagnitude = magnitude


                    }
                }
            }


            // register receiver on another thread
            val handlerThreadRespeck = HandlerThread("bgThreadRespeckLive")
            handlerThreadRespeck.start()
            looperRespeck = handlerThreadRespeck.looper
            val handlerRespeck = Handler(looperRespeck)
            this.registerReceiver(respeckLiveUpdateReceiver, filterTestRespeck, null, handlerRespeck)

        } catch (ex : Exception) {
            Toast.makeText(this, "Waiting for Respeck to start broadcasting data.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBluetoothService(sharedPreferences: SharedPreferences) {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
            sharedPreferences.edit().putString(Constants.RESPECK_STATUS, "Disconnected").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "false").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false").apply()
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "LINK").apply()
            sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, "true").apply()
        }
        val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
        Log.i("debug", "isServiceRunning = " + isServiceRunning)

        // check sharedPreferences for an existing Respeck id

        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF) && (sharedPreferences.getString(Constants.LAST_SENSOR_USED,"").equals("Respeck"))) {
            Log.i(
                "sharedpref",
                "Already saw a respeckID, starting service and attempting to reconnect"
            )
            // launch service to reconnect
            // start the bluetooth service if it's not already running
            if (!isServiceRunning) {
                Log.i("service", "Starting BLT service")
                val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
                this.startService(simpleIntent)
                Toast.makeText(this, "Restarting connection with Respeck.", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().putString(Constants.RESPECK_STATUS, "Connected").apply()
                sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "true").apply()
                sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "true").apply()
                sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "RELINK").apply()
                sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, "false").apply()
            }
        } else {
            sharedPreferences.edit().putString(Constants.RESPECK_STATUS, "Disconnected").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "false").apply()
            sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false").apply()
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "LINK").apply()
            sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, "true").apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(respeckLiveUpdateReceiver)
        looperRespeck.quit()
        System.exit(0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS) {
            if(grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    when(permissionsForRequest[i]) {
                        Manifest.permission.ACCESS_COARSE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.ACCESS_FINE_LOCATION -> locationPermissionGranted = true
                        Manifest.permission.CAMERA -> cameraPermissionGranted = true
                        Manifest.permission.BLUETOOTH_CONNECT -> bluetoothPermissionGranted = true
                    }

                }
            }
        }

        // count how many permissions need granting
        var numberOfPermissionsUngranted = 0
        if (!locationPermissionGranted) numberOfPermissionsUngranted++
        if (!cameraPermissionGranted) numberOfPermissionsUngranted++
        if (!bluetoothPermissionGranted) numberOfPermissionsUngranted++

        // show a general message if we need multiple permissions
        if (numberOfPermissionsUngranted == 3) {
            val generalSnackbar = Snackbar
                .make(window.decorView.rootView, "Several permissions needed for app to work.", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS") {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .show()
        }
    }
}