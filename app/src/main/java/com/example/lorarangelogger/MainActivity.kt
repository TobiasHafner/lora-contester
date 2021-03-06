package com.example.lorarangelogger

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.lorarangelogger.ui.main.MainFragment

//private const val TAG = "LoRaActivity"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}