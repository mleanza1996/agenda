package com.agenda.Utilities

import com.agendapersonale.src.Class.NoteData
import org.apache.commons.math3.stat.regression.SimpleRegression
import java.util.*

/**
 * DatePredictor è un oggetto singleton che fornisce funzionalità per predire la data della prossima nota e la durata
 * del prossimo evento basandosi su dati storici utilizzando la regressione lineare.
 */
object DatePredictor {

    // Costanti
    private const val MIN_DATES_FOR_DATE_PREDICTION = 10
    private const val MIN_DATES_FOR_DURATION_PREDICTION = 1
    private const val DEFAULT_NOTE_DURATION = 86400 * 1000L

    // Lista mutable per memorizzare le note.
    private val dates: MutableList<NoteData> = mutableListOf()

    /**
     * Aggiunge una nuova data alla lista.
     *
     * @param noteData L'oggetto NoteData che contiene la data da aggiungere.
     */
    @JvmStatic
    fun addDate(noteData: NoteData) {
        dates.add(noteData)
    }

    /**
     * Predice la data della prossima nota.
     *
     * @return Il timestamp della data predetta in millisecondi.
     */
    @JvmStatic
    fun predictNextNoteDate(): Long {
        return if (dates.size > MIN_DATES_FOR_DATE_PREDICTION) {
            // Usa la regressione lineare se ci sono più di 10 date
            predictUsingRegression()
        } else {
            // Restituisce l'orario corrente se ci sono meno di 10 date
            Calendar.getInstance().timeInMillis
        }
    }

    /**
     * Predice la durata del prossimo evento.
     *
     * @return La durata del prossimo evento in millisecondi.
     */
    @JvmStatic
    fun predictNextNoteDuration(): Long {
        return if (dates.size > MIN_DATES_FOR_DURATION_PREDICTION) {
            // Usa la regressione lineare se ci sono più di 1 data
            predictEventDurationUsingRegression()
        } else {
            // Restituisce una durata di default se c'è solo una data o meno
            DEFAULT_NOTE_DURATION
        }
    }

    /**
     * Calcola la previsione della durata del prossimo evento usando la regressione lineare.
     *
     * @return La durata predetta del prossimo evento in millisecondi.
     */
    private fun predictEventDurationUsingRegression(): Long {
        // Calcola le durate degli eventi basandosi sulle date di inizio e fine
        val durations = dates.map {
            it.endDate.seconds - it.startDate.seconds
        }

        // Genera gli indici per la regressione
        val indices = durations.indices.map { it.toDouble() }

        // Verifica se ci sono abbastanza dati per una previsione significativa
        if (durations.size < 2) {
            return DEFAULT_NOTE_DURATION
        }

        // Configura e utilizza SimpleRegression
        val regression = SimpleRegression()
        indices.forEachIndexed { index, i ->
            regression.addData(i, durations[index].toDouble())
        }

        // Calcola la durata prevista per il prossimo evento
        val nextIndex = indices.size.toDouble()
        val predictedDuration = regression.predict(nextIndex).toLong()

        // Assicura che la durata prevista sia positiva
        return maxOf(predictedDuration, 0)
    }

    /**
     * Calcola la previsione della prossima data usando la regressione lineare.
     *
     * @return Il timestamp della data predetta in millisecondi.
     */
    private fun predictUsingRegression(): Long {
        val dateTimestamps = dates.map { it.startDate.seconds }
        // Calcola gli intervalli tra le date consecutive
        val intervals = dateTimestamps.zipWithNext { a, b -> b - a }

        // Verifica che ci siano abbastanza dati per una previsione significativa
        if (intervals.size < 2) {
            return Calendar.getInstance().timeInMillis
        }

        val regression = SimpleRegression()
        // Aggiunge i dati alla regressione
        intervals.forEachIndexed { index, interval ->
            regression.addData(index.toDouble(), interval.toDouble())
        }

        // Predice l'intervallo successivo
        val nextInterval = regression.predict(intervals.size.toDouble()).toLong()
        // Restituisce la data predetta convertita in millisecondi
        return (dateTimestamps.last() * 1000 + nextInterval * 1000)
    }

}