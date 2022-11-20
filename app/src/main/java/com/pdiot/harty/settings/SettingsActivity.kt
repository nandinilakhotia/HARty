package com.pdiot.harty.settings

import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pdiot.harty.MainActivity
import com.pdiot.harty.profile.ProfileActivity
import com.pdiot.harty.R
import com.pdiot.harty.utils.Constants
import com.pdiot.harty.utils.Utils
import java.util.*

class SettingsActivity : AppCompatActivity() {

    // Respeck
    val REQUEST_CODE_SCAN_RESPECK = 0

    // Respeck
    private lateinit var scanRespeckButton: Button
    private lateinit var respeckID: EditText
    private lateinit var respeckStatus : TextView
    private lateinit var connectRespeckButton: Button
    private lateinit var disconnectRespeckButton: Button

    lateinit var sharedPreferences: SharedPreferences

    var nfcAdapter: NfcAdapter? = null
    val MIME_TEXT_PLAIN = "application/vnd.bluetooth.le.oob"
    private val TAG = "NFCReader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        scanRespeckButton = findViewById(R.id.scan_respeck)
        respeckID = findViewById(R.id.respeck_code)
        respeckStatus = findViewById(R.id.status_respeck)
        connectRespeckButton = findViewById(R.id.connect_respeck_button)
        disconnectRespeckButton = findViewById(R.id.disconnect_respeck_button)


