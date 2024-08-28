package com.agendapersonale.src.Class

import com.agenda.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Classe singleton che gestisce la manipolazione delle date.
 */
class DateCalendar private constructor() {

    // Data interna che viene manipolata dalla classe
    private var calendar: Date = Date()

    companion object {
        @Volatile
        private var instance: DateCalendar? = null
        private var local = Locale.getDefault()

        // Formatter per varie rappresentazioni di data
        var formatDate = SimpleDateFormat("dd MMMM yyyy", local) // Formato data
        var formatDateHour = SimpleDateFormat("dd MMMM yyyy HH:mm", local) // Formato data e ora
        var formatMonth = SimpleDateFormat("MMMM yyyy", local) // Formato mese e anno
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm") // Formato ora

        /**
         * Ottiene l'istanza singleton di DateCalendar.
         */
        fun getInstance(): DateCalendar {
            return instance ?: synchronized(this) {
                instance ?: DateCalendar().also { instance = it }
            }
        }

        // Opzioni di visualizzazione del calendario
        val options = arrayOf("Giorno", "Settimana", "Mese")
        val images = arrayOf(
            R.drawable.ic_day_calendar, // Icona giorno
            R.drawable.ic_week_calendar, // Icona settimana
            R.drawable.ic_month_calendar // Icona mese
        )

        // Date minima e massima per il calendario
        val minDate: Date = Calendar.getInstance().apply {
            set(Calendar.YEAR, 1900)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }.time

        val maxDate: Date = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2100)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
        }.time

        /**
         * Ottiene il tipo di intervallo di data in base all'opzione.
         */
        fun getType(option: String): Int {
            return when (option) {
                "Giorno" -> Calendar.DAY_OF_MONTH
                "Settimana" -> Calendar.WEEK_OF_YEAR
                "Mese" -> Calendar.MONTH
                else -> throw IllegalArgumentException("Invalid option: $option")
            }
        }

        /**
         * Converte una data e un'ora locale in un timestamp Firebase.
         */
        fun convertDateAndTimeToTimestamp(date: Date, localTime: LocalTime): Timestamp {
            val zoneId = getZoneId()
            val localDate = date.toInstant().atZone(zoneId).toLocalDate() // Converte la data in locale
            val instant = localDate.atTime(localTime).atZone(zoneId).toInstant() // Converte in istante
            return Timestamp(Date.from(instant))
        }

        /**
         * Ottiene il fuso orario di default. Usa "Europe/Rome" come fallback.
         */
        fun getZoneId(): ZoneId {
            return try {
                ZoneId.systemDefault() // Recupera il fuso orario di sistema
            } catch (e: Exception) {
                ZoneId.of("Europe/Rome") // Usa Roma come fallback
            }
        }
    }

    /**
     * Resetta il calendario alla data odierna.
     */
    fun resetCalendarToToday() {
        calendar = Date()
    }

    /**
     * Imposta il calendario su una data specifica.
     */
    fun setCalendar(date: Date) {
        calendar = date
    }

    /**
     * Modifica la data del calendario aggiungendo o sottraendo una quantità di unità specifiche.
     */
    fun adjustDate(unit: Int, amount: Int) {
        calendar = Calendar.getInstance().apply {
            time = calendar
            add(unit, amount)
        }.time
    }

    /**
     * Restituisce la data formattata in base all'opzione.
     */
    fun getFormattedDate(option: String): String {
        return when (option) {
            "Giorno" -> formatDate.format(calendar)
            "Settimana" -> {
                val (startOfWeek, endOfWeek) = getDateRange("Settimana")
                "${formatDate.format(startOfWeek)} - ${formatDate.format(endOfWeek)}"
            }
            "Mese" -> formatMonth.format(calendar)
            else -> ""
        }
    }

    /**
     * Restituisce l'intervallo di date in base all'opzione.
     */
    fun getDateRange(option: String): Pair<Date, Date> {
        return when (option) {
            "Giorno" -> getDateRangeFor(Calendar.DAY_OF_MONTH)
            "Settimana" -> getDateRangeFor(Calendar.WEEK_OF_YEAR)
            "Mese" -> getDateRangeFor(Calendar.MONTH)
            else -> throw IllegalArgumentException("Invalid option: $option")
        }
    }

    /**
     * Manipola una data applicando una specifica azione sul calendario.
     */
    private fun manipulateDate(baseDate: Date = calendar, action: Calendar.() -> Unit): Date {
        return Calendar.getInstance().apply {
            time = baseDate
            action() // Esegue l'azione specificata
        }.time
    }

    /**
     * Restituisce l'intervallo di date per un'unità specifica (giorno, settimana, mese).
     */
    private fun getDateRangeFor(unit: Int): Pair<Date, Date> {
        val startDate = manipulateDate {
            when (unit) {
                Calendar.WEEK_OF_YEAR -> set(Calendar.DAY_OF_WEEK, firstDayOfWeek) // Imposta il primo giorno della settimana
                Calendar.MONTH -> set(Calendar.DAY_OF_MONTH, 1) // Imposta il primo giorno del mese
            }
            setToStartOfDay() // Imposta l'inizio della giornata
        }

        val endDate = manipulateDate {
            when (unit) {
                Calendar.MONTH -> {
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.DAY_OF_MONTH, -1) // Imposta l'ultimo giorno del mese
                }
                Calendar.WEEK_OF_YEAR -> {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    add(Calendar.DAY_OF_WEEK, 6) // Imposta l'ultimo giorno della settimana
                }
            }
            setToEndOfDay() // Imposta la fine della giornata
        }

        return startDate to endDate // Ritorna il range di date
    }

    /**
     * Imposta il calendario all'inizio del giorno.
     */
    private fun Calendar.setToStartOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /**
     * Imposta il calendario alla fine del giorno.
     */
    private fun Calendar.setToEndOfDay() {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
}