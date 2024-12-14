package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        username = arguments?.getString("username")

        if (username != null) {
            database = FirebaseDatabase.getInstance().getReference("users")
            loadUserProfile(username!!, view)
        } else {
            Toast.makeText(requireContext(), "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadUserProfile(username: String, view: View) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            val password = userSnapshot.child("password").getValue(String::class.java)

                            view.findViewById<TextView>(R.id.usernameTextView).text = "Username: $username"
                            view.findViewById<TextView>(R.id.emailTextView).text = "Email: $email"
                            view.findViewById<TextView>(R.id.passwordTextView).text = "Password: $password"
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
