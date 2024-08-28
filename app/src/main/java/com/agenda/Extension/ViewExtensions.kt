package com.agenda.extensions

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.util.Calendar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.cardview.widget.CardView
import com.agenda.Activity.ViewMarkerActivity
import com.agenda.Class.CustomSpinnerAdapter
import com.agenda.Class.NoteIdentifier
import com.agenda.R
import com.agenda.Utilities.FirestoreHandler
import com.agendapersonale.src.Class.NoteData
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalTime
import java.util.Date
import kotlin.coroutines.resume

// Costanti
private const val DEFAULT_MINUTES = 0
private const val DEFAULT_HOUR_INCREMENT = 1
private const val NUMBER_PICKER_MIN_VALUE = 0
private const val NUMBER_PICKER_MAX_VALUE = 180
private const val NUMBER_PICKER_DEFAULT_VALUE = 30
private const val DEFAULT_DURATION = 24 * 60 * 60 * 1000L // 1 giorno in millisecondi

// Estensione per convertire LocalTime in millisecondi
fun LocalTime.toMillis(): Long {
    return this.toSecondOfDay() * 1000L
}

// Estensione per rimuovere la parte del tempo da un timestamp
fun Long.removeTimeOfDay(): Long {
    return this - (this % DEFAULT_DURATION)
}

// Estensione per estrarre solo la parte del tempo (ore, minuti, secondi) da un timestamp espresso in millisecondi
fun Long.extractTimeOfDay(): Long {
    return this % DEFAULT_DURATION
}

// Estensione per aggiungere un'ora a un timestamp espresso in millisecondi
fun Long.plusOneHour(): Long {
    return this + (60 * 60 * 1000L)
}

// Estensione per Spinner: configura lo Spinner con opzioni di testo e immagini personalizzate
fun Spinner.setup(
    context: Context,
    options: Array<String>,       // Array di stringhe con le opzioni da visualizzare nello Spinner
    images: Array<Int> = emptyArray(), // Array opzionale di risorse immagine corrispondenti alle opzioni (default: vuoto)
    onItemSelected: (position: Int) -> Unit = {} // Callback opzionale chiamato quando un elemento viene selezionato
) {
    // Crea un adapter personalizzato con le opzioni e le immagini
    val adapter = CustomSpinnerAdapter(context, options, images)
    this.adapter = adapter // Imposta l'adapter personalizzato sullo Spinner

    // Imposta il listener per la selezione degli elementi nello Spinner
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        // Metodo chiamato quando un elemento viene selezionato
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onItemSelected(position) // Chiama il callback passando la posizione dell'elemento selezionato
        }

        // Metodo chiamato quando nessun elemento è selezionato (opzionale)
        override fun onNothingSelected(parent: AdapterView<*>?) {
            // Opzionale: azioni quando nulla è selezionato
        }
    }
}

// Estensioni per RadioButton
fun RadioButton.setup(imageResource: Int, onClickAction: () -> Unit) {
    this.apply {
        setCompoundDrawablesWithIntrinsicBounds(0, imageResource, 0, 0)
        gravity = Gravity.CENTER
        setOnClickListener { onClickAction() }
    }
}

/**
 * Rimuove tutte le viste da un `GridLayout` a partire da un indice specificato,
 * e pulisce una lista associata di date selezionate.
 *
 * @param startIndex L'indice a partire dal quale le viste saranno rimosse.
 * @param selectedDateTimeList La lista di date selezionate da pulire.
 * @param context Il contesto dell'applicazione, utilizzato per mostrare un messaggio di successo.
 */
fun GridLayout.clearSelectedDateTime(startIndex: Int, selectedDateTimeList: MutableList<NoteData>, context: Context) {
    this.removeViewsInLayout(startIndex, this.childCount - startIndex)
    selectedDateTimeList.clear()
    Toast.makeText(context, "Date eliminate con successo", Toast.LENGTH_SHORT).show()
}

