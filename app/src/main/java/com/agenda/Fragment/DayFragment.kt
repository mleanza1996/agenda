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

/**
 * Fragment per visualizzare e navigare tra le note del giorno.
 */
class DayFragment : Fragment() {

    // Dichiarazione delle variabili per i componenti della UI
    private lateinit var title: TextView
    private lateinit var spinner: Spinner
    private lateinit var optionOne: RadioButton
    private lateinit var optionTwo: RadioButton
    private lateinit var optionThree: RadioButton
    private lateinit var noteLayout: LinearLayout
    private val dateManager = DateCalendar.getInstance()

    /**
     * Metodo chiamato per creare la vista del fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Infla il layout del fragment e inizializza le viste
        return inflater.inflate(R.layout.fragment_day, container, false).apply {
            initializeViews(this)
            setupSpinner()
            setupRadioButtons()
        }
    }

    /**
     * Inizializza le viste del fragment.
     */
    private fun initializeViews(root: View) {
        // Associa le variabili ai componenti UI nel layout
        with(root) {
            title = findViewById(R.id.titleCalendar)
            spinner = findViewById(R.id.spinner)
            optionOne = findViewById(R.id.optionOneMenu)
            optionTwo = findViewById(R.id.optionTwoMenu)
            optionThree = findViewById(R.id.optionThreeMenu)
            noteLayout = findViewById(R.id.listNote)
        }
    }

    /**
     * Configura lo spinner con le opzioni e le immagini del calendario.
     */
    private fun setupSpinner() {
        // Imposta lo spinner con le opzioni del calendario e le immagini corrispondenti
        spinner.setup(requireContext(), DateCalendar.options, DateCalendar.images) {
            // Resetta il calendario alla data odierna quando viene selezionata un'opzione diversa
            dateManager.resetCalendarToToday()
            refreshUI()
        }
    }

    /**
     * Configura i radio button per la navigazione e la ricerca delle date.
     */
    private fun setupRadioButtons() {
        // Configura il primo radio button per navigare alla data precedente
        optionOne.setup(R.drawable.left_arrow) { navigateCalendar(-1) }

        // Configura il secondo radio button per selezionare una data specifica
        optionTwo.setup(R.drawable.search_content) {
            requireContext().setData("Data fine:", DateCalendar.minDate.time) { selectedDate ->
                dateManager.setCalendar(selectedDate)
                refreshUI()
            }
        }

        // Configura il terzo radio button per navigare alla data successiva
        optionThree.setup(R.drawable.right_arrow) { navigateCalendar(1) }
    }

    /**
     * Naviga nel calendario nella direzione specificata.
     */
    private fun navigateCalendar(direction: Int) {
        // Ottiene il tipo di data selezionata dallo spinner e regola la data
        val dateField = DateCalendar.getType(spinner.selectedItem.toString())
        dateManager.adjustDate(dateField, direction)
        refreshUI()
    }

    /**
     * Aggiorna l'interfaccia utente con la data corrente e gli eventi.
     */
    private fun refreshUI() {
        // Aggiorna il titolo con la data formattata e mostra gli eventi corrispondenti
        title.text = dateManager.getFormattedDate(spinner.selectedItem.toString())
        val (startD, endD) = dateManager.getDateRange(spinner.selectedItem.toString())
        requireActivity().displayEvents(noteLayout, startD, endD)
    }
}