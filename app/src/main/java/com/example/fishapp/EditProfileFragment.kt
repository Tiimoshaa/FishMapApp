package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class EditProfileFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private var username: String? = null
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        username = arguments?.getString("username")

        if (username != null) {
            database = FirebaseDatabase.getInstance().getReference("users")
            loadUserDetails(username!!, emailInput, passwordInput)
        } else {
            Toast.makeText(requireContext(), "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        saveButton.setOnClickListener {
            val newEmail = emailInput.text.toString().trim()
            val newPassword = passwordInput.text.toString().trim()

            if (newEmail.isNotEmpty() && newPassword.isNotEmpty() && userId != null) {
                updateUserDetails(userId!!, newEmail, newPassword)
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun loadUserDetails(username: String, emailInput: EditText, passwordInput: EditText) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            userId = userSnapshot.key // ID пользователя в Firebase
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            val password = userSnapshot.child("password").getValue(String::class.java)

                            emailInput.setText(email)
                            passwordInput.setText(password)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка подключения к базе данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateUserDetails(userId: String, newEmail: String, newPassword: String) {
        val updates = mapOf(
            "email" to newEmail,
            "password" to newPassword
        )

        database.child(userId).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // Возвращаемся назад
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка обновления данных", Toast.LENGTH_SHORT).show()
            }
    }
}
