package com.agenda

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.agenda.Activity.AddNoteActivity
import com.agenda.Activity.LoginActivity
import com.agenda.Fragment.DayFragment
import com.agenda.Fragment.SearchFragment
import com.agenda.Class.NotificationService
import com.agenda.Fragment.SettingFragment
import com.agenda.Utilities.UserActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * MainActivity rappresenta la schermata principale dell'app, gestendo la navigazione tra i
 * diversi frammenti e controllando lo stato di autenticazione dell'utente.
 */
class MainActivity : AppCompatActivity() {

    // Metodo chiamato alla creazione dell'attività
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Controlla se l'utente è autenticato
        if (UserActivity.getCurrentUser() == null) {
            // Se l'utente non è autenticato, reindirizza alla schermata di login
            redirectToLogin()
        } else {
            // Se l'utente è autenticato, imposta l'interfaccia utente
            setupUI()

            // Sostituisce il frammento corrente con il DayFragment
            replaceFragment(DayFragment())
        }
    }

    // Metodo per reindirizzare alla schermata di login
    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish() // Termina l'attività corrente in modo che l'utente non possa tornare indietro
    }

    // Metodo per impostare l'interfaccia utente
    private fun setupUI() {
        setContentView(R.layout.activity_main)

        // Applica i padding delle barre di sistema alla vista principale
        applyWindowInsets(findViewById(R.id.main))

        // Imposta il click listener per il pulsante per aggiungere una nuova nota
        findViewById<FloatingActionButton>(R.id.buttonAddNote).setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }

        // Imposta il menu di navigazione inferiore
        setupMenu()

        // Avvia il servizio di notifica in primo piano
        startNotificationService()
    }

    // Metodo per applicare i padding delle barre di sistema alla vista principale
    private fun applyWindowInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Metodo per impostare il menu di navigazione inferiore
    private fun setupMenu() {
        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_day -> replaceFragment(DayFragment()) // Mostra il DayFragment
                R.id.navigation_search -> replaceFragment(SearchFragment()) // Mostra il SearchFragment
                R.id.navigation_setting -> replaceFragment(SettingFragment()) // Mostra il SettingFragment
                else -> false
            }
            true
        }
    }

    // Metodo per sostituire il frammento nel container specificato
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment) // Sostituisce il frammento nel container specificato
            commit()
        }
    }

    // Metodo per avviare il servizio di notifica
    private fun startNotificationService() {
        ContextCompat.startForegroundService(this,
            Intent(this, NotificationService::class.java))
    }
}