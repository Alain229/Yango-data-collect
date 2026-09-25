package com.visionplus.yangocollector

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var txtLastApp: TextView
    private lateinit var txtServiceStatus: TextView
    private lateinit var txtYangoScreen: TextView
    private lateinit var txtOcrResult: TextView

    private var screenshotObserver: ContentObserver? = null
    private var lastProcessedUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtLastApp = findViewById(R.id.txtLastApp)
        txtServiceStatus = findViewById(R.id.txtServiceStatus)
        txtYangoScreen = findViewById(R.id.txtYangoScreen)
        txtOcrResult = findViewById(R.id.txtOcrResult)

        val button = findViewById<Button>(R.id.btnEnableAccessibility)
        button.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        val refreshButton = findViewById<Button>(R.id.btnRefresh)
        refreshButton.setOnClickListener {
            refreshData()
        }

        val enableOcrButton = findViewById<Button>(R.id.btnEnableOcr)
        enableOcrButton.setOnClickListener {
            requestImagePermissionAndStart()
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
        txtLastApp.text = "Derniere app detectee : $lastPackage"
        txtServiceStatus.text = "Statut service : $serviceStatus"
        txtYangoScreen.text = yangoScreen
        txtOcrResult.text = ocrText
    }

    private fun requestImagePermissionAndStart() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            startWatching()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 2001)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWatching()
        }
    }

    private fun startWatching() {
        if (screenshotObserver != null) return

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null) {
                    handleNewImage(uri)
                }
            }
        }
        screenshotObserver = observer
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_ocr_text", "Surveillance activee, en attente d'une capture...").apply()
        refreshData()
    }

    private fun handleNewImage(uri: Uri) {
        val uriString = uri.toString()
        if (uriString == lastProcessedUri) return
        lastProcessedUri = uriString

        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return

            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    saveOcrResult(visionText.text)
                }
                .addOnFailureListener { e ->
                    saveOcrResult("ERREUR OCR: ${e.message}")
                }
        } catch (e: Exception) {
            saveOcrResult("EXCEPTION: ${e.message}")
        }
    }

    private fun saveOcrResult(text: String) {
        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_ocr_text", text.take(2000)).apply()
        runOnUiThread { refreshData() }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let { contentResolver.unregisterContentObserver(it) }
    }
}
