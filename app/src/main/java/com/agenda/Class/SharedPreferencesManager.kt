package com.agenda.Class

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

// Classe per gestire le SharedPreferences, che permettono di salvare e recuperare dati semplici
// in forma di coppie chiave-valore.
class SharedPreferencesManager(context: Context) {

    // Inizializza le SharedPreferences utilizzando le preferenze predefinite dell'app.
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    companion object {
        // Costanti che rappresentano le chiavi per accedere ai valori delle preferenze.
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_NOTIFY = "notify_enabled"
    }

    // Variabile per abilitare o disabilitare una funzionalità AI.
    // Utilizza le SharedPreferences per salvare e recuperare il valore associato alla chiave "ai_enabled".
    var isAiEnabled: Boolean
        get() = preferences.getBoolean(KEY_AI_ENABLED, false) // Recupera il valore salvato o restituisce false se non esiste.
        set(value) {
            // Salva il nuovo valore nelle SharedPreferences.
            preferences.edit().putBoolean(KEY_AI_ENABLED, value).apply()
        }

    // Variabile per abilitare o disabilitare le notifiche.
    // Utilizza le SharedPreferences per salvare e recuperare il valore associato alla chiave "notify_enabled".
    var isNotifyEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFY, false) // Recupera il valore salvato o restituisce false se non esiste.
        set(value) {
            // Salva il nuovo valore nelle SharedPreferences.
            preferences.edit().putBoolean(KEY_NOTIFY, value).apply()
        }
}