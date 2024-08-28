package com.agenda.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.agenda.R
import com.agenda.extensions.*
import com.agendapersonale.src.Class.DateCalendar
import com.agendapersonale.src.Class.Note
import com.google.android.material.textfield.TextInputEditText
import java.util.Date

/**
 * Fragment per la ricerca di note
 */
class SearchFragment : Fragment() {

    // Dichiarazione delle variabili per i componenti della UI
    private lateinit var scrollView: ScrollView
    private lateinit var noteLayout: LinearLayout
    private lateinit var spinner: Spinner
    private lateinit var searchText: TextInputEditText
    private lateinit var buttonOne: Button
    private lateinit var buttonTwo: Button
    private lateinit var buttonThree: Button

    // Variabili per memorizzare le date di inizio e fine della ricerca
    private var startD: Date = DateCalendar.minDate
    private var endD: Date = DateCalendar.maxDate

    /**
     * Metodo chiamato per creare la vista del fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del fragment e inizializza le viste
        return inflater.inflate(R.layout.fragment_search, container, false).apply {
            initializeViews(this)
            setupSpinner()
            setupButtons()
        }
    }

    /**
     * Inizializza le viste del fragment.
     */
    private fun initializeViews(root: View) {
        // Associa le variabili ai componenti UI nel layout
        with(root) {
            scrollView = findViewById(R.id.scrollViewContent)
            noteLayout = findViewById(R.id.linearLayoutSearchResults)
            spinner = findViewById(R.id.spinnerCategory)
            searchText = findViewById(R.id.editTextSearch)
            buttonOne = findViewById(R.id.buttonOne)
            buttonTwo = findViewById(R.id.buttonTwo)
            buttonThree = findViewById(R.id.buttonThree)
        }
    }

    /**
     * Configura lo spinner con le opzioni e le immagini delle note.
     */
    private fun setupSpinner() {
        // Imposta lo spinner con le opzioni di note e le immagini corrispondenti
        spinner.setup(requireContext(), Note.options, Note.images)
    }

    /**
     * Configura i pulsanti per la selezione delle date e l'avvio della ricerca.
     */
    private fun setupButtons() {
        // Configura il primo pulsante per selezionare la data di inizio
        buttonOne.setOnClickListener {
            // Mostra il date picker per selezionare la data di inizio
            requireContext().setData("Data inizio:", DateCalendar.minDate.time) { result ->
                startD = result
                buttonOne.text = DateCalendar.formatDate.format(startD)
            }
        }

        // Configura il secondo pulsante per selezionare la data di fine
        buttonTwo.setOnClickListener {
            // Mostra il date picker per selezionare la data di fine
            requireContext().setData("Data fine:", startD.time + (86400 * 1000L)) { result ->
                endD = result
                buttonTwo.text = DateCalendar.formatDate.format(endD)
            }
        }

        // Configura il terzo pulsante per avviare la ricerca
        buttonThree.setOnClickListener {
            // Ottiene il testo di ricerca e il tipo selezionato dallo spinner
            val search = searchText.text.toString()
            val type = spinner.selectedItem.toString()
            // Mostra i risultati della ricerca nella noteLayout
            requireActivity().displayEvents(noteLayout, startD, endD, search, type)
        }
    }
}