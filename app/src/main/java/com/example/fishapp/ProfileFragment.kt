package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.fishapp.databinding.FragmentProfileBinding
import com.google.firebase.database.*

class ProfileFragment : Fragment() {

    private val args: ProfileFragmentArgs by navArgs()
    private lateinit var database: DatabaseReference

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        val username = args.username

        if (username.isNotEmpty()) {
            database = FirebaseDatabase.getInstance().getReference("users")
            loadUserProfile(username)
        } else {
            Toast.makeText(requireContext(), "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }

        binding.editProfileButton.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(username)
            findNavController().navigate(action)
        }

        return view
    }

    private fun loadUserProfile(username: String) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            val password = userSnapshot.child("password").getValue(String::class.java)

                            binding.usernameTextView.text = "Username: $username"
                            binding.emailTextView.text = "Email: $email"
                            binding.passwordTextView.text = "Password: $password"
                        }
                    } else {
                        Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Ошибка подключения к базе данных: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
