package com.visionplus.yangocollector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var txtLastApp: TextView
    private lateinit var txtServiceStatus: TextView
    private lateinit var txtYangoScreen: TextView
    private lateinit var txtOcrResult: TextView
    private lateinit var txtParsed: TextView
    private lateinit var txtTripCount: TextView

    private lateinit var dbHelper: TripDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = TripDbHelper(this)

        txtLastApp = findViewById(R.id.txtLastApp)
        txtServiceStatus = findViewById(R.id.txtServiceStatus)
        txtYangoScreen = findViewById(R.id.txtYangoScreen)
        txtOcrResult = findViewById(R.id.txtOcrResult)
        txtParsed = findViewById(R.id.txtParsed)
        txtTripCount = findViewById(R.id.txtTripCount)

        findViewById<Button>(R.id.btnEnableAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refreshData()
        }

        findViewById<Button>(R.id.btnEnableOcr).setOnClickListener {
            demarrerServiceSurveillance()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        val lastPackage = prefs.getString("last_package", "(aucune)")
        val serviceStatus = prefs.getString("service_status", "(inconnu)")
        val yangoScreen = prefs.getString("yango_screen_text", "(aucun)")
        val ocrText = prefs.getString("last_ocr_text", "(aucun)")
        val parsedSummary = prefs.getString("last_parsed_summary", "(aucune)")

        txtLastApp.text = "Derniere app detectee : $lastPackage"
        txtServiceStatus.text = "Statut service : $serviceStatus"
        txtYangoScreen.text = yangoScreen
        txtOcrResult.text = ocrText
        txtParsed.text = parsedSummary
        txtTripCount.text = "Courses enregistrees : ${dbHelper.compterTrips()}"
    }

    private fun demarrerServiceSurveillance() {
        val permissionImages = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val permissionsManquantes = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, permissionImages) != PackageManager.PERMISSION_GRANTED) {
            permissionsManquantes.add(permissionImages)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsManquantes.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsManquantes.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsManquantes.toTypedArray(), 2001)
        } else {
            lancerService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001) {
            val permissionImages = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (ContextCompat.checkSelfPermission(this, permissionImages) == PackageManager.PERMISSION_GRANTED) {
                lancerService()
            }
        }
    }

    private fun lancerService() {
        val intent = Intent(this, ScreenshotForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }
}
