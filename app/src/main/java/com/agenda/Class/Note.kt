package com.agendapersonale.src.Class

import com.agenda.R
import com.google.android.gms.maps.model.LatLng

// Classe dati per rappresentare una Nota
data class Note(
    val titolo: String, // Titolo della nota
    val emailUtente: String?, // Email dell'utente associato alla nota
    val descrizione: String?, // Descrizione della nota (opzionale)
    val eventCategory: String? // Categoria dell'evento (es. Sport, Musica, Conferenza, ecc.)
) {
    companion object {
        // Opzioni e immagini per il selettore (usate per filtrare e categorizzare le note)
        val options = arrayOf("Titolo", "Descrizione", "Categoria") // Opzioni di ricerca o selezione
        val categoryOptions = arrayOf(
            "Lavoro", "Famiglia", "Intrattenimento", "Studio", "Sport", "Salute", "Viaggi",
            "Cultura", "Hobby", "Finanze", "Eventi", "Altro" // Categorie predefinite per le note
        )
        val images = arrayOf(
            R.drawable.ic_title, // Icona per il titolo
            R.drawable.ic_description, // Icona per la descrizione
            R.drawable.ic_category // Icona per la categoria
        )
    }

    // Override del metodo toString per una rappresentazione leggibile della nota
    override fun toString(): String {
        return buildString {
            append("Titolo: $titolo\n") // Aggiunge il titolo alla stringa
            if (!descrizione.isNullOrBlank()) {
                append("Descrizione: $descrizione\n") // Aggiunge la descrizione se presente
            }
            if (!eventCategory.isNullOrBlank()) {
                append("Categoria: $eventCategory\n") // Aggiunge la categoria se presente
            }
        }
    }

    // Funzione per cercare una nota in base al testo di ricerca e al tipo di campo (Titolo, Descrizione, Categoria)
    fun search(cerca: String?, searchType: String?): Boolean {
        if (cerca.isNullOrEmpty() || searchType.isNullOrEmpty()) {
            return true // Se il testo di ricerca o il tipo sono vuoti, ritorna vero per includere tutte le note
        }

        // Verifica se il testo di ricerca è presente nel campo specificato
        return when (searchType) {
            "Titolo" -> titolo.contains(cerca, ignoreCase = true) // Cerca nel titolo
            "Descrizione" -> descrizione?.contains(cerca, ignoreCase = true) ?: false // Cerca nella descrizione
            "Categoria" -> eventCategory?.contains(cerca, ignoreCase = true) ?: false // Cerca nella categoria
            else -> false // Ritorna falso se il tipo di ricerca non corrisponde a nessuna opzione
        }
    }

}