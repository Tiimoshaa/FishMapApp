package com.example.fishapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.widget.Spinner




class SecondActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var addMarkerButton: Button
    private lateinit var deleteMarkerButton: Button
    private lateinit var database: DatabaseReference
    private var username: String? = null
    private val allMarkersData = mutableListOf<MarkerData>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Настройка osmdroid
        Configuration.getInstance().load(applicationContext, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this))

        // Установка разметки с картой
        setContentView(R.layout.activity_second)

        // Извлечение имени пользователя
        username = intent.getStringExtra("username")
        if (username != null) {
            Toast.makeText(this, "Добро пожаловать, $username!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        // Инициализация MapView
        mapView = findViewById(R.id.map)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(55.7558, 37.6173) // Москва
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)

        // Настройка кнопок
        addMarkerButton = findViewById(R.id.addMarkerButton)
        deleteMarkerButton = findViewById(R.id.deleteMarkerButton)

        // Настройка Firebase Database
        database = FirebaseDatabase.getInstance().getReference("markers")

        // Загрузка маркеров из Firebase
        loadMarkersFromFirebase()

        // Добавление нового маркера
        addMarkerButton.setOnClickListener {
            showAddMarkerDialog()
        }

        // Удаление маркера
        deleteMarkerButton.setOnClickListener {
            showDeleteMarkerDialog()
        }

        // Кнопка перехода в профиль
        val profileButton = findViewById<ImageButton>(R.id.imageButton2)
        profileButton.setOnClickListener {
            if (username != null) {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("username", username) // Передаем имя пользователя в ProfileActivity
                startActivity(intent)
            } else {
                Toast.makeText(this, "Ошибка: пользователь не найден!", Toast.LENGTH_SHORT).show()
            }
        }

        val filterButton = findViewById<Button>(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()
        }

    }

    private fun showAddMarkerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Добавить метку")

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
        builder.setView(dialogView)

        val titleInput = dialogView.findViewById<EditText>(R.id.markerTitleInput)
        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massSpinner)

        // Задаём списки для спиннеров
        val speciesOptions = arrayOf("Окунь", "Карась", "Щука")
        val massOptions = arrayOf("0-5кг", "5-10кг", "10+кг")

        val speciesAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesOptions)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, massOptions)
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        massSpinner.adapter = massAdapter

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val markerTitle = titleInput.text.toString().trim()
            val selectedSpecies = speciesSpinner.selectedItem.toString()
            val selectedMass = massSpinner.selectedItem.toString()

            if (markerTitle.isNotEmpty()) {
                addMarkerAtCenter(markerTitle, selectedSpecies, selectedMass)
            } else {
                Toast.makeText(this, "Название не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }


    private fun addMarkerAtCenter(title: String, species: String, massRange: String) {
        val centerPoint = mapView.mapCenter as GeoPoint

        val markerData = MarkerData(
            title = title,
            latitude = centerPoint.latitude,
            longitude = centerPoint.longitude,
            username = username ?: "",
            species = species,
            massRange = massRange
        )

        val marker = Marker(mapView).apply {
            position = centerPoint
            this.title = markerData.title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setRelatedObject(markerData)

            setOnMarkerClickListener { clickedMarker, _ ->
                val data = clickedMarker.relatedObject as? MarkerData
                if (data != null) {
                    showMarkerInfoDialog(data)
                }
                true
            }
        }

        mapView.overlays.add(marker)
        mapView.invalidate()

        saveMarkerToFirebase(markerData)
    }


    private fun saveMarkerToFirebase(markerData: MarkerData) {
        val markerId = database.push().key
        if (markerId != null) {
            database.child(markerId).setValue(markerData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Метка сохранена", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Ошибка сохранения метки", Toast.LENGTH_SHORT).show()
                }
        }
    }



    private fun loadMarkersFromFirebase() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allMarkersData.clear()
                for (markerSnapshot in snapshot.children) {
                    val markerData = markerSnapshot.getValue(MarkerData::class.java)
                    if (markerData != null) {
                        allMarkersData.add(markerData)
                    }
                }
                // После загрузки всех маркеров отображаем их
                displayMarkers(allMarkersData)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SecondActivity, "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showFilterDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Фильтр маркеров")

        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        builder.setView(dialogView)

        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesFilterSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massFilterSpinner)

        val speciesOptions = arrayOf("Все", "Окунь", "Карась", "Щука")
        val massOptions = arrayOf("Все", "0-5кг", "5-10кг", "10+кг")

        val speciesAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, speciesOptions)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, massOptions)
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        massSpinner.adapter = massAdapter

        builder.setPositiveButton("Применить") { dialog, _ ->
            val selectedSpecies = speciesSpinner.selectedItem.toString()
            val selectedMass = massSpinner.selectedItem.toString()

            applyFilter(selectedSpecies, selectedMass)
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun applyFilter(selectedSpecies: String, selectedMass: String) {
        val filteredMarkers = allMarkersData.filter { marker ->
            val speciesMatch = (selectedSpecies == "Все" || marker.species == selectedSpecies)
            val massMatch = (selectedMass == "Все" || marker.massRange == selectedMass)
            speciesMatch && massMatch
        }

        displayMarkers(filteredMarkers)
    }


    private fun displayMarkers(markers: List<MarkerData>) {
        // Удаляем старые маркеры
        mapView.overlays.removeIf { it is Marker }

        // Добавляем новые маркеры
        for (markerData in markers) {
            addMarkerToMap(markerData)
        }
        mapView.invalidate()
    }


    private fun addMarkerToMap(markerData: MarkerData) {
        val geoPoint = GeoPoint(markerData.latitude, markerData.longitude)
        val marker = Marker(mapView).apply {
            position = geoPoint
            // Заголовок мы можем оставить так, чтобы он выглядел красиво,
            // либо просто хранить название и отдельно показывать кто добавил
            title = markerData.title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Сохраняем объект MarkerData в маркер, чтобы при нажатии на него было легко получить данные
            setRelatedObject(markerData)

            // Настраиваем обработчик клика по маркеру
            setOnMarkerClickListener { clickedMarker, _ ->
                val data = clickedMarker.relatedObject as? MarkerData
                if (data != null) {
                    showMarkerInfoDialog(data)
                }
                true // Возвращаем true, чтобы событие не передавалось дальше
            }
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    private fun showMarkerInfoDialog(markerData: MarkerData) {
        val message = """
        Название: ${markerData.title}
        Вид рыбы: ${markerData.species}
        Масса улова: ${markerData.massRange}
        Добавил: ${markerData.username}
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Информация о метке")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }


    private fun showDeleteMarkerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите метку для удаления")

        // Фильтруем маркеры, чтобы отображать только те, что принадлежат текущему пользователю
        val userMarkers = mapView.overlays
            .filterIsInstance<Marker>()
            .filter {
                val data = it.relatedObject as? MarkerData
                data?.username == username
            }

        // Если у пользователя нет своих маркеров, сообщаем об этом
        if (userMarkers.isEmpty()) {
            Toast.makeText(this, "У вас нет собственных меток для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        val markerTitles = userMarkers.map { it.title }.toTypedArray()

        builder.setItems(markerTitles) { dialog, which ->
            val selectedTitle = markerTitles[which]
            deleteMarker(selectedTitle)
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun deleteMarker(title: String) {
        // Находим маркер на карте
        val markerToRemove = mapView.overlays.filterIsInstance<Marker>().find { it.title == title }
        if (markerToRemove != null) {
            val data = markerToRemove.relatedObject as? MarkerData

            // Проверяем, принадлежит ли маркер текущему пользователю
            if (data?.username == username) {
                // Если да, сначала удаляем с карты
                mapView.overlays.remove(markerToRemove)
                mapView.invalidate()

                // Удаляем из Firebase
                database.orderByChild("title").equalTo(title).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (markerSnapshot in snapshot.children) {
                            val dbMarkerData = markerSnapshot.getValue(MarkerData::class.java)
                            // Дополнительная проверка для надежности
                            if (dbMarkerData?.username == username) {
                                markerSnapshot.ref.removeValue()
                                Toast.makeText(this@SecondActivity, "Метка удалена", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SecondActivity, "Вы не можете удалить чужую метку", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SecondActivity, "Ошибка удаления: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Вы не можете удалить чужую метку", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Метка не найдена", Toast.LENGTH_SHORT).show()
        }
    }
}
