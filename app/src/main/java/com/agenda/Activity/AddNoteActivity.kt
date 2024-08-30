package com.agenda.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agenda.Class.SharedPreferencesManager
import com.agenda.Utilities.DatePredictor
import com.agenda.R
import com.agenda.Utilities.FirestoreHandler
import com.agenda.extensions.*
import com.agendapersonale.src.Class.DateCalendar
import com.agendapersonale.src.Class.MarkerInfo
import com.agendapersonale.src.Class.Note
import com.agendapersonale.src.Class.NoteData
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Date

/**
 * Activity per aggiungere una nuova nota.
 * Permette di selezionare una data e ora, pulire le nota/e e aggiungere la nota a Firestore.
 */
class AddNoteActivity : AppCompatActivity() {

    // Elementi dell'interfaccia utente
    private lateinit var categorySpinner: Spinner
    private lateinit var gridLayout: GridLayout
    private lateinit var saveEventButton: FloatingActionButton
    private lateinit var textTitleScope: TextInputEditText
    private lateinit var textDescScope: TextInputEditText

    // Dati dell'attività
    private lateinit var selectedDateTimeList: MutableList<NoteData>
    private var markerList: List<MarkerInfo>? = null

    private lateinit var preference: SharedPreferencesManager

    companion object {
        private const val REQUEST_CODE_MAPS_ACTIVITY = 1001
    }

    private val context = this@AddNoteActivity

    /**
     * Metodo chiamato alla creazione dell'attività.
     * Inizializza la View e imposta i listener per i bottoni.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_note)
        preference = SharedPreferencesManager(this)
        initializeViews()
        setupListeners()
    }

    /**
     * Inizializza gli elementi del View.
     */
    private fun initializeViews() {
        supportActionBar?.title = "Aggiungi nota"
        selectedDateTimeList = mutableListOf()
        gridLayout = findViewById(R.id.gridLayout)
        categorySpinner = findViewById(R.id.searchCategorySpinner)
        saveEventButton = findViewById(R.id.saveEventButton)
        textTitleScope = findViewById(R.id.textTitleScope)
        textDescScope = findViewById(R.id.textDescScope)

        // Configura lo Spinner con le opzioni di categoria delle note
        categorySpinner.setup(context, Note.categoryOptions)
    }

    /**
     * Imposta i listener per i bottoni e gli eventi di interazione.
     */
    private fun setupListeners() {
        findViewById<RadioButton>(R.id.optionOne).setup(R.drawable.ic_time) {
            clickDateTimePickerButton()
        }
        findViewById<RadioButton>(R.id.optionTwo).setup(R.drawable.ic_clean) {
            clearSelectedDateTime()
        }
        findViewById<RadioButton>(R.id.optionThree).setup(R.drawable.ic_location) {
            clickLocationEvent()
        }
        saveEventButton.setOnClickListener {
            addNoteToFirestore()
        }
    }

    /**
     * Gestisce il clic sul pulsante per selezionare data e ora.
     * Aggiunge le note alla lista e aggiorna la View.
     */
    private fun clickDateTimePickerButton() {
        lifecycleScope.launch {
            // Ottiene data e ora di inizio e fine
            val (selectedStartDate, startTime) = getStartDateTime()
            val (selectedEndDate, endTime) = getEndDateTime(selectedStartDate, startTime)

            // Ottiene il numero di minuti per il promemoria
            val reminderMinutes = context.setNumber("Preavviso attivo")

            // Aggiunge i dati alla lista e aggiorna la View
            selectedDateTimeList.add(NoteData(selectedStartDate, startTime, selectedEndDate, endTime, reminderMinutes))
            addDateTimeViews(selectedStartDate, startTime, selectedEndDate, endTime, reminderMinutes)
        }
    }

    /**
     * Ottiene la data e l'ora di inizio.
     * Se l'IA è abilitata, usa DatePredictor per predire la data e ora.
     */
    private suspend fun getStartDateTime(): Pair<Date, LocalTime> {
        return if (preference.isAiEnabled) {
            val nextPreviousNote = DatePredictor.predictNextNoteDate()
            val startDate = context.setData("Giorno inizio", nextPreviousNote.removeTimeOfDay())
            val startTime = context.setTime("Ora e minuti inizio", nextPreviousNote.extractTimeOfDay())
            startDate to startTime
        } else {
            val startDate = context.setData("Giorno inizio")
            val startTime = context.setTime("Ora e minuti inizio")
            startDate to startTime
        }
    }

