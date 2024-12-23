package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.database.*
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class SecondFragment : Fragment() {

    private val args: SecondFragmentArgs by navArgs()

    private lateinit var mapView: MapView
    private lateinit var addMarkerButton: Button
    private lateinit var deleteMarkerButton: Button
    private lateinit var profileButton: ImageButton
    private lateinit var filterButton: Button

    private lateinit var database: DatabaseReference
    private val allMarkersData = mutableListOf<MarkerData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_second, container, false)

        // Инициализация OSM
        Configuration.getInstance().load(
            requireContext(),
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        )

        val username = args.username
        Toast.makeText(requireContext(), "Добро пожаловать, $username!", Toast.LENGTH_SHORT).show()

        // Настраиваем карту
        mapView = view.findViewById(R.id.map)
        mapView.setMultiTouchControls(true)
        val startPoint = GeoPoint(55.7558, 37.6173)
        mapView.controller.setZoom(10.0)
        mapView.controller.setCenter(startPoint)

        // Кнопки
        addMarkerButton = view.findViewById(R.id.addMarkerButton)
        deleteMarkerButton = view.findViewById(R.id.deleteMarkerButton)
        profileButton = view.findViewById(R.id.imageButton2)
        filterButton = view.findViewById(R.id.filterButton)

        // Ссылка на Firebase DB
        database = FirebaseDatabase.getInstance().getReference("markers")

        // Загружаем существующие метки из Firebase
        loadMarkersFromFirebase()

        // Слушатели
        addMarkerButton.setOnClickListener {
            showAddMarkerDialog(username)
        }
        deleteMarkerButton.setOnClickListener {
            // Диалог с Spinner для выбора метки
            showDeleteMarkerDialog(username)
        }
        profileButton.setOnClickListener {
            val action = SecondFragmentDirections.actionSecondFragmentToProfileFragment(username)
            findNavController().navigate(action)
        }
        filterButton.setOnClickListener {
            showFilterDialog()
        }

        return view
    }

    // ------------------------------------------------
    //  1) Диалог "Добавить метку"
    // ------------------------------------------------
    private fun showAddMarkerDialog(username: String) {
        // Подключаем кастомную тему (см. styles.xml)
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)

        builder.setTitle("Добавить метку")

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_marker, null)
        builder.setView(dialogView)

        val titleInput = dialogView.findViewById<EditText>(R.id.markerTitleInput)
        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massSpinner)

        val speciesOptions = arrayOf("Окунь", "Карась", "Щука")
        val massOptions = arrayOf("0-5кг", "5-10кг", "10+кг")

        // Адаптеры
        val speciesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speciesOptions
        )
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            massOptions
        )
        massAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        massSpinner.adapter = massAdapter

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val markerTitle = titleInput.text.toString().trim()
            val selectedSpecies = speciesSpinner.selectedItem.toString()
            val selectedMass = massSpinner.selectedItem.toString()

            if (markerTitle.isNotEmpty()) {
                addMarkerAtCenter(markerTitle, selectedSpecies, selectedMass, username)
            } else {
                Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        // Скруглённый фон диалога
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.show()
    }

    private fun addMarkerAtCenter(title: String, species: String, massRange: String, username: String) {
        val centerPoint = mapView.mapCenter as GeoPoint

        val markerData = MarkerData(
            title = title,
            latitude = centerPoint.latitude,
            longitude = centerPoint.longitude,
            username = username,
            species = species,
            massRange = massRange
        )

        val marker = Marker(mapView).apply {
            position = centerPoint
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.markericon)
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

    // ------------------------------------------------
    //  2) Диалог "Удалить метку" с Spinner
    // ------------------------------------------------
    private fun showDeleteMarkerDialog(username: String) {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)

        builder.setTitle("Удалить метку")

        // layout с Spinner (dialog_delete_marker.xml)
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_marker, null)
        builder.setView(dialogView)

        // Находим Spinner
        val markerSpinner = dialogView.findViewById<Spinner>(R.id.markerSpinnerToDelete)

        // Список меток, которые принадлежат текущему пользователю
        val userMarkers = allMarkersData.filter { it.username == username }
        val markerTitles = userMarkers.map { it.title }.toTypedArray()

        // Заполняем Spinner
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            markerTitles
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        markerSpinner.adapter = spinnerAdapter

        builder.setPositiveButton("Удалить") { dialog, _ ->
            val titleToDelete = markerSpinner.selectedItem?.toString()
            if (!titleToDelete.isNullOrEmpty()) {
                deleteMarker(titleToDelete, username)
            } else {
                Toast.makeText(requireContext(), "Нет доступных меток для удаления", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        // Скруглённый фон
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.show()
    }

    private fun deleteMarker(title: String, username: String) {
        // Удаляем с карты (если есть)
        val markerToRemove = mapView.overlays
            .filterIsInstance<Marker>()
            .find { it.title == title }

        if (markerToRemove != null) {
            val data = markerToRemove.relatedObject as? MarkerData
            if (data?.username == username) {
                mapView.overlays.remove(markerToRemove)
                mapView.invalidate()
            } else {
                Toast.makeText(requireContext(), "Вы не можете удалить чужую метку", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Удаляем из Firebase
        database.orderByChild("title").equalTo(title)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var foundAny = false
                    for (markerSnapshot in snapshot.children) {
                        val dbMarkerData = markerSnapshot.getValue(MarkerData::class.java)
                        if (dbMarkerData?.username == username) {
                            markerSnapshot.ref.removeValue()
                            foundAny = true
                        }
                    }
                    if (foundAny) {
                        Toast.makeText(requireContext(), "Метка удалена", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Метка не найдена или чужая", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка удаления: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ------------------------------------------------
    //   3) Загрузка всех меток + отображение
    // ------------------------------------------------
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
                Toast.makeText(requireContext(), "Ошибка загрузки: ${error.message}", Toast.LENGTH_SHORT).show()
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
    }

    private fun showMarkerInfoDialog(markerData: MarkerData) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_marker_info, null)
        val dialogTitleTextView = dialogView.findViewById<TextView>(R.id.dialogTitleTextView)
        val markerTitleTextView = dialogView.findViewById<TextView>(R.id.markerTitleTextView)
        val markerSpeciesTextView = dialogView.findViewById<TextView>(R.id.markerSpeciesTextView)
        val markerMassTextView = dialogView.findViewById<TextView>(R.id.markerMassTextView)
        val markerUsernameTextView = dialogView.findViewById<TextView>(R.id.markerUsernameTextView)
        val closeDialogButton = dialogView.findViewById<Button>(R.id.closeDialogButton)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val message = """
            Название: ${markerData.title}
            Вид рыбы: ${markerData.species}
            Масса улова: ${markerData.massRange}
            Добавил: ${markerData.username}
        """.trimIndent()

        // Обработка нажатия на кнопку закрытия
        closeDialogButton.setOnClickListener {
            dialog.dismiss()
        }

        // Отображение диалога
        dialog.show()
    }

    // ------------------------------------------------
    //   4) Диалог "Фильтр"
    // ------------------------------------------------
    private fun showFilterDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)

        builder.setTitle("Фильтр маркеров")

        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        builder.setView(dialogView)

        val speciesSpinner = dialogView.findViewById<Spinner>(R.id.speciesFilterSpinner)
        val massSpinner = dialogView.findViewById<Spinner>(R.id.massFilterSpinner)

        val speciesOptions = arrayOf("Все", "Окунь", "Карась", "Щука")
        val massOptions = arrayOf("Все", "0-5кг", "5-10кг", "10+кг")

        val speciesAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            speciesOptions
        )
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        speciesSpinner.adapter = speciesAdapter

        val massAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            massOptions
        )
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

        // Скруглённый фон
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.show()
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