        sharedPreferences = getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)

        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!mBluetoothAdapter.isEnabled) {
                sharedPreferences.edit().putString(Constants.RESPECK_STATUS, "Disconnected").apply()
                sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, "false")
                    .apply()
                sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false")
                    .apply()
                sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "LINK")
                    .apply()
                sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, "true").apply()
        }

        restoreUIState(sharedPreferences)
        setUpNavigation()

        scanRespeckButton.setOnClickListener {
            val barcodeScanner = Intent(this, BarcodeActivity::class.java)
            startActivityForResult(barcodeScanner, REQUEST_CODE_SCAN_RESPECK)
        }

        connectRespeckButton.setOnClickListener {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter.isEnabled) {
                sharedPreferences.edit().putString(Constants.RESPECK_MAC_ADDRESS_PREF, respeckID.text.toString()).apply()
                sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()
                startSpeckService()

                connectionUpdate("Connected", true, true, "Relink", false, "Respeck")
            } else {
                Toast.makeText(this, "Please enable bluetooth to continue.", Toast.LENGTH_LONG).show()
            }
        }

        disconnectRespeckButton.setOnClickListener {
            stopSpeckService()
            connectionUpdate("Disconnected", false, false, "Link", true, "None")

        }

        // first read shared preferences to see if there was a respeck there already
        if (sharedPreferences.contains(Constants.RESPECK_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a respeckID")
            respeckID.setText(
                sharedPreferences.getString(
                    Constants.RESPECK_MAC_ADDRESS_PREF,
                    ""
                )
            )

            connectRespeckButton.isEnabled = true
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_ENABLED, connectRespeckButton.isEnabled.toString()).apply()
            connectRespeckButton.isClickable = true
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_CLICKABLE, connectRespeckButton.isClickable.toString()).apply()
        } else {
            connectRespeckButton.isEnabled = false
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_ENABLED, connectRespeckButton.isEnabled.toString()).apply()
            connectRespeckButton.isClickable = false
            sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_CLICKABLE, connectRespeckButton.isClickable.toString()).apply()
        }

        respeckID.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
                if (cs.toString().trim().length != 17) {
                    connectRespeckButton.isEnabled = false
                    sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_ENABLED, connectRespeckButton.isEnabled.toString()).apply()
                    connectRespeckButton.isClickable = false
                    sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_CLICKABLE, connectRespeckButton.isClickable.toString()).apply()
                    disconnectRespeckButton.isEnabled = false
                    sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, disconnectRespeckButton.isEnabled.toString()).apply()
                    disconnectRespeckButton.isClickable = false
                    sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, disconnectRespeckButton.isClickable.toString()).apply()
                } else {
                    connectRespeckButton.isEnabled = true
                    sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_ENABLED, connectRespeckButton.isEnabled.toString()).apply()
                    connectRespeckButton.isClickable = true
                    sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_CLICKABLE, connectRespeckButton.isClickable.toString()).apply()
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        respeckID.filters = arrayOf<InputFilter>(InputFilter.AllCaps())
        val nfcManager = getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter

        if (nfcAdapter == null) {
            Toast.makeText(this, "Phone does not support NFC pairing", Toast.LENGTH_LONG).show()
        } else if (nfcAdapter!!.isEnabled()) {
            Toast.makeText(this, "NFC Enabled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "NFC Disabled", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectionUpdate(statusText : String, disconnectEnabled : Boolean, disconnectClicked : Boolean, buttonText : String,
    idEnabled : Boolean, sensorUpdate : String) {
        respeckStatus.text = statusText
        sharedPreferences.edit().putString(Constants.RESPECK_STATUS, respeckStatus.text.toString()).apply()

        if (statusText.equals("Connected")) {
            respeckStatus.setTextColor(resources.getColor(R.color.green))
        } else {
            respeckStatus.setTextColor(resources.getColor(R.color.red))
        }
        disconnectRespeckButton.isEnabled = disconnectEnabled
        sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_ENABLED, disconnectRespeckButton.isEnabled.toString()).apply()

        disconnectRespeckButton.isClickable = disconnectClicked
        sharedPreferences.edit().putString(Constants.DISCONNECT_RESPECK_CLICKABLE, disconnectRespeckButton.isClickable.toString()).apply()

        connectRespeckButton.text = buttonText
        sharedPreferences.edit().putString(Constants.CONNECT_RESPECK_BUTTON_TEXT, connectRespeckButton.text.toString()).apply()

        respeckID.isEnabled = idEnabled
        sharedPreferences.edit().putString(Constants.RESPECK_ID_ENABLED, respeckID.isEnabled.toString()).apply()

        scanRespeckButton.isClickable = idEnabled
        scanRespeckButton.isEnabled = idEnabled

        sharedPreferences.edit().putString(Constants.LAST_SENSOR_USED, sensorUpdate).apply()
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
                    startActivity(Intent(applicationContext, ProfileActivity::class.java))
                    overridePendingTransition(0,0)
                    return@setOnNavigationItemSelectedListener true
                }
                R.id.settings -> {
                    return@setOnNavigationItemSelectedListener true
                }
            }
            true
        }
    }

    private fun restoreUIState(@NonNull sharedPreferences: SharedPreferences) {

        //Obtain Respeck Status Text
        if (sharedPreferences.contains(Constants.RESPECK_STATUS)) {
            respeckStatus.text = sharedPreferences.getString(Constants.RESPECK_STATUS, "")
            if (respeckStatus.text.equals("Connected")) {
                respeckStatus.setTextColor(resources.getColor(R.color.green))
            }
        } else {
            respeckStatus.text = "Disconnected"
        }

        if (sharedPreferences.contains(Constants.DISCONNECT_RESPECK_ENABLED)) {
            disconnectRespeckButton.isEnabled = sharedPreferences.getString(Constants.DISCONNECT_RESPECK_ENABLED, "false")?.toBoolean() ?: false
        } else {
            disconnectRespeckButton.isEnabled = false
        }

        if (sharedPreferences.contains(Constants.DISCONNECT_RESPECK_CLICKABLE)) {
            disconnectRespeckButton.isClickable = sharedPreferences.getString(Constants.DISCONNECT_RESPECK_CLICKABLE, "false")?.toBoolean() ?: false
        } else {
            disconnectRespeckButton.isClickable = false
        }

        if (sharedPreferences.contains(Constants.CONNECT_RESPECK_ENABLED)) {
            connectRespeckButton.isEnabled = sharedPreferences.getString(Constants.CONNECT_RESPECK_ENABLED, "false")?.toBoolean() ?: false
        } else {
            connectRespeckButton.isEnabled = false
        }

        if (sharedPreferences.contains(Constants.CONNECT_RESPECK_CLICKABLE)) {
            connectRespeckButton.isClickable = sharedPreferences.getString(Constants.CONNECT_RESPECK_CLICKABLE, "false")?.toBoolean() ?: false
        } else {
            connectRespeckButton.isClickable = false
        }

        if (sharedPreferences.contains(Constants.CONNECT_RESPECK_BUTTON_TEXT)) {
            connectRespeckButton.text = sharedPreferences.getString(Constants.CONNECT_RESPECK_BUTTON_TEXT, "")
        } else {
            connectRespeckButton.text = "LINK"
        }

        if (sharedPreferences.contains(Constants.RESPECK_ID_ENABLED)) {
            respeckID.isEnabled = sharedPreferences.getString(Constants.RESPECK_ID_ENABLED, "false")?.toBoolean() ?: false
            scanRespeckButton.isClickable = sharedPreferences.getString(Constants.RESPECK_ID_ENABLED, "false")?.toBoolean() ?: false
            scanRespeckButton.isEnabled = sharedPreferences.getString(Constants.RESPECK_ID_ENABLED, "false")?.toBoolean() ?: false
        } else {
            respeckID.isEnabled = true
            scanRespeckButton.isClickable = true
            scanRespeckButton.isEnabled = true
        }
    }

    fun startSpeckService() : Boolean {
        try {
                // TODO if it's not already running
                val isServiceRunning = Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
                Log.i("service", "isServiceRunning = " + isServiceRunning)
                if (!isServiceRunning) {
                    Log.i("service", "Starting BLT service")
                    Toast.makeText(this, "Initiating connection.", Toast.LENGTH_SHORT).show()
                    val simpleIntent = Intent(this, BluetoothSpeckService::class.java)
                    this.startService(simpleIntent)
                } else {
                    Log.i("service", "Service already running, restart")
                    this.stopService(Intent(this, BluetoothSpeckService::class.java))
                    Toast.makeText(this, "Restarting connection.", Toast.LENGTH_SHORT).show()
                    this.startService(Intent(this, BluetoothSpeckService::class.java))
                }
                return isServiceRunning
            } catch (ex : Exception) {
                Toast.makeText(this, "Unexpected error, please try again.", Toast.LENGTH_SHORT).show()
                return false
            }
    }

    fun stopSpeckService() : Boolean {
        try {
            val isServiceRunning =
                Utils.isServiceRunning(BluetoothSpeckService::class.java, applicationContext)
            Toast.makeText(this, "Disconnecting...", Toast.LENGTH_SHORT).show()
            this.stopService(Intent(this, BluetoothSpeckService::class.java))
            return isServiceRunning
        } catch (ex : Exception) {
            Toast.makeText(this, "Unexpected error, please try again.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    override fun onResume() {
        super.onResume()

        if (nfcAdapter != null) {
            setupForegroundDispatch(this, nfcAdapter!!)
        }
    }

    /**
     * @param activity The corresponding [Activity] requesting the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
        Log.d(TAG, "setupForegroundDispatch: here ")
        val intent = Intent(activity.applicationContext, activity.javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent = PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)

        val filters = arrayOfNulls<IntentFilter>(2)
        val techList = arrayOf(
            arrayOf(
                NfcF::class.java.name
            )
        )

        // Notice that this is the same filter as in our manifest.
        filters[0] = IntentFilter()
        filters[0]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        filters[0]!!.addCategory(Intent.CATEGORY_DEFAULT)

        filters[1] = IntentFilter()
        filters[1]!!.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            filters[0]!!.addDataType(MIME_TEXT_PLAIN)
            filters[1]!!.addDataScheme("vnd.android.nfc")
            filters[1]!!.addDataAuthority("ext", null)
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("Check your mime type.")
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG, "onNewIntent: here")
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d(TAG, "handleIntent: here")
        val action = intent?.action

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            val type = intent.type

            Log.d(TAG, "handleIntent: type = " + type)

            if (MIME_TEXT_PLAIN.equals(type)) {
                // This is the Respeck
                val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)

                val ndef = Ndef.get(tag)

                if (ndef == null) {
                    // NDEF is not supported by this Tag
                    return
                }

                val ndefMessage = ndef.cachedNdefMessage
                val records = ndefMessage.records

                Log.i("NFCReader", "Read records")
                Log.i("NFCReader", "Found " + records.size + " record(s)")
                Log.i("NFCReader", records[0].toMimeType())

                val payload = records[0].payload
                Log.i("NFCReader", "Payload length: " + payload.size)

                val payload_str = String(payload)
                Log.i("NFCReader", "Payload : $payload_str")

                val ble_name = payload_str.substring(20)

                Log.i("NFCReader", "BLE name: $ble_name")
                val ble_addr: String = Utils.bytesToHexNfc(Arrays.copyOfRange(payload, 5, 11))
                Log.i("NFCReader", "BLE Address: $ble_addr")

                Toast.makeText(this, "NFC scanned $ble_name ($ble_addr)", Toast.LENGTH_LONG).show()

//                if (!ble_addr.contains(':')) {
//                    // insert a : after each two characters
//                }

                respeckID.setText(ble_addr.toString())

            }
        }
    }

    /**
     * @param activity The corresponding [BaseActivity] requesting to stop the foreground dispatch.
     * @param adapter The [NfcAdapter] used for the foreground dispatch.
     */
    fun stopForegroundDispatch(activity: Activity?, adapter: NfcAdapter) {
        adapter.disableForegroundDispatch(activity)
    }

    override fun onPause() {

        if(nfcAdapter != null) {
            stopForegroundDispatch(this, nfcAdapter!!)
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            var scanResult = data?.extras?.getString("ScanResult")

            if (scanResult != null) {
                Log.i("ble", "Scan result=" + scanResult)

                if (scanResult.contains(":")) {
                    // this is a respeck V6 and we should store its MAC address
                    respeckID.setText(scanResult)
                    sharedPreferences.edit().putString(
                        Constants.RESPECK_MAC_ADDRESS_PREF,
                        scanResult.toString()
                    ).apply()
                    sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 6).apply()

                }
                if (!scanResult.contains(":") && !scanResult.contains("-")) {
                    val sb = StringBuilder(scanResult)
                    if (scanResult.length == 20)
                        sb.insert(4, "-")
                    else if (scanResult.length == 16)
                        sb.insert(0, "0105-")
                    scanResult = sb.toString()

                    Log.i("Debug", "Scan result = " + scanResult)
                    respeckID.setText(scanResult)
                    sharedPreferences.edit().putString(
                        Constants.RESPECK_MAC_ADDRESS_PREF,
                        scanResult
                    ).apply()
                    sharedPreferences.edit().putInt(Constants.RESPECK_VERSION, 5).apply()
                }

                connectRespeckButton.isEnabled = true
                connectRespeckButton.isClickable = true

            } else {
                respeckID.setText("No respeck found :(")
                Toast.makeText(this, "No Respeck QR code found", Toast.LENGTH_LONG).show()
            }
        }
    }
}