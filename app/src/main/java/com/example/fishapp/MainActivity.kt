package com.example.fishapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val usernameInput = findViewById<EditText>(R.id.usernameInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        // Инициализация базы данных Firebase
        database = FirebaseDatabase.getInstance().getReference("users")

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Поиск пользователя по имени
                database.orderByChild("username").equalTo(username)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                for (userSnapshot in snapshot.children) {
                                    val storedPassword = userSnapshot.child("password").getValue(String::class.java)

                                    if (storedPassword == password) {
                                        // Вход успешен
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Успешный вход",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Переход в SecondFragment с передачей имени пользователя
                                        loadSecondFragment(username)
                                        return
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Неверный пароль",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Пользователь не найден",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@MainActivity,
                                "Ошибка подключения к базе данных",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            } else {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            // Переход в RegisterFragment
            loadFragment(RegisterFragment())
        }
    }


    private fun loadSecondFragment(username: String) {
        val secondFragment = SecondFragment().apply {
            arguments = Bundle().apply {
                putString("username", username) // Передача имени пользователя
            }
        }
        loadFragment(secondFragment)
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment) // Меняем содержимое контейнера
            .addToBackStack(null) // Добавляем транзакцию в стек
            .commit() // Выполняем транзакцию
    }
}
