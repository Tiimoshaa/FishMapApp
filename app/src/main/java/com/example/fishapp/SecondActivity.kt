package com.example.fishapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.Button


class SecondActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка osmdroid
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))

        // Установка разметки с картой
        setContentView(R.layout.activity_second)

        // Инициализация MapView
        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)

        // Установка начальной точки камеры
        val startPoint = GeoPoint(55.7558, 37.6173) // Москва
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)

        // Кнопка добавления метки
        val addMarkerButton = findViewById<Button>(R.id.addMarkerButton)
        addMarkerButton.setOnClickListener {
            val intent = Intent(this, AddMarkerActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val markerTitle = data?.getStringExtra("marker_title") ?: "Новая метка"
            val markerPosition = GeoPoint(55.7558, 37.6173) // Москва по умолчанию

            // Создание метки
            val marker = Marker(mapView)
            marker.position = markerPosition
            marker.title = markerTitle
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            mapView.overlays.add(marker)
            mapView.invalidate() // Обновление карты
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
