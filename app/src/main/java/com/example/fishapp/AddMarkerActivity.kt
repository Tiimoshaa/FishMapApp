package com.example.fishapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class AddMarkerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_marker)

        val markerNameInput = findViewById<EditText>(R.id.markerNameInput)
        val saveButton = findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val markerName = markerNameInput.text.toString()
            val resultIntent = Intent()
            resultIntent.putExtra("marker_title", markerName)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
