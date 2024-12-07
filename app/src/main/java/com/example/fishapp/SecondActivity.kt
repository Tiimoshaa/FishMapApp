package com.example.fishapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class SecondActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_second)


        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)


        val startPoint = GeoPoint(55.7558, 37.6173) // TODO: Поставил старт на москву, мб надо надо сделать запоминание ласт активности
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)


        val marker = Marker(mapView)
        marker.position = startPoint
        marker.title = "Москва"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker) // TODO: Метки надо крепить по гео, + скидывать их в firebase

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
