package com.agenda.Activity

import com.agenda.Utilities.UserActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.agenda.MainActivity
import com.agenda.R
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    // Dichiarazione dei pulsanti per login e registrazione
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegistration: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        applyWindowInsets()
        initializeViews()
    }

    private fun applyWindowInsets() {
        // Applica i margini di sistema (status bar, navigation bar) alla vista principale
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeViews() {
        // Inizializza i pulsanti di login e registrazione
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonRegistration = findViewById(R.id.buttonRegistration)

        // Listener per il pulsante di login
        buttonLogin.setOnClickListener {
            val email = findViewById<TextInputEditText>(R.id.textEmail).text.toString()
            val password = findViewById<TextInputEditText>(R.id.textPassword).text.toString()

            // Esegue il login con email e password
            UserActivity.login(email, password) { success, message ->
                if (success) {
                    // Avvia MainActivity se il login ha successo
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "Login eseguito con successo", Toast.LENGTH_SHORT).show()
                } else {
                    // Mostra un messaggio di errore in caso di fallimento
                    message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
                }
            }
        }

        // Listener per il pulsante di registrazione
        buttonRegistration.setOnClickListener {
            // Avvia RegistrationActivity
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }
}