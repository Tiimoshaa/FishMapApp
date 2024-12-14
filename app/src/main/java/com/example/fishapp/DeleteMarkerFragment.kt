package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class DeleteMarkerFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private var username: String? = null
    private val userMarkers = mutableListOf<MarkerData>() // Список меток текущего пользователя

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delete_marker, container, false)

        val markersListView = view.findViewById<ListView>(R.id.markersListView)

        username = arguments?.getString("username")

        if (username != null) {
            database = FirebaseDatabase.getInstance().getReference("markers")
            loadUserMarkers { markers ->
                // Адаптер для отображения списка меток
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    markers.map { it.title }
                )
                markersListView.adapter = adapter

                markersListView.setOnItemClickListener { _, _, position, _ ->
                    val selectedMarker = markers[position]
                    deleteMarker(selectedMarker) {
                        markers.removeAt(position)
                        adapter.notifyDataSetChanged() // Обновление списка
                        Toast.makeText(requireContext(), "Метка удалена", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(requireContext(), "Ошибка: пользователь не найден!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    /**
     * Загрузка всех меток текущего пользователя.
     */
    private fun loadUserMarkers(onMarkersLoaded: (MutableList<MarkerData>) -> Unit) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userMarkers.clear()
                    for (markerSnapshot in snapshot.children) {
                        val markerData = markerSnapshot.getValue(MarkerData::class.java)
                        if (markerData != null) {
                            userMarkers.add(markerData)
                        }
                    }
                    onMarkersLoaded(userMarkers)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка загрузки данных: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /**
     * Удаление метки из Firebase и списка.
     */
    private fun deleteMarker(markerData: MarkerData, onDeleted: () -> Unit) {
        database.orderByChild("title").equalTo(markerData.title)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (markerSnapshot in snapshot.children) {
                        val dbMarker = markerSnapshot.getValue(MarkerData::class.java)
                        if (dbMarker?.username == username) {
                            markerSnapshot.ref.removeValue()
                            onDeleted()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        requireContext(),
                        "Ошибка удаления: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}
