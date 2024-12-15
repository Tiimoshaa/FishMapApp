package com.example.fishapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.fishapp.databinding.FragmentDeleteMarkerBinding
import com.google.firebase.database.*

class DeleteMarkerFragment : Fragment() {

    private val args: DeleteMarkerFragmentArgs by navArgs()
    private lateinit var database: DatabaseReference
    private lateinit var binding: FragmentDeleteMarkerBinding

    // Список маркеров пользователя
    private val userMarkers = mutableListOf<MarkerData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentDeleteMarkerBinding.inflate(inflater, container, false)
        val view = binding.root

        val username = args.username

        // Инициализация базы данных Firebase
        database = FirebaseDatabase.getInstance().getReference("markers")

        // Загрузка маркеров пользователя
        loadUserMarkers(username)

        // Обработчик кнопки удаления маркера
        binding.deleteButton.setOnClickListener {
            val selectedPosition = binding.markerSpinner.selectedItemPosition
            if (selectedPosition != AdapterView.INVALID_POSITION) {
                val selectedMarker = userMarkers[selectedPosition]
                confirmAndDeleteMarker(selectedMarker)
            } else {
                Toast.makeText(requireContext(), "Пожалуйста, выберите маркер для удаления.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadUserMarkers(username: String) {
        // Запрос маркеров, добавленных текущим пользователем
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userMarkers.clear()
                    for (markerSnapshot in snapshot.children) {
                        val markerData = markerSnapshot.getValue(MarkerData::class.java)
                        if (markerData != null) {
                            userMarkers.add(markerData.copy(id = markerSnapshot.key))
                        }
                    }
                    populateSpinner()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка загрузки маркеров: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun populateSpinner() {
        if (userMarkers.isEmpty()) {
            Toast.makeText(requireContext(), "У вас нет маркеров для удаления.", Toast.LENGTH_SHORT).show()
            binding.deleteButton.isEnabled = false
            binding.markerSpinner.adapter = null
            return
        }

        val markerTitles = userMarkers.map { it.title }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, markerTitles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.markerSpinner.adapter = adapter

        // Включаем кнопку удаления, если есть маркеры
        binding.deleteButton.isEnabled = true
    }

    private fun confirmAndDeleteMarker(marker: MarkerData) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление маркера")
            .setMessage("Вы уверены, что хотите удалить маркер \"${marker.title}\"?")
            .setPositiveButton("Да") { dialog, _ ->
                dialog.dismiss()
                deleteMarker(marker)
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteMarker(marker: MarkerData) {
        if (marker.id == null) {
            Toast.makeText(requireContext(), "Невозможно удалить маркер: отсутствует идентификатор.", Toast.LENGTH_SHORT).show()
            return
        }

        database.child(marker.id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Метка \"${marker.title}\" успешно удалена.", Toast.LENGTH_SHORT).show()
                // Обновляем список маркеров после удаления
                val username = args.username
                loadUserMarkers(username)
            }
            .addOnFailureListener { error ->
                Toast.makeText(requireContext(), "Ошибка удаления маркера: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем binding, чтобы избежать утечек памяти
    }
}
