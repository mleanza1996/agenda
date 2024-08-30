package com.agendapersonale.src.Class

import com.google.firebase.Timestamp
import java.time.LocalTime
import java.util.Date

// Classe dati per rappresentare la data di inizio, fine e avviso di una Nota
data class NoteData(
    val startDate: Timestamp,
    val endDate: Timestamp,
    val preavviso: Int // preavviso in minuti
) {

    // Costruttore primario
    constructor(
        startDate: Date,
        startTime: LocalTime,
        endDate: Date,
        endTime: LocalTime,
        preavviso: Int
    ) : this(
        startDate = DateCalendar.convertDateAndTimeToTimestamp(startDate, startTime),
        endDate = DateCalendar.convertDateAndTimeToTimestamp(endDate, endTime),
        preavviso = preavviso
    )

    override fun toString(): String {
        val startDateFormatted = DateCalendar.formatDateHour.format(startDate.toDate()) // Converti Timestamp a Date e poi a String
        val endDateFormatted = DateCalendar.formatDateHour.format(endDate.toDate())    // Converti Timestamp a Date e poi a String

        return "Data inizio: $startDateFormatted\nData fine: $endDateFormatted\nPreavviso: $preavviso minuti"
    }

    // Funzione per verificare se l'evento è già passato
    fun isPast(): Boolean {
        val now = Timestamp.now()
        return now > endDate
    }

    fun inProgress(): Boolean {
        val now = Timestamp.now()
        return startDate <= now && now < endDate
    }

    // Funzione per verificare se siamo nel periodo di preavviso
    fun inPreavviso(): Boolean {
        val now = Timestamp.now()
        val preavvisoMillis = preavviso * 60 * 1000 // Converti i minuti di preavviso in millisecondi
        val preavvisoTimestamp = Timestamp(startDate.seconds - preavvisoMillis / 1000, startDate.nanoseconds)
        return preavvisoTimestamp <= now && now <= startDate
    }

    // Funzione per verificare la sovrapposizione tra due intervalli di tempo
    fun isOverlapping(
        startDate1: Date,
        endDate1: Date,
    ): Boolean {
        val thisStartDate = startDate.toDate()
        val thisEndDate = endDate.toDate()

        return !(thisStartDate.after(endDate1) || thisEndDate.before(startDate1))
    }
}
