package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fishapp.databinding.FragmentLoginBinding
import com.google.firebase.database.*

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root


        database = FirebaseDatabase.getInstance().getReference("users")

        binding.loginButton.setOnClickListener {
            val username = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {

                database.orderByChild("username").equalTo(username)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnapshot in snapshot.children) {
                                    val storedPassword = userSnapshot.child("password").getValue(String::class.java)

                                    if (storedPassword == password) {
                                        // Вход успешен
                                        Toast.makeText(
                                            requireContext(),
                                            "Успешный вход",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Переход в SecondFragment с передачей имени пользователя
                                        val action = LoginFragmentDirections.actionLoginFragmentToSecondFragment(username)
                                        findNavController().navigate(action)
                                        return
                                    } else {
                                        Toast.makeText(
                                            requireContext(),
                                            "Неверный пароль",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Пользователь не найден",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                requireContext(),
                                "Ошибка подключения к базе данных",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            } else {
                Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        binding.registerButton.setOnClickListener {
            // Переход в RegisterFragment
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