// Estensione per creare un TextView personalizzato per visualizzare data e ora
fun Context.createDateTimeTextView(text: String): TextView {
    // Crea un nuovo TextView nel contesto attuale
    return TextView(this).apply {
        // Imposta i parametri di layout per il TextView all'interno di un GridLayout
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0  // Imposta la larghezza del TextView a 0 (sarà ridimensionato proporzionalmente)
            height = GridLayout.LayoutParams.WRAP_CONTENT  // Imposta l'altezza per adattarsi al contenuto
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)  // Specifica che il TextView occupa una colonna con larghezza pari a una frazione del layout
        }
        this.text = text  // Imposta il testo da visualizzare nel TextView
        gravity = Gravity.CENTER  // Centra il testo all'interno del TextView
        setBackgroundResource(R.drawable.table_design)  // Applica uno sfondo personalizzato al TextView
    }
}

// Estensione per creare un CardView personalizzato con azioni mappa e elimina
fun Context.createCardView(
    text: String, // Testo da visualizzare all'interno del CardView
    noteIdentifier: NoteIdentifier, // Identificatore della nota, utilizzato per azioni specifiche
    onMapAction: () -> Unit, // Azione da eseguire quando viene cliccata l'icona della mappa
    onDeleteAction: () -> Unit // Azione da eseguire quando viene eliminata la nota
): CardView {
    // Crea un inflater per caricare il layout del CardView dal file XML
    val inflater = LayoutInflater.from(this)
    // Inflate del layout personalizzato del CardView (message_item)
    val cardView = inflater.inflate(R.layout.message_item, null) as CardView

    // Imposta il testo del messaggio nel TextView all'interno del CardView
    val messageTextView = cardView.findViewById<TextView>(R.id.messageTextView)
    messageTextView.text = text

    // Imposta un listener di clic sull'icona della mappa per eseguire l'azione di visualizzazione mappa
    val iconMapView = cardView.findViewById<ImageView>(R.id.iconMapView)
    iconMapView.setOnClickListener { onMapAction() }

    // Imposta un listener di clic sull'icona di eliminazione per eseguire l'azione di eliminazione
    val iconImageView = cardView.findViewById<ImageView>(R.id.iconImageView)
    iconImageView.setOnClickListener {
        // Mostra un dialogo di conferma per l'eliminazione della nota
        this.showConfirmationDialog(
            "Elimina nota",
            "Sei sicuro di voler eliminare la nota?"
        ) {
            // Gestisce l'eliminazione della nota su Firestore
            FirestoreHandler.deleteNote(
                noteIdentifier.noteId,
                noteIdentifier.noteDateId
            ) { deleted ->
                if (deleted) {
                    // Mostra un messaggio di successo e chiama l'azione di eliminazione
                    Toast.makeText(this, "L'evento è stato eliminato con successo", Toast.LENGTH_LONG).show()
                    onDeleteAction()
                } else {
                    // Se l'eliminazione fallisce, cambia il colore dell'icona di eliminazione
                    iconImageView.setColorFilter(Color.BLACK)
                }
            }
        }
    }

    // Imposta i parametri di layout del CardView, includendo i margini superiori e inferiori
    cardView.layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins(0, 16, 0, 16) // Imposta i margini del CardView
    }
    return cardView
}

// Estensione per mostrare un dialogo di conferma con titolo, contenuto e azione su conferma
fun Context.showConfirmationDialog(
    title: String, // Titolo del dialogo di conferma
    content: String, // Messaggio di contenuto da visualizzare nel dialogo
    onAction: () -> Unit, // Azione da eseguire quando l'utente conferma cliccando su "Sì"
) {
    // Crea un AlertDialog utilizzando il contesto attuale
    AlertDialog.Builder(this)
        .setTitle(title) // Imposta il titolo del dialogo
        .setMessage(content) // Imposta il messaggio di contenuto
        .setPositiveButton("Sì") { _, _ -> onAction() } // Bottone positivo ("Sì") che esegue l'azione onAction se cliccato
        .setNegativeButton("No") { dialog, _ -> dialog.dismiss() } // Bottone negativo ("No") che chiude il dialogo senza ulteriori azioni
        .show() // Mostra il dialogo all'utente
}

