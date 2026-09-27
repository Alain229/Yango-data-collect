package com.visionplus.yangocollector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ScreenshotForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "screenshot_watch_channel"
        const val NOTIFICATION_ID = 1001
    }

    private var screenshotObserver: ContentObserver? = null
    private var lastProcessedUri: String? = null

    override fun onCreate() {
        super.onCreate()
        creerCanalNotification()
        val notification = construireNotification("En attente d'une capture d'ecran...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        marquerStatutService("actif")
        demarrerSurveillance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Surveillance captures Yango Collector",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun construireNotification(texte: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yango Data Collector")
            .setContentText(texte)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun mettreAJourNotification(texte: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, construireNotification(texte))
    }

    private fun demarrerSurveillance() {
        if (screenshotObserver != null) return

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null) {
                    traiterNouvelleImage(uri)
                }
            }
        }
        screenshotObserver = observer
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    private fun traiterNouvelleImage(uri: Uri, tentative: Int = 0) {
        val uriString = uri.toString()
        if (tentative == 0) {
            if (uriString == lastProcessedUri) return
            lastProcessedUri = uriString
        }

        val maxTentatives = 5
        val delaiMs = 400L

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val projection = arrayOf(MediaStore.Images.Media.IS_PENDING)
                var enAttente = false
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Images.Media.IS_PENDING)
                        enAttente = idx >= 0 && cursor.getInt(idx) == 1
                    }
                }

                if (enAttente) {
                    if (tentative < maxTentatives) {
                        traiterNouvelleImage(uri, tentative + 1)
                    } else {
                        enregistrerResultatOcr("ECHEC: fichier reste en pending apres $maxTentatives tentatives")
                    }
                    return@postDelayed
                }

                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap == null) {
                    if (tentative < maxTentatives) {
                        traiterNouvelleImage(uri, tentative + 1)
                    } else {
                        enregistrerResultatOcr("ECHEC: image illisible apres $maxTentatives tentatives")
                    }
                    return@postDelayed
                }

                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        enregistrerResultatOcr(visionText.text)
                    }
                    .addOnFailureListener { e ->
                        enregistrerResultatOcr("ERREUR OCR: ${e.message}")
                    }

            } catch (e: SecurityException) {
                if (tentative < maxTentatives) {
                    traiterNouvelleImage(uri, tentative + 1)
                } else {
                    enregistrerResultatOcr("ECHEC apres $maxTentatives tentatives (SecurityException): ${e.message}")
                }
            } catch (e: Exception) {
                enregistrerResultatOcr("EXCEPTION: ${e.message}")
            }
        }, if (tentative == 0) delaiMs else delaiMs * tentative)
    }

    private fun enregistrerResultatOcr(texte: String) {
        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("last_ocr_text", texte.take(2000)).apply()

        val trip = TripParser.parse(texte)
        val db = TripDbHelper(this)
        db.insererTrip(trip)

        val resume = buildString {
            append("Distance : ${trip.distanceKm ?: "?"} km\n")
            append("Duree : ${trip.dureeMin ?: "?"} min\n")
            append("Revenu : ${trip.revenuFcfa ?: "?"} FCFA\n")
            append("Date : ${trip.dateCourse ?: "?"}\n")
            append("Passager : ${trip.nomPassager ?: "?"}\n")
            append("Adresses : ${trip.adressesBrutes.ifEmpty { "?" }}")
        }
        prefs.edit().putString("last_parsed_summary", resume).apply()

        val nbCourses = db.compterTrips()
        mettreAJourNotification("Courses enregistrees : $nbCourses")
    }

    private fun marquerStatutService(statut: String) {
        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("service_status", statut).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let { contentResolver.unregisterContentObserver(it) }
        marquerStatutService("arrete")
    }
}
