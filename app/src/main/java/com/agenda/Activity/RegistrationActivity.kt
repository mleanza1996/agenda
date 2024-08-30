package com.agenda.Activity

import com.agenda.Utilities.UserActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.agenda.R
import com.google.android.material.textfield.TextInputEditText

class RegistrationActivity : AppCompatActivity() {

    private lateinit var buttonRegistrationData: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        initializeViews() // Inizializza le viste della UI
    }

    private fun initializeViews() {
        // Inizializza il pulsante di registrazione
        buttonRegistrationData = findViewById(R.id.buttonRegistration)

        // Imposta il listener per il click sul pulsante di registrazione
        buttonRegistrationData.setOnClickListener {
            // Recupera i dati inseriti nei campi di testo
            val nome = findViewById<TextInputEditText>(R.id.textRegNome).text.toString()
            val cognome = findViewById<TextInputEditText>(R.id.textRegCognome).text.toString()
            val email = findViewById<TextInputEditText>(R.id.textRegEmail).text.toString()
            val password = findViewById<TextInputEditText>(R.id.textRegPassword).text.toString()
            val confPassword = findViewById<TextInputEditText>(R.id.textRegConfPassword).text.toString()

            // Esegue la registrazione con i dati inseriti
            UserActivity.register(nome, cognome, email, password, confPassword) { success, message ->
                if (success) {
                    // Mostra un messaggio di successo e chiude l'activity
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Mostra un messaggio di errore
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}