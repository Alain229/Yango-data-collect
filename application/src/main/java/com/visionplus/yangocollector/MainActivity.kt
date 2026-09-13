package com.visionplus.yangocollector

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var txtLastApp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtLastApp = findViewById(R.id.txtLastApp)

        val button = findViewById<Button>(R.id.btnEnableAccessibility)
        button.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("yango_collector_prefs", Context.MODE_PRIVATE)
        val lastPackage = prefs.getString("last_package", "(aucune)")
        txtLastApp.text = "Derniere app detectee : $lastPackage"
    }
}
