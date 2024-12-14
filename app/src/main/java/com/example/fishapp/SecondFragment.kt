package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class SecondFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var addMarkerButton: Button
    private lateinit var deleteMarkerButton: Button
    private lateinit var database: DatabaseReference
    private var username: String? = null
    private val allMarkersData = mutableListOf<MarkerData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_second, container, false)

        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        username = arguments?.getString("username")
        if (username != null) {
            Toast.makeText(requireContext(), "Добро пожаловать, $username!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        mapView = view.findViewById(R.id.map)
        mapView.setMultiTouchControls(true)

        val startPoint = GeoPoint(55.7558, 37.6173) // Москва
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)

        addMarkerButton = view.findViewById(R.id.addMarkerButton)
        deleteMarkerButton = view.findViewById(R.id.deleteMarkerButton)

        database = FirebaseDatabase.getInstance().getReference("markers")

        loadMarkersFromFirebase()

        addMarkerButton.setOnClickListener {
            showAddMarkerDialog()
        }

        deleteMarkerButton.setOnClickListener {
            showDeleteMarkerDialog()
        }

        val profileButton = view.findViewById<ImageButton>(R.id.imageButton2)
        profileButton.setOnClickListener {
            val profileFragment = ProfileFragment().apply {
                arguments = Bundle().apply { putString("username", username) }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        val filterButton = view.findViewById<Button>(R.id.filterButton)
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        return view
    }

    private fun showAddMarkerDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Добавить метку")

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
        builder.setView(dialogView)

        val titleInput = dialogView.findViewById<EditText>(R.id.markerTitleInput)
        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massSpinner)

        val speciesOptions = arrayOf("Окунь", "Карась", "Щука")
        val massOptions = arrayOf("0-5кг", "5-10кг", "10+кг")

        val speciesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, speciesOptions)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, massOptions)
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        massSpinner.adapter = massAdapter

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val markerTitle = titleInput.text.toString().trim()
            val selectedSpecies = speciesSpinner.selectedItem.toString()
            val selectedMass = massSpinner.selectedItem.toString()

            if (markerTitle.isNotEmpty()) {
                addMarkerAtCenter(markerTitle, selectedSpecies, selectedMass)
            } else {
                Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(requireContext(), "Метка сохранена", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Ошибка сохранения метки", Toast.LENGTH_SHORT).show()
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
                displayMarkers(allMarkersData)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Ошибка загрузки данных: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayMarkers(markers: List<MarkerData>) {
        mapView.overlays.removeIf { it is Marker }

        for (markerData in markers) {
            addMarkerToMap(markerData)
        }
        mapView.invalidate()
    }

    private fun addMarkerToMap(markerData: MarkerData) {
        val geoPoint = GeoPoint(markerData.latitude, markerData.longitude)
        val marker = Marker(mapView).apply {
            position = geoPoint
            title = markerData.title
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
    }

    private fun showMarkerInfoDialog(markerData: MarkerData) {
        val message = """
            Название: ${markerData.title}
            Вид рыбы: ${markerData.species}
            Масса улова: ${markerData.massRange}
            Добавил: ${markerData.username}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Информация о метке")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDeleteMarkerDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Выберите метку для удаления")

        val userMarkers = mapView.overlays
            .filterIsInstance<Marker>()
            .filter {
                val data = it.relatedObject as? MarkerData
                data?.username == username
            }

        if (userMarkers.isEmpty()) {
            Toast.makeText(requireContext(), "У вас нет собственных меток для удаления", Toast.LENGTH_SHORT).show()
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
        val markerToRemove = mapView.overlays.filterIsInstance<Marker>().find { it.title == title }
        if (markerToRemove != null) {
            val data = markerToRemove.relatedObject as? MarkerData

            if (data?.username == username) {
                mapView.overlays.remove(markerToRemove)
                mapView.invalidate()

                database.orderByChild("title").equalTo(title).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (markerSnapshot in snapshot.children) {
                            val dbMarkerData = markerSnapshot.getValue(MarkerData::class.java)

                            if (dbMarkerData?.username == username) {
                                markerSnapshot.ref.removeValue()
                                Toast.makeText(requireContext(), "Метка удалена", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Вы не можете удалить чужую метку", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Ошибка удаления: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(requireContext(), "Вы не можете удалить чужую метку", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Метка не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFilterDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Фильтр маркеров")

        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        builder.setView(dialogView)

        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesFilterSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massFilterSpinner)

        val speciesOptions = arrayOf("Все", "Окунь", "Карась", "Щука")
        val massOptions = arrayOf("Все", "0-5кг", "5-10кг", "10+кг")

        val speciesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, speciesOptions)
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, massOptions)
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
}
