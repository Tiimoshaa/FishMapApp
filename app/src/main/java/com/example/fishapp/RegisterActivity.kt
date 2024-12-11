package com.example.fishapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        database = FirebaseDatabase.getInstance().reference

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val email = emailInput.text.toString().trim()


            if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (username.length < 4) {
                Toast.makeText(this, "Логин должен содержать не менее 4 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (!email.contains("@")) {
                Toast.makeText(this, "Некорректный адрес электронной почты", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            if (password.length < 8) {
                Toast.makeText(this, "Пароль должен содержать не менее 8 символов", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasUppercase = password.any { it.isUpperCase() }
            if (!hasUppercase) {
                Toast.makeText(this, "Пароль должен содержать хотя бы одну заглавную букву", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasDigit = password.any { it.isDigit() }
            if (!hasDigit) {
                Toast.makeText(this, "Пароль должен содержать хотя бы одну цифру", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val userId = database.push().key
            if (userId != null) {
                val user = mapOf(
                    "username" to username,
                    "password" to password,
                    "email" to email
                )
                database.child("users").child(userId).setValue(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Ошибка генерации идентификатора пользователя", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
