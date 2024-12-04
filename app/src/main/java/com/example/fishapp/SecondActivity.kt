package com.example.fishapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SecondActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        // Подключение к Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        val dataInput = findViewById<EditText>(R.id.dataInput)
        val sendDataButton = findViewById<Button>(R.id.sendDataButton)

        sendDataButton.setOnClickListener {
            val data = dataInput.text.toString()

            if (data.isNotEmpty()) {
                // Генерация уникального ключа и сохранение данных
                val key = database.push().key
                if (key != null) {
                    database.child("user_data").child(key).setValue(data)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Данные успешно отправлены!", Toast.LENGTH_SHORT).show()
                            dataInput.text.clear() // Очистка поля ввода
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Ошибка отправки данных", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Поле ввода не должно быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