// Estensione per visualizzare gli eventi in un LinearLayout
fun Activity.displayEvents(
    noteLayout: LinearLayout,
    startD: Date, // Data di inizio filtro
    endD: Date, // Data di fine filtro
    cerca: String? = null, // Filtro di ricerca testo
    searchType: String? = null // Tipo di ricerca
) {
    noteLayout.removeAllViews() // Pulisce il layout

    // Recupera e filtra gli eventi
    FirestoreHandler.viewNote { events ->
        events.forEach { (date, note, noteID) ->
            // Verifica il filtro di ricerca e sovrapposizione delle date
            if (note.search(cerca, searchType) && date.isOverlapping(startD, endD)) {
                // Crea e aggiunge una CardView per ogni evento
                val cardView = this.createCardView(
                    buildString {
                        append(note.toString())
                        append("\n")
                        append(date.toString())
                    },
                    noteID,
                    onMapAction = { // Azione per visualizzare marker su mappa
                        FirestoreHandler.listInfoMarker(noteID.noteId) { markerInfos ->
                            if (markerInfos.isNotEmpty()) {
                                val markerInfosString = markerInfos.joinToString(";") { "${it.latitude},${it.longitude}" }
                                val intent = Intent(this, ViewMarkerActivity::class.java).apply {
                                    putExtra("MARKER_INFOS", markerInfosString)
                                }
                                this.startActivity(intent)
                            } else {
                                Toast.makeText(this, "Nessun marker trovato", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteAction = { // Azione per eliminare e aggiornare gli eventi
                        this.displayEvents(noteLayout, startD, endD, cerca, searchType)
                    }
                )
                noteLayout.addView(cardView) // Aggiunge la CardView al layout
            }
        }
    }
}

// Funzione per ottenere il calendario corrente
fun getCurrentCalendar(): Calendar = Calendar.getInstance()

// Funzione per mostrare un DatePickerDialog con un titolo personalizzato e callback per la data selezionata
fun Context.setData(
    titleDay: String, // Titolo del dialogo
    minDate: Long = getCurrentCalendar().timeInMillis, // Data minima selezionabile, di default la data corrente
    callback: ((Date) -> Unit)? = null // Callback opzionale che restituisce la data selezionata
) {
    // Ottiene il calendario corrente con la data attuale
    val calendar = getCurrentCalendar()
    val year = calendar.get(Calendar.YEAR) // Anno corrente
    val month = calendar.get(Calendar.MONTH) // Mese corrente
    val day = calendar.get(Calendar.DAY_OF_MONTH) // Giorno corrente

    // Inflate del layout personalizzato per il titolo del DatePickerDialog
    val customTitleView = LayoutInflater.from(this).inflate(R.layout.custom_title_layout, null)
    val titleTextView = customTitleView.findViewById<TextView>(R.id.titleTextView)
    titleTextView.text = titleDay // Imposta il testo del titolo personalizzato

    // Crea un DatePickerDialog per selezionare la data
    val datePickerDialog = DatePickerDialog(
        this,
        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            // Crea un calendario con la data selezionata
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            callback?.invoke(selectedCalendar.time) // Chiama il callback con la data selezionata
        },
        year,
        month,
        day
    )

    datePickerDialog.datePicker.minDate = minDate // Imposta la data minima selezionabile
    datePickerDialog.setCustomTitle(customTitleView) // Imposta il titolo personalizzato
    datePickerDialog.show() // Mostra il dialogo
}

// Funzione sospesa per impostare la data e restituire la data selezionata
suspend fun Context.setData(titleDay: String, minDate: Long = getCurrentCalendar().timeInMillis): Date {
    // Usa una coroutine sospesa per attendere la selezione della data da parte dell'utente
    return suspendCancellableCoroutine { continuation ->
        // Chiama la funzione setData con il titolo e la data minima
        setData(titleDay, minDate) { selectedDate ->
            // Riprende l'esecuzione della coroutine restituendo la data selezionata
            continuation.resume(selectedDate)
        }
    }
}

// Funzione sospesa per mostrare un TimePickerDialog e selezionare un'ora
suspend fun Context.setTime(
    titleHourMin: String, // Titolo da mostrare nel dialogo per selezionare l'ora
    setTime: Long = LocalTime.now().plusHours(DEFAULT_HOUR_INCREMENT.toLong()).toSecondOfDay() * 1000L // Ora iniziale di default incrementata in millisecondi
): LocalTime {
    return suspendCancellableCoroutine { continuation ->
        // Converte il tempo da millisecondi a LocalTime e imposta i minuti a 0
        val initialTime = LocalTime.ofSecondOfDay(setTime / 1000).withMinute(0)

        // Crea un TimePickerDialog per la selezione dell'ora
        val timePickerDialog = TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Riprende la coroutine con l'ora selezionata dall'utente
                val selectedTime = LocalTime.of(selectedHour, selectedMinute)
                continuation.resume(selectedTime)
            },
            initialTime.hour, // Ora iniziale da mostrare nel dialogo
            DEFAULT_MINUTES, // Minuti iniziali impostati a 0
            true // Usa il formato 24 ore
        )

        // Imposta il titolo personalizzato del dialogo
        val customTitleView = LayoutInflater.from(this).inflate(R.layout.custom_title_layout, null)
        val titleTextView = customTitleView.findViewById<TextView>(R.id.titleTextView)
        titleTextView.text = titleHourMin // Assegna il testo del titolo personalizzato

        timePickerDialog.setCustomTitle(customTitleView) // Aggiunge il titolo personalizzato al dialogo
        timePickerDialog.show() // Mostra il TimePickerDialog all'utente
    }
}

// Funzione sospesa per mostrare un dialogo di selezione numero e restituire il numero scelto
suspend fun Context.setNumber(titoloPicker: String): Int {
    // Crea un AlertDialog.Builder utilizzando il contesto attuale
    val alertDialogBuilder = AlertDialog.Builder(this)
    val inflater = LayoutInflater.from(this)
    // Inflate del layout personalizzato per il dialogo contenente il NumberPicker
    val dialogView = inflater.inflate(R.layout.dialog_number_picker, null)

    // Imposta il titolo del dialogo con il testo fornito
    val titleTextView = dialogView.findViewById<TextView>(R.id.titleTextView)
    titleTextView.text = titoloPicker

    // Configura il NumberPicker con valori minimi, massimi e di default
    val numberPicker = dialogView.findViewById<NumberPicker>(R.id.numberPicker)
    numberPicker.minValue = NUMBER_PICKER_MIN_VALUE // Valore minimo del picker
    numberPicker.maxValue = NUMBER_PICKER_MAX_VALUE // Valore massimo del picker
    numberPicker.value = NUMBER_PICKER_DEFAULT_VALUE // Valore di default del picker

    return suspendCancellableCoroutine { continuation ->
        // Imposta il pulsante di conferma del dialogo
        alertDialogBuilder.setPositiveButton("Conferma") { dialog, _ ->
            val selectedNumber = numberPicker.value // Ottiene il numero selezionato
            dialog.dismiss() // Chiude il dialogo
            continuation.resume(selectedNumber) // Riprende la coroutine restituendo il numero selezionato
        }

        alertDialogBuilder.setView(dialogView) // Imposta il layout del dialogo
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show() // Mostra il dialogo all'utente

        // Chiude il dialogo se la coroutine viene cancellata
        continuation.invokeOnCancellation {
            alertDialog.dismiss()
        }
    }
}
