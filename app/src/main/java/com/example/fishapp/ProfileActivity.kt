package com.example.fishapp

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        username = intent.getStringExtra("username")

        if (username != null) {

            database = FirebaseDatabase.getInstance().getReference("users")

            loadUserProfile(username!!)
        } else {
            Toast.makeText(this, "Ошибка: имя пользователя не передано!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserProfile(username: String) {
        database.orderByChild("username").equalTo(username)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        for (userSnapshot in snapshot.children) {
                            val email = userSnapshot.child("email").getValue(String::class.java)
                            val password = userSnapshot.child("password").getValue(String::class.java)


                            findViewById<TextView>(R.id.usernameTextView).text = "Username: $username"
                            findViewById<TextView>(R.id.emailTextView).text = "Email: $email"
                            findViewById<TextView>(R.id.passwordTextView).text = "Password: $password"
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Ошибка подключения к базе данных", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
