package com.agenda.Fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.agenda.Activity.LoginActivity
import com.agenda.Class.SharedPreferencesManager
import com.agenda.Utilities.UserActivity
import com.agenda.R
import com.agenda.extensions.showConfirmationDialog

/**
 * Fragment per le impostazioni dell'app.
 */
class SettingFragment : Fragment() {

    // Componenti UI
    private lateinit var notifySwitch: Switch
    private lateinit var aiSwitch: Switch
    private lateinit var logoutButton: Button

    // Gestore delle preferenze condivise
    private lateinit var preferences: SharedPreferencesManager

    /**
     * Metodo chiamato per creare la vista del fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Inizializza il gestore delle preferenze condivise
        preferences = SharedPreferencesManager(requireContext())

        // Inizializza i componenti UI
        initializeUIComponents(view)

        // Configura i componenti UI
        configureUIComponents()

        return view
    }

    /**
     * Inizializza i componenti UI.
     */
    private fun initializeUIComponents(root: View) {
        with(root) {
            // Trova i componenti UI nel layout
            notifySwitch = findViewById(R.id.switch_notify)
            aiSwitch = findViewById(R.id.switch_ai)
            logoutButton = findViewById(R.id.button_logout)
        }
    }

    /**
     * Configura i componenti UI.
     */
    private fun configureUIComponents() {
        // Imposta lo stato iniziale degli switch basato sulle preferenze salvate
        notifySwitch.isChecked = preferences.isNotifyEnabled
        aiSwitch.isChecked = preferences.isAiEnabled

        // Imposta i listener per gli switch
        notifySwitch.setOnCheckedChangeListener { _, isChecked ->
            // Salva lo stato dello switch nelle preferenze
            preferences.isNotifyEnabled = isChecked
        }

        aiSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Salva lo stato dello switch nelle preferenze
            preferences.isAiEnabled = isChecked
        }

        // Imposta il listener per il pulsante di logout
        logoutButton.setOnClickListener {
            performLogout()
        }
    }

    /**
     * Esegue il logout dell'utente.
     */
    private fun performLogout() {
        // Mostra una finestra di dialogo di conferma per il logout
        requireContext().showConfirmationDialog("Logout", "Sei sicuro di voler uscire?") {
            // Esegui il logout dell'utente
            UserActivity.logout()

            // Avvia l'activity di login
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            // Termina l'activity corrente
            requireActivity().finish()
        }
    }

}