    /**
     * Ottiene la data e l'ora di fine.
     * Calcola il tempo di fine iniziale e assicura che sia dopo il tempo di inizio.
     */
    private suspend fun getEndDateTime(startDate: Date, startTime: LocalTime): Pair<Date, LocalTime> {
        val startMillis = startDate.time + startTime.toMillis() // Timestamp in millisecondi dell'inizio

        var endDate: Date
        var endTime: LocalTime

        if (preference.isAiEnabled) {
            // Calcolo con l'IA abilitata
            do {
                val predictedDuration = DatePredictor.predictNextNoteDuration()
                endDate = context.setData("Giorno fine", predictedDuration.removeTimeOfDay())
                endTime = context.setTime("Ora e minuti fine", predictedDuration.extractTimeOfDay())
            } while (endDate.time + endTime.toMillis() <= startMillis)
        } else {
            // Calcolo senza l'IA
            do {
                endDate = context.setData("Giorno fine", startDate.time.removeTimeOfDay())
                endTime =
                    context.setTime("Ora e minuti fine", startTime.toMillis().plusOneHour()) // Aggiungi un'ora
            } while (endDate.time + endTime.toMillis() <= startMillis)
        }

        return endDate to endTime
    }

    /**
     * Gestisce il clic sul pulsante per selezionare la posizione.
     * Avvia l'attività della mappa per selezionare una posizione.
     */
    private fun clickLocationEvent() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_MAPS_ACTIVITY)
    }

    /**
     * Gestisce il risultato dell'attività della mappa.
     * Aggiorna la lista dei marker con le coordinate ricevute.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MAPS_ACTIVITY && resultCode == RESULT_OK) {
            val coordinates = data?.getStringExtra("marker_locations")
            coordinates?.let {
                markerList = it.split(";").map { coord ->
                    val (lat, lng) = coord.split(",").map { it.toDouble() }
                    MarkerInfo(lat, lng)
                }
                Toast.makeText(this, "Received coordinates: $markerList", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Aggiunge le viste di data e ora al layout.
     */
    private fun addDateTimeViews(
        selectedStartDate: Date,
        startTime: LocalTime,
        selectedEndDate: Date,
        endTime: LocalTime,
        reminderMinutes: Int
    ) {
        val startDateString = DateCalendar.formatDate.format(selectedStartDate)
        val endDateString = DateCalendar.formatDate.format(selectedEndDate)
        val startTimeString = startTime.format(DateCalendar.timeFormatter)
        val endTimeString = endTime.format(DateCalendar.timeFormatter)

        val startDateTextView = context.createDateTimeTextView("$startDateString\n$startTimeString")
        val endDateTextView = context.createDateTimeTextView("$endDateString\n$endTimeString")
        val reminderTextView = context.createDateTimeTextView("$reminderMinutes\nminuti")

        gridLayout.addView(startDateTextView)
        gridLayout.addView(endDateTextView)
        gridLayout.addView(reminderTextView)
    }

    /**
     * Pulisce le selezioni di data e ora e aggiorna la vista.
     */
    private fun clearSelectedDateTime() {
        if (selectedDateTimeList.isNotEmpty()) {
            gridLayout.clearSelectedDateTime(3, selectedDateTimeList, this)
        } else{
            Toast.makeText(this, "Non ci sono date da eliminare", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Aggiunge la nota a Firestore.
     * Verifica che il titolo e le date siano compilati prima di inviare.
     */
    private fun addNoteToFirestore() {
        val title = textTitleScope.text.toString()
        val description = textDescScope.text.toString()
        val eventCategory = categorySpinner.selectedItem.toString()

        if (title.isNotEmpty() && selectedDateTimeList.isNotEmpty()) {
            FirestoreHandler.addNoteWithDates(
                title, description, eventCategory, markerList, selectedDateTimeList
            ) { success, message ->
                if (success) {
                    Toast.makeText(this, "Nota aggiunta con successo", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                }
            }
        } else {
            Toast.makeText(this, "Compilare tutti i campi", Toast.LENGTH_SHORT).show()
        }
    }
}