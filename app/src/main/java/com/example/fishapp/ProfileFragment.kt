package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private var username: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val usernameTextView = view.findViewById<TextView>(R.id.usernameTextView)
        val emailTextView = view.findViewById<TextView>(R.id.emailTextView)
        val passwordTextView = view.findViewById<TextView>(R.id.passwordTextView)
        val editProfileButton = view.findViewById<Button>(R.id.editProfileButton)

        username = arguments?.getString("username")

        if (username != null) {
            database = FirebaseDatabase.getInstance().getReference("users")
            loadUserProfile(username!!, usernameTextView, emailTextView, passwordTextView)
        } else {
            Toast.makeText(requireContext(), "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        editProfileButton.setOnClickListener {
            // Переход в EditProfileFragment
            val editProfileFragment = EditProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    private fun loadUserProfile(username: String, usernameTextView: TextView, emailTextView: TextView, passwordTextView: TextView) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            val password = userSnapshot.child("password").getValue(String::class.java)

                            usernameTextView.text = "Username: $username"
                            emailTextView.text = "Email: $email"
                            passwordTextView.text = "Password: $password"
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
}
