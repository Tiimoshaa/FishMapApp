package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.database.*

class DeleteMarkerFragment : Fragment() {

    private val args: DeleteMarkerFragmentArgs by navArgs()
    private lateinit var database: DatabaseReference
    private lateinit var deleteButton: Button
    private lateinit var markerTitleInput: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delete_marker, container, false)

        deleteButton = view.findViewById(R.id.deleteButton)
        markerTitleInput = view.findViewById(R.id.markerTitleInput)

        val username = args.username

        database = FirebaseDatabase.getInstance().getReference("markers")

        deleteButton.setOnClickListener {
            val title = markerTitleInput.text.toString().trim()
            if (title.isNotEmpty()) {
                deleteMarker(title, username)
            } else {
                Toast.makeText(requireContext(), "Название метки не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun deleteMarker(title: String, username: String) {
        database.orderByChild("title").equalTo(title)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var markerFound = false
                    for (markerSnapshot in snapshot.children) {
                        val markerData = markerSnapshot.getValue(MarkerData::class.java)
                        if (markerData?.username == username) {
                            markerSnapshot.ref.removeValue()
                            Toast.makeText(requireContext(), "Метка удалена", Toast.LENGTH_SHORT).show()
                            markerFound = true
                            break
                        }
                    }
                    if (!markerFound) {
                        Toast.makeText(requireContext(), "Метка не найдена или принадлежит другому пользователю", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка удаления: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
