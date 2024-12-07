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

        // Подключение к Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            val email = emailInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty()) {
                // Сохранение данных пользователя в Firebase
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
                            finish() // Вернуться на главный экран
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Ошибка регистрации", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
