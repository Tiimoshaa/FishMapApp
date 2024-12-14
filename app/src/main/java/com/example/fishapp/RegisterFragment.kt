package com.example.fishapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterFragment : Fragment() {

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        database = FirebaseDatabase.getInstance().reference

        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val email = emailInput.text.toString().trim()

            if (validateInputs(username, password, email)) {
                registerUser(username, password, email)
            }
        }

        return view
    }

    private fun validateInputs(username: String, password: String, email: String): Boolean {
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(context, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return false
        }
        if (username.length < 4) {
            Toast.makeText(context, "Логин должен содержать не менее 4 символов", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!email.contains("@")) {
            Toast.makeText(context, "Некорректный адрес электронной почты", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8 || !password.any { it.isUpperCase() } || !password.any { it.isDigit() }) {
            Toast.makeText(context, "Пароль должен содержать не менее 8 символов, заглавную букву и цифру", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun registerUser(username: String, password: String, email: String) {
        val userId = database.push().key
        if (userId != null) {
            val user = mapOf(
                "username" to username,
                "password" to password,
                "email" to email
            )
            database.child("users").child(userId).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(context, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Ошибка генерации идентификатора пользователя", Toast.LENGTH_SHORT).show()
        }
    }
}
